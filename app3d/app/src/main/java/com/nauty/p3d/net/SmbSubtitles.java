package com.nauty.p3d.net;

import android.content.Context;
import android.net.Uri;

import com.hierynomus.msdtyp.AccessMask;
import com.hierynomus.mssmb2.SMB2CreateDisposition;
import com.hierynomus.mssmb2.SMB2ShareAccess;
import com.hierynomus.smbj.SMBClient;
import com.hierynomus.smbj.auth.AuthenticationContext;
import com.hierynomus.smbj.connection.Connection;
import com.hierynomus.smbj.session.Session;
import com.hierynomus.smbj.share.DiskShare;
import com.hierynomus.smbj.share.File;

import com.nauty.p3d.subtitle.Subtitles;

import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;

/**
 * SMB 공유 위의 영상 옆에 있는 자막을 찾아 로컬로 받아온다.
 *
 * {@link Subtitles#findSibling} 과 {@code PlayerActivity.pickSubtitle()} 은
 * java.io.File 기반이라 smb:// 소스에는 애초에 안 통한다 — SMB로 재생할 때는
 * 자막이 자동으로도 수동으로도 하나도 안 잡히던 것이 그래서였다(실기에서 확인:
 * 같은 폴더에 같은 이름의 .smi 가 있는데도 못 찾음). 여기서 SMB 쪽 목록/다운로드를
 * 따로 맡는다 — 로컬 파일용 파서(Subtitles.load)는 그대로 재사용한다.
 */
public final class SmbSubtitles {

    private SmbSubtitles() {}

    /** 수동 선택 목록에 보여줄 후보 하나. */
    public static final class Candidate {
        public final Uri uri;
        public final String name;
        Candidate(Uri uri, String name) { this.uri = uri; this.name = name; }
    }

    /**
     * 영상이 있는 SMB 폴더를 한 번만 훑어서 그 안의 항목을 받아온다. findSibling()
     * (자동 인식, 이름이 겹치는 것 하나만)과 list()(수동 선택, 폴더 안 자막 전부)가
     * 이걸 같이 쓴다. 네트워크 호출이라 반드시 배경 스레드에서 불러야 한다.
     */
    private static final class Dir {
        final String dir;      // smbj 식 "\\" 구분자, 공유 루트면 빈 문자열
        final String stem;     // 영상 파일명에서 확장자를 뗀 것, 소문자
        final SmbUri parsed;
        final List<SmbBrowser.Entry> entries;

        Dir(SmbUri parsed, String dir, String stem, List<SmbBrowser.Entry> entries) {
            this.parsed = parsed; this.dir = dir; this.stem = stem; this.entries = entries;
        }
    }

    private static Dir list(Context ctx, Uri videoUri) throws Exception {
        SmbUri parsed = SmbUri.parse(videoUri);
        int slash = parsed.path.lastIndexOf('\\');
        String dir = slash >= 0 ? parsed.path.substring(0, slash) : "";
        String videoName = slash >= 0 ? parsed.path.substring(slash + 1) : parsed.path;

        String base = videoName;
        int dot = base.lastIndexOf('.');
        if (dot > 0) base = base.substring(0, dot);
        String stem = base.toLowerCase(Locale.US);

        SmbCredentials.Entry cred = parsed.hasCredentials()
                ? new SmbCredentials.Entry(parsed.user, parsed.pass, null)
                : SmbCredentials.find(ctx, parsed.host);
        String user = cred == null ? parsed.user : cred.user;
        String pass = cred == null ? parsed.pass : cred.pass;

        List<SmbBrowser.Entry> entries =
                SmbBrowser.list(parsed.host, parsed.port, parsed.share, user, pass, dir);
        return new Dir(parsed, dir, stem, entries);
    }

