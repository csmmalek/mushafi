package com.csm.mushafi;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Layout;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.style.AlignmentSpan;
import android.text.style.BackgroundColorSpan;
import android.text.style.ClickableSpan;
import android.text.style.ForegroundColorSpan;
import android.text.style.RelativeSizeSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.csm.mushafi.data.AyahEntity;
import com.csm.mushafi.data.BookmarkEntity;
import com.csm.mushafi.data.QuranRepository;
import com.csm.mushafi.mushaf.MushafMeta;
import com.csm.mushafi.mushaf.MuqattaatDisplay;
import com.csm.mushafi.quran.ArabicDigits;
import com.csm.mushafi.view.AyahMarkerSpan;
import com.csm.mushafi.view.FitPageTextView;
import com.csm.mushafi.view.PageTheme;
import com.csm.mushafi.view.SurahFrameSpan;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * قراءة بالصفحات: 604 صفحة تُقلَّب بالسحب أو بالمنزلق. نص كل صفحة من النص القرآني الموثّق،
 * ويُقاس الخط ليملأ الصفحة بلا فراغ. راية السورة والبسملة في أول السورة فقط.
 * الضغط على آية يحددها ويفتح النسخ والمشاركة والعلامة.
 */
public class PageReaderActivity extends AppCompatActivity {

    private final ExecutorService io = Executors.newSingleThreadExecutor();
    private final Handler main = new Handler(Looper.getMainLooper());

    private ViewPager2 pager;
    private SeekBar seek;
    private TextView positionText;
    private TextView titleText;
    private View selectionBar;
    private TextView selectionText;
    private ReadingPrefs prefs;
    private QuranRepository repo;
    private PageAdapter adapter;

    private MushafMeta meta;
    private MuqattaatDisplay muqattaat;
    private String[][] texts;
    private String basmala;
    private int currentPage = 1;

    private int selSurah;
    private int selAyah;
    private int selPage;

    private int appliedTheme = -1;
    private boolean appliedMuq;

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
        selectionBar = findViewById(R.id.page_selection_bar);
        selectionText = findViewById(R.id.page_selection_text);
        findViewById(R.id.btn_back).setOnClickListener(v -> finish());
        findViewById(R.id.btn_bookmark).setOnClickListener(v -> bookmarkPage());
        findViewById(R.id.btn_sel_copy).setOnClickListener(v -> copySelected());
        findViewById(R.id.btn_sel_share).setOnClickListener(v -> shareSelected());
        findViewById(R.id.btn_sel_bookmark).setOnClickListener(v -> bookmarkSelected());
        findViewById(R.id.btn_sel_clear).setOnClickListener(v -> clearSelection());
        titleText.setOnClickListener(v -> startActivity(new Intent(this, SurahListActivity.class)));
        seek.setMax(MushafMeta.PAGES - 1);

        int surah = getIntent().getIntExtra(ReaderActivity.EXTRA_SURAH, 1);
        int ayah = getIntent().getIntExtra(ReaderActivity.EXTRA_AYAH, 1);

