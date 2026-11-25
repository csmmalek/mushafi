package com.csm.mushafi;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.RelativeSizeSpan;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.csm.mushafi.data.QuranRepository;
import com.csm.mushafi.quran.ArabicDigits;
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
        Button btnDownloads = findViewById(R.id.btn_downloads);
        Button btnSettings = findViewById(R.id.btn_settings);

        int[] disabledControls = {};
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
        btnDownloads.setOnClickListener(v -> startActivity(new Intent(this, DownloadsActivity.class)));
        btnDownloads.setEnabled(true);
        btnSettings.setOnClickListener(v -> startActivity(new Intent(this, SettingsActivity.class)));
        btnDownloads.setAlpha(1f);

        showContinueLabel();
        loadQuran();
    }

    @Override
    protected void onResume() {
        super.onResume();
        showContinueLabel();
    }

    /** يعرض آخر موضع قراءة محفوظ تحت عنوان زر المتابعة، أو نصاً بديلاً إن لم توجد قراءة سابقة. */
    private void showContinueLabel() {
        int surah = prefs.lastSurah();
        String second = surah > 0
                ? getString(R.string.continue_position,
                        ArabicDigits.format(surah), ArabicDigits.format(prefs.lastAyah()))
                : getString(R.string.continue_from_start);
        String title = getString(R.string.btn_continue);
        SpannableString label = new SpannableString(title + "\n" + second);
        label.setSpan(new RelativeSizeSpan(0.7f), title.length() + 1, label.length(),
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        btnContinue.setText(label);
    }

    @Override
    protected void onDestroy() {
        io.shutdownNow();
        super.onDestroy();
    }

    private void openContinueReading() {
        Intent intent = new Intent(this, PageReaderActivity.class);
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
