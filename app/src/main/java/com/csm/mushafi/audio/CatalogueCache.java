package com.csm.mushafi.audio;

/** ذاكرة الفهرس للجلسة الحالية فقط. لا تُحفظ على القرص في هذه المرحلة. */
public final class CatalogueCache {
    private static volatile ReciterCatalogue.Result current;

    private CatalogueCache() {
    }

    public static ReciterCatalogue.Result get() {
        return current;
    }

    public static void set(ReciterCatalogue.Result result) {
        current = result;
    }
}
