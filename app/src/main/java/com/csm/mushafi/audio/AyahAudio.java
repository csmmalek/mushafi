package com.csm.mushafi.audio;

import com.csm.mushafi.mushaf.MushafMeta;
import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * تلاوة آية بآية للقارئ داخل صفحة المصحف: رابط ملف كل آية، وقائمة التشغيل من آية معينة.
 * لا يوجد هنا أي نص قرآني ولا بيانات قرّاء مكتوبة من الذاكرة؛ القرّاء من assets/reader-reciters.json.
 */
public final class AyahAudio {

    private static final String BASE = "https://everyayah.com/data/";
    private static final Pattern FOLDER = Pattern.compile("[A-Za-z0-9_.-]{3,64}");

    private AyahAudio() {
    }

    public static final class Reciter {
        public final String id;
        public final String name;
        public final String folder;

        Reciter(String id, String name, String folder) {
            this.id = id;
            this.name = name;
            this.folder = folder;
        }
    }

    private static final class Raw {
        List<RawReciter> reciters;
    }

    private static final class RawReciter {
        String id;
        String name;
        String folder;
    }

    public static List<Reciter> parse(String json) {
        Raw raw = new Gson().fromJson(json, Raw.class);
        if (raw == null || raw.reciters == null || raw.reciters.isEmpty()) {
            throw new IllegalArgumentException("قائمة القرّاء فارغة");
        }
        List<Reciter> out = new ArrayList<>();
        for (RawReciter r : raw.reciters) {
            if (r.id == null || r.name == null || r.folder == null
                    || !FOLDER.matcher(r.folder).matches()) {
                throw new IllegalArgumentException("بيانات قارئ غير صالحة");
            }
            out.add(new Reciter(r.id, r.name, r.folder));
        }
        return out;
    }

    public static String ayahUrl(String folder, int surah, int ayah) {
        if (folder == null || !FOLDER.matcher(folder).matches()) {
            throw new IllegalArgumentException("مجلد القارئ غير صالح");
        }
        if (surah < 1 || surah > MushafMeta.SURAHS || ayah < 1 || ayah > 286) {
            throw new IllegalArgumentException("آية خارج النطاق");
        }
        return BASE + folder + "/" + String.format(Locale.ROOT, "%03d%03d.mp3", surah, ayah);
    }

    /** الآيات من (surah, ayah) فصاعداً، عبر حدود السور، بحد أقصى max آية. */
    public static List<int[]> playlist(MushafMeta meta, int surah, int ayah, int max) {
        List<int[]> out = new ArrayList<>();
        int s = surah;
        int a = ayah;
        while (s <= MushafMeta.SURAHS && out.size() < max) {
            out.add(new int[]{s, a});
            a++;
            if (a > meta.ayahCount(s)) {
                s++;
                a = 1;
            }
        }
        return out;
    }
}
