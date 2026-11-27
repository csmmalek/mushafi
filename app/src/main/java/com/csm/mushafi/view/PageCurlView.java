package com.csm.mushafi.view;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;

/**
 * تقليب صفحة كالورق: الصفحة العليا تُطوى على أسطوانة يتحرك محورها من الحافة اليسرى إلى اليمنى؛
 * يظهر ظهر الورقة بعد المنحنى، وظلّ ناعم على الصفحة التي تحتها. يُرسم من صورتين ثابتتين (لا إعادة
 * تخطيط أثناء الحركة) فيبقى سلساً.
 *
 * <p>الحالة الوحيدة هي c: موضع محور الطيّ بالبكسل (0 = الصفحة مستوية، عرض الشاشة + نصف القطر =
 * انصرفت الصفحة بالكامل).
 */
public final class PageCurlView extends View {

    public interface Done {
        void onFinished(boolean completed);
    }

    private Bitmap top;
    private Bitmap under;
    private int paper = Color.parseColor("#F6EBCF");
    private float c;
    private boolean forward = true;
    private ValueAnimator animator;

    private final Paint bitmapPaint = new Paint(Paint.FILTER_BITMAP_FLAG);
    private final Paint ghostPaint = new Paint(Paint.FILTER_BITMAP_FLAG);
    private final Paint fill = new Paint();
    private final Paint shade = new Paint();
    private final Rect src = new Rect();
    private final RectF dst = new RectF();

    public PageCurlView(Context context, AttributeSet attrs) {
        super(context, attrs);
        ghostPaint.setAlpha(34);
    }

    /** نصف قطر الأسطوانة. */
    private float radius() {
        return getWidth() * 0.085f;
    }

    public float maxC() {
        return getWidth() + radius();
    }

    /** يبدأ تقليباً: top هي الصفحة التي تُطوى و under هي التي تنكشف. */
    public void begin(Bitmap topPage, Bitmap underPage, int paperColor, boolean forwardTurn) {
        cancelAnimation();
        this.top = topPage;
        this.under = underPage;
        this.paper = paperColor;
        this.forward = forwardTurn;
        this.c = forwardTurn ? 0f : maxC();
        setVisibility(VISIBLE);
        invalidate();
    }

    public void end() {
        cancelAnimation();
        setVisibility(GONE);
        top = null;
        under = null;
    }

    public boolean isBusy() {
        return animator != null && animator.isRunning();
    }

    /** موضع محور الطيّ بحيث تتبع الحافة الحرّة إصبعَ المستخدم (للتقليب للأمام). */
    public void dragForward(float fingerDx) {
        float fx = Math.max(0f, Math.min(getWidth() * 0.9f, fingerDx));
        setC(solve(fx));
    }

    /** للتقليب للخلف: fraction من 0 (الصفحة لم تدخل) إلى 1 (استقرّت). */
    public void dragBackward(float fraction) {
        float f = Math.max(0f, Math.min(1f, fraction));
        setC(maxC() * (1f - f));
    }

    public float progress() {
        return Math.max(0f, Math.min(1f, c / maxC()));
    }

    private void setC(float value) {
        c = Math.max(0f, Math.min(maxC(), value));
        invalidate();
    }

    /** يكمل الحركة إلى حالة الانتهاء أو العودة. */
    public void finish(boolean complete, Done done) {
        cancelAnimation();
        // الإتمام: للأمام تنصرف الصفحة (c→max)، وللخلف تستقر (c→0)
        float target = (complete == forward) ? maxC() : 0f;
        animator = ValueAnimator.ofFloat(c, target);
        float dist = Math.abs(target - c) / Math.max(1f, maxC());
        animator.setDuration((long) (160 + 320 * dist));
        animator.setInterpolator(new AccelerateDecelerateInterpolator());
        animator.addUpdateListener(a -> setC((float) a.getAnimatedValue()));
        animator.addListener(new AnimatorListenerAdapter() {
            private boolean canceled;

            @Override
            public void onAnimationCancel(Animator animation) {
                canceled = true;
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                if (!canceled && done != null) {
                    done.onFinished(complete);
                }
            }
        });
        animator.start();
    }

    private void cancelAnimation() {
        if (animator != null) {
            animator.cancel();
            animator = null;
        }
    }

    /** موضع الحافة الحرّة على الشاشة لمحور c. */
    private float edge(float cc, float r) {
        float arc = (float) Math.PI * r;
        return cc >= arc ? 2 * cc - arc : cc - r * (float) Math.sin(cc / r);
    }

    private float solve(float fx) {
        float r = radius();
        float lo = 0f;
        float hi = getWidth() + r;
        for (int i = 0; i < 24; i++) {
            float mid = (lo + hi) / 2f;
            if (edge(mid, r) < fx) {
                lo = mid;
            } else {
                hi = mid;
            }
        }
        return (lo + hi) / 2f;
    }

