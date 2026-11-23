package com.csm.mushafi.download;

/** فحص بسيط لرأس الملف الصوتي: إما ID3 أو إطار MPEG (مزامنة 11 بت). */
public final class Mp3Check {
    private Mp3Check() {
    }

    public static boolean looksLikeMp3(byte[] head) {
        if (head == null || head.length < 3) {
            return false;
        }
        if (head[0] == 'I' && head[1] == 'D' && head[2] == '3') {
            return true;
        }
        return head.length >= 2 && (head[0] & 0xFF) == 0xFF && (head[1] & 0xE0) == 0xE0;
    }

    /** رأس Range للاستئناف من الموضع الحالي؛ 0 يعني بدء جديد. */
    public static String rangeHeader(long existingBytes) {
        return existingBytes > 0 ? "bytes=" + existingBytes + "-" : null;
    }
}
