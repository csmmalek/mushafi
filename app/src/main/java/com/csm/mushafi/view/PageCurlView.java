package com.csm.mushafi.view;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.DecelerateInterpolator;

/**
 * تقليب صفحة بطيّ قطري يتبع الإصبع: تُمسك زاوية الصفحة أو حافتها، فينطوي الجزء الذي يلي الزاوية
 * على خطّ الطيّ (العمود المنصّف بين الزاوية والإصبع) ويظهر ظهر الورقة فوق الصفحة، مع ظلال وانحناء
 * مُحاكى عند الطيّ وسُمك للحافة. تُرسم الصفحتان من صورتين ثابتتين (لا إعادة تخطيط أثناء الحركة).
 *
 * <p>الحالة هي موضع الإصبع F (الزاوية المسحوبة). F = الزاوية A تعني صفحة مستوية، و F بعيدة جهة
 * اليمين تعني أن الصفحة انصرفت بالكامل.
 */
public final class PageCurlView extends View {

    public interface Done {
        void onFinished(boolean completed);
    }

    private Bitmap top;
    private Bitmap under;
    private int paper = Color.parseColor("#F6EBCF");
    private boolean forward = true;
    private float ax;
    private float ay;
    private float fx;
    private float fy;
    private ValueAnimator animator;

    private final Paint bitmapPaint = new Paint(Paint.FILTER_BITMAP_FLAG);
    private final Paint ghostPaint = new Paint(Paint.FILTER_BITMAP_FLAG);
    private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint shade = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint line = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Matrix mirror = new Matrix();
    private final float[] values = new float[9];

    public PageCurlView(Context context, AttributeSet attrs) {
        super(context, attrs);
        ghostPaint.setAlpha(30);
        line.setStyle(Paint.Style.STROKE);
        fill.setStyle(Paint.Style.FILL);
    }

    /** يبدأ تقليباً: topPage تنطوي و underPage تنكشف. bottomCorner: تُمسك الزاوية السفلية. */
    public void begin(Bitmap topPage, Bitmap underPage, int paperColor, boolean forwardTurn,
                      boolean bottomCorner) {
        cancelAnimation();
        top = topPage;
        under = underPage;
        paper = paperColor;
        forward = forwardTurn;
        ax = 0f;
        ay = bottomCorner ? topPage.getHeight() : 0f;
        if (forward) {
            fx = ax;
            fy = ay;
        } else {
            fx = 2.3f * topPage.getWidth();
            fy = ay;
        }
        setVisibility(VISIBLE);
        invalidate();
    }

    public void end() {
        cancelAnimation();
        setVisibility(GONE);
        top = null;
        under = null;
    }

    private int pw() {
        return top != null ? top.getWidth() : getWidth();
    }

    private int ph() {
        return top != null ? top.getHeight() : getHeight();
    }

    public boolean isBusy() {
        return animator != null && animator.isRunning();
    }

    /** الزاوية تتبع الإصبع: dx إزاحته الأفقية عن نقطة اللمس، dy إزاحته الرأسية. */
    public void dragForward(float dx, float dy) {
        fx = clamp(ax + Math.max(0f, dx), 0f, 2.3f * pw());
        fy = clamp(ay + dy * 0.9f, 0f, ph());
        invalidate();
    }

    /** للخلف: خطّ الطيّ يتبع الإصبع؛ left = مسافة سحب الإصبع نحو اليسار من الحافة اليمنى. */
    public void dragBackward(float left, float dy) {
        fx = clamp(2.3f * pw() - 2f * Math.max(0f, left) , 0f, 2.3f * pw());
        fy = clamp(ay + dy * 0.5f, 0f, ph());
        invalidate();
    }

    /** نسبة امتداد الطيّ: موضع خطّ الطيّ الأفقي ÷ العرض (0 مستوية، 1 انصرفت). */
    public float progress() {
        return clamp(((ax + fx) / 2f) / Math.max(1f, pw()), 0f, 1f);
    }

