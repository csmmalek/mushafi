package com.csm.mushafi;

import android.app.AlertDialog;
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

import com.csm.mushafi.audio.CatalogueCache;
import com.csm.mushafi.audio.CatalogueClient;
import com.csm.mushafi.audio.ReciterCatalogue;
import com.csm.mushafi.quran.ArabicDigits;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** مكتبة القراء: فهرس من MP3Quran API v3 مع بحث بالاسم. يحتاج إنترنت، والقرآن يعمل بدونه. */
public class LibraryActivity extends BaseActivity {

    private final ExecutorService io = Executors.newSingleThreadExecutor();
    private final Handler main = new Handler(Looper.getMainLooper());
    private TextView status;
    private Button retry;
    private EditText search;
    private RecyclerView list;
    private List<ReciterCatalogue.Reciter> all = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_library);
        status = findViewById(R.id.library_status);
        retry = findViewById(R.id.btn_retry_library);
        search = findViewById(R.id.library_search);
        list = findViewById(R.id.library_list);
        list.setLayoutManager(new LinearLayoutManager(this));
        findViewById(R.id.btn_back).setOnClickListener(v -> finish());
        retry.setOnClickListener(v -> load());
        search.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int a, int b, int c) {
            }

            @Override
            public void onTextChanged(CharSequence s, int a, int b, int c) {
                render(s.toString());
            }

            @Override
            public void afterTextChanged(android.text.Editable s) {
            }
        });

        ReciterCatalogue.Result cached = CatalogueCache.get();
        if (cached != null) {
            onLoaded(cached);
        } else {
            load();
        }
    }

    @Override
    protected void onDestroy() {
        io.shutdownNow();
        super.onDestroy();
    }

    private void load() {
        status.setText(R.string.library_loading);
        retry.setVisibility(View.GONE);
        io.execute(() -> {
            try {
                String body = CatalogueClient.fetch();
                ReciterCatalogue.Result result = ReciterCatalogue.parse(body);
                CatalogueCache.set(result);
                main.post(() -> onLoaded(result));
            } catch (CatalogueClient.NetworkException e) {
                main.post(() -> showError(e.getMessage()));
            } catch (ReciterCatalogue.InvalidCatalogueException e) {
                main.post(() -> showError(e.getMessage()));
            }
        });
    }

    private void showError(String message) {
        if (isFinishing()) {
            return;
        }
        status.setText(message);
        retry.setVisibility(View.VISIBLE);
    }

    private void onLoaded(ReciterCatalogue.Result result) {
        if (isFinishing()) {
            return;
        }
        all = result.reciters;
        String base = getString(R.string.library_count_format, ArabicDigits.format(all.size()));
        if (result.skipped > 0) {
            base += " · " + getString(R.string.library_skipped_format, ArabicDigits.format(result.skipped));
        }
        status.setText(base + "\n" + getString(R.string.library_source));
        retry.setVisibility(View.GONE);
        render(search.getText().toString());
    }

    private void render(String query) {
        String q = query.trim().toLowerCase(Locale.ROOT);
        List<ReciterCatalogue.Reciter> shown = new ArrayList<>();
        for (ReciterCatalogue.Reciter r : all) {
            if (q.isEmpty() || r.name.toLowerCase(Locale.ROOT).contains(q)) {
                shown.add(r);
            }
        }
        list.setAdapter(new ReciterAdapter(shown, this::onReciterClick));
    }

    private void onReciterClick(ReciterCatalogue.Reciter reciter) {
        int index = all.indexOf(reciter);
        if (index < 0) {
            return;
        }
        if (reciter.moshafs.size() == 1) {
            openReciter(index, 0);
            return;
        }
        String[] names = new String[reciter.moshafs.size()];
        for (int i = 0; i < names.length; i++) {
            names[i] = reciter.moshafs.get(i).name;
        }
        new AlertDialog.Builder(this)
                .setTitle(R.string.library_choose_moshaf)
                .setItems(names, (d, which) -> openReciter(index, which))
                .setNegativeButton(R.string.btn_cancel, null)
                .show();
    }

    private void openReciter(int reciterIndex, int moshafIndex) {
        Intent intent = new Intent(this, ReciterActivity.class);
        intent.putExtra(ReciterActivity.EXTRA_RECITER_INDEX, reciterIndex);
        intent.putExtra(ReciterActivity.EXTRA_MOSHAF_INDEX, moshafIndex);
        startActivity(intent);
    }
}
