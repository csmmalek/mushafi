package com.csm.mushafi.view;

import android.view.View;

import androidx.annotation.NonNull;
import androidx.viewpager2.widget.ViewPager2;

/**
 * تقليب صفحة كالكتاب: الصفحة المنصرفة تدور حول كعب الكتاب (حافتها اليسرى) فتكشف التالية
 * التي تبقى مثبتة تحتها مع ظل يخفّ تدريجياً.
 */
public final class BookPageTransformer implements ViewPager2.PageTransformer {

    @Override
    public void transformPage(@NonNull View page, float position) {
        float density = page.getResources().getDisplayMetrics().density;
        page.setCameraDistance(9000f * density);
        if (position <= -1f || position >= 1f) {
            reset(page);
            page.setAlpha(position <= -1f ? 0f : 0f);
            return;
        }
        page.setAlpha(1f);
        if (position < 0f) {
            // الصفحة التي تحت: ثابتة، تُظلَّم قليلاً ثم تتضح
            page.setTranslationX(-position * page.getWidth());
            page.setRotationY(0f);
            page.setTranslationZ(0f);
            float shade = 0.72f + 0.28f * (1f + position);
            page.setAlpha(shade);
            page.setScaleX(0.97f + 0.03f * (1f + position));
            page.setScaleY(0.97f + 0.03f * (1f + position));
        } else {
            // الصفحة التي تنقلب: تدور حول حافتها اليسرى (كعب الكتاب)
            page.setTranslationX(-position * page.getWidth());
            page.setPivotX(0f);
            page.setPivotY(page.getHeight() / 2f);
            page.setRotationY(-position * 88f);
            page.setTranslationZ(2f);
            page.setScaleX(1f);
            page.setScaleY(1f);
            page.setAlpha(1f - 0.45f * position * position);
        }
    }

    private static void reset(View page) {
        page.setTranslationX(0f);
        page.setRotationY(0f);
        page.setTranslationZ(0f);
        page.setScaleX(1f);
        page.setScaleY(1f);
        page.setPivotX(page.getWidth() / 2f);
    }
}
