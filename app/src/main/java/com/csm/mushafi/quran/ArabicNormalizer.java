package com.csm.mushafi.quran;

/**
 * يُنتج نسخة مبسطة للبحث فقط: يحذف التشكيل والتطويل ويوحّد الألف والياء والتاء المربوطة.
 * لا يُستخدم أبداً لعرض النص ولا يستبدل النص الأصلي.
 */
public final class ArabicNormalizer {

    private ArabicNormalizer() {
    }

    public static String normalize(String input) {
        if (input == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder(input.length());
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            if (isDiacritic(c) || c == 'ـ' || c == '۟' || c == '۠') {
                continue;
            }
            switch (c) {
                case 'آ': // آ
                case 'أ': // أ
                case 'إ': // إ
                case 'ٱ': // ٱ
                    sb.append('ا'); // ا
                    break;
                case 'ى': // ى
                    sb.append('ي'); // ي
                    break;
                case 'ة': // ة
                    sb.append('ه'); // ه
                    break;
                default:
                    sb.append(c);
            }
        }
        return sb.toString().replaceAll("\\s+", " ").trim();
    }

    private static boolean isDiacritic(char c) {
        return (c >= 'ً' && c <= 'ٟ') || c == 'ٰ'
                || (c >= 'ۖ' && c <= 'ۭ');
    }
}
