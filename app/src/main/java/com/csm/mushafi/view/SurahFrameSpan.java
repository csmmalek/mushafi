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
    private final String order;
    private final String ayahs;
    private final FitPageTextView host;
    private final PageTheme theme;

    public SurahFrameSpan(String name, String order, String ayahs, FitPageTextView host,
                          PageTheme theme) {
        this.name = name;
        this.order = order;
        this.ayahs = ayahs;
        this.host = host;
        this.theme = theme;
    }

    @Override
    public int getSize(@NonNull Paint paint, CharSequence text, int start, int end,
                       @Nullable Paint.FontMetricsInt fm) {
        if (fm != null) {
            int pad = (int) (paint.getTextSize() * 0.42f);
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
        float w = Math.max(1, host.contentWidth() - 6);
        float t = top + size * 0.1f;
        float b = bottom - size * 0.1f;
        float h = b - t;
        float left = x + 3;
        float right = left + w;
        RectF box = new RectF(left, t, right, b);
        float stroke = Math.max(2f, h * 0.045f);
        float cy = (t + b) / 2f;

        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setStyle(Paint.Style.FILL);
        p.setColor(theme.frame);
        canvas.drawRect(box, p);

        // شبكة معينات زخرفية على الجانبين
        float r = h * 0.36f;
        float cxR = right - h * 0.95f;
        float cxL = left + h * 0.95f;
        p.setColor(theme.ring);
        float d = h * 0.11f;
        float step = d * 2.6f;
        for (float gx = left + h * 0.22f; gx < right - h * 0.2f; gx += step) {
            boolean sideZone = gx < left + h * 1.9f || gx > right - h * 1.9f;
            if (!sideZone) {
                continue;
            }
            int row = 0;
            for (float gy = t + h * 0.2f + (row % 2) * step / 2; gy < b - h * 0.15f; gy += step) {
                float ox = (Math.round((gy - t) / step) % 2 == 0) ? 0 : step / 2;
                float px = gx + ox;
                if (Math.hypot(px - cxR, gy - cy) < r + d * 2.2f
                        || Math.hypot(px - cxL, gy - cy) < r + d * 2.2f) {
                    continue;
                }
                if (px > right - h * 0.12f || px < left + h * 0.12f) {
                    continue;
                }
                Path diamond = new Path();
                diamond.moveTo(px, gy - d);
                diamond.lineTo(px + d, gy);
                diamond.lineTo(px, gy + d);
                diamond.lineTo(px - d, gy);
                diamond.close();
                canvas.drawPath(diamond, p);
            }
        }

        // إطاران: خارجي سميك وداخلي رفيع
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(stroke);
        canvas.drawRect(box, p);
        float gap = stroke * 2.4f;
        p.setStrokeWidth(Math.max(1f, stroke * 0.45f));
        canvas.drawRect(new RectF(left + gap, t + gap, right - gap, b - gap), p);

        // اللوحة الوسطى التي يُكتب فيها اسم السورة
        float cartL = cxL + r + d;
        float cartR = cxR - r - d;
        RectF cart = new RectF(cartL, t + h * 0.14f, cartR, b - h * 0.14f);
        p.setStyle(Paint.Style.FILL);
        p.setColor(theme.paper);
        canvas.drawRoundRect(cart, cart.height() / 2f, cart.height() / 2f, p);
        p.setStyle(Paint.Style.STROKE);
        p.setColor(theme.ring);
        p.setStrokeWidth(stroke * 0.8f);
        canvas.drawRoundRect(cart, cart.height() / 2f, cart.height() / 2f, p);

        Paint label = new Paint(paint);
        label.setColor(theme.ink);
        label.setTextAlign(Paint.Align.CENTER);
        label.setFakeBoldText(true);
        label.setTextSize(h * 0.5f);
        float available = cart.width() - cart.height() * 0.9f;
        float measured = label.measureText(name);
        if (available > 0 && measured > available) {
            label.setTextSize(label.getTextSize() * available / measured);
        }
        Paint.FontMetrics m = label.getFontMetrics();
        canvas.drawText(name, cart.centerX(), cy - (m.ascent + m.descent) / 2f, label);

        // دائرتان: ترتيب السورة (يمين) وعدد آياتها (يسار)
        drawBadge(canvas, cxR, cy, r, stroke, "ترتيبها", order, paint);
        drawBadge(canvas, cxL, cy, r, stroke, "آياتها", ayahs, paint);
    }

    private void drawBadge(Canvas canvas, float cx, float cy, float r, float stroke,
                           String caption, String value, Paint base) {
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setStyle(Paint.Style.FILL);
        p.setColor(theme.paper);
        canvas.drawCircle(cx, cy, r, p);
        p.setStyle(Paint.Style.STROKE);
        p.setColor(theme.ring);
        p.setStrokeWidth(stroke * 0.8f);
        canvas.drawCircle(cx, cy, r, p);
        canvas.drawCircle(cx, cy, r - stroke * 1.6f, p);

        Paint label = new Paint(base);
        label.setColor(theme.ink);
        label.setTextAlign(Paint.Align.CENTER);
        label.setFakeBoldText(false);
        label.setTextSize(r * 0.42f);
        float maxW = r * 1.5f;
        float cw = label.measureText(caption);
        if (cw > maxW) {
            label.setTextSize(label.getTextSize() * maxW / cw);
        }
        canvas.drawText(caption, cx, cy - r * 0.18f, label);
        label.setTextSize(r * 0.62f);
        label.setFakeBoldText(true);
        float vw = label.measureText(value);
        if (vw > maxW) {
            label.setTextSize(label.getTextSize() * maxW / vw);
        }
        canvas.drawText(value, cx, cy + r * 0.62f, label);
    }
}
