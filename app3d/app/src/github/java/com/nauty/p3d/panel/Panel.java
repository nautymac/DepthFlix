package com.nauty.p3d.panel;

import android.content.Context;

import com.nauty.p3d.leia.LeiaBackend;

/**
 * github 배포판: Leia 백엔드({@link LeiaBackend}, src/leiabridge/)가 앱에 그대로
 * 컴파일돼 있으니 바로 만든다. play 배포판은 같은 클래스를 기기의 CalibrationK68 APK 와
 * 함께 런타임에 올린다 — src/play/.../panel/Panel.java 참고.
 */
public final class Panel {

    public static PanelBackend create(Context ctx) { return new LeiaBackend(); }

    private Panel() {}
}