    /** موضع الشاشة لنقطة تبعد d على الورقة بعد محور الطيّ. */
    private static float screenX(float d, float cc, float r) {
        float arc = (float) Math.PI * r;
        if (d < arc) {
            return cc - r * (float) Math.sin(d / r);
        }
        return cc + (d - arc);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (top == null || under == null) {
            return;
        }
        int w = getWidth();
        int h = getHeight();
        float r = radius();
        canvas.drawBitmap(under, 0, 0, bitmapPaint);
        if (c <= 0.5f) {
            canvas.drawBitmap(top, 0, 0, bitmapPaint);
            return;
        }
        if (c >= maxC() - 0.5f) {
            return;
        }
        float cc = Math.min(c, w + r);
        float arc = (float) Math.PI * r;

        // ظلّ الأسطوانة على الصفحة التي تحتها (يسار المحور)
        float shadowLeft = Math.max(0f, cc - r - r * 1.6f);
        if (cc - r > 0) {
            shade.setShader(new LinearGradient(cc - r, 0, shadowLeft, 0,
                    Color.argb(95, 0, 0, 0), Color.TRANSPARENT, Shader.TileMode.CLAMP));
            canvas.drawRect(shadowLeft, 0, cc - r, h, shade);
            shade.setShader(null);
        }

        // الجزء المثبّت من الصفحة (يمين المحور)
        if (cc < w) {
            canvas.save();
            canvas.clipRect(cc, 0, w, h);
            canvas.drawBitmap(top, 0, 0, bitmapPaint);
            canvas.restore();
        }

        // ظلّ الجزء المنقلب على الجزء المثبّت
        float freeEdge = edge(cc, r);
        if (freeEdge < w) {
            float sw = r * 1.1f;
            shade.setShader(new LinearGradient(freeEdge, 0, freeEdge + sw, 0,
                    Color.argb(85, 0, 0, 0), Color.TRANSPARENT, Shader.TileMode.CLAMP));
            canvas.drawRect(freeEdge, 0, Math.min(w, freeEdge + sw), h, shade);
            shade.setShader(null);
        }

        // الجزء المطويّ: شرائح رأسية من أقصى اليسار (طرف الورقة الحرّ) إلى المحور
        float step = 3f;
        float xFrom = 0f;
        float xTo = Math.min(cc, w);
        // نبدأ من أبعد مسافة d (الطرف الحرّ) إلى المحور لتُرسم الطبقات من الأسفل إلى الأعلى
        for (float x0 = xFrom; x0 < xTo; x0 += step) {
            float x1 = Math.min(xTo, x0 + step);
            float dA = cc - x1;
            float dB = cc - x0;
            float sxA = screenX(dA, cc, r);
            float sxB = screenX(dB, cc, r);
            float left = Math.min(sxA, sxB);
            float right = Math.max(sxA, sxB);
            if (right - left < 0.6f) {
                right = left + 0.6f;
            }
            if (right < 0 || left > w) {
                continue;
            }
            float dm = (dA + dB) / 2f;
            boolean onCylinder = dm < arc;
            float theta = onCylinder ? dm / r : (float) Math.PI;
            boolean back = theta > Math.PI / 2;
            src.set((int) x0, 0, (int) Math.ceil(x1), h);
            dst.set(left, 0, right, h);
            if (back) {
                fill.setColor(paper);
                canvas.drawRect(dst, fill);
                canvas.drawBitmap(top, src, dst, ghostPaint);
            } else {
                canvas.drawBitmap(top, src, dst, bitmapPaint);
            }
            // إضاءة الأسطوانة: أظلم عند الانحناء الحادّ
            int alpha;
            if (onCylinder) {
                double s = Math.sin(theta);
                alpha = back ? (int) (30 + 95 * s) : (int) (115 * s * s);
            } else {
                alpha = 12;
            }
            if (alpha > 0) {
                shade.setColor(Color.argb(Math.min(160, alpha), 0, 0, 0));
                canvas.drawRect(dst, shade);
            }
        }
        // لمعة خفيفة على قمة الأسطوانة
        float peak = cc - r * 0.35f;
        if (peak > 0 && peak < w) {
            shade.setShader(new LinearGradient(peak - r * 0.35f, 0, peak + r * 0.35f, 0,
                    new int[]{Color.TRANSPARENT, Color.argb(70, 255, 255, 255), Color.TRANSPARENT},
                    null, Shader.TileMode.CLAMP));
            canvas.drawRect(peak - r * 0.35f, 0, peak + r * 0.35f, h, shade);
            shade.setShader(null);
        }
    }
}
