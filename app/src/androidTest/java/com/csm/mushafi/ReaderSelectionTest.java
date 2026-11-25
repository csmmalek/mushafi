package com.csm.mushafi;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import android.content.Intent;
import android.view.View;

import androidx.recyclerview.widget.RecyclerView;
import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.csm.mushafi.data.QuranRepository;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/** يتحقق من تحديد الآية بالضغط وإلغائه في شاشة القراءة، بعد استيراد النص المعتمد. */
@RunWith(AndroidJUnit4.class)
public class ReaderSelectionTest {

    @Before
    public void importQuran() throws Exception {
        new QuranRepository(ApplicationProvider.getApplicationContext()).verifyAndImportIfNeeded();
    }

    @Test
    public void tapSelectsAyahAndClearHidesBar() throws Exception {
        Intent intent = new Intent(ApplicationProvider.getApplicationContext(), ReaderActivity.class);
        intent.putExtra(ReaderActivity.EXTRA_SURAH, 1);
        intent.putExtra(ReaderActivity.EXTRA_AYAH, 1);

        try (ActivityScenario<ReaderActivity> scenario = ActivityScenario.launch(intent)) {
            // ينتظر حتى تُحمَّل آيات السورة في القائمة (الاستعلام يتم في الخلفية).
            waitFor(scenario, () -> itemCount(scenario) > 0, 15_000);

            scenario.onActivity(a -> {
                RecyclerView list = a.findViewById(R.id.ayah_list);
                RecyclerView.ViewHolder first = list.findViewHolderForAdapterPosition(0);
                assertTrue("first ayah must be on screen", first != null);
                first.itemView.performClick();
            });

            scenario.onActivity(a -> assertEquals(View.VISIBLE,
                    a.findViewById(R.id.selection_bar).getVisibility()));

            scenario.onActivity(a -> a.findViewById(R.id.btn_clear_selection).performClick());

            scenario.onActivity(a -> assertEquals(View.GONE,
                    a.findViewById(R.id.selection_bar).getVisibility()));
        }
    }

    private static int itemCount(ActivityScenario<ReaderActivity> scenario) {
        int[] count = {0};
        scenario.onActivity(a -> {
            RecyclerView list = a.findViewById(R.id.ayah_list);
            if (list.getAdapter() != null) {
                count[0] = list.getAdapter().getItemCount();
            }
        });
        return count[0];
    }

    private interface Check {
        boolean ok();
    }

    private static void waitFor(ActivityScenario<ReaderActivity> scenario, Check check, long timeoutMs)
            throws InterruptedException {
        long end = System.currentTimeMillis() + timeoutMs;
        while (!check.ok() && System.currentTimeMillis() < end) {
            Thread.sleep(200);
        }
        assertTrue("condition not met within " + timeoutMs + " ms", check.ok());
    }
}
