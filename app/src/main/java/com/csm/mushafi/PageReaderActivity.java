package com.csm.mushafi;

import com.csm.mushafi.view.SurahHeaderSpan;
import androidx.core.content.res.ResourcesCompat;
import android.graphics.Typeface;
import android.Manifest;
import android.content.ComponentName;
import android.content.pm.PackageManager;
import android.os.Build;
import android.widget.Button;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.media3.common.MediaItem;
import androidx.media3.common.MediaMetadata;
import androidx.media3.common.PlaybackException;
import androidx.media3.common.Player;
import androidx.media3.session.MediaController;
import androidx.media3.session.SessionToken;
import com.csm.mushafi.audio.AyahAudio;
import com.csm.mushafi.playback.PlaybackService;
import com.google.common.util.concurrent.ListenableFuture;
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
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.csm.mushafi.data.AyahEntity;
import com.csm.mushafi.data.BookmarkEntity;
import com.csm.mushafi.data.QuranRepository;
import com.csm.mushafi.mushaf.MushafLines;
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
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * قراءة بالصفحات: 604 صفحة تُقلَّب بالسحب أو بالمنزلق. نص كل صفحة من النص القرآني الموثّق،
 * ويُقاس الخط ليملأ الصفحة بلا فراغ. راية السورة والبسملة في أول السورة فقط.
 * الضغط على آية يحددها ويفتح النسخ والمشاركة والعلامة.
 */
public class PageReaderActivity extends BaseActivity {

    private final ExecutorService io = Executors.newSingleThreadExecutor();
    private final Handler main = new Handler(Looper.getMainLooper());

    private ViewPager2 pager;
    private com.csm.mushafi.view.PageCurlView curl;
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
    private MushafLines lines;
    private android.media.SoundPool soundPool;
    private int flipSound;
    private String basmala;
    private int currentPage = 1;

    private int selSurah;
    private int selAyah;
    private int selPage;

    private List<AyahAudio.Reciter> reciters;
    private AyahAudio.Reciter reciter;
    private ListenableFuture<MediaController> controllerFuture;
    private MediaController controller;
    private Button listenButton;
    private Button reciterButton;
    private boolean quizMode;
    private final java.util.Map<Integer, Integer> quiz = new java.util.HashMap<>();
    private static final int Q_HIDDEN = 0;
    private static final int Q_REVEALED = 1;
    private static final int Q_RIGHT = 2;
    private static final int Q_WRONG = 3;
    private int playSurah;
    private int playAyah;

