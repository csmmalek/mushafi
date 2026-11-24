package com.csm.mushafi;

import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Layout;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.AlignmentSpan;
import android.text.style.BackgroundColorSpan;
import android.text.style.ForegroundColorSpan;
import android.text.style.RelativeSizeSpan;
import android.text.style.StyleSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.csm.mushafi.data.AyahEntity;
import com.csm.mushafi.data.BookmarkEntity;
import com.csm.mushafi.data.QuranRepository;
import com.csm.mushafi.mushaf.MushafMeta;
import com.csm.mushafi.quran.ArabicDigits;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * قراءة بالصفحات: 604 صفحة تُقلَّب بالسحب أو بالمنزلق. نص كل صفحة من النص القرآني الموثّق،
 * ويُكبَّر الخط تلقائياً ليملأ الصفحة. البسملة وراية السورة في أول السورة فقط.
 */
public class PageReaderActivity extends AppCompatActivity {

    private static final int MARKER_COLOR = Color.parseColor("#A07C1D");
    private static final int BASMALA_COLOR = Color.parseColor("#0E4D3A");

    private final ExecutorService io = Executors.newSingleThreadExecutor();
    private final Handler main = new Handler(Looper.getMainLooper());

    private ViewPager2 pager;
    private SeekBar seek;
    private TextView positionText;
    private TextView titleText;
    private ReadingPrefs prefs;
    private QuranRepository repo;

