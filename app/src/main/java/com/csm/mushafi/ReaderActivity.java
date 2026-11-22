package com.csm.mushafi;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.csm.mushafi.data.AyahEntity;
import com.csm.mushafi.data.QuranRepository;
import com.csm.mushafi.quran.ArabicDigits;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** القارئ: يعرض آيات السورة من قاعدة البيانات المستوردة من الملف المعتمد فقط. */
public class ReaderActivity extends AppCompatActivity {

    static final String EXTRA_SURAH = "surah";
    static final String EXTRA_AYAH = "ayah";

    private final ExecutorService io = Executors.newSingleThreadExecutor();
    private final Handler main = new Handler(Looper.getMainLooper());

    private RecyclerView list;
    private AyahAdapter adapter;
    private LinearLayoutManager layout;
    private TextView titleText;
    private TextView positionText;
    private Button fontMinus;
    private Button fontPlus;
    private ReadingPrefs prefs;
    private int surah;
    private int startAyah;
    private int ayahCount;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reader);
        prefs = new ReadingPrefs(this);

        surah = getIntent().getIntExtra(EXTRA_SURAH, 1);
        startAyah = getIntent().getIntExtra(EXTRA_AYAH, 1);

        titleText = findViewById(R.id.reader_title);
        positionText = findViewById(R.id.reader_position);
        fontMinus = findViewById(R.id.btn_font_minus);
        fontPlus = findViewById(R.id.btn_font_plus);
        list = findViewById(R.id.ayah_list);
        layout = new LinearLayoutManager(this);
        list.setLayoutManager(layout);

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());
        fontMinus.setOnClickListener(v -> changeFont(-2));
        fontPlus.setOnClickListener(v -> changeFont(2));

        titleText.setText(getString(R.string.surah_number_format, ArabicDigits.format(surah)));
        adapter = new AyahAdapter(prefs.fontSp());
        list.setAdapter(adapter);
        list.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(RecyclerView rv, int newState) {
                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    saveCurrentPosition();
                }
            }

            @Override
            public void onScrolled(RecyclerView rv, int dx, int dy) {
                updatePositionText();
            }
        });

        io.execute(() -> {
            List<AyahEntity> ayahs = new QuranRepository(getApplicationContext())
                    .dao().ayahsOfSurah(surah);
            main.post(() -> {
                if (isFinishing()) {
                    return;
                }
                adapter.setAyahs(ayahs);
                ayahCount = ayahs.size();
                int index = Math.max(0, Math.min(startAyah - 1, ayahCount - 1));
                layout.scrollToPositionWithOffset(index, 0);
                updatePositionText();
                saveCurrentPosition();
            });
        });
    }

    @Override
    protected void onDestroy() {
        io.shutdownNow();
        super.onDestroy();
    }

    private int firstVisibleAyah() {
        int pos = layout.findFirstVisibleItemPosition();
        return pos == RecyclerView.NO_POSITION ? startAyah : pos + 1;
    }

    private void updatePositionText() {
        if (ayahCount == 0) {
            return;
        }
        positionText.setText(getString(R.string.reader_position_format,
                ArabicDigits.format(surah),
                ArabicDigits.format(firstVisibleAyah()),
                ArabicDigits.format(ayahCount)));
    }

    private void saveCurrentPosition() {
        if (ayahCount > 0) {
            prefs.saveLast(surah, firstVisibleAyah());
        }
    }

    private void changeFont(int delta) {
        int next = prefs.fontSp() + delta;
        prefs.saveFontSp(next);
        adapter.setFontSp(prefs.fontSp());
        fontMinus.setEnabled(prefs.fontSp() > ReadingPrefs.MIN_FONT);
        fontPlus.setEnabled(prefs.fontSp() < ReadingPrefs.MAX_FONT);
    }
}
