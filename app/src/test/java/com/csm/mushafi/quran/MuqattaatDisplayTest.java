package com.csm.mushafi.quran;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.csm.mushafi.mushaf.MuqattaatDisplay;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

/** كل استبدال لشكل الحروف المقطعة يجب أن يطابق حروف النص الموثّق حرفاً بحرف. */
public class MuqattaatDisplayTest {

    @Test
    public void everyEntryMatchesVerifiedTextLetters() throws Exception {
        byte[] bytes = Files.readAllBytes(Paths.get("src/main/assets/quran-simple.txt"));
        QuranVerifier.Result result = QuranVerifier.parse(bytes, 6236, 114);
        Map<Integer, String> text = new HashMap<>();
        String basmala = null;
        for (QuranVerifier.Verse v : result.verses) {
            text.put(v.surah * 1000 + v.ayah, v.text);
            if (v.surah == 1 && v.ayah == 1) {
                basmala = v.text;
            }
        }
        MuqattaatDisplay display = MuqattaatDisplay.parse(new String(Files.readAllBytes(
                Paths.get("src/main/assets/muqattaat-display.json")), StandardCharsets.UTF_8));
        // 28 سورة بعلامات مد، مع آيتين في الشورى (حم، عسق). سورة طه تُكتب بلا علامة فلا استبدال لها.
        assertEquals(29, display.size());
        for (String[] e : display.all()) {
            int s = Integer.parseInt(e[0]);
            int a = Integer.parseInt(e[1]);
            String verified = text.get(s * 1000 + a);
            if (a == 1 && verified.startsWith(basmala)) {
                verified = verified.substring(basmala.length()).trim();
            }
            assertTrue("surah " + s + ":" + a, verified.startsWith(e[2]));
            assertEquals("letters differ in " + s + ":" + a,
                    ArabicNormalizer.normalize(e[2]), ArabicNormalizer.normalize(e[3]));
            String shown = display.apply(s, a, verified);
            assertTrue(shown.startsWith(e[3]));
            assertEquals(ArabicNormalizer.normalize(verified), ArabicNormalizer.normalize(shown));
        }
    }

    @Test
    public void leavesOtherTextUntouched() {
        MuqattaatDisplay display = MuqattaatDisplay.parse(
                "[{\"s\":2,\"a\":1,\"from\":\"x\",\"to\":\"y\"}]");
        assertEquals("xz", display.apply(2, 1, "xz"));
        assertEquals("x z", display.apply(3, 1, "x z"));
        assertEquals("y z", display.apply(2, 1, "x z"));
    }
}
