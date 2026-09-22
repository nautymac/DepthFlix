#extension GL_OES_EGL_image_external : require
#ifdef GL_FRAGMENT_PRECISION_HIGH
precision highp float;    // PQ 곡선은 0 근처 값이 작아 mediump 으론 계단이 생긴다
#else
precision mediump float;
#endif
varying vec2 vTex;
uniform samplerExternalOES sTexture;
// 2D->3D 시어. 원본 frag2dto3d.sh 수식:
//   tmp.x += 0.004 - (vTex.y * screenHeight) * 0.0000122
// screenHeight 를 uShearSlope 에 흡수시켜 파라미터 2개로 정리.
uniform float uShearTop;     // 화면 상단에서의 이동량 (기본 0.004), 0 이면 시어 없음
uniform float uShearSlope;   // 아래로 갈수록 감소하는 기울기 (기본 0.0000122 * srcHeightPx)
uniform float uBottomCut;    // 하단 잘라내기 경계 (원본 0.990740741, 1.0 이면 비활성)

// HDR -> SDR. 0 = 없음, 1 = PQ(HDR10), 2 = HLG.
// Android 13 이상은 디코더에 SDR 출력을 요청하므로 늘 0 이고, 그 아래(Lume Pad 2 =
// Android 12, ProMa = Android 8)에서만 켠다. OES 텍스처는 드라이버가 YUV->RGB 만 하고
// 전달함수·원색은 그대로 두므로 BT.2020 PQ/HLG 인코딩 값이 들어온다 — 그걸 sRGB 로 만든다.
// 안 하면 빨강이 오렌지로 빠지고 전체가 뿌옇다 (RedMagic 에서 실측한 증상).
uniform int uHdr;

const float PQ_M1 = 0.1593017578125;
const float PQ_M2 = 78.84375;
const float PQ_C1 = 0.8359375;
const float PQ_C2 = 18.8515625;
const float PQ_C3 = 18.6875;
const float SDR_WHITE_NIT = 203.0;            // BT.2408 기준 SDR 흰색
const float TONE_W = 1000.0 / SDR_WHITE_NIT;  // 이 밝기까지를 화면에 눌러 담는다

// PQ 전달함수 역변환. 결과 1.0 = 10000 nit.
vec3 pqToLinear(vec3 n) {
    vec3 p   = pow(max(n, vec3(1e-6)), vec3(1.0 / PQ_M2));
    vec3 num = max(p - PQ_C1, vec3(0.0));
    vec3 den = PQ_C2 - PQ_C3 * p;
    return pow(max(num / den, vec3(1e-6)), vec3(1.0 / PQ_M1));
}

// HLG 역 OETF + OOTF(감마 1.2). 결과 1.0 = 1000 nit 기준 최대.
vec3 hlgToLinear(vec3 e) {
    vec3 lo = e * e / 3.0;
    vec3 hi = (exp((e - 0.55991073) / 0.17883277) + 0.28466892) / 12.0;
    vec3 s  = mix(lo, hi, step(vec3(0.5), e));
    float y = dot(s, vec3(0.2627, 0.6780, 0.0593));   // BT.2020 휘도
    return s * pow(max(y, 1e-6), 0.2);
}

// BT.2020 -> BT.709 원색 (선형). GLSL mat3 는 열 우선이라 열 단위로 적었다.
const mat3 BT2020_TO_709 = mat3(
     1.6605, -0.1246, -0.0182,
    -0.5876,  1.1329, -0.1006,
    -0.0728, -0.0083,  1.1187);

void main() {
    vec2 t = vTex;
    t.x += uShearTop - vTex.y * uShearSlope;
    vec4 c = texture2D(sTexture, t);
    if (uHdr != 0) {
        vec3 lin;
        if (uHdr == 1) lin = pqToLinear(c.rgb)  * (10000.0 / SDR_WHITE_NIT);
        else           lin = hlgToLinear(c.rgb) * (1000.0  / SDR_WHITE_NIT);
        lin = max(BT2020_TO_709 * lin, vec3(0.0));
        // 확장 Reinhard: 1.0(SDR 흰색)까지는 거의 그대로, 그 위 하이라이트만 눌러 담는다.
        lin = lin * (1.0 + lin / (TONE_W * TONE_W)) / (1.0 + lin);
        c.rgb = pow(clamp(lin, 0.0, 1.0), vec3(1.0 / 2.2));
    }
    gl_FragColor = c;
    if (vTex.y > uBottomCut) {
        gl_FragColor = vec4(0.0, 0.0, 0.0, 1.0);
    }
}
