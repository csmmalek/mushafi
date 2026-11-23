package com.csm.mushafi.audio;

import java.util.Locale;

/** بناء رابط ملف السورة من خادم القارئ. لا يوجد رابط ثابت لكل القراء. */
public final class AudioUrls {

    private AudioUrls() {
    }

    /** الخادم يجب أن يكون HTTPS وينتهي بـ "/". */
    public static boolean isAcceptableServer(String server) {
        if (server == null || !server.startsWith("https://")) {
            return false;
        }
        return server.endsWith("/") && server.length() > "https://".length() + 3;
    }

    public static String surahUrl(String server, int surah) {
        if (!isAcceptableServer(server)) {
            throw new IllegalArgumentException("خادم الصوت غير صالح");
        }
        if (surah < 1 || surah > 114) {
            throw new IllegalArgumentException("رقم السورة خارج النطاق");
        }
        return server + String.format(Locale.ROOT, "%03d.mp3", surah);
    }
}
