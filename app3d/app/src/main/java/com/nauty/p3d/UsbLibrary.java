package com.nauty.p3d;

import android.content.Context;
import android.content.Intent;
import android.content.UriPermission;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.storage.StorageManager;
import android.os.storage.StorageVolume;
import android.provider.DocumentsContract;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * USB 메모리 같은 외장 장치를 Android 의 폴더 선택(저장소 액세스 프레임워크)으로 읽는다.
 *
 * 왜 따로인가. 기기에 꽂은 USB 드라이브는 "공개 볼륨"으로 붙지만 앱에 보이는 표시
 * (VISIBLE)가 없다 — /storage 에도 안 나오고 MediaStore 도 색인하지 않는다 (S25 Ultra 에서
 * 확인: sm list-volumes 에 public 으로 mounted, mountFlags=0). 그래서 지금까지의 목록
 * ({@link MediaLibrary})에는 아무것도 안 잡혔다. 이 장치는 사용자가 폴더 선택 창에서
 * 한 번 고르게 하고, 그 허락을 영구 보관(takePersistableUriPermission)해 다음부터 바로 연다.
 *
 * 여는 것은 document:// 가 아니라 content:// 문서 URI 그대로다. 플레이어(ExoPlayer, 사진
 * 디코더)는 ContentResolver 로 읽으므로 따로 손댈 것이 없다.
 */
public final class UsbLibrary {

    private static final String PREFS = "p3d";
    private static final String KEY_TREE = "usb_tree";

    /** 목록 한 줄: 폴더이거나 영상·사진 파일. */
    public static final class Entry {
        public final String name;
        public final String docId;
        public final Uri uri;            // 파일: 열 문서 URI. 폴더: null
        public final boolean dir;
        public final boolean photo;

        Entry(String name, String docId, Uri uri, boolean dir, boolean photo) {
            this.name = name; this.docId = docId; this.uri = uri; this.dir = dir; this.photo = photo;
        }
    }

    private UsbLibrary() {}