    private final ActivityResultLauncher<String> notificationPermission =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> { });

    private final Player.Listener playerListener = new Player.Listener() {
        @Override
        public void onMediaItemTransition(MediaItem item, int reason) {
            applyPlaying(item);
        }

        @Override
        public void onIsPlayingChanged(boolean isPlaying) {
            renderListenButton();
        }

        @Override
        public void onPlaybackStateChanged(int state) {
            if (state == Player.STATE_ENDED) {
                setPlaying(0, 0);
            }
            renderListenButton();
        }

        @Override
        public void onPlayerError(PlaybackException error) {
            Toast.makeText(PageReaderActivity.this, R.string.listen_error, Toast.LENGTH_LONG).show();
            setPlaying(0, 0);
            renderListenButton();
        }
    };

    private int appliedTheme = -1;
    private boolean appliedMuq;
    private int appliedFont;
    private boolean appliedKashida;
    private boolean appliedExact;

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
        setupImmersive();
        selectionText = findViewById(R.id.page_selection_text);
        findViewById(R.id.btn_back).setOnClickListener(v -> finish());
        findViewById(R.id.btn_bookmark).setOnClickListener(v -> bookmarkPage());
        findViewById(R.id.btn_sel_copy).setOnClickListener(v -> copySelected());
        findViewById(R.id.btn_sel_share).setOnClickListener(v -> shareSelected());
        findViewById(R.id.btn_sel_bookmark).setOnClickListener(v -> bookmarkSelected());
        findViewById(R.id.btn_sel_clear).setOnClickListener(v -> clearSelection());
        Button quizButton = findViewById(R.id.btn_quiz);
        quizButton.setOnClickListener(v -> toggleQuiz(quizButton));
        findViewById(R.id.btn_reader_search).setOnClickListener(
                v -> startActivity(new Intent(this, SearchActivity.class)));
        findViewById(R.id.btn_font_smaller).setOnClickListener(v -> changeFont(-10));
        findViewById(R.id.btn_font_larger).setOnClickListener(v -> changeFont(10));
        listenButton = findViewById(R.id.btn_listen);
        reciterButton = findViewById(R.id.btn_reciter);
        listenButton.setOnClickListener(v -> toggleListen());
        reciterButton.setOnClickListener(v -> chooseReciter());
        titleText.setOnClickListener(v -> startActivity(new Intent(this, SurahListActivity.class)));
        seek.setMax(MushafMeta.PAGES - 1);

        int surah = getIntent().getIntExtra(ReaderActivity.EXTRA_SURAH, 1);
        int ayah = getIntent().getIntExtra(ReaderActivity.EXTRA_AYAH, 1);

        io.execute(() -> {
            try {
                MushafMeta loaded = MushafMeta.parse(readAsset("mushaf-meta.json"));
                MuqattaatDisplay loadedMuq = MuqattaatDisplay.parse(readAsset("muqattaat-display.json"));
                MushafLines loadedLines = null;
                try {
                    loadedLines = MushafLines.parse(readAsset("mushaf-lines.json"));
                } catch (Exception ignored) {
                    // بدون بيانات الأسطر يرجع العرض إلى التدفق الحر
                }
                final MushafLines linesFinal = loadedLines;
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
                    lines = linesFinal;
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
        if (adapter != null && (theme != appliedTheme || muq != appliedMuq
                || prefs.pageFontPercent() != appliedFont || prefs.kashida() != appliedKashida || prefs.exactLayout() != appliedExact)) {
            adapter.notifyDataSetChanged();
        }
        appliedTheme = theme;
        appliedMuq = muq;
        appliedFont = prefs.pageFontPercent();
        appliedKashida = prefs.kashida();
        appliedExact = prefs.exactLayout();
    }


    // ---- قراءة بملء الشاشة: لا شيء على الصفحة، والنقر يُظهر زر القائمة (ثلاثة خطوط) ----

    private boolean menuOpen;
    private final Runnable hideMenuButton = () -> {
        View b = findViewById(R.id.btn_menu);
        if (b != null && !menuOpen) {
            b.setVisibility(View.GONE);
        }
    };

    private void setupImmersive() {
        androidx.core.view.WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        getWindow().getAttributes().layoutInDisplayCutoutMode =
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
        hideSystemBars();
        findViewById(R.id.btn_menu).setOnClickListener(v -> setMenuOpen(!menuOpen));
    }

    private void hideSystemBars() {
        androidx.core.view.WindowInsetsControllerCompat c =
                androidx.core.view.WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        c.hide(androidx.core.view.WindowInsetsCompat.Type.systemBars());
        c.setSystemBarsBehavior(
                androidx.core.view.WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            hideSystemBars();
        }
    }

    private void revealMenuButton() {
        View b = findViewById(R.id.btn_menu);
        if (b == null || menuOpen) {
            return;
        }
        b.setVisibility(View.VISIBLE);
        main.removeCallbacks(hideMenuButton);
        main.postDelayed(hideMenuButton, 4000);
    }

    private void setMenuOpen(boolean open) {
        menuOpen = open;
        findViewById(R.id.reader_top).setVisibility(open ? View.VISIBLE : View.GONE);
        findViewById(R.id.reader_controls).setVisibility(open ? View.VISIBLE : View.GONE);
        main.removeCallbacks(hideMenuButton);
        if (!open) {
            main.postDelayed(hideMenuButton, 4000);
        }
    }


    // ---- تقليب الصفحة بطيّ ورقي ----

    private float downX;
    private float downY;
    private boolean tracking;
    private boolean curling;
    private boolean bottomGrab;
    private int curlDir;
    private android.view.VelocityTracker velocity;

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        if (menuOpen || curl == null || pager == null) {
            return super.dispatchTouchEvent(ev);
        }
        switch (ev.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                tracking = !curl.isBusy() && !curling;
                downX = ev.getX();
                downY = ev.getY();
                bottomGrab = downY > pager.getHeight() / 2f;
                if (velocity != null) {
                    velocity.recycle();
                }
                velocity = android.view.VelocityTracker.obtain();
                velocity.addMovement(ev);
                break;
            case MotionEvent.ACTION_MOVE:
                if (velocity != null) {
                    velocity.addMovement(ev);
                }
                if (tracking && !curling) {
                    float dx = ev.getX() - downX;
                    float dy = ev.getY() - downY;
                    int slop = android.view.ViewConfiguration.get(this).getScaledTouchSlop();
                    if (Math.abs(dx) > slop * 2 && Math.abs(dx) > Math.abs(dy) * 1.4f) {
                        if (startCurl(dx > 0 ? 1 : -1)) {
                            MotionEvent cancel = MotionEvent.obtain(ev);
                            cancel.setAction(MotionEvent.ACTION_CANCEL);
                            super.dispatchTouchEvent(cancel);
                            cancel.recycle();
                        } else {
                            tracking = false;
                        }
                    }
                }
                if (curling) {
                    float dx = ev.getX() - downX;
                    float dy = ev.getY() - downY;
                    if (curlDir > 0) {
                        curl.dragForward(dx, bottomGrab ? dy : dy);
                    } else {
                        curl.dragBackward(-dx, dy);
                    }
                    return true;
                }
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                tracking = false;
                if (curling) {
                    velocity.addMovement(ev);
                    velocity.computeCurrentVelocity(1000);
                    float vx = velocity.getXVelocity();
                    boolean towards = curlDir > 0 ? vx > 900 : vx < -900;
                    boolean away = curlDir > 0 ? vx < -900 : vx > 900;
                    boolean complete = towards || (!away && (curlDir > 0
                            ? curl.progress() > 0.22f : curl.progress() < 0.65f));
                    if (ev.getActionMasked() == MotionEvent.ACTION_CANCEL) {
                        complete = false;
                    }
                    finishCurl(complete);
                    return true;
                }
                break;
            default:
                break;
        }
        return super.dispatchTouchEvent(ev);
    }

    private Bitmap capturePage(int position) {
        RecyclerView rv = (RecyclerView) pager.getChildAt(0);
        RecyclerView.ViewHolder h = rv.findViewHolderForAdapterPosition(position);
        int w = pager.getWidth();
        int hgt = pager.getHeight();
        if (h == null || w <= 0 || hgt <= 0) {
            return null;
        }
        Bitmap b = Bitmap.createBitmap(w, hgt, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(b);
        View root = (View) pager.getParent();
        if (root.getBackground() != null) {
            root.getBackground().setBounds(0, 0, w, hgt);
            root.getBackground().draw(canvas);
        }
        View item = h.itemView;
        canvas.save();
        canvas.translate(-item.getLeft(), -item.getTop());
        canvas.translate(item.getLeft(), item.getTop());
        item.draw(canvas);
        canvas.restore();
        return b;
    }

    private boolean startCurl(int dir) {
        int pos = pager.getCurrentItem();
        int other = pos + dir;
        if (other < 0 || other >= MushafMeta.PAGES) {
            return false;
        }
        Bitmap current = capturePage(pos);
        Bitmap neighbour = capturePage(other);
        if (current == null || neighbour == null) {
            return false;
        }
        int paper = PageTheme.of(prefs.pageTheme()).paper;
        if (dir > 0) {
            curl.begin(current, neighbour, paper, true, bottomGrab);
        } else {
            curl.begin(neighbour, current, paper, false, bottomGrab);
        }
        curling = true;
        curlDir = dir;
        playFlip();
        return true;
    }

    private void finishCurl(boolean complete) {
        final int target = pager.getCurrentItem() + (complete ? curlDir : 0);
        curl.finish(complete, done -> {
            if (complete) {
                pager.setCurrentItem(target, false);
            }
            curl.post(() -> curl.end());
            curling = false;
        });
    }

    private void playFlip() {
        if (soundPool != null && prefs.pageSound()) {
            soundPool.play(flipSound, 0.8f, 0.8f, 1, 0, 1f);
        }
    }

    @Override
    protected void onDestroy() {
        if (soundPool != null) {
            soundPool.release();
            soundPool = null;
        }
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
        try {
            reciters = AyahAudio.parse(readAsset("reader-reciters.json"));
        } catch (Exception e) {
            reciters = new ArrayList<>();
        }
        reciter = null;
        for (AyahAudio.Reciter r : reciters) {
            if (r.id.equals(prefs.readerReciter())) {
                reciter = r;
            }
        }
        if (reciter == null && !reciters.isEmpty()) {
            reciter = reciters.get(0);
        }
        renderReciter();
        int s = Math.max(1, Math.min(MushafMeta.SURAHS, surah));
        int a = Math.max(1, Math.min(meta.ayahCount(s), ayah));
        int startPage = meta.pageOf(s, a);

        adapter = new PageAdapter();
        pager.setAdapter(adapter);
        pager.setOffscreenPageLimit(1);
        pager.setUserInputEnabled(false);
        curl = findViewById(R.id.page_curl);
        soundPool = new android.media.SoundPool.Builder().setMaxStreams(2).build();
        flipSound = soundPool.load(this, R.raw.page_flip, 1);
        Button soundButton = findViewById(R.id.btn_sound);
        soundButton.setText(prefs.pageSound() ? R.string.sound_on : R.string.sound_off);
        soundButton.setOnClickListener(v -> {
            boolean on = !prefs.pageSound();
            prefs.savePageSound(on);
            soundButton.setText(on ? R.string.sound_on : R.string.sound_off);
        });
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
        String position = getString(R.string.page_position,
                ArabicDigits.format(page), ArabicDigits.format(MushafMeta.PAGES));
        if (quizMode) {
            int right = 0;
            int wrong = 0;
            for (int v : quiz.values()) {
                if (v == Q_RIGHT) {
                    right++;
                } else if (v == Q_WRONG) {
                    wrong++;
                }
            }
            position += "  ·  " + getString(R.string.quiz_score,
                    ArabicDigits.format(right), ArabicDigits.format(wrong));
        }
        positionText.setText(position);
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

    // ---- الاستماع مع تظليل الآية الجارية ----

    @Override
    protected void onStart() {
        super.onStart();
        SessionToken token = new SessionToken(this, new ComponentName(this, PlaybackService.class));
        controllerFuture = new MediaController.Builder(this, token).buildAsync();
        final ListenableFuture<MediaController> f = controllerFuture;
        f.addListener(() -> {
            try {
                if (f != controllerFuture || isDestroyed()) {
                    return;
                }
                controller = f.get();
                controller.addListener(playerListener);
                if (controller.isPlaying() && controller.getCurrentMediaItem() != null) {
                    applyPlaying(controller.getCurrentMediaItem());
                }
                renderListenButton();
            } catch (Exception ignored) {
                controller = null;
            }
        }, ContextCompat.getMainExecutor(this));
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (controller != null) {
            controller.removeListener(playerListener);
        }
        if (controllerFuture != null) {
            MediaController.releaseFuture(controllerFuture);
        }
        controllerFuture = null;
        controller = null;
        // التلاوة تستمر في الخدمة؛ يُزال التظليل هنا ويعود عند فتح الشاشة من جديد
        setPlaying(0, 0);
    }

    private void changeFont(int delta) {
        if (lines != null && !quizMode && prefs.exactLayout()) {
            Toast.makeText(this, R.string.exact_font_fixed, Toast.LENGTH_LONG).show();
            return;
        }
        int now = prefs.pageFontPercent();
        int next = Math.max(60, Math.min(160, now + delta));
        if (next == now) {
            return;
        }
        prefs.savePageFontPercent(next);
        Toast.makeText(this, getString(R.string.settings_font_size,
                ArabicDigits.format(next)), Toast.LENGTH_SHORT).show();
        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }
    }

    private void renderReciter() {
        reciterButton.setText(reciter == null
                ? getString(R.string.listen_no_reciter)
                : getString(R.string.listen_reciter_format, reciter.name));
        reciterButton.setEnabled(reciter != null);
        listenButton.setEnabled(reciter != null);
    }

    private void renderListenButton() {
        boolean playing = controller != null && controller.isPlaying();
        listenButton.setText(playing ? R.string.listen_pause : R.string.listen_play);
    }

    private void toggleListen() {
        if (controller == null || reciter == null || meta == null) {
            Toast.makeText(this, R.string.listen_not_ready, Toast.LENGTH_SHORT).show();
            return;
        }
        if (controller.isPlaying()) {
            controller.pause();
            return;
        }
        if (playSurah != 0 && controller.getMediaItemCount() > 0
                && controller.getPlaybackState() != Player.STATE_ENDED) {
            controller.play();
            return;
        }
        int s;
        int a;
        if (selSurah != 0) {
            s = selSurah;
            a = selAyah;
        } else {
            int[] first = meta.ayahsOnPage(currentPage).get(0);
            s = first[0];
            a = first[1];
        }
        startListening(s, a);
    }

    private void startListening(int surah, int ayah) {
        if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(this,
                Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS);
        }
        List<MediaItem> items = new ArrayList<>();
        for (int[] item : AyahAudio.playlist(meta, surah, ayah, 300)) {
            MediaMetadata data = new MediaMetadata.Builder()
                    .setTitle(getString(R.string.surah_name_format, meta.name(item[0]))
                            + " - " + ArabicDigits.format(item[1]))
                    .setArtist(reciter.name)
                    .build();
            items.add(new MediaItem.Builder()
                    .setMediaId(item[0] + ":" + item[1])
                    .setUri(AyahAudio.ayahUrl(reciter.folder, item[0], item[1]))
                    .setMediaMetadata(data)
                    .build());
        }
        playSurah = 0;
        controller.setMediaItems(items, 0, 0L);
        controller.prepare();
        controller.play();
        setPlaying(surah, ayah);
    }

    private void chooseReciter() {
        if (reciters == null || reciters.isEmpty()) {
            return;
        }
        String[] names = new String[reciters.size()];
        int checked = 0;
        for (int i = 0; i < names.length; i++) {
            names[i] = reciters.get(i).name;
            if (reciters.get(i) == reciter) {
                checked = i;
            }
        }
        new AlertDialog.Builder(this)
                .setTitle(R.string.listen_choose_reciter)
                .setSingleChoiceItems(names, checked, (dialog, which) -> {
                    reciter = reciters.get(which);
                    prefs.saveReaderReciter(reciter.id);
                    renderReciter();
                    dialog.dismiss();
                    if (controller != null && playSurah != 0) {
                        startListening(playSurah, playAyah);
                    }
                })
                .show();
    }

    private void applyPlaying(MediaItem item) {
        if (item == null || item.mediaId == null) {
            return;
        }
        String[] parts = item.mediaId.split(":");
        if (parts.length != 2) {
            return;
        }
        try {
            setPlaying(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]));
        } catch (NumberFormatException ignored) {
            // عنصر ليس من هذا القارئ (مثلاً من شاشة السور)
        }
    }

    private void setPlaying(int surah, int ayah) {
        if (meta == null || adapter == null) {
            return;
        }
        int oldPage = playSurah == 0 ? 0 : meta.pageOf(playSurah, playAyah);
        playSurah = surah;
        playAyah = ayah;
        int newPage = surah == 0 ? 0 : meta.pageOf(surah, ayah);
        if (newPage > 0 && newPage != currentPage && (oldPage == 0 || oldPage == currentPage)) {
            pager.setCurrentItem(newPage - 1, true);
        }
        if (oldPage > 0 && oldPage != newPage) {
            adapter.notifyItemChanged(oldPage - 1);
        }
        if (newPage > 0) {
            adapter.notifyItemChanged(newPage - 1);
        }
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
            if (quizMode) {
                onQuizTap(surah, ayah);
                return;
            }
            revealMenuButton();
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
            text = MushafLines.stripBasmala(s, a, text);
            if (useMuq) {
                text = muqattaat.apply(s, a, text);
            }
            int ayahStart = sb.length();
            int quizState = !quizMode ? Q_REVEALED : (quiz.containsKey(s * 1000 + a)
                    ? quiz.get(s * 1000 + a) : Q_HIDDEN);
            if (quizState != Q_HIDDEN) {
                sb.append(text).append(' ');
                if (quizState == Q_WRONG) {
                    sb.setSpan(new ForegroundColorSpan(0xFFE53935), ayahStart, sb.length() - 1,
                            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                } else if (quizState == Q_RIGHT) {
                    sb.setSpan(new ForegroundColorSpan(0xFF43A047), ayahStart, sb.length() - 1,
                            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                }
            }
            int numberStart = sb.length();
            String number = ArabicDigits.format(a);
            sb.append(number);
            sb.setSpan(new AyahMarkerSpan(number, theme), numberStart, sb.length(),
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            sb.setSpan(new AyahClick(s, a), ayahStart, sb.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            if ((s == selSurah && a == selAyah) || (s == playSurah && a == playAyah)) {
                sb.setSpan(new BackgroundColorSpan(theme.highlight), ayahStart, sb.length(),
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
            sb.append(' ');
        }
        return sb;
    }


    /** صفحة مطابقة لمصحف المدينة: كل سطر مطبوع سطر في النص، مفصولة بـ \n. */
    private CharSequence buildExactPage(int page, FitPageTextView host, PageTheme theme,
                                        boolean useMuq) {
        SpannableStringBuilder sb = new SpannableStringBuilder();
        boolean first = true;
        for (MushafLines.Line line : lines.page(page)) {
            if (!first) {
                sb.append('\n');
            }
            first = false;
            int lineStart = sb.length();
            if (line.kind == MushafLines.HEADER) {
                sb.append("سُورَةُ ").append(meta.name(line.surah));
                Typeface headerFont = null;
                try {
                    headerFont = ResourcesCompat.getFont(this, R.font.surah_header);
                } catch (Exception ignored) {
                    // يُستعمل الإطار المرسوم بالكود
                }
                Object frame = headerFont != null
                        ? new SurahHeaderSpan(line.surah, ArabicDigits.format(line.surah),
                                ArabicDigits.format(meta.ayahCount(line.surah)), headerFont, host, theme)
                        : new SurahFrameSpan(sb.subSequence(lineStart, sb.length()).toString(),
                                ArabicDigits.format(line.surah),
                                ArabicDigits.format(meta.ayahCount(line.surah)), host, theme);
                sb.setSpan(frame, lineStart, sb.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                sb.setSpan(new AlignmentSpan.Standard(Layout.Alignment.ALIGN_CENTER),
                        lineStart, sb.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                continue;
            }
            if (line.kind == MushafLines.BASMALA) {
                sb.append(basmala);
                sb.setSpan(new ForegroundColorSpan(theme.basmala), lineStart, sb.length(),
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                sb.setSpan(new AlignmentSpan.Standard(Layout.Alignment.ALIGN_CENTER),
                        lineStart, sb.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                continue;
            }
            boolean firstSeg = true;
            for (MushafLines.Seg seg : line.segs) {
                if (!firstSeg) {
                    sb.append(' ');
                }
                firstSeg = false;
                String[] words = ayahWords(seg.surah, seg.ayah, useMuq);
                int segStart = sb.length();
                for (int w = seg.from; w < seg.to && w < words.length; w++) {
                    if (w > seg.from) {
                        sb.append(' ');
                    }
                    sb.append(words[w]);
                }
                if (seg.to >= words.length) {
                    String number = ArabicDigits.format(seg.ayah);
                    sb.append(' ');
                    int numberStart = sb.length();
                    sb.append(number);
                    sb.setSpan(new AyahMarkerSpan(number, theme), numberStart, sb.length(),
                            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                }
                sb.setSpan(new AyahClick(seg.surah, seg.ayah), segStart, sb.length(),
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                if ((seg.surah == selSurah && seg.ayah == selAyah)
                        || (seg.surah == playSurah && seg.ayah == playAyah)) {
                    sb.setSpan(new BackgroundColorSpan(theme.highlight), segStart, sb.length(),
                            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                }
            }
            if (line.centered) {
                sb.setSpan(new AlignmentSpan.Standard(Layout.Alignment.ALIGN_CENTER),
                        lineStart, sb.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
        }
        return sb;
    }

    /** كلمات الآية بعد حذف البسملة (وبعد تعديل المدّ المعروض إن فُعّل) بنفس عدّ بيانات الأسطر. */
    private String[] ayahWords(int s, int a, boolean useMuq) {
        String text = texts[s][a - 1];
        text = MushafLines.stripBasmala(s, a, text);
        String[] base = text.trim().split("\\s+");
        if (useMuq) {
            String[] changed = muqattaat.apply(s, a, text).trim().split("\\s+");
            if (changed.length == base.length) {
                return changed;
            }
        }
        return base;
    }

    private void toggleQuiz(Button button) {
        quizMode = !quizMode;
        quiz.clear();
        clearSelection();
        button.setText(quizMode ? R.string.quiz_stop : R.string.quiz_start);
        if (quizMode) {
            Toast.makeText(this, R.string.quiz_hint, Toast.LENGTH_LONG).show();
        }
        showPosition(currentPage);
        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }
    }

    private void onQuizTap(int surah, int ayah) {
        final int key = surah * 1000 + ayah;
        int state = quiz.containsKey(key) ? quiz.get(key) : Q_HIDDEN;
        if (state == Q_HIDDEN) {
            quiz.put(key, Q_REVEALED);
            refreshQuizPage(surah, ayah);
            return;
        }
        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.quiz_rate_title, ArabicDigits.format(ayah)))
                .setItems(new String[]{getString(R.string.quiz_right),
                        getString(R.string.quiz_wrong), getString(R.string.quiz_hide)},
                        (dialog, which) -> {
                            if (which == 0) {
                                quiz.put(key, Q_RIGHT);
                            } else if (which == 1) {
                                quiz.put(key, Q_WRONG);
                            } else {
                                quiz.remove(key);
                            }
                            refreshQuizPage(surah, ayah);
                        })
                .show();
    }

    private void refreshQuizPage(int surah, int ayah) {
        showPosition(currentPage);
        if (adapter != null) {
            adapter.notifyItemChanged(meta.pageOf(surah, ayah) - 1);
        }
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
        Typeface headerFont = null;
        try {
            headerFont = ResourcesCompat.getFont(this, R.font.surah_header);
        } catch (Exception ignored) {
            // يُستعمل الإطار المرسوم بالكود إن تعذّر تحميل الخط
        }
        Object frame = headerFont != null
                ? new SurahHeaderSpan(surah, ArabicDigits.format(surah),
                        ArabicDigits.format(meta.ayahCount(surah)), headerFont, host, theme)
                : new SurahFrameSpan(name, ArabicDigits.format(surah),
                        ArabicDigits.format(meta.ayahCount(surah)), host, theme);
        sb.setSpan(frame, start, sb.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
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
                final float[] touch = new float[2];
                text.setOnClickListener(v -> revealMenuButton());
                text.setOnTouchListener((v, event) -> {
                    touch[0] = event.getX();
                    touch[1] = event.getY();
                    return false;
                });
                // ضغط مطوّل على آية: تُحدَّد وتُنسخ مباشرة
                text.setOnLongClickListener(v -> {
                    if (quizMode) {
                        return false;
                    }
                    int offset = text.getOffsetForPosition(touch[0], touch[1]);
                    CharSequence shown = text.getText();
                    if (offset < 0 || !(shown instanceof Spanned)) {
                        return false;
                    }
                    AyahClick[] hit = ((Spanned) shown).getSpans(offset, offset, AyahClick.class);
                    if (hit.length == 0) {
                        return false;
                    }
                    selectAyah(hit[0].surah, hit[0].ayah);
                    return true;
                });
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
            holder.text.setKashidaEnabled(prefs.kashida());
            holder.text.setFontScale(prefs.pageFontPercent() / 100f);
            boolean exact = lines != null && !quizMode && prefs.exactLayout();
            int count = exact ? lines.page(position + 1).size() : 0;
            holder.text.setExactLines(exact, count, count >= 15 ? 15 : 10);
            holder.text.setText(exact
                    ? buildExactPage(position + 1, holder.text, theme, prefs.muqattaatUthmani())
                    : buildPage(position + 1, holder.text, theme, prefs.muqattaatUthmani()));
        }

        @Override
        public int getItemCount() {
            return MushafMeta.PAGES;
        }
    }
}
