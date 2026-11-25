package com.csm.mushafi.view;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.text.style.ReplacementSpan;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/** راية السورة: إطار مزخرف بعرض السطر مع اسم السورة في الوسط. */
public final class SurahFrameSpan extends ReplacementSpan {

    private final String name;
    private final FitPageTextView host;
    private final PageTheme theme;

    public SurahFrameSpan(String name, FitPageTextView host, PageTheme theme) {
        this.name = name;
        this.host = host;
        this.theme = theme;
    }

    @Override
    public int getSize(@NonNull Paint paint, CharSequence text, int start, int end,
                       @Nullable Paint.FontMetricsInt fm) {
        if (fm != null) {
            int pad = (int) (paint.getTextSize() * 0.45f);
            fm.ascent -= pad;
            fm.top -= pad;
            fm.descent += pad;
            fm.bottom += pad;
        }
        return Math.max(1, host.contentWidth() - 2);
    }

    @Override
    public void draw(@NonNull Canvas canvas, CharSequence text, int start, int end, float x,
                     int top, int y, int bottom, @NonNull Paint paint) {
        float size = paint.getTextSize();
        float w = Math.max(1, host.contentWidth() - 2);
        float t = top + size * 0.15f;
        float b = bottom - size * 0.15f;
        float h = b - t;
        RectF box = new RectF(x + 2, t, x + w, b);
        float radius = h * 0.30f;
        float stroke = Math.max(2f, size * 0.09f);

        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setStyle(Paint.Style.FILL);
        p.setColor(theme.frame);
        canvas.drawRoundRect(box, radius, radius, p);

        p.setStyle(Paint.Style.STROKE);
        p.setColor(theme.ring);
        p.setStrokeWidth(stroke);
        canvas.drawRoundRect(box, radius, radius, p);

        float gap = stroke * 2.2f;
        RectF inner = new RectF(box.left + gap, box.top + gap, box.right - gap, box.bottom - gap);
        p.setStrokeWidth(Math.max(1f, stroke * 0.4f));
        canvas.drawRoundRect(inner, radius * 0.7f, radius * 0.7f, p);

        // زخارف طرفي الراية: معيّن تحيط به نقطتان
        float cy = (t + b) / 2f;
        float s = h * 0.17f;
        p.setStyle(Paint.Style.FILL);
        for (int side = -1; side <= 1; side += 2) {
            float cx = side < 0 ? box.left + h * 0.85f : box.right - h * 0.85f;
            Path diamond = new Path();
            diamond.moveTo(cx, cy - s * 1.5f);
            diamond.lineTo(cx + s * 1.5f, cy);
            diamond.lineTo(cx, cy + s * 1.5f);
            diamond.lineTo(cx - s * 1.5f, cy);
            diamond.close();
            canvas.drawPath(diamond, p);
            canvas.drawCircle(cx - s * 3f, cy, s * 0.55f, p);
            canvas.drawCircle(cx + s * 3f, cy, s * 0.55f, p);
        }

        Paint label = new Paint(paint);
        label.setColor(0xFFF0D98C);
        label.setTextAlign(Paint.Align.CENTER);
        label.setFakeBoldText(true);
        Paint.FontMetrics metrics = label.getFontMetrics();
        canvas.drawText(name, box.centerX(), cy - (metrics.ascent + metrics.descent) / 2f, label);
    }
}
