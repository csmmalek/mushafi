package com.csm.mushafi;

import android.content.Context;

import com.csm.mushafi.mushaf.MushafMeta;
import com.csm.mushafi.quran.ArabicDigits;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/** أسماء السور من assets/mushaf-meta.json (مصدر معتمد من مكتبة quran-library)، مع رجوع للرقم عند الفشل. */
public final class SurahNames {

    private static MushafMeta meta;

    private SurahNames() {
    }

    private static synchronized MushafMeta meta(Context context) {
        if (meta == null) {
            try (InputStream in = context.getApplicationContext().getAssets().open("mushaf-meta.json")) {
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                byte[] buf = new byte[8192];
                int n;
                while ((n = in.read(buf)) != -1) {
                    out.write(buf, 0, n);
                }
                meta = MushafMeta.parse(new String(out.toByteArray(), StandardCharsets.UTF_8));
            } catch (Exception e) {
                return null;
            }
        }
        return meta;
    }

    /** اسم السورة بلا «سورة»، أو رقمها إن لم يتوفر الاسم. */
    public static String name(Context context, int surah) {
        MushafMeta m = meta(context);
        return m == null ? ArabicDigits.format(surah) : m.name(surah);
    }

    /** «سورة الفاتحة». */
    public static String title(Context context, int surah) {
        MushafMeta m = meta(context);
        if (m == null) {
            return context.getString(R.string.surah_number_format, ArabicDigits.format(surah));
        }
        return context.getString(R.string.surah_name_format, m.name(surah));
    }
}