        io.execute(() -> {
            try {
                MushafMeta loaded = MushafMeta.parse(readAsset("mushaf-meta.json"));
                MuqattaatDisplay loadedMuq = MuqattaatDisplay.parse(readAsset("muqattaat-display.json"));
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
                main.post(() -> {
                    muqattaat = loadedMuq;
                    start(loaded, loadedTexts, surah, ayah);
                });
            } catch (Exception e) {
                main.post(() -> positionText.setText(R.string.quran_error_generic));
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (prefs.keepScreenOn()) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        } else {
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }
        int theme = prefs.pageTheme();
        boolean muq = prefs.muqattaatUthmani();
        if (adapter != null && (theme != appliedTheme || muq != appliedMuq)) {
            adapter.notifyDataSetChanged();
        }
        appliedTheme = theme;
        appliedMuq = muq;
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

        adapter = new PageAdapter();
        pager.setAdapter(adapter);
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
        if (selSurah != 0 && selPage != page) {
            clearSelection();
        }
        int[] first = meta.ayahsOnPage(page).get(0);
        prefs.saveLast(first[0], first[1]);
        titleText.setText(getString(R.string.surah_name_format, meta.name(first[0])));
        showPosition(page);
        seek.setProgress(page - 1);
    }

    // ---- تحديد الآية ----

    private void selectAyah(int surah, int ayah) {
        if (selSurah == surah && selAyah == ayah) {
            clearSelection();
            return;
        }
        selSurah = surah;
        selAyah = ayah;
        selPage = currentPage;
        selectionText.setText(getString(R.string.selection_ayah_format,
                meta.name(surah), ArabicDigits.format(ayah)));
        selectionBar.setVisibility(View.VISIBLE);
        adapter.notifyItemChanged(selPage - 1);
    }

    private void clearSelection() {
        int page = selPage;
        selSurah = 0;
        selAyah = 0;
        selectionBar.setVisibility(View.GONE);
        if (adapter != null && page > 0) {
            adapter.notifyItemChanged(page - 1);
        }
    }

    private String selectedFullText() {
        if (selSurah == 0) {
            return null;
        }
        return texts[selSurah][selAyah - 1] + " ﴿" + ArabicDigits.format(selAyah) + "﴾";
    }

    private void copySelected() {
        String full = selectedFullText();
        ClipboardManager cm = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
        if (full != null && cm != null) {
            cm.setPrimaryClip(ClipData.newPlainText("ayah", full));
            Toast.makeText(this, R.string.reader_copied, Toast.LENGTH_SHORT).show();
        }
    }

    private void shareSelected() {
        String full = selectedFullText();
        if (full == null) {
            return;
        }
        Intent send = new Intent(Intent.ACTION_SEND);
        send.setType("text/plain");
        send.putExtra(Intent.EXTRA_TEXT,
                full + "\n" + getString(R.string.surah_name_format, meta.name(selSurah)));
        startActivity(Intent.createChooser(send, getString(R.string.share_title)));
    }

    private void bookmarkSelected() {
        if (selSurah != 0) {
            saveBookmark(selSurah, selAyah);
        }
    }

    private void bookmarkPage() {
        if (meta == null) {
            return;
        }
        int[] first = meta.ayahsOnPage(currentPage).get(0);
        saveBookmark(first[0], first[1]);
    }

    private void saveBookmark(int surah, int ayah) {
        io.execute(() -> {
            long id = repo.dao().insertBookmark(new BookmarkEntity(
                    surah, ayah, "", System.currentTimeMillis()));
            main.post(() -> Toast.makeText(this,
                    id == -1 ? R.string.bookmark_exists : R.string.bookmark_saved,
                    Toast.LENGTH_SHORT).show());
        });
    }

    // ---- بناء نص الصفحة ----

    private final class AyahClick extends ClickableSpan {
        private final int surah;
        private final int ayah;

        AyahClick(int surah, int ayah) {
            this.surah = surah;
            this.ayah = ayah;
        }

        @Override
        public void onClick(@NonNull View widget) {
            selectAyah(surah, ayah);
        }

        @Override
        public void updateDrawState(@NonNull TextPaint ds) {
            // لا تغيير على شكل النص
        }
    }

    private CharSequence buildPage(int page, FitPageTextView host, PageTheme theme, boolean useMuq) {
        SpannableStringBuilder sb = new SpannableStringBuilder();
        int lastSurah = -1;
        for (int[] item : meta.ayahsOnPage(page)) {
            int s = item[0];
            int a = item[1];
            if (s != lastSurah) {
                if (lastSurah != -1) {
                    endParagraph(sb);
                }
                if (a == 1) {
                    appendHeader(sb, s, host, theme);
                    if (s != 1 && s != 9) {
                        appendBasmala(sb, theme);
                    }
                }
                lastSurah = s;
            }
            String text = texts[s][a - 1];
            if (a == 1 && s != 1 && s != 9 && text.startsWith(basmala)) {
                text = text.substring(basmala.length()).trim();
            }
            if (useMuq) {
                text = muqattaat.apply(s, a, text);
            }
            int ayahStart = sb.length();
            sb.append(text).append(' ');
            int numberStart = sb.length();
            String number = ArabicDigits.format(a);
            sb.append(number);
            sb.setSpan(new AyahMarkerSpan(number, theme), numberStart, sb.length(),
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            sb.setSpan(new AyahClick(s, a), ayahStart, sb.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            if (s == selSurah && a == selAyah) {
                sb.setSpan(new BackgroundColorSpan(theme.highlight), ayahStart, sb.length(),
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
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

    private void appendHeader(SpannableStringBuilder sb, int surah, FitPageTextView host,
                              PageTheme theme) {
        int start = sb.length();
        String name = "سُورَةُ " + meta.name(surah);
        sb.append(name);
        sb.setSpan(new SurahFrameSpan(name, ArabicDigits.format(surah), ArabicDigits.format(meta.ayahCount(surah)), host, theme), start, sb.length(),
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        sb.setSpan(new RelativeSizeSpan(1.0f), start, sb.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        sb.append('\n');
        sb.setSpan(new AlignmentSpan.Standard(Layout.Alignment.ALIGN_CENTER),
                start, sb.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
    }

    private void appendBasmala(SpannableStringBuilder sb, PageTheme theme) {
        int start = sb.length();
        sb.append(basmala);
        sb.setSpan(new ForegroundColorSpan(theme.basmala), start, sb.length(),
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        sb.append('\n');
        sb.setSpan(new AlignmentSpan.Standard(Layout.Alignment.ALIGN_CENTER),
                start, sb.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
    }

    private final class PageAdapter extends RecyclerView.Adapter<PageAdapter.Holder> {

        final class Holder extends RecyclerView.ViewHolder {
            final View root;
            final FitPageTextView text;

            Holder(View view) {
                super(view);
                root = view.findViewById(R.id.page_root);
                text = view.findViewById(R.id.page_text);
            }
        }

        @NonNull
        @Override
        public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new Holder(LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_mushaf_page, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull Holder holder, int position) {
            PageTheme theme = PageTheme.of(prefs.pageTheme());
            GradientDrawable paper = new GradientDrawable();
            paper.setColor(theme.paper);
            paper.setStroke((int) (4 * getResources().getDisplayMetrics().density), theme.frame);
            holder.root.setBackground(paper);
            holder.text.setTextColor(theme.ink);
            holder.text.setText(buildPage(position + 1, holder.text, theme, prefs.muqattaatUthmani()));
        }

        @Override
        public int getItemCount() {
            return MushafMeta.PAGES;
        }
    }
}
