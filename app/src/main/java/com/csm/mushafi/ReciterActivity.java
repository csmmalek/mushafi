package com.csm.mushafi;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.csm.mushafi.audio.CatalogueCache;
import com.csm.mushafi.audio.ReciterCatalogue;
import com.csm.mushafi.data.AppDatabase;
import com.csm.mushafi.data.DownloadEntity;
import com.csm.mushafi.download.DownloadScheduler;
import com.csm.mushafi.download.NetworkStatus;
import com.csm.mushafi.playback.PlayerActivity;
import com.csm.mushafi.quran.ArabicDigits;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** تفاصيل القارئ: السور المتاحة، وحالة كل سورة (محمّلة أو لا)، والتنزيل لكل سورة أو للكل. */
public class ReciterActivity extends AppCompatActivity {

    static final String EXTRA_RECITER_INDEX = "reciterIndex";
    static final String EXTRA_MOSHAF_INDEX = "moshafIndex";

    private final ExecutorService io = Executors.newSingleThreadExecutor();
    private final Handler main = new Handler(Looper.getMainLooper());
    private ReciterCatalogue.Reciter reciter;
    private ReciterCatalogue.Moshaf moshaf;
    private RecyclerView list;
    private Button downloadAll;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reciter);
        findViewById(R.id.btn_back).setOnClickListener(v -> finish());

        ReciterCatalogue.Result result = CatalogueCache.get();
        int ri = getIntent().getIntExtra(EXTRA_RECITER_INDEX, -1);
        int mi = getIntent().getIntExtra(EXTRA_MOSHAF_INDEX, 0);
        if (result == null || ri < 0 || ri >= result.reciters.size()) {
            finish();
            return;
        }
        reciter = result.reciters.get(ri);
        if (mi < 0 || mi >= reciter.moshafs.size()) {
            mi = 0;
        }
        moshaf = reciter.moshafs.get(mi);

        ((TextView) findViewById(R.id.reciter_name)).setText(reciter.name);
        ((TextView) findViewById(R.id.reciter_moshaf)).setText(moshaf.name);
        ((TextView) findViewById(R.id.reciter_source)).setText(getString(R.string.reciter_source_format,
                ArabicDigits.format(moshaf.surahs.size())));

        list = findViewById(R.id.reciter_surahs);
        list.setLayoutManager(new LinearLayoutManager(this));
        downloadAll = findViewById(R.id.btn_download_all);
        downloadAll.setOnClickListener(v -> confirmDownloadAll());
        reload();
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
            Map<Integer, DownloadEntity> completed = new HashMap<>();
            for (DownloadEntity d : AppDatabase.get(getApplicationContext()).downloadDao()
                    .completedFor(moshaf.server)) {
                if (new File(d.path).exists()) {
                    completed.put(d.surah, d);
                }
            }
            boolean online = NetworkStatus.isOnline(getApplicationContext());
            main.post(() -> {
                if (isFinishing()) {
                    return;
                }
                list.setAdapter(new SurahAudioAdapter(moshaf.surahs, completed, online,
                        new SurahAudioAdapter.Listener() {
                            @Override
                            public void onPlay(int index) {
                                play(index);
                            }

                            @Override
                            public void onLongPress(int surah) {
                                manageSurah(surah);
                            }

                            @Override
                            public void onBlocked(int surah) {
                                Toast.makeText(ReciterActivity.this,
                                        R.string.play_needs_network, Toast.LENGTH_SHORT).show();
                            }
                        }));
            });
        });
    }

    private void play(int index) {
        Intent intent = new Intent(this, PlayerActivity.class);
        intent.putExtra(PlayerActivity.EXTRA_RECITER, reciter.name);
        intent.putExtra(PlayerActivity.EXTRA_MOSHAF, moshaf.name);
        intent.putExtra(PlayerActivity.EXTRA_SERVER, moshaf.server);
        intent.putExtra(PlayerActivity.EXTRA_SURAHS, toArray(moshaf.surahs));
        intent.putExtra(PlayerActivity.EXTRA_START_INDEX, index);
        startActivity(intent);
    }

    private void manageSurah(int surah) {
        String download = getString(R.string.download_surah);
        String delete = getString(R.string.download_delete_file);
        new AlertDialog.Builder(this)
                .setTitle(SurahNames.title(this, surah))
                .setItems(new String[]{download, delete}, (d, which) -> {
                    if (which == 0) {
                        enqueue(java.util.Collections.singletonList(surah));
                    } else {
                        io.execute(() -> {
                            com.csm.mushafi.data.AppDatabase db = AppDatabase.get(getApplicationContext());
                            DownloadEntity item = db.downloadDao().find(moshaf.server, surah);
                            if (item != null) {
                                new File(item.path).delete();
                                new File(item.path + ".part").delete();
                                db.downloadDao().delete(item.id);
                            }
                            main.post(this::reload);
                        });
                    }
                })
                .setNegativeButton(R.string.btn_cancel, null)
                .show();
    }

    private void confirmDownloadAll() {
        int count = moshaf.surahs.size();
        new AlertDialog.Builder(this)
                .setTitle(R.string.download_all_title)
                .setMessage(getString(R.string.download_all_message, ArabicDigits.format(count)))
                .setPositiveButton(R.string.btn_save, (d, w) -> enqueue(moshaf.surahs))
                .setNegativeButton(R.string.btn_cancel, null)
                .show();
    }

    private void enqueue(List<Integer> surahs) {
        io.execute(() -> {
            if (!com.csm.mushafi.download.DownloadStore.hasSpace(getApplicationContext())) {
                main.post(() -> Toast.makeText(this, R.string.download_no_space, Toast.LENGTH_LONG).show());
                return;
            }
            int added = DownloadScheduler.enqueue(getApplicationContext(),
                    moshaf.server, reciter.name, moshaf.name, surahs);
            main.post(() -> {
                Toast.makeText(this, getString(R.string.download_added_format,
                        ArabicDigits.format(added)), Toast.LENGTH_SHORT).show();
                reload();
            });
        });
    }

    private static int[] toArray(List<Integer> values) {
        int[] out = new int[values.size()];
        for (int i = 0; i < out.length; i++) {
            out[i] = values.get(i);
        }
        return out;
    }
}
