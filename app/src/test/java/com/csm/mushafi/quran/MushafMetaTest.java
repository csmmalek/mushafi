package com.csm.mushafi.quran;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.csm.mushafi.mushaf.MushafMeta;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

/** يقارن بيانات الصفحات وأسماء السور بالنص القرآني الموثّق (نفس الملف الذي يتحقق منه التطبيق). */
public class MushafMetaTest {

    private static MushafMeta meta() throws Exception {
        String json = new String(Files.readAllBytes(Paths.get("src/main/assets/mushaf-meta.json")),
                StandardCharsets.UTF_8);
        return MushafMeta.parse(json);
    }

    @Test
    public void ayahCountsMatchVerifiedQuranText() throws Exception {
        byte[] bytes = Files.readAllBytes(Paths.get("src/main/assets/quran-simple.txt"));
        QuranVerifier.Result result = QuranVerifier.parse(bytes, 6236, 114);
        int[] counts = new int[115];
        for (QuranVerifier.Verse v : result.verses) {
            counts[v.surah]++;
        }
        MushafMeta meta = meta();
        for (int s = 1; s <= 114; s++) {
            assertEquals("surah " + s, counts[s], meta.ayahCount(s));
        }
    }

    @Test
    public void everyPageHasAyahsAndTotalsMatch() throws Exception {
        MushafMeta meta = meta();
        int total = 0;
        for (int p = 1; p <= MushafMeta.PAGES; p++) {
            assertTrue("page " + p, !meta.ayahsOnPage(p).isEmpty());
            total += meta.ayahsOnPage(p).size();
        }
        assertEquals(6236, total);
        assertEquals(1, meta.pageOf(1, 1));
        assertEquals(MushafMeta.PAGES, meta.pageOf(114, meta.ayahCount(114)));
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsOutOfOrderPages() {
        StringBuilder names = new StringBuilder("[");
        StringBuilder pages = new StringBuilder("[");
        for (int i = 0; i < 114; i++) {
            names.append(i == 0 ? "" : ",").append("\"x\"");
            pages.append(i == 0 ? "" : ",").append(i == 0 ? "[2,1]" : "[3]");
        }
        MushafMeta.parse("{\"names\":" + names + "],\"pages\":" + pages + "]}");
    }
}
