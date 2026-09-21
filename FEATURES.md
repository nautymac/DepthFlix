# DepthFlix — 기능 정리 / Feature Overview

[한국어](#한국어) · [English](#english)

## 한국어

**대상 기기**: Leia Lume Pad 2, RedMagic Tablet 3D Explorer Edition (같은 CNSDK 무안경 3D 라이트필드 패널 계열)

### 1. 미디어 소스
- 기기 내장 사진/영상 갤러리 (자동 스캔, 새 파일 감지)
- URL/스트리밍 직접 열기 — **유튜브 링크 지원**
- DLNA 서버 탐색 및 재생 (Plex, Jellyfin, Serviio 등)
- SMB 네트워크 공유 탐색 및 재생 (호스트/계정 저장, 익명 접속 지원)
- 폴더 고정(즐겨찾기) 기능

### 2. 3D 입체 처리
- 소스 포맷 자동 판별: 2D(→3D 변환), 좌우 SBS(half/full), 상하 TB(half/full)
- 자동 판별이 틀렸을 때 수동 선택 가능, 좌우 반전(Swap L/R) 지원 — 사진은 기본값 ON, 영상은 기본값 OFF
- 2D→3D 변환 시 깊이(시차 강도) 조절
- 수렴(컨버전스) 보정 — 화면 앞/뒤로 수동 조절 + 장면 중심 자동 측정
- 화면비(Aspect) 자동/수동 미세조정
- 3D/2D 출력 전환, SBS 확인 모드

### 3. 자막
- SRT, SMI(SAMI) 파싱 지원 — EUC-KR 인코딩 자동 감지
- 영상 내장 자막 트랙 인식 (이미지 자막 PGS/VOBSUB은 표시만, 실제 렌더링은 미지원)
- 같은 폴더 자막 자동 탐색 + 수동 선택 다이얼로그
- 로컬 파일뿐 아니라 SMB로 재생하는 영상도 같은 폴더 자막 자동 탐색/다운로드 지원
- 자막 크기, 위치, (입체감을 위한) 깊이 조절

### 4. 오디오
- 다중 오디오 트랙 선택
- 유튜브 재생 시 원본 언어 오디오 자동 우선 선택 (AI 더빙/자동 번역 오디오가 아닌 원본 트랙을 yt-dlp의 `language_preference` 값으로 판별)

### 5. 유튜브 전용 기능
- 화질 선택
- 오디오 트랙(언어) 선택
- 자막 언어 선택
- 기기별 AV1 디코더 지원 여부에 따라 화질 후보군 자동 필터링 (AV1 미지원 기기에서 재생 실패 방지)

### 6. 재생 편의
- 이어보기 (재생 위치 저장/복원)
- 재생 오류 시 사용자에게 원인 안내 (지원 안 되는 오디오 코덱 등)
- 설정판에서 바로 파일 삭제 (확인 대화상자 후 삭제, 사진은 다음 사진으로 자동 이동, 영상은 목록으로 복귀 — SMB 등 네트워크 소스는 대상 아님)
- 플레이어에서 뒤로 나오면 목록이 보던 자리를 그대로 유지 (수백 장짜리 폴더에서 처음으로 튀지 않음)

### 7. 기기별 차별화 항목
- 재생 중 화면 밝기: Lume Pad 2는 3D 모드 특성상 재생 중 밝기를 강제로 최대화(고정), RedMagic은 기기 자체 자동 밝기로 충분해 강제하지 않는 것이 기본값 (비교용으로 강제 버전도 별도 배포)
- CNSDK 라이트필드 연동은 기기 세대별로 다른 SDK(0.6/0.8 대 0.10)를 각각 사용

---

## English

**Supported devices**: Leia Lume Pad 2, RedMagic Tablet 3D Explorer Edition (same CNSDK glasses-free 3D lightfield panel family)

### 1. Media sources
- Built-in photo/video gallery (auto-scan, new-file detection)
- Open a URL/stream directly — **YouTube links supported**
- DLNA server discovery and playback (Plex, Jellyfin, Serviio, etc.)
- SMB network share browsing and playback (saved host/credentials, anonymous login supported)
- Pin folders as favorites

### 2. 3D stereo handling
- Automatic source format detection: 2D (→3D conversion), side-by-side (half/full), over-under (half/full)
- Manual override when auto-detection is wrong, plus left/right swap — defaults to ON for photos, OFF for video
- Depth (parallax strength) adjustment for 2D→3D conversion
- Convergence correction — manual push in front of/behind the screen, plus automatic scene-center measurement
- Automatic/manual aspect-ratio fine-tuning
- 3D/2D output toggle, SBS check mode

### 3. Subtitles
- SRT and SMI (SAMI) parsing — automatic EUC-KR encoding detection
- Recognizes embedded subtitle tracks in the video (image subtitles like PGS/VOBSUB are listed but not rendered)
- Automatic same-folder subtitle detection plus a manual picker dialog
- Subtitle auto-detection/download now also works for videos played over SMB, not just local files
- Adjustable subtitle size, position, and (for stereo depth) apparent depth

### 4. Audio
- Multiple audio track selection
- For YouTube playback, automatically prefers the original-language audio track over AI-dubbed/auto-translated audio, using yt-dlp's `language_preference` field

### 5. YouTube-specific features
- Quality selection
- Audio track (language) selection
- Subtitle language selection
- Automatically filters quality candidates by whether the device actually supports AV1 decoding, preventing playback failures on devices without an AV1 decoder

### 6. Playback convenience
- Resume playback (saves/restores position)
- Clear error messages on playback failure (e.g. unsupported audio codec)
- Delete the current file right from the settings panel (confirmation dialog first; advances to the next photo, or returns to the list for video — not available for network sources like SMB)
- The file list keeps its scroll position when you back out of the player (no more jumping to the top of a folder with hundreds of photos)

### 7. Per-device differences
- Playback brightness: the Lume Pad 2 forces maximum screen brightness during playback because of how its 3D mode handles backlighting, while RedMagic's own adaptive brightness is sufficient on its own, so forcing is off by default there (a forced variant is also published for side-by-side comparison)
- CNSDK lightfield integration uses a different SDK generation per device family (0.6/0.8 vs. 0.10)
