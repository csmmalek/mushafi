package com.csm.mushafi;

import android.content.Context;
import android.util.TypedValue;

/** قراءة ألوان الثيم الحالي من الأكواد. */
final class Themes {
    private Themes() {
    }

    static int color(Context ctx, int attr) {
        TypedValue v = new TypedValue();
        ctx.getTheme().resolveAttribute(attr, v, true);
        return v.data;
    }
}
