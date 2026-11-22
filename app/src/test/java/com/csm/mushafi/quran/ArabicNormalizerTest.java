package com.csm.mushafi.quran;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class ArabicNormalizerTest {

    @Test
    public void removesDiacriticsAndTatweel() {
        assertEquals("ab", ArabicNormalizer.normalize("aَbـ"));
    }

    @Test
    public void unifiesAlefAndTaMarbutaAndYaa() {
        assertEquals("اااهي",
                ArabicNormalizer.normalize("أإآةى"));
    }

    @Test
    public void collapsesWhitespaceAndTrims() {
        assertEquals("a b", ArabicNormalizer.normalize("  a   b  "));
    }

    @Test
    public void nullBecomesEmpty() {
        assertEquals("", ArabicNormalizer.normalize(null));
    }
}
