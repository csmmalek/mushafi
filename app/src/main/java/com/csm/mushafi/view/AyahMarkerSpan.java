package com.csm.mushafi.view;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.text.style.ReplacementSpan;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/** ميدالية رقم الآية: حلقة بتويجات ذهبية يتوسطها الرقم. الرقم يأتي من بيانات الآية فقط. */
public final class AyahMarkerSpan extends ReplacementSpan {

    private final String number;
    private final PageTheme theme;

    public AyahMarkerSpan(String number, PageTheme theme) {
        this.number = number;
        this.theme = theme;
    }

    @Override
    public int getSize(@NonNull Paint paint, CharSequence text, int start, int end,
                       @Nullable Paint.FontMetricsInt fm) {
        return (int) (paint.getTextSize() * 1.5f);
    }

    @Override
    public void draw(@NonNull Canvas canvas, CharSequence text, int start, int end, float x,
                     int top, int y, int bottom, @NonNull Paint paint) {
        float size = paint.getTextSize();
        float cx = x + size * 0.75f;
        float cy = y - size * 0.32f;
        float r = size * 0.60f;

        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setStyle(Paint.Style.FILL);
        p.setColor(theme.ring);
        for (int i = 0; i < 12; i++) {
            double a = Math.PI * 2 * i / 12;
            canvas.drawCircle(cx + (float) Math.cos(a) * r * 0.88f,
                    cy + (float) Math.sin(a) * r * 0.88f, r * 0.2f, p);
        }
        p.setColor(theme.markerFill);
        canvas.drawCircle(cx, cy, r * 0.8f, p);
        p.setStyle(Paint.Style.STROKE);
        p.setColor(theme.ring);
        p.setStrokeWidth(Math.max(1.5f, size * 0.05f));
        canvas.drawCircle(cx, cy, r * 0.8f, p);
        p.setStrokeWidth(Math.max(1f, size * 0.025f));
        canvas.drawCircle(cx, cy, r * 0.62f, p);

        Paint label = new Paint(paint);
        label.setColor(theme.markerText);
        label.setTextAlign(Paint.Align.CENTER);
        label.setFakeBoldText(false);
        int len = number.length();
        label.setTextSize(r * (len == 1 ? 1.05f : len == 2 ? 0.9f : 0.72f));
        Paint.FontMetrics m = label.getFontMetrics();
        canvas.drawText(number, cx, cy - (m.ascent + m.descent) / 2f, label);
    }
}
