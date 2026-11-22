package com.csm.mushafi.quran;

import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

/**
 * بوابة التحقق من ملف النص القرآني قبل أي استيراد.
 * لا تعدّل هذه الكلاسات النص أبداً: تقرأ البايتات كما هي، وتفشل عند أي خلل.
 * الأسطر الفارغة والتعليقات (التي تبدأ بـ #) تُتجاهل لأنها ليست سجلات.
 */
public final class QuranVerifier {

    /** بصمة SHA-256 للملف المعتمد (quran-simple.txt، Tanzil v1.1). */
    public static final String EXPECTED_SHA256 =
            "b3638f5cf03e2b05e4000efc0e4b656aafb3cf6ed28f8770b6497c5b43c46089";
    public static final int EXPECTED_RECORDS = 6236;
    public static final int EXPECTED_SURAHS = 114;

    private QuranVerifier() {
    }

    /** آية واحدة كما وردت من الملف. */
    public static final class Verse {
        public final int surah;
        public final int ayah;
        public final String text;

        Verse(int surah, int ayah, String text) {
            this.surah = surah;
            this.ayah = ayah;
            this.text = text;
        }
    }

    /** نتيجة التحليل الناجح. */
    public static final class Result {
        public final List<Verse> verses;
        /** عدد آيات كل سورة؛ الفهرس = رقم السورة - 1. */
        public final int[] ayahCounts;

        Result(List<Verse> verses, int[] ayahCounts) {
            this.verses = verses;
            this.ayahCounts = ayahCounts;
        }
    }

    public static String sha256Hex(byte[] data) throws NoSuchAlgorithmException {
        byte[] digest = MessageDigest.getInstance("SHA-256").digest(data);
        StringBuilder sb = new StringBuilder(digest.length * 2);
        for (byte b : digest) {
            sb.append(String.format("%02x", b & 0xff));
        }
        return sb.toString();
    }

    /** يتحقق من بصمة الملف الكاملة. */
    public static void checkHash(byte[] data, String expectedSha256) throws QuranIntegrityException {
        String actual;
        try {
            actual = sha256Hex(data);
        } catch (NoSuchAlgorithmException e) {
            throw new QuranIntegrityException("تعذّر حساب بصمة الملف على هذا الجهاز.");
        }
        if (!actual.equals(expectedSha256)) {
            throw new QuranIntegrityException(
                    "فشل التحقق من بصمة ملف القرآن. الملف لا يطابق النسخة المعتمدة، ولن يتم عرضه.");
        }
    }

    /** يحلل السجلات بصيغة "سورة|آية|نص" ويتحقق من العدد والترتيب والتكرار والفجوات. */
    public static Result parse(byte[] data, int expectedRecords, int expectedSurahs)
            throws QuranIntegrityException {
        String content;
        try {
            content = StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(data))
                    .toString();
        } catch (CharacterCodingException e) {
            throw new QuranIntegrityException("ملف القرآن ليس بترميز UTF-8 صالح.");
        }

        List<Verse> verses = new ArrayList<>(expectedRecords);
        int prevSurah = 0;
        int prevAyah = 0;
        String[] lines = content.split("\n", -1);
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }
            int lineNo = i + 1;
            if (line.indexOf('\r') >= 0) {
                throw new QuranIntegrityException("سطر " + lineNo + " يحتوي على نهاية سطر غير متوقعة.");
            }
            String[] parts = line.split("\\|", -1);
            if (parts.length != 3) {
                throw new QuranIntegrityException("سطر " + lineNo + " لا يطابق صيغة سورة|آية|نص.");
            }
            int surah;
            int ayah;
            try {
                surah = Integer.parseInt(parts[0]);
                ayah = Integer.parseInt(parts[1]);
            } catch (NumberFormatException e) {
                throw new QuranIntegrityException("سطر " + lineNo + " يحتوي على رقم غير صالح.");
            }
            if (parts[2].isEmpty()) {
                throw new QuranIntegrityException("سطر " + lineNo + " بلا نص.");
            }

            if (surah == prevSurah) {
                if (ayah != prevAyah + 1) {
                    throw new QuranIntegrityException(
                            "ترقيم الآيات غير متسلسل أو مكرر عند السورة " + surah + "، سطر " + lineNo + ".");
                }
            } else if (surah == prevSurah + 1) {
                if (ayah != 1) {
                    throw new QuranIntegrityException(
                            "السورة " + surah + " لا تبدأ بالآية 1، سطر " + lineNo + ".");
                }
            } else {
                throw new QuranIntegrityException(
                        "ترتيب السور غير متسلسل عند السورة " + surah + "، سطر " + lineNo + ".");
            }
            prevSurah = surah;
            prevAyah = ayah;
            verses.add(new Verse(surah, ayah, parts[2]));
        }

        if (verses.size() != expectedRecords) {
            throw new QuranIntegrityException("عدد الآيات " + verses.size()
                    + " والمتوقع " + expectedRecords + ".");
        }
        if (prevSurah != expectedSurahs) {
            throw new QuranIntegrityException("عدد السور " + prevSurah
                    + " والمتوقع " + expectedSurahs + ".");
        }

        int[] counts = new int[expectedSurahs];
        for (Verse v : verses) {
            counts[v.surah - 1] = Math.max(counts[v.surah - 1], v.ayah);
        }
        return new Result(verses, counts);
    }
}
