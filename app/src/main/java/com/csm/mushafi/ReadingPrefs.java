package com.csm.mushafi;

import android.content.Context;
import android.content.SharedPreferences;

/** تفضيلات القراءة المحلية: آخر موضع وحجم الخط. */
final class ReadingPrefs {
    private static final String FILE = "mushafi_reading";
    private static final String KEY_SURAH = "last_surah";
    private static final String KEY_AYAH = "last_ayah";
    private static final String KEY_FONT = "font_sp";

    static final int DEFAULT_FONT = 24;
    static final int MIN_FONT = 16;
    static final int MAX_FONT = 36;

    private final SharedPreferences prefs;

    ReadingPrefs(Context context) {
        prefs = context.getApplicationContext().getSharedPreferences(FILE, Context.MODE_PRIVATE);
    }

    int lastSurah() {
        return prefs.getInt(KEY_SURAH, 0);
    }

    int lastAyah() {
        return prefs.getInt(KEY_AYAH, 0);
    }

    void saveLast(int surah, int ayah) {
        prefs.edit().putInt(KEY_SURAH, surah).putInt(KEY_AYAH, ayah).apply();
    }

    int fontSp() {
        return prefs.getInt(KEY_FONT, DEFAULT_FONT);
    }

    void saveFontSp(int sp) {
        prefs.edit().putInt(KEY_FONT, Math.max(MIN_FONT, Math.min(MAX_FONT, sp))).apply();
    }
}
