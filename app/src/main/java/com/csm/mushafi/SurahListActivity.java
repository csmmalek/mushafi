package com.csm.mushafi;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.csm.mushafi.data.QuranRepository;
import com.csm.mushafi.data.SurahEntity;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** فهرس السور: الرقم وعدد الآيات. الأسماء تظهر لاحقاً عند توفر مصدر معتمد. */
public class SurahListActivity extends AppCompatActivity {

    private final ExecutorService io = Executors.newSingleThreadExecutor();
    private final Handler main = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_surah_list);

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());
        RecyclerView list = findViewById(R.id.surah_list);
        list.setLayoutManager(new LinearLayoutManager(this));

        io.execute(() -> {
            List<SurahEntity> surahs = new QuranRepository(getApplicationContext())
                    .dao().allSurahs();
            main.post(() -> {
                if (isFinishing()) {
                    return;
                }
                list.setAdapter(new SurahAdapter(surahs, number -> {
                    Intent intent = new Intent(this, ReaderActivity.class);
                    intent.putExtra(ReaderActivity.EXTRA_SURAH, number);
                    intent.putExtra(ReaderActivity.EXTRA_AYAH, 1);
                    startActivity(intent);
                }));
            });
        });
    }

    @Override
    protected void onDestroy() {
        io.shutdownNow();
        super.onDestroy();
    }
}