    private MushafMeta meta;
    private String[][] texts;
    private String basmala;
    private int currentPage = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_page_reader);
        prefs = new ReadingPrefs(this);
        repo = new QuranRepository(getApplicationContext());
        pager = findViewById(R.id.page_pager);
        seek = findViewById(R.id.page_seek);
        positionText = findViewById(R.id.page_position);
        titleText = findViewById(R.id.reader_title);
        findViewById(R.id.btn_back).setOnClickListener(v -> finish());
        findViewById(R.id.btn_bookmark).setOnClickListener(v -> bookmarkPage());
        seek.setMax(MushafMeta.PAGES - 1);

        int surah = getIntent().getIntExtra(ReaderActivity.EXTRA_SURAH, 1);
        int ayah = getIntent().getIntExtra(ReaderActivity.EXTRA_AYAH, 1);

        io.execute(() -> {
            try {
                MushafMeta loaded = MushafMeta.parse(readAsset("mushaf-meta.json"));
                String[][] loadedTexts = new String[MushafMeta.SURAHS + 1][];
                for (int s = 1; s <= MushafMeta.SURAHS; s++) {
                    List<AyahEntity> rows = repo.dao().ayahsOfSurah(s);
                    if (rows.size() != loaded.ayahCount(s)) {
                        throw new IllegalStateException("ayah count mismatch in surah " + s);
                    }
                    loadedTexts[s] = new String[rows.size()];
                    for (AyahEntity row : rows) {
                        loadedTexts[s][row.ayah - 1] = row.text;
                    }
                }
                main.post(() -> start(loaded, loadedTexts, surah, ayah));
            } catch (Exception e) {
                main.post(() -> positionText.setText(R.string.quran_error_generic));
            }
        });
    }

    @Override
    protected void onDestroy() {
        io.shutdownNow();
        super.onDestroy();
    }

    private String readAsset(String name) throws Exception {
        try (InputStream in = getAssets().open(name)) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) != -1) {
                out.write(buf, 0, n);
            }
            return new String(out.toByteArray(), StandardCharsets.UTF_8);
        }
    }

    private void start(MushafMeta loaded, String[][] loadedTexts, int surah, int ayah) {
        if (isFinishing()) {
            return;
        }
        meta = loaded;
        texts = loadedTexts;
        basmala = texts[1][0];
        int s = Math.max(1, Math.min(MushafMeta.SURAHS, surah));
        int a = Math.max(1, Math.min(meta.ayahCount(s), ayah));
        int startPage = meta.pageOf(s, a);

        pager.setAdapter(new PageAdapter());
        pager.setOffscreenPageLimit(1);
        pager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                updatePage(position + 1);
            }
        });
        seek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar bar, int progress, boolean fromUser) {
                if (fromUser) {
                    showPosition(progress + 1);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar bar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar bar) {
                pager.setCurrentItem(bar.getProgress(), false);
            }
        });
        pager.setCurrentItem(startPage - 1, false);
        updatePage(startPage);
    }

    private void showPosition(int page) {
        positionText.setText(getString(R.string.page_position,
                ArabicDigits.format(page), ArabicDigits.format(MushafMeta.PAGES)));
    }

    private void updatePage(int page) {
        currentPage = page;
        int[] first = meta.ayahsOnPage(page).get(0);
        prefs.saveLast(first[0], first[1]);
        titleText.setText(getString(R.string.surah_name_format, meta.name(first[0])));
        showPosition(page);
        seek.setProgress(page - 1);
    }

    private void bookmarkPage() {
        if (meta == null) {
            return;
        }
        int[] first = meta.ayahsOnPage(currentPage).get(0);
        io.execute(() -> {
            long id = repo.dao().insertBookmark(new BookmarkEntity(
                    first[0], first[1], "", System.currentTimeMillis()));
            main.post(() -> Toast.makeText(this,
                    id == -1 ? R.string.bookmark_exists : R.string.bookmark_saved,
                    Toast.LENGTH_SHORT).show());
        });
    }

    /** يبني نص الصفحة: راية السورة والبسملة عند بداية السورة، ثم الآيات بعلامات الأرقام. */
    private CharSequence buildPage(int page) {
        SpannableStringBuilder sb = new SpannableStringBuilder();
        List<int[]> ayahs = meta.ayahsOnPage(page);
        int lastSurah = -1;
        for (int[] item : ayahs) {
            int s = item[0];
            int a = item[1];
            if (s != lastSurah) {
                if (lastSurah != -1) {
                    endParagraph(sb);
                }
                if (a == 1) {
                    appendHeader(sb, s);
                    if (s != 1 && s != 9) {
                        appendBasmala(sb);
                    }
                }
                lastSurah = s;
            }
            String text = texts[s][a - 1];
            if (a == 1 && s != 1 && s != 9 && text.startsWith(basmala)) {
                text = text.substring(basmala.length()).trim();
            }
            sb.append(text);
            int markerStart = sb.length();
            sb.append(" ﴿").append(ArabicDigits.format(a)).append("﴾");
            sb.setSpan(new ForegroundColorSpan(MARKER_COLOR), markerStart, sb.length(),
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            sb.append(' ');
        }
        return sb;
    }

    private void endParagraph(SpannableStringBuilder sb) {
        if (sb.length() > 0 && sb.charAt(sb.length() - 1) == ' ') {
            sb.delete(sb.length() - 1, sb.length());
        }
        sb.append('\n');
    }

    private void appendHeader(SpannableStringBuilder sb, int surah) {
        int start = sb.length();
        sb.append("  ").append(getString(R.string.surah_name_format, meta.name(surah)))
                .append("  ");
        int end = sb.length();
        sb.setSpan(new ForegroundColorSpan(ContextCompat.getColor(this, R.color.gold_light)),
                start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        sb.setSpan(new BackgroundColorSpan(ContextCompat.getColor(this, R.color.emerald_mid)),
                start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        sb.setSpan(new RelativeSizeSpan(1.15f), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        sb.setSpan(new StyleSpan(Typeface.BOLD), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        sb.append('\n');
        sb.setSpan(new AlignmentSpan.Standard(Layout.Alignment.ALIGN_CENTER),
                start, sb.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
    }

    private void appendBasmala(SpannableStringBuilder sb) {
        int start = sb.length();
        sb.append(basmala);
        sb.setSpan(new ForegroundColorSpan(BASMALA_COLOR), start, sb.length(),
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        sb.append('\n');
        sb.setSpan(new AlignmentSpan.Standard(Layout.Alignment.ALIGN_CENTER),
                start, sb.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
    }

    private final class PageAdapter extends RecyclerView.Adapter<PageAdapter.Holder> {

        final class Holder extends RecyclerView.ViewHolder {
            final TextView text;

            Holder(View view) {
                super(view);
                text = view.findViewById(R.id.page_text);
            }
        }

        @NonNull
        @Override
        public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_mushaf_page, parent, false);
            return new Holder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull Holder holder, int position) {
            holder.text.setText(buildPage(position + 1));
        }

        @Override
        public int getItemCount() {
            return MushafMeta.PAGES;
        }
    }
}
