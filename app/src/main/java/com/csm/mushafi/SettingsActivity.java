package com.csm.mushafi;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.csm.mushafi.data.AppDatabase;
import com.csm.mushafi.data.DownloadDao;
import com.csm.mushafi.data.DownloadEntity;
import com.csm.mushafi.quran.ArabicDigits;

import java.io.File;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** الإعدادات: المصادر والحقوق، والتخزين، وسياسة الخصوصية. */
public class SettingsActivity extends AppCompatActivity {

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
}
