package com.csm.mushafi.audio;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Test;

import java.util.Arrays;

/** بيانات الاختبار تركيبية (أسماء وروابط وهمية) لاختبار المنطق فقط، وليست نصاً قرآنياً ولا بيانات قراء حقيقية. */
public class ReciterCatalogueTest {

    private static final String SAMPLE = "{\"reciters\":["
            + "{\"id\":1,\"name\":\"قارئ أ\",\"moshaf\":["
            + "{\"id\":10,\"name\":\"رواية 1\",\"rewaya_id\":1,\"server\":\"https://example.test/a/\",\"surah_list\":\"1,2,3,2\"}]},"
            + "{\"id\":2,\"name\":\"قارئ بخادم غير آمن\",\"moshaf\":["
            + "{\"id\":20,\"name\":\"رواية\",\"server\":\"http://example.test/b/\",\"surah_list\":\"1\"}]},"
            + "{\"id\":3,\"name\":\"بلا سور\",\"moshaf\":["
            + "{\"id\":30,\"name\":\"رواية\",\"server\":\"https://example.test/c/\",\"surah_list\":\"\"}]},"
            + "{\"name\":\"بلا معرف\",\"moshaf\":[]}"
            + "]}";

    @Test
    public void keepsOnlyValidRecitersAndCountsSkipped() throws Exception {
        ReciterCatalogue.Result r = ReciterCatalogue.parse(SAMPLE);
        assertEquals(1, r.reciters.size());
        assertEquals(3, r.skipped);
        assertEquals("قارئ أ", r.reciters.get(0).name);
    }

    @Test
    public void surahListIsDeduplicatedAndKeepsOrder() throws Exception {
        ReciterCatalogue.Result r = ReciterCatalogue.parse(SAMPLE);
        ReciterCatalogue.Moshaf m = r.reciters.get(0).moshafs.get(0);
        assertEquals(Arrays.asList(1, 2, 3), m.surahs);
        assertEquals("https://example.test/a/", m.server);
    }

    @Test
    public void parseSurahListRejectsOutOfRangeAndGarbage() {
        assertEquals(Arrays.asList(5, 114), ReciterCatalogue.parseSurahList("0,5,115,x,114,5"));
        assertTrue(ReciterCatalogue.parseSurahList(null).isEmpty());
    }

    @Test
    public void invalidJsonIsReported() {
        try {
            ReciterCatalogue.parse("<html>301</html>");
            fail("non-JSON must be rejected");
        } catch (ReciterCatalogue.InvalidCatalogueException e) {
            assertTrue(e.getMessage().contains("صالحة"));
        }
    }

    @Test
    public void missingRecitersArrayIsReported() {
        try {
            ReciterCatalogue.parse("{\"other\":[]}");
            fail("missing reciters array must be rejected");
        } catch (ReciterCatalogue.InvalidCatalogueException e) {
            assertTrue(e.getMessage().contains("قائمة القراء"));
        }
    }

    @Test
    public void audioUrlIsBuiltFromServerAndSurahOnly() {
        assertEquals("https://example.test/a/001.mp3", AudioUrls.surahUrl("https://example.test/a/", 1));
        assertEquals("https://example.test/a/114.mp3", AudioUrls.surahUrl("https://example.test/a/", 114));
    }

    @Test
    public void unsafeServersAreRejected() {
        assertFalse(AudioUrls.isAcceptableServer("http://example.test/a/"));
        assertFalse(AudioUrls.isAcceptableServer("https://example.test/a"));
        assertFalse(AudioUrls.isAcceptableServer(null));
    }

    @Test(expected = IllegalArgumentException.class)
    public void surahOutsideRangeIsRejected() {
        AudioUrls.surahUrl("https://example.test/a/", 115);
    }
}
