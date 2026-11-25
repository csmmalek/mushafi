package com.csm.mushafi.view;

/** ألوان صفحة المصحف: ورقي، أبيض، رملي، ليلي. */
public final class PageTheme {
    public final int paper;
    public final int ink;
    public final int frame;
    public final int ring;
    public final int markerFill;
    public final int markerText;
    public final int basmala;
    public final int highlight;

    private PageTheme(int paper, int ink, int frame, int ring, int markerFill, int markerText,
                      int basmala, int highlight) {
        this.paper = paper;
        this.ink = ink;
        this.frame = frame;
        this.ring = ring;
        this.markerFill = markerFill;
        this.markerText = markerText;
        this.basmala = basmala;
        this.highlight = highlight;
    }

    public static final PageTheme[] ALL = {
            new PageTheme(0xFFF5EDD6, 0xFF1B1B17, 0xFF14583F, 0xFFB08A2E, 0xFFFBF5E3, 0xFF14583F, 0xFF0E4D3A, 0x55C9A24A),
            new PageTheme(0xFFFFFFFF, 0xFF111111, 0xFF14583F, 0xFFB08A2E, 0xFFFFFFFF, 0xFF14583F, 0xFF0E4D3A, 0x44C9A24A),
            new PageTheme(0xFFE8D9B5, 0xFF2A2112, 0xFF6B4F1D, 0xFF8A6A25, 0xFFF3E7C8, 0xFF3A2C12, 0xFF3A2C12, 0x66A07C1D),
            new PageTheme(0xFF0B100E, 0xFFE9E4D3, 0xFF1F4A38, 0xFFD8B65A, 0xFF14221C, 0xFFE3C77A, 0xFFE3C77A, 0x55E3C77A),
    };

    public static PageTheme of(int index) {
        return ALL[Math.max(0, Math.min(ALL.length - 1, index))];
    }
}
