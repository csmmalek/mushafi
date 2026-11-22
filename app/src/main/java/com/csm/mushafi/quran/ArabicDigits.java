package com.csm.mushafi.quran;

/** تحويل الأرقام إلى الأرقام العربية الهندية للعرض فقط؛ لا يغيّر النص القرآني. */
public final class ArabicDigits {
    private static final char[] DIGITS = {'٠', '١', '٢', '٣', '٤', '٥', '٦', '٧', '٨', '٩'};

    private ArabicDigits() {
    }

    public static String format(int value) {
        String s = Integer.toString(value);
        StringBuilder sb = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            sb.append(c >= '0' && c <= '9' ? DIGITS[c - '0'] : c);
        }
        return sb.toString();
    }
}
