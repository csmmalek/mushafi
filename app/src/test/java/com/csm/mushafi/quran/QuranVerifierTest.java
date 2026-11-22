package com.csm.mushafi.quran;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

public class QuranVerifierTest {

    private static final String ASSET = "src/main/assets/quran-simple.txt";

    private static byte[] realAsset() throws IOException {
        return Files.readAllBytes(new File(ASSET).toPath());
    }

    private static byte[] bytes(String s) {
        return s.getBytes(StandardCharsets.UTF_8);
    }

    private static String failureOf(byte[] data, int records, int surahs) {
        try {
            QuranVerifier.parse(data, records, surahs);
        } catch (QuranIntegrityException e) {
            return e.getMessage();
        }
        fail("expected the integrity gate to reject this input");
        return null;
    }

    @Test
    public void realAssetMatchesPinnedHash() throws Exception {
        QuranVerifier.checkHash(realAsset(), QuranVerifier.EXPECTED_SHA256);
    }

    @Test
    public void realAssetPassesStructuralGate() throws Exception {
        QuranVerifier.Result r = QuranVerifier.parse(
                realAsset(), QuranVerifier.EXPECTED_RECORDS, QuranVerifier.EXPECTED_SURAHS);
        assertEquals(QuranVerifier.EXPECTED_RECORDS, r.verses.size());
        assertEquals(QuranVerifier.EXPECTED_SURAHS, r.ayahCounts.length);
        int sum = 0;
        for (int c : r.ayahCounts) {
            sum += c;
        }
        assertEquals(QuranVerifier.EXPECTED_RECORDS, sum);
    }

    @Test
    public void firstVerseIsStoredExactlyAsInFile() throws Exception {
        byte[] data = realAsset();
        String content = new String(data, StandardCharsets.UTF_8);
        String fileLine = null;
        for (String line : content.split("\n")) {
            if (line.startsWith("1|1|")) {
                fileLine = line;
                break;
            }
        }
        assertTrue("record 1|1 must exist in the asset", fileLine != null);
        String expected = fileLine.substring("1|1|".length());

        QuranVerifier.Result r = QuranVerifier.parse(
                data, QuranVerifier.EXPECTED_RECORDS, QuranVerifier.EXPECTED_SURAHS);
        assertEquals(expected, r.verses.get(0).text);
    }

    @Test
    public void oneChangedByteFailsHashCheck() throws Exception {
        byte[] data = realAsset();
        byte[] tampered = new byte[data.length + 1];
        System.arraycopy(data, 0, tampered, 0, data.length);
        tampered[data.length] = '\n';
        try {
            QuranVerifier.checkHash(tampered, QuranVerifier.EXPECTED_SHA256);
            fail("tampered data must fail the hash gate");
        } catch (QuranIntegrityException e) {
            assertTrue(e.getMessage().contains("بصمة"));
        }
    }

    @Test
    public void validSyntheticInputPasses() throws Exception {
        QuranVerifier.Result r = QuranVerifier.parse(
                bytes("# comment\n\n1|1|a\n1|2|b\n2|1|c\n"), 3, 2);
        assertArrayEquals(new int[]{2, 1}, r.ayahCounts);
        assertEquals("c", r.verses.get(2).text);
    }

    @Test
    public void duplicateAyahIsRejected() {
        String msg = failureOf(bytes("1|1|a\n1|1|b\n"), 2, 1);
        assertTrue(msg.contains("مكرر"));
    }

    @Test
    public void gapInAyahNumberingIsRejected() {
        String msg = failureOf(bytes("1|1|a\n1|3|b\n"), 2, 1);
        assertTrue(msg.contains("متسلسل"));
    }

    @Test
    public void surahOutOfOrderIsRejected() {
        String msg = failureOf(bytes("2|1|a\n1|1|b\n"), 2, 2);
        assertTrue(msg.contains("ترتيب السور"));
    }

    @Test
    public void surahNotStartingAtAyahOneIsRejected() {
        String msg = failureOf(bytes("1|1|a\n2|2|b\n"), 2, 2);
        assertTrue(msg.contains("لا تبدأ بالآية 1"));
    }

    @Test
    public void wrongRecordCountIsRejected() {
        String msg = failureOf(bytes("1|1|a\n"), 2, 1);
        assertTrue(msg.contains("عدد الآيات"));
    }

    @Test
    public void wrongSurahCountIsRejected() {
        String msg = failureOf(bytes("1|1|a\n1|2|b\n"), 2, 2);
        assertTrue(msg.contains("عدد السور"));
    }

    @Test
    public void malformedRecordIsRejected() {
        String msg = failureOf(bytes("1|1\n"), 1, 1);
        assertTrue(msg.contains("صيغة"));
    }

    @Test
    public void emptyTextIsRejected() {
        String msg = failureOf(bytes("1|1|\n"), 1, 1);
        assertTrue(msg.contains("بلا نص"));
    }

    @Test
    public void invalidUtf8IsRejected() {
        String msg = failureOf(new byte[]{'1', '|', '1', '|', (byte) 0xC3, (byte) 0x28, '\n'}, 1, 1);
        assertTrue(msg.contains("UTF-8"));
    }

    @Test
    public void carriageReturnIsRejected() {
        String msg = failureOf(bytes("1|1|a\r\n"), 1, 1);
        assertTrue(msg.contains("نهاية سطر"));
    }

    @Test
    public void arabicDigitsFormatting() {
        assertEquals("١١٤", ArabicDigits.format(114));
        assertEquals("٠", ArabicDigits.format(0));
    }
}
