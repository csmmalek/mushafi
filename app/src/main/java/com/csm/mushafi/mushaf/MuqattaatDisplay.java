package com.csm.mushafi.mushaf;

import com.google.gson.Gson;

import java.util.HashMap;
import java.util.Map;

/**
 * عرض الحروف المقطعة بعلامات المد العثمانية (مثل الٓمٓ). هذا استبدال للعرض فقط؛ كل عنصر
 * تُثبَّت مطابقته لحروف النص الموثّق باختبار وحدة، وأي عدم تطابق يترك النص الأصلي كما هو.
 */
public final class MuqattaatDisplay {

    private static final class Entry {
        int s;
        int a;
        String from;
        String to;
    }

    private final Map<Integer, Entry> entries = new HashMap<>();

    public static MuqattaatDisplay parse(String json) {
        Entry[] list = new Gson().fromJson(json, Entry[].class);
        MuqattaatDisplay display = new MuqattaatDisplay();
        for (Entry e : list) {
            display.entries.put(e.s * 1000 + e.a, e);
        }
        return display;
    }

    public int size() {
        return entries.size();
    }

    /** يرجع {سورة، آية، الأصل، البديل} لكل عنصر، لاستعماله في الاختبارات. */
    public String[][] all() {
        String[][] out = new String[entries.size()][];
        int i = 0;
        for (Entry e : entries.values()) {
            out[i++] = new String[]{String.valueOf(e.s), String.valueOf(e.a), e.from, e.to};
        }
        return out;
    }

    /** يستبدل أول كلمة فقط إذا طابقت الأصل كلمةً كاملة، وإلا يرجع النص كما هو. */
    public String apply(int surah, int ayah, String text) {
        Entry e = entries.get(surah * 1000 + ayah);
        if (e == null || !text.startsWith(e.from)) {
            return text;
        }
        int end = e.from.length();
        if (text.length() > end && text.charAt(end) != ' ') {
            return text;
        }
        return e.to + text.substring(end);
    }
}
