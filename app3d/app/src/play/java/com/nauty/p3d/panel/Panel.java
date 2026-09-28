package com.nauty.p3d.panel;

import android.app.Activity;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.util.Log;
import android.view.View;

import com.nauty.p3d.BuildConfig;
import com.nauty.p3d.gl.Stereo3DView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

import dalvik.system.DexClassLoader;

/**
 * play 배포판: Leia CNSDK 커넥터를 APK 에 넣지 않고 기기에서 빌려 쓴다.
 *
 * RedMagic 에는 우리가 쓰던 커넥터(com.leia.sdk.* 클래스 + libleiaSDK-jni.so +
 * libleiaCore-loader.so, CNSDK 0.10.21)가 시스템 앱 CalibrationK68
 * (com.leiainc.calibrationapp, /system/app/) 안에 그대로 들어 있다 — 실기에서 확인했고
 * assets/cnsdk.version 도 우리 것과 같다. 그 APK 와, 우리 쪽 Leia 연동 클래스
 * (LeiaBackend, 빌드 때 assets/leiabridge.dex 로 따로 dex 한 것)를 한 DexClassLoader 에
 * 올리면 LeiaBackend 가 참조하는 com.leia.* 가 그 로더에서 해석된다. 부모는 앱 로더라
 * PanelBackend·Stereo3DView 같은 우리 클래스는 그대로 보인다. LeiaMediaSDK 가
 * com.leiainc.media.service 를 올리는 것과 같은 방식이다.
 *
 * 왜 이렇게까지 하나: Play 는 (1) 우리에게 재배포권이 없는 Leia 바이너리를 올리는
 * 문제와 (2) 그 .so 가 4KB 페이지 정렬이라 16KB 요구를 못 넘는 문제가 있다. APK 에
 * 그 바이트가 하나도 없으면 둘 다 사라진다. 대신 ZTE 가 시스템 업데이트로 그 앱을
 * 바꾸면 이 경로가 깨질 수 있다 — 그때는 로그의 "CNSDK 런타임 로드 실패" 를 볼 것.
 */
public final class Panel {

    private static final String TAG = "P3DLeia";
    /** 커넥터를 빌려 오는 시스템 앱. 매니페스트 <queries> 에도 선언돼 있어야 조회된다. */
    private static final String CONNECTOR_PACKAGE = "com.leiainc.calibrationapp";
    private static final String BRIDGE_ASSET = "leiabridge.dex";
    private static final String BRIDGE_CLASS = "com.nauty.p3d.leia.LeiaBackend";

    /**
     * 로더는 프로세스에 하나만 둔다.
     *
     * 재생 화면을 열 때마다 새 DexClassLoader 를 만들면 두 번째부터 깨진다 — Android 는
     * 네이티브 라이브러리 하나를 한 프로세스에서 클래스 로더 하나에만 열어 주므로, 새
     * 로더의 LeiaSDK 가 libleiaSDK-jni.so 를 다시 열려다 거부당한다. 실기에서 "첫 영상은
     * 되는데 끄고 다른 영상을 틀면 안 된다" 로 나타났다. 백엔드 객체는 화면마다 새로
     * 만들되(상태를 들고 있다) 클래스는 같은 로더에서 꺼낸다.
     */
    private static ClassLoader loader;

    private static synchronized ClassLoader loader(Context ctx) throws Exception {
        if (loader == null) {
            Context app = ctx.getApplicationContext();
            ApplicationInfo info = app.getPackageManager().getApplicationInfo(CONNECTOR_PACKAGE, 0);
            File bridge = extractBridge(app);
            String dexPath = info.sourceDir + File.pathSeparator + bridge.getAbsolutePath();
            loader = new DexClassLoader(dexPath,
                    app.getCodeCacheDir().getAbsolutePath(), info.nativeLibraryDir, app.getClassLoader());
            Log.i(TAG, "CNSDK 런타임 로드: " + info.sourceDir + " / " + info.nativeLibraryDir);
        }
        return loader;
    }

    public static PanelBackend create(Context ctx) {
        try {
            Class<?> c = loader(ctx).loadClass(BRIDGE_CLASS);
            return (PanelBackend) c.getDeclaredConstructor().newInstance();
        } catch (Throwable t) {
            Log.e(TAG, "CNSDK 런타임 로드 실패 — 위빙 없이 2D 로 그린다", t);
            return new NoPanel();
        }
    }

    /**
     * assets 의 dex 를 앱 전용 저장소로 꺼낸다. 앱 버전마다 파일명을 달리해 갱신을
     * 놓치지 않고, Android 14+ 가 요구하는 대로 읽기 전용으로 만든다.
     */
    private static File extractBridge(Context ctx) throws Exception {
        File dir = new File(ctx.getCodeCacheDir(), "leiabridge");
        if (!dir.isDirectory()) dir.mkdirs();
        File dex = new File(dir, "leiabridge-" + BuildConfig.VERSION_CODE + ".dex");
        if (dex.isFile() && dex.length() > 0) return dex;
        File tmp = new File(dir, dex.getName() + ".tmp");
        try (InputStream in = ctx.getAssets().open(BRIDGE_ASSET);
             FileOutputStream out = new FileOutputStream(tmp)) {
            byte[] buf = new byte[64 * 1024];
            int n;
            while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
        }
        if (!tmp.renameTo(dex)) throw new IllegalStateException("dex 파일을 놓을 수 없다");
        dex.setReadOnly();
        return dex;
    }

    /** 커넥터를 못 올렸을 때의 대체 — Stereo3DView 가 화면에 직접 나간다(위빙 없음). */
    private static final class NoPanel implements PanelBackend {
        @Override public View outputView(Activity a) { return null; }
        @Override public void whenReady(Runnable r) { r.run(); }
        @Override public void attach(Activity a, Stereo3DView gl) { }
        @Override public void setThreeD(boolean on) { }
        @Override public void setMonoSource(boolean mono) { }
        @Override public void setConversionStrength(float v) { }
        @Override public void onResume() { }
        @Override public void onPause() { }
        @Override public void onDestroy() { }
    }

    private Panel() {}
}