    /** يُكمل الحركة بعد رفع الإصبع إلى الإتمام أو العودة، بسرعة تناسب المسافة المتبقية. */
    public void finish(boolean complete, Done done) {
        cancelAnimation();
        final float tx;
        final float ty = ay;
        // الإتمام: للأمام تنصرف الصفحة (F بعيدة)، وللخلف تستقرّ (F = الزاوية)
        if (complete == forward) {
            tx = 2.3f * pw();
        } else {
            tx = ax;
        }
        final float sx = fx;
        final float sy = fy;
        animator = ValueAnimator.ofFloat(0f, 1f);
        float dist = Math.abs(tx - sx) / (2.3f * Math.max(1, pw()));
        animator.setDuration((long) (140 + 380 * dist));
        animator.setInterpolator(new DecelerateInterpolator(1.4f));
        animator.addUpdateListener(a -> {
            float t = (float) a.getAnimatedValue();
            fx = sx + (tx - sx) * t;
            fy = sy + (ty - sy) * t;
            invalidate();
        });
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

    private static float clamp(float v, float lo, float hi) {
        return Math.max(lo, Math.min(hi, v));
    }

    /** يقصّ مضلّعاً بنصف مستوى: يُبقي النقاط التي (P-M)·n بإشارة sign. */
    private static float[] clipPoly(float[] poly, float mx, float my, float nx, float ny, int sign) {
        float[] out = new float[poly.length * 2 + 4];
        int count = 0;
        int n = poly.length / 2;
        for (int i = 0; i < n; i++) {
            float x1 = poly[2 * i];
            float y1 = poly[2 * i + 1];
            float x2 = poly[2 * ((i + 1) % n)];
            float y2 = poly[2 * ((i + 1) % n) + 1];
            float s1 = ((x1 - mx) * nx + (y1 - my) * ny) * sign;
            float s2 = ((x2 - mx) * nx + (y2 - my) * ny) * sign;
            if (s1 >= 0) {
                out[2 * count] = x1;
                out[2 * count + 1] = y1;
                count++;
            }
            if ((s1 >= 0) != (s2 >= 0)) {
                float t = s1 / (s1 - s2);
                out[2 * count] = x1 + (x2 - x1) * t;
                out[2 * count + 1] = y1 + (y2 - y1) * t;
                count++;
            }
        }
        float[] res = new float[count * 2];
        System.arraycopy(out, 0, res, 0, count * 2);
        return res;
    }

    private static Path pathOf(float[] p) {
        Path path = new Path();
        for (int i = 0; i < p.length / 2; i++) {
            if (i == 0) {
                path.moveTo(p[0], p[1]);
            } else {
                path.lineTo(p[2 * i], p[2 * i + 1]);
            }
        }
        path.close();
        return path;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (top == null || under == null) {
            return;
        }
        int w = getWidth();
        int h = getHeight();
        float dxv = fx - ax;
        float dyv = fy - ay;
        float dist = (float) Math.hypot(dxv, dyv);
        canvas.drawBitmap(under, 0, 0, bitmapPaint);
        if (dist < 1.5f) {
            canvas.drawBitmap(top, 0, 0, bitmapPaint);
            return;
        }
        float nx = dxv / dist;
        float ny = dyv / dist;
        float mx = (ax + fx) / 2f;
        float my = (ay + fy) / 2f;
        float[] rect = {0, 0, w, 0, w, h, 0, h};
        float[] kept = clipPoly(rect, mx, my, nx, ny, 1);
        float[] corner = clipPoly(rect, mx, my, nx, ny, -1);
        if (kept.length < 6) {
            return;
        }
        float density = getResources().getDisplayMetrics().density;
        float soft = Math.max(26f * density, w * 0.07f);

        // ظلّ خطّ الطيّ على الصفحة التي تحتها (جهة الزاوية)
        if (corner.length >= 6) {
            canvas.save();
            canvas.clipPath(pathOf(corner));
            shade.setShader(new LinearGradient(mx, my, mx - nx * soft * 1.4f, my - ny * soft * 1.4f,
                    Color.argb(110, 0, 0, 0), Color.TRANSPARENT, Shader.TileMode.CLAMP));
            canvas.drawPaint(shade);
            shade.setShader(null);
            canvas.restore();
        }

        // الجزء المثبّت من الصفحة العليا
        canvas.save();
        canvas.clipPath(pathOf(kept));
        canvas.drawBitmap(top, 0, 0, bitmapPaint);
        // ظلّ الجزء المنقلب على الجزء المثبّت قرب الطيّ
        shade.setShader(new LinearGradient(mx, my, mx + nx * soft * 1.2f, my + ny * soft * 1.2f,
                Color.argb(95, 0, 0, 0), Color.TRANSPARENT, Shader.TileMode.CLAMP));
        canvas.drawPaint(shade);
        shade.setShader(null);

        // الجزء المنقلب (ظهر الورقة): انعكاس منطقة الزاوية حول خطّ الطيّ
        if (corner.length >= 6) {
            float c2 = 2f * (nx * mx + ny * my);
            values[Matrix.MSCALE_X] = 1f - 2f * nx * nx;
            values[Matrix.MSKEW_X] = -2f * nx * ny;
            values[Matrix.MTRANS_X] = c2 * nx;
            values[Matrix.MSKEW_Y] = -2f * nx * ny;
            values[Matrix.MSCALE_Y] = 1f - 2f * ny * ny;
            values[Matrix.MTRANS_Y] = c2 * ny;
            values[Matrix.MPERSP_0] = 0f;
            values[Matrix.MPERSP_1] = 0f;
            values[Matrix.MPERSP_2] = 1f;
            mirror.setValues(values);

            // ظلّ ناعم لحافة الجزء المنقلب على الصفحة تحته
            float[] flapPoly = new float[corner.length];
            for (int i = 0; i < corner.length / 2; i++) {
                float[] pt = {corner[2 * i], corner[2 * i + 1]};
                mirror.mapPoints(pt);
                flapPoly[2 * i] = pt[0];
                flapPoly[2 * i + 1] = pt[1];
            }
            Path flapPath = pathOf(flapPoly);
            line.setColor(Color.argb(26, 0, 0, 0));
            for (int k = 3; k >= 1; k--) {
                line.setStrokeWidth(k * 5f * density);
                canvas.drawPath(flapPath, line);
            }

            canvas.save();
            canvas.concat(mirror);
            canvas.clipPath(pathOf(corner));
            fill.setColor(paper);
            canvas.drawPaint(fill);
            canvas.drawBitmap(top, 0, 0, ghostPaint);
            // انحناء الورقة عند الطيّ: ظلّ عميق عند الخط ثم لمعة ثم رجوع للون الورق
            shade.setShader(new LinearGradient(mx, my, mx - nx * soft, my - ny * soft,
                    new int[]{Color.argb(150, 0, 0, 0), Color.argb(0, 0, 0, 0),
                            Color.argb(60, 255, 255, 255), Color.argb(0, 255, 255, 255)},
                    new float[]{0f, 0.45f, 0.62f, 1f}, Shader.TileMode.CLAMP));
            canvas.drawPaint(shade);
            shade.setShader(null);
            canvas.restore();

            // سُمك حافة الورقة المنقلبة
            line.setStrokeWidth(1.6f * density);
            line.setColor(Color.argb(150, 255, 255, 255));
            canvas.drawPath(flapPath, line);
            line.setStrokeWidth(0.8f * density);
            line.setColor(Color.argb(120, 0, 0, 0));
            canvas.drawPath(flapPath, line);
        }
        canvas.restore();
    }
}