    /** 전에 골라 둔 폴더(트리). 허락이 아직 살아 있을 때만. */
    public static Uri savedTree(Context ctx) {
        String s = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY_TREE, null);
        if (s == null) return null;
        Uri tree = Uri.parse(s);
        for (UriPermission p : ctx.getContentResolver().getPersistedUriPermissions()) {
            if (p.getUri().equals(tree) && p.isReadPermission()) return tree;
        }
        return null;
    }

    /** 폴더 선택 창이 돌려준 트리를 영구 보관한다. */
    public static void saveTree(Context ctx, Uri tree) {
        try {
            ctx.getContentResolver().takePersistableUriPermission(
                    tree, Intent.FLAG_GRANT_READ_URI_PERMISSION);
        } catch (SecurityException ignored) {
            // 영구 보관이 안 되는 제공자라도 이번 실행 동안은 읽힌다.
        }
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit().putString(KEY_TREE, tree.toString()).apply();
    }

    /**
     * 폴더 선택 창을 연다. 꽂혀 있는 USB(이동식, 내장 아님) 볼륨이 보이면 그 드라이브에서
     * 바로 시작하게 하고, 아니면 일반 선택 창이다.
     */
    public static Intent pickIntent(Context ctx) {
        if (Build.VERSION.SDK_INT >= 29) {
            StorageManager sm = (StorageManager) ctx.getSystemService(Context.STORAGE_SERVICE);
            if (sm != null) {
                for (StorageVolume v : sm.getStorageVolumes()) {
                    if (!v.isPrimary() && v.isRemovable()) {
                        return v.createOpenDocumentTreeIntent();
                    }
                }
            }
        }
        Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
        i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION
                | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        return i;
    }

    /** 시스템의 외장 저장소 제공자. USB 드라이브의 폴더는 여기서 온다. */
    private static final String EXTERNAL = "com.android.externalstorage.documents";

    /** 이 트리가 있는 볼륨("1234-ABCD"). 내장 저장소나 다른 제공자면 null — 해제할 것이 없다. */
    public static String volumeOf(Uri tree) {
        if (tree == null || !EXTERNAL.equals(tree.getAuthority())) return null;
        String id = rootDocId(tree);
        int colon = id.indexOf(':');
        String root = colon < 0 ? id : id.substring(0, colon);
        return root.isEmpty() || "primary".equals(root) || "home".equals(root) ? null : root;
    }

    /**
     * 지금 꽂혀(마운트돼) 있는 뺄 수 있는 드라이브의 볼륨 이름("2CB8-C259"). 없으면 null.
     * 앱의 허락(트리 권한)이 없어도 알 수 있다.
     */
    public static String mountedVolume(Context ctx) {
        StorageManager sm = (StorageManager) ctx.getSystemService(Context.STORAGE_SERVICE);
        if (sm == null) return null;
        for (StorageVolume v : sm.getStorageVolumes()) {
            if (!v.isPrimary() && v.isRemovable() && v.getUuid() != null
                    && android.os.Environment.MEDIA_MOUNTED.equals(v.getState())) {
                return v.getUuid();
            }
        }
        return null;
    }

    /** 이 볼륨이 아직 마운트돼 있는가. */
    public static boolean isMounted(Context ctx, String volume) {
        if (volume == null) return false;
        StorageManager sm = (StorageManager) ctx.getSystemService(Context.STORAGE_SERVICE);
        if (sm == null) return false;
        for (StorageVolume v : sm.getStorageVolumes()) {
            if (volume.equals(v.getUuid())) {
                return android.os.Environment.MEDIA_MOUNTED.equals(v.getState());
            }
        }
        return false;
    }

    /** 볼륨의 뿌리 트리. 읽는 데는 못 쓰고(허락이 없다), 해제 화면을 찾는 데만 쓴다. */
    public static Uri volumeTree(String volume) {
        return DocumentsContract.buildTreeDocumentUri(EXTERNAL, volume + ":");
    }

    /** 해제할 수 있는 종류(뺄 수 있는 드라이브)인가. */
    public static boolean removable(Uri tree) {
        return volumeOf(tree) != null;
    }

    /** 드라이브가 지금 붙어 있어 읽히는가. UI 스레드에서 부르지 말 것. */
    public static boolean attached(Context ctx, Uri tree) {
        Cursor c = null;
        try {
            c = ctx.getContentResolver().query(
                    DocumentsContract.buildDocumentUriUsingTree(tree, rootDocId(tree)),
                    new String[]{DocumentsContract.Document.COLUMN_DOCUMENT_ID}, null, null, null);
            return c != null && c.moveToFirst();
        } catch (Exception e) {
            return false;   // 해제됐거나 뽑혔다
        } finally {
            if (c != null) c.close();
        }
    }

    /**
     * 이 드라이브를 해제할 수 있는 화면. 앱은 볼륨을 직접 해제할 수 없다 — 권한
     * (MOUNT_UNMOUNT_FILESYSTEMS)이 시스템 앱 것이고, 공개된 길인 ejectRoot 도 S25 Ultra
     * 에서 권한 거부로 확인했다. 설정의 저장공간 화면은 삼성에서 해제 메뉴가 없는 곳(내 파일
     * 저장공간 관리)으로 넘어가므로, 내 파일의 그 USB 화면을 바로 연다 (⋮ 에 마운트 해제).
     *
     * @return 열 수 없는 폰(내 파일이 없음)이면 null
     */
    public static Intent myFilesIntent(Context ctx, Uri tree) {
        String volume = volumeOf(tree);
        if (volume == null) return null;
        Intent i = new Intent("samsung.myfiles.intent.action.LAUNCH_MY_FILES");
        i.setPackage("com.sec.android.app.myfiles");
        i.putExtra("samsung.myfiles.intent.extra.START_PATH", "/mnt/media_rw/" + volume);
        return i.resolveActivity(ctx.getPackageManager()) == null ? null : i;
    }

    /**
     * 내 파일이 없는 폰(Lume Pad, RedMagic 등)에서: 안드로이드 설정의 이 드라이브 화면.
     * 기본 설정 앱은 드라이브(문서 뿌리)마다 화면이 있고 거기에 꺼내기가 있다.
     *
     * @return 받아 줄 화면이 없으면 null
     */
    public static Intent rootSettingsIntent(Context ctx, Uri tree) {
        String volume = volumeOf(tree);
        if (volume == null) return null;
        Intent i = new Intent("android.provider.action.DOCUMENT_ROOT_SETTINGS");
        i.setDataAndType(DocumentsContract.buildRootUri(EXTERNAL, volume), "vnd.android.document/root");
        return i.resolveActivity(ctx.getPackageManager()) == null ? null : i;
    }

    public static String rootDocId(Uri tree) {
        return DocumentsContract.getTreeDocumentId(tree);
    }

    /**
     * 한 폴더 안의 하위 폴더와 영상·사진. 폴더가 먼저, 이름 순.
     * USB 는 느릴 수 있으니 UI 스레드에서 부르지 말 것.
     */
    public static List<Entry> children(Context ctx, Uri tree, String docId) {
        List<Entry> dirs = new ArrayList<>();
        List<Entry> files = new ArrayList<>();
        Uri q = DocumentsContract.buildChildDocumentsUriUsingTree(tree, docId);
        Cursor c = null;
        try {
            c = ctx.getContentResolver().query(q, new String[]{
                    DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                    DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                    DocumentsContract.Document.COLUMN_MIME_TYPE}, null, null, null);
            if (c != null) {
                while (c.moveToNext()) {
                    String id = c.getString(0);
                    String name = c.getString(1);
                    String mime = c.getString(2);
                    if (name == null || name.startsWith(".")) continue;   // 숨김·시스템 파일
                    if (DocumentsContract.Document.MIME_TYPE_DIR.equals(mime)) {
                        dirs.add(new Entry(name, id, null, true, false));
                        continue;
                    }
                    boolean video = (mime != null && mime.startsWith("video/"))
                            || MediaLibrary.isVideoName(name);
                    boolean photo = (mime != null && mime.startsWith("image/"))
                            || MediaLibrary.isPhotoName(name);
                    if (!video && !photo) continue;
                    files.add(new Entry(name, id,
                            DocumentsContract.buildDocumentUriUsingTree(tree, id), false, photo && !video));
                }
            }
        } catch (Exception ignored) {
            // 드라이브를 뽑았거나 허락이 사라졌다. 빈 목록으로 보인다.
        } finally {
            if (c != null) c.close();
        }
        Comparator<Entry> byName = new Comparator<Entry>() {
            @Override public int compare(Entry a, Entry b) { return a.name.compareToIgnoreCase(b.name); }
        };
        Collections.sort(dirs, byName);
        Collections.sort(files, byName);
        dirs.addAll(files);
        return dirs;
    }

    // ------------------------------------------------------------- 자막

    /**
     * 영상과 같은 USB 폴더에서 이름이 겹치는 자막(.srt/.smi/...)을 찾아 앱 캐시로 복사한다.
     * 로컬 {@code Subtitles.findSibling} 과 같은 규칙: 영상 파일명(확장자 뺀 것)으로 시작해야
     * 하고, 여럿이면 한국어 표시가 있는 쪽. 파서({@code Subtitles.load})는 java.io.File 을
     * 받으므로 USB 문서를 그대로 줄 수 없어 복사한다. 없으면 null. 배경 스레드에서.
     */
    public static java.io.File findSubtitle(Context ctx, Uri tree, String folderDoc, String videoName) {
        String stem = videoName == null ? "" : videoName;
        int dot = stem.lastIndexOf('.');
        if (dot > 0) stem = stem.substring(0, dot);
        stem = stem.toLowerCase(java.util.Locale.US);

        Entry best = null;
        for (Entry e : subtitleEntries(ctx, tree, folderDoc)) {
            String n = e.name.toLowerCase(java.util.Locale.US);
            if (!n.startsWith(stem)) continue;
            if (n.contains(".ko") || n.contains(".kor") || n.contains("korean") || n.contains("한글")) {
                best = e;
                break;
            }
            if (best == null) best = e;
        }
        return best == null ? null : copyToCache(ctx, best);
    }

    /** 같은 USB 폴더의 자막 전부를 캐시로 복사해 돌려준다 (수동 선택 목록용). */
    public static List<java.io.File> allSubtitles(Context ctx, Uri tree, String folderDoc) {
        List<java.io.File> out = new ArrayList<>();
        for (Entry e : subtitleEntries(ctx, tree, folderDoc)) {
            java.io.File f = copyToCache(ctx, e);
            if (f != null) out.add(f);
        }
        return out;
    }

    private static List<Entry> subtitleEntries(Context ctx, Uri tree, String folderDoc) {
        List<Entry> out = new ArrayList<>();
        Uri q = DocumentsContract.buildChildDocumentsUriUsingTree(tree, folderDoc);
        Cursor c = null;
        try {
            c = ctx.getContentResolver().query(q, new String[]{
                    DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                    DocumentsContract.Document.COLUMN_DISPLAY_NAME}, null, null, null);
            if (c != null) {
                while (c.moveToNext()) {
                    String id = c.getString(0);
                    String name = c.getString(1);
                    if (name == null || !com.nauty.p3d.subtitle.Subtitles.isSubtitle(
                            name.toLowerCase(java.util.Locale.US))) continue;
                    out.add(new Entry(name, id, DocumentsContract.buildDocumentUriUsingTree(tree, id),
                            false, false));
                }
            }
        } catch (Exception ignored) {
        } finally {
            if (c != null) c.close();
        }
        return out;
    }

    /** 원래 이름 그대로(확장자로 형식을 가리므로) 캐시 폴더에 복사한다. */
    private static java.io.File copyToCache(Context ctx, Entry e) {
        java.io.File dir = new java.io.File(ctx.getCacheDir(), "usb_subs");
        //noinspection ResultOfMethodCallIgnored
        dir.mkdirs();
        java.io.File out = new java.io.File(dir, e.name.replace('/', '_'));
        java.io.InputStream in = null;
        java.io.OutputStream os = null;
        try {
            in = ctx.getContentResolver().openInputStream(e.uri);
            if (in == null) return null;
            os = new java.io.FileOutputStream(out);
            byte[] buf = new byte[64 * 1024];
            int n;
            while ((n = in.read(buf)) > 0) os.write(buf, 0, n);
            return out;
        } catch (Exception ex) {
            return null;
        } finally {
            try { if (in != null) in.close(); } catch (Exception ignored) { }
            try { if (os != null) os.close(); } catch (Exception ignored) { }
        }
    }

    /** 사진 넘겨 보기용: 한 폴더의 사진만 {@link MediaLibrary.Item} 으로. */
    public static List<MediaLibrary.Item> photos(Context ctx, Uri tree, String docId) {
        List<MediaLibrary.Item> out = new ArrayList<>();
        for (Entry e : children(ctx, tree, docId)) {
            if (!e.dir && e.photo) out.add(new MediaLibrary.Item(e.uri, e.name, docId));
        }
        return out;
    }
}
