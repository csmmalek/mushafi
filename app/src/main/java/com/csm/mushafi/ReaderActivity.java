package com.csm.mushafi;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.csm.mushafi.data.AyahEntity;
import com.csm.mushafi.data.BookmarkEntity;
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
    private Button prevSurah;
    private Button nextSurah;
    private LinearLayout selectionBar;
    private TextView selectionText;
    private int selectedAyah;
    private ReadingPrefs prefs;
    private QuranRepository repo;
    private int surah;
    private int startAyah;
    private int ayahCount;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reader);
        prefs = new ReadingPrefs(this);
        repo = new QuranRepository(getApplicationContext());

        surah = getIntent().getIntExtra(EXTRA_SURAH, 1);
        startAyah = getIntent().getIntExtra(EXTRA_AYAH, 1);

        titleText = findViewById(R.id.reader_title);
        positionText = findViewById(R.id.reader_position);
        fontMinus = findViewById(R.id.btn_font_minus);
        fontPlus = findViewById(R.id.btn_font_plus);
        prevSurah = findViewById(R.id.btn_prev_surah);
        nextSurah = findViewById(R.id.btn_next_surah);
        ((TextView) findViewById(R.id.surah_header)).setText(
                getString(R.string.reader_surah_header, ArabicDigits.format(surah)));
        selectionBar = findViewById(R.id.selection_bar);
        selectionText = findViewById(R.id.selection_text);
        findViewById(R.id.btn_copy_ayah).setOnClickListener(v -> copySelectedAyah());
        findViewById(R.id.btn_bookmark_ayah).setOnClickListener(v -> bookmarkSelectedAyah());
        findViewById(R.id.btn_clear_selection).setOnClickListener(v -> selectAyah(0));
        list = findViewById(R.id.ayah_list);
        layout = new LinearLayoutManager(this);
        list.setLayoutManager(layout);

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());
        findViewById(R.id.btn_bookmark).setOnClickListener(v -> saveBookmark());
        fontMinus.setOnClickListener(v -> changeFont(-2));
        fontPlus.setOnClickListener(v -> changeFont(2));
        prevSurah.setOnClickListener(v -> openSurah(surah - 1));
        nextSurah.setOnClickListener(v -> openSurah(surah + 1));
        prevSurah.setEnabled(surah > 1);
        nextSurah.setEnabled(surah < QuranRepository.SURAH_TOTAL);

        titleText.setText(getString(R.string.surah_number_format, ArabicDigits.format(surah)));
        adapter = new AyahAdapter(prefs.fontSp());
        adapter.setOnAyahClick(this::selectAyah);
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

        changeFontButtons();
        io.execute(() -> {
            List<AyahEntity> ayahs = repo.dao().ayahsOfSurah(surah);
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

    private void saveBookmark() {
        // إن وُجدت آية محددة تُحفظ علامتها، وإلا تُحفظ أول آية ظاهرة.
        bookmarkAyah(selectedAyah > 0 ? selectedAyah : firstVisibleAyah());
    }

    private void bookmarkSelectedAyah() {
        if (selectedAyah > 0) {
            bookmarkAyah(selectedAyah);
        }
    }

    private void bookmarkAyah(int ayah) {
        io.execute(() -> {
            long id = repo.dao().insertBookmark(new BookmarkEntity(
                    surah, ayah, "", System.currentTimeMillis()));
            main.post(() -> Toast.makeText(this,
                    id == -1 ? R.string.bookmark_exists : R.string.bookmark_saved,
                    Toast.LENGTH_SHORT).show());
        });
    }

    /** يحدد آية بالضغط عليها، ويلغي التحديد عند الضغط مرة أخرى أو عند القيمة 0. */
    private void selectAyah(int ayah) {
        selectedAyah = (ayah == selectedAyah) ? 0 : ayah;
        adapter.setSelectedAyah(selectedAyah);
        if (selectedAyah > 0) {
            selectionText.setText(getString(R.string.reader_selected_format,
                    ArabicDigits.format(selectedAyah)));
            selectionBar.setVisibility(View.VISIBLE);
        } else {
            selectionBar.setVisibility(View.GONE);
        }
    }

    private void copySelectedAyah() {
        String text = selectedAyah > 0 ? adapter.rawTextOf(selectedAyah) : null;
        if (text == null) {
            return;
        }
        String full = text + "  ﴿" + ArabicDigits.format(selectedAyah) + "﴾";
        ClipboardManager cm = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
        if (cm != null) {
            cm.setPrimaryClip(ClipData.newPlainText("ayah", full));
            Toast.makeText(this, R.string.reader_copied, Toast.LENGTH_SHORT).show();
        }
    }

    private void openSurah(int target) {
        if (target < 1 || target > QuranRepository.SURAH_TOTAL) {
            return;
        }
        Intent intent = new Intent(this, ReaderActivity.class);
        intent.putExtra(EXTRA_SURAH, target);
        intent.putExtra(EXTRA_AYAH, 1);
        startActivity(intent);
        finish();
    }

    private void changeFont(int delta) {
        prefs.saveFontSp(prefs.fontSp() + delta);
        adapter.setFontSp(prefs.fontSp());
        changeFontButtons();
    }

    private void changeFontButtons() {
        fontMinus.setEnabled(prefs.fontSp() > ReadingPrefs.MIN_FONT);
        fontPlus.setEnabled(prefs.fontSp() < ReadingPrefs.MAX_FONT);
    }
}