    /**
     * 영상과 같은 폴더에서 이름이 겹치는 자막을 찾는다(자동 인식용). 없으면 null.
     * 로컬 {@link Subtitles#findSibling} 과 같은 규칙 — 이름이 영상 파일명으로
     * 시작해야 하고, 후보가 여럿이면 한국어 표시가 있는 쪽을 우선한다.
     * 네트워크 호출이라 반드시 배경 스레드에서 불러야 한다.
     */
    public static Uri findSibling(Context ctx, Uri videoUri) {
        try {
            Dir d = list(ctx, videoUri);
            SmbBrowser.Entry best = null;
            for (SmbBrowser.Entry e : d.entries) {
                if (e.directory) continue;
                String n = e.name.toLowerCase(Locale.US);
                if (!Subtitles.isSubtitle(n)) continue;
                if (!n.startsWith(d.stem)) continue;
                if (n.contains(".ko") || n.contains(".kor") || n.contains("korean")) { best = e; break; }
                if (best == null) best = e;
            }
            if (best == null) return null;
            return uriOf(d, best.name);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 영상과 같은 SMB 폴더에 있는 자막 파일을 전부 보여준다(수동 선택용) — 로컬
     * {@code PlayerActivity.pickSubtitle()} 처럼 이름이 영상과 겹치는지는 안 따진다.
     * 실패하면 빈 목록. 네트워크 호출이라 반드시 배경 스레드에서 불러야 한다.
     */
    public static List<Candidate> listSubtitles(Context ctx, Uri videoUri) {
        List<Candidate> out = new ArrayList<>();
        try {
            Dir d = list(ctx, videoUri);
            for (SmbBrowser.Entry e : d.entries) {
                if (e.directory) continue;
                String n = e.name.toLowerCase(Locale.US);
                if (!Subtitles.isSubtitle(n)) continue;
                out.add(new Candidate(uriOf(d, e.name), e.name));
            }
        } catch (Exception ignored) { }
        return out;
    }

    private static Uri uriOf(Dir d, String fileName) {
        String subPath = d.dir.isEmpty() ? fileName : d.dir + "\\" + fileName;
        return SmbUri.build(d.parsed.host, d.parsed.port, d.parsed.share, subPath,
                d.parsed.user, d.parsed.pass);
    }

    /**
     * 고른 자막을 앱 캐시 폴더에 내려받는다. 실패하면 null — 원본 확장자(.srt/.smi)를
     * 그대로 유지해야 {@link Subtitles#load} 가 형식을 옳게 판별한다.
     * 네트워크 호출이라 반드시 배경 스레드에서 불러야 한다.
     */
    public static java.io.File download(Context ctx, Uri subtitleUri) {
        SMBClient client = null;
        try {
            SmbUri parsed = SmbUri.parse(subtitleUri);
            SmbCredentials.Entry cred = parsed.hasCredentials()
                    ? new SmbCredentials.Entry(parsed.user, parsed.pass, null)
                    : SmbCredentials.find(ctx, parsed.host);

            client = new SMBClient();
            Connection connection = client.connect(parsed.host, parsed.port);
            AuthenticationContext auth = (cred == null || cred.user == null || cred.user.isEmpty())
                    ? AuthenticationContext.anonymous()
                    : new AuthenticationContext(cred.user,
                            cred.pass == null ? new char[0] : cred.pass.toCharArray(), cred.domain);
            Session session = connection.authenticate(auth);
            DiskShare share = (DiskShare) session.connectShare(parsed.share);
            try {
                File f = share.openFile(parsed.path,
                        EnumSet.of(AccessMask.GENERIC_READ), null, SMB2ShareAccess.ALL,
                        SMB2CreateDisposition.FILE_OPEN, null);
                try {
                    String name = parsed.path.substring(parsed.path.lastIndexOf('\\') + 1);
                    int dot = name.lastIndexOf('.');
                    String ext = dot > 0 ? name.substring(dot) : ".srt";
                    long len = f.getFileInformation().getStandardInformation().getEndOfFile();

                    java.io.File out = java.io.File.createTempFile("smb_sub_", ext, ctx.getCacheDir());
                    byte[] buf = new byte[64 * 1024];
                    long pos = 0;
                    try (FileOutputStream fos = new FileOutputStream(out)) {
                        while (pos < len) {
                            int n = f.read(buf, pos, 0, buf.length);
                            if (n <= 0) break;
                            fos.write(buf, 0, n);
                            pos += n;
                        }
                    }
                    return out;
                } finally {
                    f.close();
                }
            } finally {
                share.close();
            }
        } catch (Exception e) {
            return null;
        } finally {
            if (client != null) try { client.close(); } catch (Exception ignored) { }
        }
    }
}
