package com.csm.mushafi;

import android.content.Context;
import android.content.SharedPreferences;

/** تفضيلات القراءة المحلية: آخر موضع، حجم الخط، ومدة الختمة. */
final class ReadingPrefs {
    private static final String FILE = "mushafi_reading";
    private static final String KEY_SURAH = "last_surah";
    private static final String KEY_AYAH = "last_ayah";
    private static final String KEY_FONT = "font_sp";
    private static final String KEY_KHATMAH_DAYS = "khatmah_days";

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

    /** مقياس خط الصفحة بالنسبة المئوية (60–160)، 100 = الحجم الذي يملأ الصفحة. */
    int pageFontPercent() {
        return Math.max(60, Math.min(160, prefs.getInt("page_font_percent", 100)));
    }

    void savePageFontPercent(int percent) {
        prefs.edit().putInt("page_font_percent", Math.max(60, Math.min(160, percent))).apply();
    }

    /** عرض الصفحات سطراً بسطر كمصحف المدينة (الافتراضي). */
    boolean exactLayout() {
        return prefs.getBoolean("exactLayout", true);
    }

    void saveExactLayout(boolean on) {
        prefs.edit().putBoolean("exactLayout", on).apply();
    }

    boolean kashida() {
        return prefs.getBoolean("kashida", true);
    }

    void saveKashida(boolean on) {
        prefs.edit().putBoolean("kashida", on).apply();
    }

    int appTheme() {
        return Math.max(0, Math.min(AppThemes.COUNT - 1, prefs.getInt("app_theme", 0)));
    }

    void saveAppTheme(int index) {
        prefs.edit().putInt("app_theme", index).apply();
    }

    String readerReciter() {
        return prefs.getString("reader_reciter", "alafasy");
    }

    void saveReaderReciter(String id) {
        prefs.edit().putString("reader_reciter", id).apply();
    }

    int pageTheme() {
        return prefs.getInt("page_theme", 0);
    }

    void savePageTheme(int index) {
        prefs.edit().putInt("page_theme", index).apply();
    }

    boolean keepScreenOn() {
        return prefs.getBoolean("keep_screen_on", false);
    }

    void saveKeepScreenOn(boolean on) {
        prefs.edit().putBoolean("keep_screen_on", on).apply();
    }

    /** عرض الحروف المقطعة بعلامات المد العثمانية (مفعّل افتراضياً؛ عند الإيقاف يُعرض نص الملف حرفياً). */
    boolean muqattaatUthmani() {
        return prefs.getBoolean("muqattaat_uthmani", true);
    }

    void saveMuqattaatUthmani(boolean on) {
        prefs.edit().putBoolean("muqattaat_uthmani", on).apply();
    }

    int khatmahDays() {
        return prefs.getInt(KEY_KHATMAH_DAYS, 0);
    }

    void saveKhatmahDays(int days) {
        prefs.edit().putInt(KEY_KHATMAH_DAYS, days).apply();
    }
}
