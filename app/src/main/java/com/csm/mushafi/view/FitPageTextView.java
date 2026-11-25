package com.csm.mushafi.view;

import android.content.Context;
import android.graphics.Color;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextDirectionHeuristics;
import android.text.TextPaint;
import android.text.method.LinkMovementMethod;
import android.util.AttributeSet;
import android.util.TypedValue;

import androidx.appcompat.widget.AppCompatTextView;

/**
 * نص صفحة يملأ ارتفاعها بالكامل: يبحث عن أكبر حجم خط يتسع للنص، ثم يوزّع ما تبقّى من الارتفاع
 * على الأسطر ليمتلئ كل الفراغ. لا يغيّر النص ولا ترتيبه.
 */
public class FitPageTextView extends AppCompatTextView {

    private static final float LINE_MULT = 1.12f;

    private float minPx;
    private float maxPx;
    private boolean fitting;

    public FitPageTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
        float density = getResources().getDisplayMetrics().scaledDensity;
        minPx = 10 * density;
        maxPx = 22 * density;
        setMovementMethod(LinkMovementMethod.getInstance());
        setHighlightColor(Color.TRANSPARENT);
    }

    /** عرض منطقة النص، تستعمله رايات السور لتملأ السطر. */
    public int contentWidth() {
        return Math.max(0, getWidth() - getPaddingLeft() - getPaddingRight());
    }

    @Override
    public void setText(CharSequence text, BufferType type) {
        super.setText(text, type);
        if (!fitting) {
            fit();
        }
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        fit();
    }

    private Layout build(CharSequence text, float px, float extra, int width) {
        TextPaint paint = new TextPaint(getPaint());
        paint.setTextSize(px);
        return StaticLayout.Builder.obtain(text, 0, text.length(), paint, width)
                .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                .setTextDirection(TextDirectionHeuristics.RTL)
                .setLineSpacing(extra, LINE_MULT)
                .setJustificationMode(Layout.JUSTIFICATION_MODE_INTER_WORD)
                .setBreakStrategy(getBreakStrategy())
                .setHyphenationFrequency(Layout.HYPHENATION_FREQUENCY_NONE)
                .setIncludePad(getIncludeFontPadding())
                .build();
    }

    private void fit() {
        CharSequence text = getText();
        int width = contentWidth();
        int height = getHeight() - getPaddingTop() - getPaddingBottom();
        if (maxPx <= 0 || text == null || text.length() == 0 || width <= 0 || height <= 0) {
            return;
        }
        float lo = minPx;
        float hi = maxPx;
        if (build(text, hi, 0, width).getHeight() <= height) {
            lo = hi;
        } else {
            for (int i = 0; i < 10; i++) {
                float mid = (lo + hi) / 2f;
                if (build(text, mid, 0, width).getHeight() <= height) {
                    lo = mid;
                } else {
                    hi = mid;
                }
            }
        }
        Layout layout = build(text, lo, 0, width);
        int lines = layout.getLineCount();
        float extra = 0;
        if (lines > 1) {
            extra = (height - layout.getHeight()) / (float) (lines - 1);
            extra = Math.max(0, Math.min(extra, lo * 0.55f));
            Layout spaced = build(text, lo, extra, width);
            if (spaced.getHeight() > height) {
                extra = Math.max(0, extra - (spaced.getHeight() - height) / (float) (lines - 1));
            }
        }
        fitting = true;
        setTextSize(TypedValue.COMPLEX_UNIT_PX, lo);
        setLineSpacing(extra, LINE_MULT);
        fitting = false;
    }
}
