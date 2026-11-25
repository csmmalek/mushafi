package com.csm.mushafi.mushaf;

import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.List;

/**
 * أسماء السور وخريطة الآيات إلى صفحات المصحف (604 صفحة).
 * البيانات من assets/mushaf-meta.json، ولا تتضمن أي نص قرآني؛ النص يأتي من الملف الموثّق فقط.
 */
public final class MushafMeta {

    public static final int PAGES = 604;
    public static final int SURAHS = 114;

    private final String[] names;
    private final int[][] pages;
    private final List<List<int[]>> byPage;

    private MushafMeta(String[] names, int[][] pages) {
        this.names = names;
        this.pages = pages;
        this.byPage = new ArrayList<>(PAGES + 1);
        for (int p = 0; p <= PAGES; p++) {
            byPage.add(new ArrayList<>());
        }
        for (int s = 1; s <= SURAHS; s++) {
            for (int a = 1; a <= pages[s - 1].length; a++) {
                byPage.get(pages[s - 1][a - 1]).add(new int[]{s, a});
            }
        }
    }

    private static final class Raw {
        String[] names;
        int[][] pages;
    }

    /** يقرأ البيانات ويرفضها إن كانت ناقصة أو غير مرتبة. */
    public static MushafMeta parse(String json) {
        Raw raw = new Gson().fromJson(json, Raw.class);
        if (raw == null || raw.names == null || raw.pages == null
                || raw.names.length != SURAHS || raw.pages.length != SURAHS) {
            throw new IllegalArgumentException("mushaf meta must hold 114 surahs");
        }
        int previous = 1;
        boolean[] seen = new boolean[PAGES + 1];
        for (int s = 0; s < SURAHS; s++) {
            if (raw.names[s] == null || raw.names[s].isEmpty() || raw.pages[s].length == 0) {
                throw new IllegalArgumentException("surah " + (s + 1) + " is incomplete");
            }
            for (int page : raw.pages[s]) {
                if (page < 1 || page > PAGES || page < previous) {
                    throw new IllegalArgumentException("bad page " + page + " in surah " + (s + 1));
                }
                previous = page;
                seen[page] = true;
            }
        }
        for (int p = 1; p <= PAGES; p++) {
            if (!seen[p]) {
                throw new IllegalArgumentException("page " + p + " has no ayah");
            }
        }
        return new MushafMeta(raw.names, raw.pages);
    }

    public String name(int surah) {
        return names[surah - 1];
    }

    public int ayahCount(int surah) {
        return pages[surah - 1].length;
    }

    public int pageOf(int surah, int ayah) {
        return pages[surah - 1][ayah - 1];
    }

    /** آيات الصفحة بالترتيب، كل عنصر {سورة، آية}. */
    public List<int[]> ayahsOnPage(int page) {
        return byPage.get(page);
    }
}
