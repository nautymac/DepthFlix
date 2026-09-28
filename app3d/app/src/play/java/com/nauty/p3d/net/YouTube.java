package com.nauty.p3d.net;

import android.content.Context;
import android.net.Uri;

import java.io.File;
import java.util.Collections;
import java.util.List;

/**
 * play 배포판의 빈 스텁. 유튜브 스트림 추출(yt-dlp)은 Play 정책 위반 사유라 기능째
 * 뺐다 — isYoutubeUrl() 이 늘 false 라 MainActivity 는 유튜브 링크도 평범한 URL 로
 * 다루고, youtubedl-android 의존성도 이 배포판엔 없다. 실제 구현은 src/github/.
 */
public final class YouTube {

    private YouTube() {}

    public static boolean isYoutubeUrl(String url) { return false; }

    public static final class Quality {
        public final String label;
        public final String url;
        public final String audioUrl;
        Quality(String label, String url, String audioUrl) {
            this.label = label; this.url = url; this.audioUrl = audioUrl;
        }
        public Uri playUri() { return Uri.parse(url); }
    }

    public static final class Caption {
        public final String code;
        public final String label;
        public final String url;
        Caption(String code, String label, String url) {
            this.code = code; this.label = label; this.url = url;
        }
    }

    public static final class Probe {
        public final String title = "";
        public final List<Quality> qualities = Collections.emptyList();
        public final List<Caption> captions  = Collections.emptyList();
    }

    public static Probe probe(Context ctx, String youtubeUrl) throws Exception {
        throw new UnsupportedOperationException("이 배포판은 유튜브를 지원하지 않는다");
    }

    public static File downloadCaption(Context ctx, Caption c) { return null; }
}
