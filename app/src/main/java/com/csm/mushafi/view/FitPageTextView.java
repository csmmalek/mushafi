package com.csm.mushafi.view;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Paint;
import android.text.Layout;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.AlignmentSpan;
import android.text.StaticLayout;
import android.text.TextDirectionHeuristics;
import android.text.TextPaint;
import android.text.method.LinkMovementMethod;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;

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
    private CharSequence source;
    private boolean kashidaEnabled = true;
    private float fontScale = 1f;
    private int viewportHeight;
    private View.OnLayoutChangeListener parentListener;
    private boolean exact;
    private int exactLines;
    private int exactSlots;
    private int exactTop;

    /** مقياس الخط بالنسبة للحجم الذي يملأ الصفحة: أقل من 1 يصغّر، وأكثر من 1 يكبّر مع تمرير عمودي. */
    public void setFontScale(float scale) {
        float clamped = Math.max(0.6f, Math.min(1.6f, scale));
        if (clamped != fontScale) {
            fontScale = clamped;
            fit();
        }
    }

    /**
     * وضع المصحف المطابق: كل سطر من النص (مفصول بـ \n) هو سطر مطبوع في مصحف المدينة، فيُختار أكبر خط
     * يتسع لأعرض سطر دون التفاف، ثم يُوزَّع الباقي مدّاً وتباعداً بين الكلمات ليملأ كل سطر العرض.
     *
     * @param slots عدد خانات الأسطر المتساوية في ارتفاع الصفحة (15، أو أقل للصفحتين الأوليين)
     * @param lines عدد أسطر النص الفعلية
     */
    public void setExactLines(boolean on, int lines, int slots) {
        if (exact != on || exactLines != lines || exactSlots != slots) {
            exact = on;
            exactLines = lines;
            exactSlots = slots;
        }
    }

    public boolean isExact() {
        return exact;
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (getParent() instanceof View) {
            final View parent = (View) getParent();
            parentListener = (v, l, t, r, b, ol, ot, or, ob) -> {
                if (b - t != viewportHeight) {
                    viewportHeight = b - t;
                    fit();
                }
            };
            parent.addOnLayoutChangeListener(parentListener);
            viewportHeight = parent.getHeight();
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        if (parentListener != null && getParent() instanceof View) {
            ((View) getParent()).removeOnLayoutChangeListener(parentListener);
        }
        parentListener = null;
        super.onDetachedFromWindow();
    }

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

    /** تمديد الكلمات (تطويل) لملء الأسطر بدل تباعد الكلمات؛ للعرض فقط ولا يغيّر النص المحفوظ. */
    public void setKashidaEnabled(boolean enabled) {
        if (kashidaEnabled != enabled) {
            kashidaEnabled = enabled;
            fit();
        }
    }

    @Override
    public void setText(CharSequence text, BufferType type) {
        if (!fitting) {
            source = text;
            super.setText(text, BufferType.SPANNABLE);
            fit();
            return;
        }
        super.setText(text, type);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        fit();
    }

    private Layout build(CharSequence text, float px, float extra, int width) {
        return build(text, px, extra, width, true);
    }

    private Layout build(CharSequence text, float px, float extra, int width, boolean justify) {
        TextPaint paint = new TextPaint(getPaint());
        paint.setTextSize(px);
        return StaticLayout.Builder.obtain(text, 0, text.length(), paint, width)
                .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                .setTextDirection(TextDirectionHeuristics.RTL)
                .setLineSpacing(extra, LINE_MULT)
                .setJustificationMode(justify ? Layout.JUSTIFICATION_MODE_INTER_WORD
                        : Layout.JUSTIFICATION_MODE_NONE)
                .setBreakStrategy(getBreakStrategy())
                .setHyphenationFrequency(Layout.HYPHENATION_FREQUENCY_NONE)
                .setIncludePad(getIncludeFontPadding())
                .build();
    }

    private void fit() {
        if (exact) {
            fitExact();
            return;
        }
        CharSequence text = source != null ? source : getText();
        int width = contentWidth();
        int height = (viewportHeight > 0 ? viewportHeight : getHeight()) - getPaddingTop() - getPaddingBottom();
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
        lo = lo * fontScale;
        Layout layout = build(text, lo, 0, width);
        int lines = layout.getLineCount();
        float extra = 0;
        if (fontScale > 1f) {
            setMinHeight(0);
        } else {
            setMinHeight(viewportHeight > 0 ? viewportHeight : getHeight());
        }
        if (lines > 1 && fontScale <= 1f) {
            extra = (height - layout.getHeight()) / (float) (lines - 1);
            extra = Math.max(0, Math.min(extra, lo * 0.9f));
            Layout spaced = build(text, lo, extra, width);
            if (spaced.getHeight() > height) {
                extra = Math.max(0, extra - (spaced.getHeight() - height) / (float) (lines - 1));
            }
        }
        fitting = true;
        setTextSize(TypedValue.COMPLEX_UNIT_PX, lo);
        setLineSpacing(extra, LINE_MULT);
        CharSequence shown = kashidaEnabled ? stretched(text, lo, extra, width) : text;
        if (shown != getText()) {
            super.setText(shown, BufferType.SPANNABLE);
        }
        fitting = false;
    }

    // ---- وضع المصحف المطابق ----

    private Layout buildExact(CharSequence text, float px, float extra, int width) {
        TextPaint paint = new TextPaint(getPaint());
        paint.setTextSize(px);
        return StaticLayout.Builder.obtain(text, 0, text.length(), paint, width)
                .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                .setTextDirection(TextDirectionHeuristics.RTL)
                .setLineSpacing(extra, 1f)
                .setJustificationMode(Layout.JUSTIFICATION_MODE_NONE)
                .setBreakStrategy(Layout.BREAK_STRATEGY_SIMPLE)
                .setHyphenationFrequency(Layout.HYPHENATION_FREQUENCY_NONE)
                .setIncludePad(false)
                .build();
    }

    private float fontHeight(float px) {
        TextPaint paint = new TextPaint(getPaint());
        paint.setTextSize(px);
        Paint.FontMetrics fm = paint.getFontMetrics();
        return fm.descent - fm.ascent;
    }

    private void fitExact() {
        CharSequence text = source != null ? source : getText();
        int width = contentWidth();
        int fullHeight = viewportHeight > 0 ? viewportHeight : getHeight();
        if (text == null || text.length() == 0 || width <= 0 || fullHeight <= 0 || exactLines <= 0) {
            return;
        }
        int height = fullHeight - getPaddingBottom();
        float slot = (float) height / Math.max(exactLines, exactSlots);
        float lo = minPx * 0.6f;
        float hi = 30 * getResources().getDisplayMetrics().scaledDensity;
        for (int i = 0; i < 14; i++) {
            float mid = (lo + hi) / 2f;
            boolean ok = fontHeight(mid) <= slot * 0.92f
                    && buildExact(text, mid, 0, width).getLineCount() == exactLines;
            if (ok) {
                lo = mid;
            } else {
                hi = mid;
            }
        }
        float extra = Math.max(0, slot - fontHeight(lo));
        int top = Math.max(0, Math.round((height - slot * exactLines) / 2f));
        setMinHeight(fullHeight);
        fitting = true;
        setTextSize(TypedValue.COMPLEX_UNIT_PX, lo);
        setLineSpacing(extra, 1f);
        setIncludeFontPadding(false);
        if (getPaddingTop() != top) {
            setPadding(getPaddingLeft(), top, getPaddingRight(), getPaddingBottom());
        }
        CharSequence shown = stretchExact(text, lo, extra, width);
        if (shown != getText()) {
            super.setText(shown, BufferType.SPANNABLE);
        }
        fitting = false;
    }

    /** يمدّ الكلمات ثم يوسّع المسافات بين الكلمات حتى يملأ كل سطر العرض كاملاً دون أن يلتف. */
    private CharSequence stretchExact(CharSequence src, float px, float extra, int width) {
        if (!(src instanceof Spanned) || src.length() == 0) {
            return src;
        }
        Layout plain = buildExact(src, px, extra, width);
        if (plain.getLineCount() != exactLines) {
            return src;
        }
        TextPaint paint = new TextPaint(getPaint());
        paint.setTextSize(px);
        float tatweelWidth = paint.measureText(String.valueOf(TATWEEL));
        Spanned spanned = (Spanned) src;
        float[] factors = kashidaEnabled ? new float[]{0.6f, 0.35f, 0f} : new float[]{0f};
        for (float factor : factors) {
            SpannableStringBuilder out = new SpannableStringBuilder(src);
            int shift = 0;
            if (factor > 0 && tatweelWidth > 0.5f) {
                for (int line = 0; line < plain.getLineCount(); line++) {
                    int start = plain.getLineStart(line);
                    int end = plain.getLineEnd(line);
                    if (end <= start || isCentered(spanned, start)) {
                        continue;
                    }
                    float slack = width - plain.getLineMax(line);
                    if (slack < tatweelWidth) {
                        continue;
                    }
                    java.util.List<Integer> positions = candidates(src, start, end);
                    if (positions.isEmpty()) {
                        continue;
                    }
                    int total = (int) (slack * factor / tatweelWidth);
                    int[] counts = new int[positions.size()];
                    int given = 0;
                    for (int round = 0; round < 3 && given < total; round++) {
                        for (int k = 0; k < counts.length && given < total; k++) {
                            if (counts[k] == round) {
                                counts[k]++;
                                given++;
                            }
                        }
                    }
                    int added = 0;
                    for (int k = positions.size() - 1; k >= 0; k--) {
                        if (counts[k] > 0) {
                            StringBuilder t = new StringBuilder();
                            for (int n = 0; n < counts[k]; n++) {
                                t.append(TATWEEL);
                            }
                            out.insert(positions.get(k) + shift, t);
                            added += counts[k];
                        }
                    }
                    shift += added;
                }
            }
            Layout mid = buildExact(out, px, extra, width);
            if (mid.getLineCount() != exactLines) {
                continue;
            }
            for (int line = 0; line < mid.getLineCount(); line++) {
                int start = mid.getLineStart(line);
                int end = mid.getLineEnd(line);
                if (end <= start || isCentered(out, start)) {
                    continue;
                }
                float slack = width - mid.getLineMax(line);
                java.util.List<Integer> spaces = new java.util.ArrayList<>();
                for (int i = start; i < end; i++) {
                    if (out.charAt(i) == ' ') {
                        spaces.add(i);
                    }
                }
                if (spaces.isEmpty() || slack < 1f) {
                    continue;
                }
                int each = (int) Math.floor((slack - 1f) / spaces.size());
                if (each <= 0) {
                    continue;
                }
                for (int i : spaces) {
                    out.setSpan(new SpaceSpan(each), i, i + 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                }
            }
            if (buildExact(out, px, extra, width).getLineCount() == exactLines) {
                return out;
            }
        }
        return src;
    }

    private static boolean isCentered(Spanned text, int start) {
        return text.getSpans(start, Math.min(text.length(), start + 1), AlignmentSpan.class).length > 0;
    }

    /** مسافة بين كلمتين بعرض الفراغ المعتاد مضافاً إليه توسيع ثابت. */
    private static final class SpaceSpan extends android.text.style.ReplacementSpan {
        private final int extra;

        SpaceSpan(int extra) {
            this.extra = extra;
        }

        @Override
        public int getSize(Paint paint, CharSequence text, int start, int end,
                           Paint.FontMetricsInt fm) {
            return (int) Math.floor(paint.measureText(" ")) + extra;
        }

        @Override
        public void draw(android.graphics.Canvas canvas, CharSequence text, int start, int end,
                         float x, int top, int y, int bottom, Paint paint) {
            // فراغ فقط
        }
    }

    // ---- تمديد الكلمات ----

    private static final char TATWEEL = '\u0640';

    private static boolean isMark(char c) {
        return (c >= '\u064B' && c <= '\u065F') || c == '\u0670' || (c >= '\u06D6' && c <= '\u06ED');
    }

    private static boolean isArabicLetter(char c) {
        return (c >= '\u0621' && c <= '\u064A') || c == '\u0671';
    }

    /** حروف لا تتصل بما بعدها فلا يُمدّ بعدها. */
    private static boolean joinsNext(char c) {
        switch (c) {
            case '\u0621': case '\u0622': case '\u0623': case '\u0624': case '\u0625':
            case '\u0627': case '\u0629': case '\u062F': case '\u0630': case '\u0631':
            case '\u0632': case '\u0648': case '\u0649': case '\u0671':
                return false;
            default:
                return isArabicLetter(c);
        }
    }

    private boolean isAlefLike(char c) {
        return c == '\u0627' || c == '\u0622' || c == '\u0623' || c == '\u0625' || c == '\u0671';
    }

    /** مواضع صالحة للتمديد داخل [start,end): فهرس الحرف التالي بعد حركات الحرف الحالي. */
    private java.util.List<Integer> candidates(CharSequence text, int start, int end) {
        java.util.List<Integer> out = new java.util.ArrayList<>();
        for (int i = start; i < end; i++) {
            char c = text.charAt(i);
            if (!joinsNext(c)) {
                continue;
            }
            int j = i + 1;
            while (j < end && isMark(text.charAt(j))) {
                j++;
            }
            if (j >= end) {
                continue;
            }
            char next = text.charAt(j);
            if (!isArabicLetter(next) || next == '\u0621') {
                continue;
            }
            if (c == '\u0644' && isAlefLike(next)) {
                continue; // لا يُفصل لام-ألف
            }
            out.add(j);
        }
        return out;
    }

    private CharSequence stretched(CharSequence src, float px, float extra, int width) {
        if (!(src instanceof Spanned) || src.length() == 0) {
            return src;
        }
        Layout plain = build(src, px, extra, width, false);
        TextPaint paint = new TextPaint(getPaint());
        paint.setTextSize(px);
        float tatweelWidth = paint.measureText(String.valueOf(TATWEEL));
        if (tatweelWidth <= 0.5f) {
            return src;
        }
        Spanned spanned = (Spanned) src;
        for (float factor = 0.92f; factor > 0.5f; factor -= 0.17f) {
            SpannableStringBuilder out = new SpannableStringBuilder(src);
            int shift = 0;
            for (int line = 0; line < plain.getLineCount(); line++) {
                int start = plain.getLineStart(line);
                int end = plain.getLineEnd(line);
                if (end <= start) {
                    continue;
                }
                if (spanned.getSpans(start, Math.min(end, start + 1), AlignmentSpan.class).length > 0) {
                    continue; // سطر العنوان والبسملة يبقيان في الوسط
                }
                float slack = width - plain.getLineMax(line);
                if (slack < tatweelWidth || slack > width * 0.5f) {
                    continue;
                }
                java.util.List<Integer> positions = candidates(src, start, end);
                if (positions.isEmpty()) {
                    continue;
                }
                int total = (int) (slack * factor / tatweelWidth);
                int[] counts = new int[positions.size()];
                int given = 0;
                for (int round = 0; round < 4 && given < total; round++) {
                    for (int k = 0; k < counts.length && given < total; k++) {
                        if (counts[k] == round) {
                            counts[k]++;
                            given++;
                        }
                    }
                }
                int added = 0;
                for (int k = positions.size() - 1; k >= 0; k--) {
                    if (counts[k] > 0) {
                        StringBuilder t = new StringBuilder();
                        for (int n = 0; n < counts[k]; n++) {
                            t.append(TATWEEL);
                        }
                        out.insert(positions.get(k) + shift, t);
                        added += counts[k];
                    }
                }
                shift += added;
            }
            Layout check = build(out, px, extra, width, true);
            if (check.getLineCount() == plain.getLineCount()) {
                return out;
            }
        }
        return src;
    }
}
