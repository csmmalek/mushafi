package com.csm.mushafi.quran;

import static org.junit.Assert.assertEquals;

import com.csm.mushafi.view.SurahHeaderGlyphs;

import java.util.HashSet;
import java.util.Set;

import org.junit.Test;

public class SurahHeaderGlyphsTest {
    @Test
    public void hasOneDistinctGlyphPerSurah() {
        Set<Integer> unique = new HashSet<>();
        for (int code : SurahHeaderGlyphs.CODES) {
            unique.add(code);
        }
        assertEquals(114, SurahHeaderGlyphs.CODES.length);
        assertEquals(114, unique.size());
    }
}
