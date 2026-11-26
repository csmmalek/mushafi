package com.csm.mushafi;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.csm.mushafi.data.AppDatabase;
import com.csm.mushafi.data.DownloadDao;
import com.csm.mushafi.data.DownloadEntity;
import com.csm.mushafi.download.DownloadScheduler;
import com.csm.mushafi.quran.ArabicDigits;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** مدير التنزيلات: حالة كل سورة، وإيقاف واستئناف وإعادة محاولة وحذف. */
public class DownloadsActivity extends AppCompatActivity {

    private final ExecutorService io = Executors.newSingleThreadExecutor();
    private final Handler main = new Handler(Looper.getMainLooper());
    private DownloadDao dao;
    private RecyclerView list;
    private TextView empty;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_downloads);
        dao = AppDatabase.get(getApplicationContext()).downloadDao();
        list = findViewById(R.id.download_list);
        empty = findViewById(R.id.download_empty);
        list.setLayoutManager(new LinearLayoutManager(this));
        findViewById(R.id.btn_back).setOnClickListener(v -> finish());
    }

    @Override
    protected void onResume() {
        super.onResume();
        reload();
    }

    @Override
    protected void onDestroy() {
        io.shutdownNow();
        super.onDestroy();
    }

    private void reload() {
        io.execute(() -> {
            List<DownloadEntity> items = dao.all();
            main.post(() -> {
                if (isFinishing()) {
                    return;
                }
                empty.setVisibility(items.isEmpty() ? View.VISIBLE : View.GONE);
                list.setAdapter(new DownloadAdapter(items, this::onItemClick));
            });
        });
    }

    private void onItemClick(DownloadEntity item) {
        String[] labels;
        boolean canRetry = DownloadEntity.FAILED.equals(item.state) || DownloadEntity.PAUSED.equals(item.state);
        boolean running = DownloadEntity.RUNNING.equals(item.state) || DownloadEntity.QUEUED.equals(item.state);
        if (DownloadEntity.COMPLETED.equals(item.state)) {
            labels = new String[]{getString(R.string.download_delete)};
        } else if (running) {
            labels = new String[]{getString(R.string.download_pause), getString(R.string.download_delete)};
        } else if (canRetry) {
            labels = new String[]{getString(R.string.download_resume), getString(R.string.download_delete)};
        } else {
            labels = new String[]{getString(R.string.download_delete)};
        }
        new AlertDialog.Builder(this)
                .setTitle(SurahNames.title(this, item.surah))
                .setItems(labels, (d, which) -> {
                    String label = labels[which];
                    io.execute(() -> {
                        if (label.equals(getString(R.string.download_delete))) {
                            dao.delete(item.id);
                            new java.io.File(item.path).delete();
                            new java.io.File(item.path + ".part").delete();
                        } else if (label.equals(getString(R.string.download_pause))) {
                            item.state = DownloadEntity.PAUSED;
                            dao.update(item);
                        } else {
                            DownloadScheduler.requeue(getApplicationContext(), item);
                        }
                        reload();
                    });
                })
                .setNegativeButton(R.string.btn_cancel, null)
                .show();
    }
}
