package com.nauty.p3d;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.provider.DocumentsContract;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * 이어보기: 파일마다 본 위치, 그리고 마지막으로 본 영상·사진.
 *
 * 파일은 이름이 아니라 자리(경로, USB 는 문서 ID)로 구별한다. 예전에는 이름으로만 기억해서
 * 다른 폴더의 같은 이름 영상끼리 위치가 섞였다. 예전 기록은 새 기록이 생기기 전까지만 쓴다.
 *
 * "마지막으로 본 것"은 기기의 파일과 USB 것을 따로 하나씩 갖는다 — USB 를 뽑아 둔 동안에도
 * 기기에서 보던 것은 이어 볼 수 있게. 유튜브·네트워크 재생은 기록하지 않는다.
 */
final class Recents {

    private static final String PREFS = "p3d";
    private static final String KEY_ON = "recent_on";
    private static final String KEY_POS = "pos2:";
    private static final String KEY_DUR = "dur:";
    private static final String KEY_DONE = "done:";
    /** 예전 방식: 파일 이름만으로 기억한 위치. */
    private static final String KEY_OLD_POS = "pos:";

    static final class Entry {
        String key;
        Uri uri;
        String title;
        String folder;      // 기기 파일의 폴더
        String usbTree;     // USB 것이면 그 드라이브, 아니면 null
        String usbDoc;      // USB 것의 폴더 문서 ID
        String note;        // 사진: "12 / 48"
        boolean photo;
        long time;

        boolean usb() { return usbTree != null; }
    }

    private Recents() {}

