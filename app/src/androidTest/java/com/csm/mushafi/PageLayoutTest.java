package com.csm.mushafi;

import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.content.Intent;
import android.view.View;

import androidx.recyclerview.widget.RecyclerView;
import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.viewpager2.widget.ViewPager2;

import com.csm.mushafi.data.QuranRepository;
import com.csm.mushafi.mushaf.MushafLines;
import com.csm.mushafi.view.FitPageTextView;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/** يمرّ على صفحات المصحف الـ604 ويتأكد أن كل صفحة تُرسم بعدد أسطرها المطبوع دون التفاف. */
@RunWith(AndroidJUnit4.class)
public class PageLayoutTest {

    @Test
    public void everyPageKeepsItsPrintedLineCount() throws Exception {
        Context ctx = ApplicationProvider.getApplicationContext();
        new QuranRepository(ctx).verifyAndImportIfNeeded();
        MushafLines lines;
        try (InputStream in = ctx.getAssets().open("mushaf-lines.json")) {
            lines = MushafLines.parse(new String(in.readAllBytes(), "UTF-8"));
        }
        Intent intent = new Intent(ctx, PageReaderActivity.class);
        intent.putExtra(ReaderActivity.EXTRA_SURAH, 1);
        intent.putExtra(ReaderActivity.EXTRA_AYAH, 1);
        List<String> bad = new ArrayList<>();
        try (ActivityScenario<PageReaderActivity> scenario = ActivityScenario.launch(intent)) {
            Thread.sleep(3000);
            for (int page = 1; page <= 604; page += 1) {
                final int p = page;
                final int[] result = {-1, 0};
                scenario.onActivity(a -> {
                    ViewPager2 pager = a.findViewById(R.id.page_pager);
                    pager.setCurrentItem(p - 1, false);
                });
                Thread.sleep(120);
                scenario.onActivity(a -> {
                    ViewPager2 pager = a.findViewById(R.id.page_pager);
                    RecyclerView rv = (RecyclerView) pager.getChildAt(0);
                    RecyclerView.ViewHolder h = rv.findViewHolderForAdapterPosition(p - 1);
                    if (h != null) {
                        View t = h.itemView.findViewById(R.id.page_text);
                        if (t instanceof FitPageTextView && ((FitPageTextView) t).getLayout() != null) {
                            result[0] = ((FitPageTextView) t).getLayout().getLineCount();
                        }
                    }
                });
                int expected = lines.page(p).size();
                if (result[0] != expected) {
                    bad.add(p + ":" + result[0] + "!=" + expected);
                }
            }
        }
        assertTrue("pages with wrong line count: " + bad, bad.isEmpty());
    }
}
