package com.csm.mushafi;

/** ثيمات التطبيق العامة (تختلف عن ثيم ورقة المصحف داخل القارئ). */
final class AppThemes {

    static final int COUNT = 5;

    private AppThemes() {
    }

    static int style(int index) {
        switch (index) {
            case 1:
                return R.style.Theme_Mushafi_Night;
            case 2:
                return R.style.Theme_Mushafi_Sapphire;
            case 3:
                return R.style.Theme_Mushafi_Amber;
            case 4:
                return R.style.Theme_Mushafi_Pearl;
            default:
                return R.style.Theme_Mushafi_Emerald;
        }
    }
}
