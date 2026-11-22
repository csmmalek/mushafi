package com.csm.mushafi.data;

import android.content.Context;

import com.csm.mushafi.quran.QuranIntegrityException;
import com.csm.mushafi.quran.QuranVerifier;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * يتحقق من ملف الـ asset ثم يستورده إلى Room.
 * يُستدعى من خيط خلفي فقط. التحقق يتم عند كل تشغيل، والاستيراد يتم فقط إذا لم تطابق قاعدة البيانات الملف.
 */
public final class QuranRepository {

    static final String ASSET_NAME = "quran-simple.txt";
    private static final int BATCH = 500;

    private final AppDatabase db;
    private final Context appContext;

    public QuranRepository(Context context) {
        this.appContext = context.getApplicationContext();
        this.db = AppDatabase.get(appContext);
    }

    public QuranDao dao() {
        return db.quranDao();
    }

    /** يرجع true إذا تم الاستيراد الآن، وfalse إذا كانت البيانات مطابقة مسبقاً. */
    public boolean verifyAndImportIfNeeded() throws QuranIntegrityException {
        byte[] data = readAsset();
        QuranVerifier.checkHash(data, QuranVerifier.EXPECTED_SHA256);
        QuranVerifier.Result result = QuranVerifier.parse(
                data, QuranVerifier.EXPECTED_RECORDS, QuranVerifier.EXPECTED_SURAHS);

        QuranDao dao = db.quranDao();
        if (dao.countAyahs() == QuranVerifier.EXPECTED_RECORDS
                && dao.countSurahs() == QuranVerifier.EXPECTED_SURAHS) {
            return false;
        }
        db.runInTransaction(() -> {
            dao.clearAyahs();
            dao.clearSurahs();
            List<SurahEntity> surahs = new ArrayList<>();
            for (int i = 0; i < result.ayahCounts.length; i++) {
                surahs.add(new SurahEntity(i + 1, result.ayahCounts[i]));
            }
            dao.insertSurahs(surahs);
            List<AyahEntity> batch = new ArrayList<>(BATCH);
            for (QuranVerifier.Verse v : result.verses) {
                batch.add(new AyahEntity(v.surah, v.ayah, v.text));
                if (batch.size() == BATCH) {
                    dao.insertAyahs(batch);
                    batch = new ArrayList<>(BATCH);
                }
            }
            if (!batch.isEmpty()) {
                dao.insertAyahs(batch);
            }
        });
        return true;
    }

    private byte[] readAsset() throws QuranIntegrityException {
        try (InputStream in = appContext.getAssets().open(ASSET_NAME)) {
            ByteArrayOutputStream out = new ByteArrayOutputStream(1 << 21);
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) != -1) {
                out.write(buf, 0, n);
            }
            return out.toByteArray();
        } catch (IOException e) {
            throw new QuranIntegrityException("تعذّر قراءة ملف القرآن المرفق بالتطبيق.");
        }
    }
}
