package com.csm.mushafi;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.csm.mushafi.data.QuranRepository;
import com.csm.mushafi.data.SearchHit;
import com.csm.mushafi.quran.ArabicDigits;
import com.csm.mushafi.quran.ArabicNormalizer;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** البحث في نص الآيات عبر فهرس مشتق؛ النتيجة تفتح الآية نفسها. */
public class SearchActivity extends BaseActivity {

    private final ExecutorService io = Executors.newSingleThreadExecutor();
    private final Handler main = new Handler(Looper.getMainLooper());
    private EditText query;
    private TextView status;
    private RecyclerView list;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);
        query = findViewById(R.id.search_input);
        status = findViewById(R.id.search_status);
        list = findViewById(R.id.search_results);
        list.setLayoutManager(new LinearLayoutManager(this));
        findViewById(R.id.btn_back).setOnClickListener(v -> finish());
        Button go = findViewById(R.id.btn_search);
        go.setOnClickListener(v -> runSearch());
        query.setOnEditorActionListener((v, actionId, event) -> {
            runSearch();
            return true;
        });
    }

    @Override
    protected void onDestroy() {
        io.shutdownNow();
        super.onDestroy();
    }

    private void runSearch() {
        String raw = query.getText().toString();
        // نزيل علامات LIKE حتى لا تُفسَّر كأنماط.
        String needle = ArabicNormalizer.normalize(raw).replace("%", "").replace("_", "");
        if (needle.length() < 2) {
            status.setText(R.string.search_too_short);
            list.setAdapter(null);
            return;
        }
        status.setText(R.string.search_running);
        io.execute(() -> {
            List<SearchHit> hits = new QuranRepository(getApplicationContext()).dao().search(needle);
            main.post(() -> {
                if (isFinishing()) {
                    return;
                }
                status.setText(hits.isEmpty()
                        ? getString(R.string.search_no_results)
                        : getString(R.string.search_results_format, ArabicDigits.format(hits.size())));
                list.setAdapter(new SearchAdapter(hits, hit -> {
                    Intent intent = new Intent(this, PageReaderActivity.class);
                    intent.putExtra(ReaderActivity.EXTRA_SURAH, hit.surah);
                    intent.putExtra(ReaderActivity.EXTRA_AYAH, hit.ayah);
                    startActivity(intent);
                }));
            });
        });
    }
}
