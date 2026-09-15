# DepthFlix — 기능 안내

[한국어](#한국어) · [English](#english)

## 한국어

Leia Lume Pad 2와 RedMagic Tablet 3D Explorer Edition에서 **3D 영상·사진을 제대로 보기 위한** 뷰어. 기기에 원래 있는 플레이어보다 배치 판별과 초점이 훨씬 정확하다.

### 무엇을 볼 수 있나

- 기기에 저장된 영상·사진
- URL 직접 열기 — `http(s)`, HLS(`.m3u8`), DASH(`.mpd`), RTSP
- **유튜브** — 실시간 방송 포함, 화질과 자막을 골라서 재생
- **DLNA**·**SMB** 네트워크 저장소 — 집 안의 NAS나 미디어 서버를 검색해서 바로 재생

### 3D 배치를 알아서 찾아낸다

파일 이름이 아니라 **화면 픽셀을 직접 분석**해서 좌우(SBS)인지 상하(TB)인지, 반반인지 전체인지 판별한다. 잘못 판별됐으면 손으로 고를 수도 있고, 한 번 고르면 그 파일은 기억해뒀다가 다음에 자동으로 적용한다.

### 2D 영상도 3D로

기기 안에 이미 있는 Leia의 AI 변환기를 그대로 써서, 평범한 2D 영상을 3D로 바꿔 볼 수 있다.

### 초점(수렴) 보정 — 이 앱의 핵심 기능

게임에서 뽑은 3D 스크린샷·영상은 원래 만들어진 화면 기준으로 초점이 맞춰져 있어서, 다른 크기의 화면에 그대로 올리면 눈이 편하게 모이지 않는다. DepthFlix는:

- 초점을 손으로 밀고 당길 수 있는 슬라이더를 제공하고,
- **자동 보정** 버튼을 누르면 화면의 시차를 실제로 측정해서 장면 중심을 화면 평면에 맞춰준다.

한 번 맞추면 그 파일에 저장된다.

### 자막

- 영상 옆에 자막 파일(`.srt`/`.smi`)이 있으면 자동으로 찾아서 띄운다.
- 영상 **내부에 들어 있는** 자막·오디오 트랙도 목록에서 골라 쓸 수 있다.
- 자막 크기·위치·튀어나오는 정도(깊이)를 조절할 수 있고, 3D 위빙 후에도 겹쳐 보이지 않도록 좌우 눈에 각각 그려 넣는다.

### 그 밖에

- 화면 비율을 자동/16:9/2.40:1/1.85:1/4:3/꽉 채우기 중 고르거나, 임의 비율로 미세조정.
- 폴더를 길게 눌러 목록 맨 위에 고정.
- AC3·E-AC3·DTS·TrueHD 오디오도 소리 나게 재생.

### 지원 기기

| 기기 | 밝기 |
|---|---|
| Leia Lume Pad 2 | 재생 중 화면을 최대 밝기로 고정 (3D 모드가 어두워지는 문제 때문) |
| RedMagic Tablet 3D Explorer Edition | 적응형 밝기로 충분해서 강제하지 않음 |

---

## English

A viewer built to watch **3D video and photos properly** on the Leia Lume Pad 2 and the RedMagic Tablet 3D Explorer Edition — far more accurate at detecting layout and setting focus than the stock player.

### What it plays

- Video and photos stored on the device
- URLs directly — `http(s)`, HLS (`.m3u8`), DASH (`.mpd`), RTSP
- **YouTube** — including live streams, with quality and subtitle language pickers
- **DLNA** and **SMB** network storage — finds NAS boxes and media servers on the network and plays straight from them

### It works out the 3D layout itself

Instead of trusting the filename, it **analyzes the actual pixels** to tell side-by-side from over-under, and half-width from full-width. If it gets it wrong, you can pick manually, and that choice is remembered for that file from then on.

### 2D video, turned into 3D

Uses the Leia AI converter already on the device to turn ordinary 2D video into 3D.

### Convergence correction — the heart of this app

3D screenshots and video pulled from games carry a focus point tuned for the screen they were made on, so dropping them onto a different-sized screen doesn't let your eyes converge comfortably. DepthFlix offers:

- A slider to nudge the focus point by hand, and
- An **auto-correct** button that measures the actual parallax on screen and centers the scene on the screen plane.

Once set, it's remembered for that file.

### Subtitles

- Finds and loads a subtitle file (`.srt`/`.smi`) sitting next to the video automatically.
- Subtitle and audio tracks **embedded in the video** can also be picked from a list.
- Size, position and depth (how far it pops toward you) are all adjustable, and subtitles are drawn into both eyes separately so they don't come apart in the weave.

### Also

- Aspect ratio: auto, 16:9, 2.40:1, 1.85:1, 4:3, or fill screen — plus manual fine-tuning for anything else.
- Long-press a folder to pin it to the top of the list.
- AC3, E-AC3, DTS and TrueHD audio all actually play.

### Supported devices

| Device | Brightness |
|---|---|
| Leia Lume Pad 2 | Held at full brightness during playback (3D mode goes dim otherwise) |
| RedMagic Tablet 3D Explorer Edition | Not forced — adaptive brightness already does the right thing |
