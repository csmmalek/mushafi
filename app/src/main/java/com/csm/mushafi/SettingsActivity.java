package com.csm.mushafi;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.Button;
import android.widget.RadioGroup;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.SwitchCompat;

import com.csm.mushafi.data.AppDatabase;
import com.csm.mushafi.data.DownloadDao;
import com.csm.mushafi.data.DownloadEntity;
import com.csm.mushafi.quran.ArabicDigits;

import java.io.File;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** الإعدادات: المصادر والحقوق، والتخزين، وسياسة الخصوصية. */
public class SettingsActivity extends BaseActivity {

    private final ExecutorService io = Executors.newSingleThreadExecutor();
    private final Handler main = new Handler(Looper.getMainLooper());
    private TextView storage;
    private Button clear;
    private DownloadDao dao;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        dao = AppDatabase.get(getApplicationContext()).downloadDao();
        storage = findViewById(R.id.settings_storage);
        clear = findViewById(R.id.btn_clear_audio);
        findViewById(R.id.btn_back).setOnClickListener(v -> finish());
        bindReadingSettings();
        clear.setOnClickListener(v -> confirmClear());
        refreshStorage();
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshStorage();
    }

    @Override
    protected void onDestroy() {
        io.shutdownNow();
        super.onDestroy();
    }

    private void refreshStorage() {
        io.execute(() -> {
            List<DownloadEntity> items = dao.all();
            long bytes = 0;
            int count = 0;
            for (DownloadEntity d : items) {
                if (DownloadEntity.COMPLETED.equals(d.state) && new File(d.path).exists()) {
                    bytes += new File(d.path).length();
                    count++;
                }
            }
            long total = bytes;
            int done = count;
            main.post(() -> {
                if (isFinishing()) {
                    return;
                }
                storage.setText(getString(R.string.settings_storage_format,
                        ArabicDigits.format(done), ArabicDigits.format((int) (total / (1024 * 1024)))));
                clear.setEnabled(done > 0);
            });
        });
    }

    private void confirmClear() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.settings_clear_title)
                .setMessage(R.string.settings_clear_message)
                .setPositiveButton(R.string.btn_delete, (d, w) -> io.execute(() -> {
                    for (DownloadEntity item : dao.all()) {
                        new File(item.path).delete();
                        new File(item.path + ".part").delete();
                        dao.delete(item.id);
                    }
                    main.post(() -> {
                        Toast.makeText(this, R.string.settings_cleared, Toast.LENGTH_SHORT).show();
                        refreshStorage();
                    });
                }))
                .setNegativeButton(R.string.btn_cancel, null)
                .show();
    }

    private void bindReadingSettings() {
        ReadingPrefs reading = new ReadingPrefs(this);
        int[] appIds = {R.id.app_theme_0, R.id.app_theme_1, R.id.app_theme_2, R.id.app_theme_3, R.id.app_theme_4};
        RadioGroup appGroup = findViewById(R.id.settings_app_theme);
        appGroup.check(appIds[reading.appTheme()]);
        appGroup.setOnCheckedChangeListener((g, checkedId) -> {
            for (int i = 0; i < appIds.length; i++) {
                if (appIds[i] == checkedId && reading.appTheme() != i) {
                    reading.saveAppTheme(i);
                    recreate();
                }
            }
        });
        int[] themeIds = {R.id.theme_0, R.id.theme_1, R.id.theme_2, R.id.theme_3};
        RadioGroup group = findViewById(R.id.settings_page_theme);
        group.check(themeIds[Math.max(0, Math.min(themeIds.length - 1, reading.pageTheme()))]);
        group.setOnCheckedChangeListener((g, id) -> {
            for (int i = 0; i < themeIds.length; i++) {
                if (themeIds[i] == id) {
                    reading.savePageTheme(i);
                }
            }
        });
        TextView fontLabel = findViewById(R.id.settings_font_label);
        SeekBar fontSeek = findViewById(R.id.settings_font_seek);
        fontSeek.setProgress((reading.pageFontPercent() - 60) / 5);
        fontLabel.setText(getString(R.string.settings_font_size, com.csm.mushafi.quran.ArabicDigits.format(reading.pageFontPercent())));
        fontSeek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar bar, int progress, boolean fromUser) {
                int percent = 60 + progress * 5;
                fontLabel.setText(getString(R.string.settings_font_size, com.csm.mushafi.quran.ArabicDigits.format(percent)));
                if (fromUser) {
                    reading.savePageFontPercent(percent);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar bar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar bar) {
            }
        });
        SwitchCompat exactSwitch = findViewById(R.id.switch_exact);
        exactSwitch.setChecked(reading.exactLayout());
        exactSwitch.setOnCheckedChangeListener((b, on) -> reading.saveExactLayout(on));
        SwitchCompat kashida = findViewById(R.id.switch_kashida);
        kashida.setChecked(reading.kashida());
        kashida.setOnCheckedChangeListener((b, on) -> reading.saveKashida(on));
        SwitchCompat keep = findViewById(R.id.switch_keep_screen);
        keep.setChecked(reading.keepScreenOn());
        keep.setOnCheckedChangeListener((b, on) -> reading.saveKeepScreenOn(on));
        SwitchCompat muq = findViewById(R.id.switch_muqattaat);
        muq.setChecked(reading.muqattaatUthmani());
        muq.setOnCheckedChangeListener((b, on) -> reading.saveMuqattaatUthmani(on));
    }
}
