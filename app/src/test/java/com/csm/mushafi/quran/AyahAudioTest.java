package com.csm.mushafi.quran;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.csm.mushafi.audio.AyahAudio;
import com.csm.mushafi.mushaf.MushafMeta;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

public class AyahAudioTest {

    private static String read(String path) throws Exception {
        return new String(Files.readAllBytes(Paths.get(path)), StandardCharsets.UTF_8);
    }

    @Test
    public void urlsUseSixDigitFileNames() {
        assertEquals("https://everyayah.com/data/Alafasy_64kbps/002255.mp3",
                AyahAudio.ayahUrl("Alafasy_64kbps", 2, 255));
        assertEquals("https://everyayah.com/data/Alafasy_64kbps/001001.mp3",
                AyahAudio.ayahUrl("Alafasy_64kbps", 1, 1));
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsUnsafeFolder() {
        AyahAudio.ayahUrl("../evil", 1, 1);
    }

    @Test
    public void recitersFileIsValid() throws Exception {
        List<AyahAudio.Reciter> list = AyahAudio.parse(read("src/main/assets/reader-reciters.json"));
        assertTrue(list.size() >= 3);
        for (AyahAudio.Reciter r : list) {
            AyahAudio.ayahUrl(r.folder, 1, 1);
        }
    }

    @Test
    public void playlistCrossesSurahBoundaryAndStopsAtEnd() throws Exception {
        MushafMeta meta = MushafMeta.parse(read("src/main/assets/mushaf-meta.json"));
        List<int[]> p = AyahAudio.playlist(meta, 1, 6, 4);
        assertEquals(4, p.size());
        assertEquals(1, p.get(1)[0]);
        assertEquals(7, p.get(1)[1]);
        assertEquals(2, p.get(2)[0]);
        assertEquals(1, p.get(2)[1]);
        List<int[]> tail = AyahAudio.playlist(meta, 114, 5, 50);
        assertEquals(2, tail.size());
    }
}
