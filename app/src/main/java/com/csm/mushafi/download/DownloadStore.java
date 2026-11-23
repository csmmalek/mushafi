package com.csm.mushafi.download;

import android.content.Context;
import android.os.StatFs;

import java.io.File;

/** مكان الملفات داخل مساحة التطبيق الخاصة فقط، وفحص المساحة قبل البدء. */
public final class DownloadStore {

    /** هامش مساحة حرة مطلوب قبل بدء أي تنزيل. */
    public static final long MIN_FREE_BYTES = 100L * 1024 * 1024;

    private DownloadStore() {
    }

    public static File fileFor(Context context, String server, int surah) {
        File dir = new File(context.getFilesDir(), "audio/" + Integer.toHexString(server.hashCode()));
        if (!dir.exists() && !dir.mkdirs()) {
            throw new IllegalStateException("تعذّر إنشاء مجلد الصوتيات");
        }
        return new File(dir, String.format(java.util.Locale.ROOT, "%03d.mp3", surah));
    }

    public static boolean hasSpace(Context context) {
        StatFs stat = new StatFs(context.getFilesDir().getAbsolutePath());
        return stat.getAvailableBytes() > MIN_FREE_BYTES;
    }
}
