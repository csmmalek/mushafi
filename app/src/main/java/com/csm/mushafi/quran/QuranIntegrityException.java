package com.csm.mushafi.quran;

/** يُرمى عند فشل أي فحص على ملف النص القرآني. الرسالة عربية وتُعرض للمستخدم. */
public class QuranIntegrityException extends Exception {
    public QuranIntegrityException(String message) {
        super(message);
    }
}
