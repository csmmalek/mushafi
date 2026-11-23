package com.csm.mushafi;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.csm.mushafi.data.QuranRepository;
import com.csm.mushafi.quran.QuranIntegrityException;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * المرحلة الثالثة: التحقق من النص القرآني واستيراده، ثم فتح القارئ أو الفهرس.
 * الأزرار غير المنفذة بعد (التلاوات والمحفوظات والإعدادات) تبقى معطّلة.
 */
public class MainActivity extends AppCompatActivity {

    private final ExecutorService io = Executors.newSingleThreadExecutor();
    private final Handler main = new Handler(Looper.getMainLooper());

    private TextView statusText;
    private Button btnContinue;
    private Button btnQuran;
    private Button btnRetry;
    private ReadingPrefs prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        prefs = new ReadingPrefs(this);

        statusText = findViewById(R.id.status_text);
        btnContinue = findViewById(R.id.btn_continue);
        btnQuran = findViewById(R.id.btn_quran);
        btnRetry = findViewById(R.id.btn_retry);
        Button btnSearch = findViewById(R.id.btn_search);
        Button btnKhatmah = findViewById(R.id.btn_khatmah);
        Button btnBookmarks = findViewById(R.id.btn_bookmarks);
        Button btnRecitations = findViewById(R.id.btn_recitations);

        int[] disabledControls = {
                R.id.btn_settings
        };
        for (int id : disabledControls) {
            View v = findViewById(id);
            v.setEnabled(false);
            v.setAlpha(0.55f);
        }

        btnContinue.setOnClickListener(v -> openContinueReading());
        btnQuran.setOnClickListener(v ->
                startActivity(new Intent(this, SurahListActivity.class)));
        btnRetry.setOnClickListener(v -> loadQuran());
        btnSearch.setOnClickListener(v -> startActivity(new Intent(this, SearchActivity.class)));
        btnKhatmah.setOnClickListener(v -> startActivity(new Intent(this, KhatmahActivity.class)));
        btnBookmarks.setOnClickListener(v -> startActivity(new Intent(this, BookmarksActivity.class)));
        btnRecitations.setOnClickListener(v -> startActivity(new Intent(this, LibraryActivity.class)));
        // التلاوات تعتمد على الإنترنت فقط، ولا تتعطل بسبب تحميل النص القرآني.
        btnRecitations.setEnabled(true);
        btnRecitations.setAlpha(1f);

        loadQuran();
    }

    @Override
    protected void onDestroy() {
        io.shutdownNow();
        super.onDestroy();
    }

    private void openContinueReading() {
        Intent intent = new Intent(this, ReaderActivity.class);
        int surah = prefs.lastSurah();
        if (surah > 0) {
            intent.putExtra(ReaderActivity.EXTRA_SURAH, surah);
            intent.putExtra(ReaderActivity.EXTRA_AYAH, prefs.lastAyah());
        } else {
            intent.putExtra(ReaderActivity.EXTRA_SURAH, 1);
            intent.putExtra(ReaderActivity.EXTRA_AYAH, 1);
        }
        startActivity(intent);
    }

    private void loadQuran() {
        setQuranControlsEnabled(false);
        btnRetry.setVisibility(View.GONE);
        statusText.setText(R.string.quran_loading);

        io.execute(() -> {
            try {
                new QuranRepository(getApplicationContext()).verifyAndImportIfNeeded();
                main.post(() -> {
                    statusText.setText(R.string.quran_ready);
                    setQuranControlsEnabled(true);
                });
            } catch (QuranIntegrityException e) {
                showError(e.getMessage());
            } catch (RuntimeException e) {
                showError(getString(R.string.quran_error_generic));
            }
        });
    }

    private void showError(String message) {
        main.post(() -> {
            statusText.setText(message);
            btnRetry.setVisibility(View.VISIBLE);
            setQuranControlsEnabled(false);
        });
    }

    private void setQuranControlsEnabled(boolean enabled) {
        btnContinue.setEnabled(enabled);
        btnQuran.setEnabled(enabled);
        btnContinue.setAlpha(enabled ? 1f : 0.55f);
        btnQuran.setAlpha(enabled ? 1f : 0.55f);
        findViewById(R.id.btn_search).setEnabled(enabled);
        findViewById(R.id.btn_khatmah).setEnabled(enabled);
        findViewById(R.id.btn_bookmarks).setEnabled(enabled);
    }
}
