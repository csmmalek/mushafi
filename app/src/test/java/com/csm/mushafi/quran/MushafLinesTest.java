package com.csm.mushafi.quran;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.csm.mushafi.mushaf.MushafLines;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

/** تقسيم الأسطر يغطي كل كلمات كل آية مرة واحدة وبالترتيب، و15 سطراً لكل صفحة. */
public class MushafLinesTest {

    private static MushafLines lines() throws Exception {
        return MushafLines.parse(new String(
                Files.readAllBytes(Paths.get("src/main/assets/mushaf-lines.json")),
                StandardCharsets.UTF_8));
    }

    @Test
    public void lineCountsAreFifteenExceptFirstTwoPages() throws Exception {
        MushafLines l = lines();
        for (int p = 1; p <= 604; p++) {
            assertEquals("page " + p, p <= 2 ? 8 : 15, l.page(p).size());
        }
    }

    @Test
    public void everyWordOfEveryAyahIsCoveredExactlyOnce() throws Exception {
        byte[] bytes = Files.readAllBytes(Paths.get("src/main/assets/quran-simple.txt"));
        QuranVerifier.Result result = QuranVerifier.parse(bytes, 6236, 114);
        Map<Integer, Integer> words = new HashMap<>();
        for (QuranVerifier.Verse v : result.verses) {
            String t = MushafLines.stripBasmala(v.surah, v.ayah, v.text);
            words.put(v.surah * 1000 + v.ayah, t.split("\\s+").length);
        }
        MushafLines l = lines();
        Map<Integer, Integer> next = new HashMap<>();
        for (int p = 1; p <= 604; p++) {
            for (MushafLines.Line line : l.page(p)) {
                for (MushafLines.Seg s : line.segs) {
                    int key = s.surah * 1000 + s.ayah;
                    assertEquals("order at " + key, (int) next.getOrDefault(key, 0), s.from);
                    assertTrue(s.to > s.from);
                    next.put(key, s.to);
                }
            }
        }
        assertEquals(6236, next.size());
        for (Map.Entry<Integer, Integer> e : words.entrySet()) {
            assertEquals("ayah " + e.getKey(), e.getValue(), next.get(e.getKey()));
        }
    }
}
