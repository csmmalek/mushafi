package com.csm.mushafi.view;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.text.style.ReplacementSpan;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/** إطار السورة الأصلي (خط مجمع الملك فهد) باسم السورة مشكولاً، مع دائرتي الترتيب وعدد الآيات. */
public final class SurahHeaderSpan extends ReplacementSpan {

    /** عرض رمز الإطار بوحدة em (8256/2500 في الخط). */
    private static final float GLYPH_EM = 8256f / 2500f;

    private final int surah;
    private final String order;
    private final String ayahs;
    private final Typeface header;
    private final FitPageTextView host;
    private final PageTheme theme;

    public SurahHeaderSpan(int surah, String order, String ayahs, Typeface header,
                           FitPageTextView host, PageTheme theme) {
        this.surah = surah;
        this.order = order;
        this.ayahs = ayahs;
        this.header = header;
        this.host = host;
        this.theme = theme;
    }

    private float glyphSize() {
        return Math.max(1, host.contentWidth() - 4) / GLYPH_EM;
    }

    @Override
    public int getSize(@NonNull Paint paint, CharSequence text, int start, int end,
                       @Nullable Paint.FontMetricsInt fm) {
        if (fm != null) {
            float ts = glyphSize();
            int half = (int) (ts * 0.2f + paint.getTextSize() * 0.35f);
            fm.ascent = -half;
            fm.top = -half;
            fm.descent = half;
            fm.bottom = half;
        }
        return Math.max(1, host.contentWidth() - 2);
    }

    @Override
    public void draw(@NonNull Canvas canvas, CharSequence text, int start, int end, float x,
                     int top, int y, int bottom, @NonNull Paint paint) {
        float ts = glyphSize();
        float cy = (top + bottom) / 2f;
        float left = x + 2;

        Paint glyph = new Paint(Paint.ANTI_ALIAS_FLAG);
        glyph.setTypeface(header);
        glyph.setTextSize(ts);
        glyph.setColor(theme.ink);
        String g = SurahHeaderGlyphs.glyph(surah);
        Rect bounds = new Rect();
        glyph.getTextBounds(g, 0, g.length(), bounds);
        float baseline = cy - (bounds.top + bounds.bottom) / 2f;
        canvas.drawText(g, left, baseline, glyph);

        float frameH = Math.max(1, bounds.height());
        float r = frameH * 0.30f;
        float w = ts * GLYPH_EM;
        drawBadge(canvas, left + w * 0.215f, cy, r, "آياتها", ayahs, paint);
        drawBadge(canvas, left + w * 0.785f, cy, r, "ترتيبها", order, paint);
    }

    private void drawBadge(Canvas canvas, float cx, float cy, float r, String caption,
                           String value, Paint base) {
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setStyle(Paint.Style.FILL);
        p.setColor(theme.paper);
        canvas.drawCircle(cx, cy, r, p);
        p.setStyle(Paint.Style.STROKE);
        p.setColor(theme.ink);
        p.setStrokeWidth(Math.max(1.5f, r * 0.07f));
        canvas.drawCircle(cx, cy, r, p);
        canvas.drawCircle(cx, cy, r * 0.88f, p);

        Paint label = new Paint(base);
        label.setColor(theme.ink);
        label.setTextAlign(Paint.Align.CENTER);
        label.setTypeface(Typeface.create("sans-serif", Typeface.BOLD));
        float maxW = r * 1.5f;
        label.setTextSize(r * 0.38f);
        float cw = label.measureText(caption);
        if (cw > maxW) {
            label.setTextSize(label.getTextSize() * maxW / cw);
        }
        canvas.drawText(caption, cx, cy - r * 0.15f, label);
        label.setTextSize(r * 0.62f);
        float vw = label.measureText(value);
        if (vw > maxW) {
            label.setTextSize(label.getTextSize() * maxW / vw);
        }
        canvas.drawText(value, cx, cy + r * 0.58f, label);
    }
}
