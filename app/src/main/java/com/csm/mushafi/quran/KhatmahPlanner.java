package com.csm.mushafi.quran;

/** حساب خطة الختمة من عدد آيات المصحف المتحقق منه. */
public final class KhatmahPlanner {

    private KhatmahPlanner() {
    }

    /** عدد الآيات اليومية لإتمام الختمة في المدة المحددة (تقريب للأعلى). */
    public static int dailyAyahs(int totalAyahs, int days) {
        if (days <= 0) {
            throw new IllegalArgumentException("المدة يجب أن تكون يوماً واحداً على الأقل");
        }
        return (totalAyahs + days - 1) / days;
    }

    /** نسبة التقدم بالآيات، من 0 إلى 100. */
    public static int progressPercent(int readAyahs, int totalAyahs) {
        if (totalAyahs <= 0) {
            return 0;
        }
        int p = (int) (100L * Math.max(0, Math.min(readAyahs, totalAyahs)) / totalAyahs);
        return p;
    }

    /** موضع الآية في المصحف كله (من 1 إلى 6236) من السورة والآية، بالاعتماد على عدد آيات كل سورة. */
    public static int globalIndex(int[] ayahCounts, int surah, int ayah) {
        int index = 0;
        for (int i = 0; i < surah - 1 && i < ayahCounts.length; i++) {
            index += ayahCounts[i];
        }
        return index + ayah;
    }
}