    private static SharedPreferences prefs(Context c) {
        return c.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    // ------------------------------------------------------------- 파일의 열쇠

    /** 기기 파일의 열쇠. */
    static String fileKey(String path) {
        return path == null ? null : "file:" + path;
    }

    /** USB 문서의 열쇠 (문서 ID 에 볼륨이 들어 있다). */
    static String usbKey(String docId) {
        return docId == null ? null : "usb:" + docId;
    }

    /** 플레이어가 연 것의 열쇠. 기기 파일도 USB 도 아니면 null (네트워크 등). */
    static String keyOf(Context c, Uri uri, String usbTree) {
        if (uri == null) return null;
        if (usbTree != null) {
            try {
                return usbKey(DocumentsContract.getDocumentId(uri));
            } catch (Exception e) {
                return null;
            }
        }
        return "content".equals(uri.getScheme()) && "media".equals(uri.getAuthority())
                ? fileKey(MediaLibrary.pathOf(c, uri)) : null;
    }

    // ------------------------------------------------------------- 본 위치

    static long position(Context c, String key, String name) {
        SharedPreferences p = prefs(c);
        if (key != null) {
            if (p.contains(KEY_POS + key)) return p.getLong(KEY_POS + key, 0);
            if (p.getBoolean(KEY_DONE + key, false)) return 0;
        }
        return name == null || name.isEmpty() ? 0 : p.getLong(KEY_OLD_POS + name, 0);
    }

    /** 본 위치를 기억한다. started: 처음 몇 초를 지났다. finished: 거의 끝까지 봤다. */
    static void savePosition(Context c, String key, String name, long pos, long dur,
                             boolean started, boolean finished) {
        SharedPreferences.Editor e = prefs(c).edit();
        boolean keep = started && !finished;
        if (key == null) {                         // 네트워크 재생: 예전처럼 이름으로
            if (name == null || name.isEmpty()) return;
            if (keep) e.putLong(KEY_OLD_POS + name, pos); else e.remove(KEY_OLD_POS + name);
            e.apply();
            return;
        }
        e.remove(KEY_OLD_POS + name);
        if (keep) {
            e.putLong(KEY_POS + key, pos).putLong(KEY_DUR + key, dur).remove(KEY_DONE + key);
        } else {
            e.remove(KEY_POS + key);               // 초반이거나 다 봤으면 이어 볼 것이 없다
            if (finished) e.putBoolean(KEY_DONE + key, true);
        }
        e.apply();
    }

    /** 목록에 붙일 표시: "▸ 42:10 / 1:58:00", 다 봤으면 "✓", 안 봤으면 빈 글. */
    static String progress(Context c, String key, String name) {
        SharedPreferences p = prefs(c);
        long pos = position(c, key, name);
        if (pos > 0) {
            long dur = key == null ? 0 : p.getLong(KEY_DUR + key, 0);
            return "▸ " + time(pos) + (dur > 0 ? " / " + time(dur) : "");
        }
        return key != null && p.getBoolean(KEY_DONE + key, false) ? "✓" : "";
    }

    static String time(long ms) {
        long s = Math.max(0, ms) / 1000;
        long h = s / 3600;
        long m = (s % 3600) / 60;
        s %= 60;
        return h > 0 ? String.format(Locale.US, "%d:%02d:%02d", h, m, s)
                     : String.format(Locale.US, "%02d:%02d", m, s);
    }

    // ------------------------------------------------------------- 마지막으로 본 것

    static boolean isOn(Context c) {
        return prefs(c).getBoolean(KEY_ON, true);
    }

    /** 끄면 지금까지의 기록(마지막으로 본 것)도 지운다. 파일마다 본 위치는 남는다. */
    static void setOn(Context c, boolean on) {
        SharedPreferences.Editor e = prefs(c).edit().putBoolean(KEY_ON, on);
        if (!on) {
            for (boolean photo : new boolean[]{false, true}) {
                e.remove(slot(photo, false)).remove(slot(photo, true));
            }
        }
        e.apply();
    }

    private static String slot(boolean photo, boolean usb) {
        return "recent:" + (photo ? "photo" : "video") + (usb ? ":usb" : ":device");
    }

    static void remember(Context c, Entry en) {
        if (en.key == null || en.uri == null || !isOn(c)) return;
        try {
            JSONObject o = new JSONObject();
            o.put("key", en.key).put("uri", en.uri.toString()).put("title", en.title)
                    .put("time", System.currentTimeMillis());
            if (en.folder != null) o.put("folder", en.folder);
            if (en.usbTree != null) o.put("tree", en.usbTree).put("doc", en.usbDoc);
            if (en.note != null) o.put("note", en.note);
            prefs(c).edit().putString(slot(en.photo, en.usb()), o.toString()).apply();
        } catch (JSONException ignored) { }
    }

    /** 마지막으로 본 것들 (기기, USB), 최근 것이 앞. */
    static List<Entry> last(Context c, boolean photo) {
        List<Entry> out = new ArrayList<>();
        if (!isOn(c)) return out;
        for (boolean usb : new boolean[]{false, true}) {
            String s = prefs(c).getString(slot(photo, usb), null);
            if (s == null) continue;
            try {
                JSONObject o = new JSONObject(s);
                Entry e = new Entry();
                e.key = o.getString("key");
                e.uri = Uri.parse(o.getString("uri"));
                e.title = o.getString("title");
                e.folder = o.isNull("folder") ? null : o.getString("folder");
                e.usbTree = o.isNull("tree") ? null : o.getString("tree");
                e.usbDoc = o.isNull("doc") ? null : o.getString("doc");
                e.note = o.isNull("note") ? null : o.getString("note");
                e.time = o.getLong("time");
                e.photo = photo;
                out.add(e);
            } catch (JSONException ignored) { }
        }
        Collections.sort(out, new Comparator<Entry>() {
            @Override public int compare(Entry a, Entry b) { return Long.compare(b.time, a.time); }
        });
        return out;
    }

    /** 이 파일을 "마지막으로 본 것"에서 뺀다 (다 봤다, 지웠다, 사용자가 뺐다). */
    static void forget(Context c, String key) {
        if (key == null) return;
        for (boolean photo : new boolean[]{false, true}) {
            for (Entry e : last(c, photo)) {
                if (key.equals(e.key)) prefs(c).edit().remove(slot(photo, e.usb())).apply();
            }
        }
    }

    /**
     * 기기 파일이 아직 있는가. 색인이 다시 매겨져 주소가 바뀌었으면 새 주소로 고친다.
     * UI 스레드에서 부르지 말 것.
     */
    static boolean locate(Context c, Entry e) {
        if (e.usb()) return true;
        if (e.key.equals(fileKey(MediaLibrary.pathOf(c, e.uri)))) return true;
        for (MediaLibrary.Item it : MediaLibrary.list(c,
                e.photo ? MediaLibrary.Kind.IMAGE : MediaLibrary.Kind.VIDEO, e.folder)) {
            if (e.key.equals(fileKey(it.path))) {
                e.uri = it.uri;
                return true;
            }
        }
        return false;
    }

    static Intent playerIntent(Context c, Entry e) {
        Intent i = new Intent(c, PlayerActivity.class);
        i.setData(e.uri);
        i.putExtra(PlayerActivity.EXTRA_TITLE, e.title);
        if (e.photo) i.putExtra(PlayerActivity.EXTRA_PHOTO, true);
        if (e.usb()) {
            i.putExtra(PlayerActivity.EXTRA_USB_TREE, e.usbTree);
            i.putExtra(PlayerActivity.EXTRA_USB_DOC, e.usbDoc);
        } else if (e.photo) {
            i.putExtra(PlayerActivity.EXTRA_FOLDER, e.folder);
        }
        return i;
    }
}
