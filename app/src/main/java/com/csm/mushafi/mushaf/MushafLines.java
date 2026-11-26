package com.csm.mushafi.mushaf;

import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.List;

/**
 * تقسيم صفحات مصحف المدينة إلى أسطر (15 سطراً في كل صفحة، و8 في الأولى والثانية).
 * البيانات من assets/mushaf-lines.json وهي مواضع الكلمات فقط (سورة، آية، من كلمة إلى كلمة)
 * دون أي نص؛ النص القرآني يبقى من الملف الموثّق. تُبنى بأداة tools/layout/build_lines.py.
 */
public final class MushafLines {

    public static final char HEADER = 'h';
    public static final char BASMALA = 'b';
    public static final char WORDS = 'w';

    /** مقطع من آية داخل سطر: الكلمات [from, to) بحسب كلمات النص بعد حذف البسملة. */
    public static final class Seg {
        public final int surah;
        public final int ayah;
        public final int from;
        public final int to;

        Seg(int[] v) {
            this.surah = v[0];
            this.ayah = v[1];
            this.from = v[2];
            this.to = v[3];
        }
    }

    public static final class Line {
        public final char kind;
        public final int surah;
        public final boolean centered;
        public final List<Seg> segs;

        Line(Raw.RawLine r) {
            this.kind = r.k.charAt(0);
            this.surah = r.s;
            this.centered = r.c == 1;
            this.segs = new ArrayList<>();
            if (r.g != null) {
                for (int[] g : r.g) {
                    segs.add(new Seg(g));
                }
            }
        }
    }

    private static final class Raw {
        static final class RawLine {
            String k;
            int s;
            int c;
            int[][] g;
        }

        int v;
        List<List<RawLine>> pages;
    }

    private final List<List<Line>> pages = new ArrayList<>();

    private MushafLines(Raw raw) {
        for (List<Raw.RawLine> page : raw.pages) {
            List<Line> lines = new ArrayList<>();
            for (Raw.RawLine r : page) {
                lines.add(new Line(r));
            }
            pages.add(lines);
        }
    }

    /**
     * يحذف البسملة الملصوقة بأول آية (كل السور عدا الفاتحة والتوبة). يقارن حروف أول كلمة دون
     * الحركات لأن النص في سورتي التين والقدر يبدأ بـ«بِّسْمِ» بحركات مختلفة.
     */
    public static String stripBasmala(int surah, int ayah, String text) {
        if (ayah != 1 || surah == 1 || surah == 9) {
            return text;
        }
        String[] words = text.trim().split("\\s+");
        if (words.length <= 4) {
            return text;
        }
        StringBuilder letters = new StringBuilder();
        for (char c : words[0].toCharArray()) {
            if (Character.getType(c) != Character.NON_SPACING_MARK && c != '\u0640') {
                letters.append(c);
            }
        }
        if (!letters.toString().equals("بسم")) {
            return text;
        }
        StringBuilder out = new StringBuilder();
        for (int i = 4; i < words.length; i++) {
            if (out.length() > 0) {
                out.append(' ');
            }
            out.append(words[i]);
        }
        return out.toString();
    }

    public static MushafLines parse(String json) {
        Raw raw = new Gson().fromJson(json, Raw.class);
        if (raw == null || raw.pages == null || raw.pages.size() != MushafMeta.PAGES) {
            throw new IllegalArgumentException("mushaf lines must hold 604 pages");
        }
        return new MushafLines(raw);
    }

    /** أسطر الصفحة (1..604). */
    public List<Line> page(int page) {
        return pages.get(page - 1);
    }
}
