package com.csm.mushafi.audio;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.ArrayList;
import java.util.List;

/**
 * يحلل استجابة MP3Quran API v3 (reciters). يتجاهل أي عنصر غير صالح ولا يفترض أي رابط ثابت.
 * كل رابط صوت يُبنى لاحقاً من خادم القارئ ورقم السورة فقط.
 */
public final class ReciterCatalogue {

    private ReciterCatalogue() {
    }

    public static final class Moshaf {
        public final int id;
        public final String name;
        public final int rewayaId;
        public final String server;
        public final List<Integer> surahs;

        Moshaf(int id, String name, int rewayaId, String server, List<Integer> surahs) {
            this.id = id;
            this.name = name;
            this.rewayaId = rewayaId;
            this.server = server;
            this.surahs = surahs;
        }
    }

    public static final class Reciter {
        public final int id;
        public final String name;
        public final List<Moshaf> moshafs;

        Reciter(int id, String name, List<Moshaf> moshafs) {
            this.id = id;
            this.name = name;
            this.moshafs = moshafs;
        }
    }

    public static final class Result {
        public final List<Reciter> reciters;
        public final int skipped;

        Result(List<Reciter> reciters, int skipped) {
            this.reciters = reciters;
            this.skipped = skipped;
        }
    }

    /** يرمى إذا كانت الاستجابة نفسها غير صالحة (ليست JSON أو لا تحتوي قائمة). */
    public static final class InvalidCatalogueException extends Exception {
        public InvalidCatalogueException(String message) {
            super(message);
        }
    }

    public static Result parse(String json) throws InvalidCatalogueException {
        JsonElement root;
        try {
            root = JsonParser.parseString(json);
        } catch (RuntimeException e) {
            throw new InvalidCatalogueException("استجابة الفهرس ليست بصيغة صالحة.");
        }
        if (!root.isJsonObject() || !root.getAsJsonObject().has("reciters")
                || !root.getAsJsonObject().get("reciters").isJsonArray()) {
            throw new InvalidCatalogueException("الفهرس لا يحتوي قائمة القراء المتوقعة.");
        }
        JsonArray items = root.getAsJsonObject().getAsJsonArray("reciters");
        List<Reciter> reciters = new ArrayList<>();
        int skipped = 0;
        for (JsonElement el : items) {
            Reciter r = parseReciter(el);
            if (r == null) {
                skipped++;
            } else {
                reciters.add(r);
            }
        }
        return new Result(reciters, skipped);
    }

    private static Reciter parseReciter(JsonElement el) {
        if (!el.isJsonObject()) {
            return null;
        }
        JsonObject o = el.getAsJsonObject();
        int id = intOf(o, "id", -1);
        String name = stringOf(o, "name");
        if (id < 0 || name.isEmpty() || !o.has("moshaf") || !o.get("moshaf").isJsonArray()) {
            return null;
        }
        List<Moshaf> moshafs = new ArrayList<>();
        for (JsonElement m : o.getAsJsonArray("moshaf")) {
            Moshaf parsed = parseMoshaf(m);
            if (parsed != null) {
                moshafs.add(parsed);
            }
        }
        if (moshafs.isEmpty()) {
            return null;
        }
        return new Reciter(id, name, moshafs);
    }

    private static Moshaf parseMoshaf(JsonElement el) {
        if (!el.isJsonObject()) {
            return null;
        }
        JsonObject o = el.getAsJsonObject();
        int id = intOf(o, "id", -1);
        String name = stringOf(o, "name");
        String server = stringOf(o, "server");
        if (id < 0 || name.isEmpty() || !AudioUrls.isAcceptableServer(server)) {
            return null;
        }
        List<Integer> surahs = parseSurahList(stringOf(o, "surah_list"));
        if (surahs.isEmpty()) {
            return null;
        }
        return new Moshaf(id, name, intOf(o, "rewaya_id", 0), server, surahs);
    }

    /** "1,2,3" إلى قائمة أرقام بين 1 و114، مع إزالة التكرار والقيم الخاطئة. */
    static List<Integer> parseSurahList(String raw) {
        List<Integer> out = new ArrayList<>();
        if (raw == null || raw.isEmpty()) {
            return out;
        }
        for (String part : raw.split(",")) {
            try {
                int n = Integer.parseInt(part.trim());
                if (n >= 1 && n <= 114 && !out.contains(n)) {
                    out.add(n);
                }
            } catch (NumberFormatException ignored) {
                // قيمة غير رقمية تُتجاهل
            }
        }
        return out;
    }

    private static int intOf(JsonObject o, String key, int fallback) {
        JsonElement e = o.get(key);
        if (e == null || !e.isJsonPrimitive() || !e.getAsJsonPrimitive().isNumber()) {
            return fallback;
        }
        return e.getAsInt();
    }

    private static String stringOf(JsonObject o, String key) {
        JsonElement e = o.get(key);
        return e == null || !e.isJsonPrimitive() ? "" : e.getAsString().trim();
    }
}
