package com.csm.mushafi;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;


import com.csm.mushafi.data.QuranRepository;
import com.csm.mushafi.data.SurahEntity;
import com.csm.mushafi.quran.ArabicDigits;
import com.csm.mushafi.quran.KhatmahPlanner;
import com.csm.mushafi.quran.QuranVerifier;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * خطة الختمة: مدة تختارها، والورد اليومي يُحسب من عدد آيات المصحف المتحقق منه.
 * التقدم مشتق من آخر موضع قراءة محفوظ. لا إشعارات.
 */
public class KhatmahActivity extends BaseActivity {

    private static final int[] PRESETS = {7, 14, 30};

    private final ExecutorService io = Executors.newSingleThreadExecutor();
    private final Handler main = new Handler(Looper.getMainLooper());
    private ReadingPrefs prefs;
    private TextView summary;
    private int[] ayahCounts = new int[0];

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_khatmah);
        prefs = new ReadingPrefs(this);
        summary = findViewById(R.id.khatmah_summary);
        findViewById(R.id.btn_back).setOnClickListener(v -> finish());

        int[] buttons = {R.id.btn_plan_7, R.id.btn_plan_14, R.id.btn_plan_30};
        for (int i = 0; i < buttons.length; i++) {
            final int days = PRESETS[i];
            ((Button) findViewById(buttons[i])).setOnClickListener(v -> {
                prefs.saveKhatmahDays(days);
                render();
            });
        }
        findViewById(R.id.btn_plan_reset).setOnClickListener(v -> {
            prefs.saveKhatmahDays(0);
            prefs.saveLast(0, 0);
            render();
        });

        io.execute(() -> {
            List<SurahEntity> surahs = new QuranRepository(getApplicationContext()).dao().allSurahs();
            int[] counts = new int[surahs.size()];
            for (int i = 0; i < counts.length; i++) {
                counts[i] = surahs.get(i).ayahCount;
            }
            main.post(() -> {
                ayahCounts = counts;
                render();
            });
        });
    }

    @Override
    protected void onDestroy() {
        io.shutdownNow();
        super.onDestroy();
    }

    private void render() {
        int days = prefs.khatmahDays();
        int total = QuranVerifier.EXPECTED_RECORDS;
        if (days <= 0) {
            summary.setText(R.string.khatmah_no_plan);
            return;
        }
        int daily = KhatmahPlanner.dailyAyahs(total, days);
        int read = 0;
        if (prefs.lastSurah() > 0 && ayahCounts.length > 0) {
            read = KhatmahPlanner.globalIndex(ayahCounts, prefs.lastSurah(), prefs.lastAyah());
        }
        int pct = KhatmahPlanner.progressPercent(read, total);
        summary.setText(getString(R.string.khatmah_summary_format,
                ArabicDigits.format(days),
                ArabicDigits.format(daily),
                ArabicDigits.format(read),
                ArabicDigits.format(total),
                ArabicDigits.format(pct)));
    }
}
