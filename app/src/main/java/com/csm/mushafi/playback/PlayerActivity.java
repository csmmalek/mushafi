package com.csm.mushafi.playback;

import android.Manifest;
import android.content.ComponentName;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.media3.common.MediaItem;
import androidx.media3.common.MediaMetadata;
import androidx.media3.common.PlaybackParameters;
import androidx.media3.common.Player;
import androidx.media3.session.MediaController;
import androidx.media3.session.SessionToken;

import com.csm.mushafi.R;
import com.csm.mushafi.audio.AudioUrls;
import com.csm.mushafi.quran.ArabicDigits;
import com.google.common.util.concurrent.ListenableFuture;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** شاشة المشغل: تتصل بخدمة التشغيل. الإيقاف والاستئناف يعملان حتى عند إغلاق الشاشة. */
public class PlayerActivity extends AppCompatActivity {

    static final String EXTRA_RECITER = "reciter";
    static final String EXTRA_MOSHAF = "moshaf";
    static final String EXTRA_SERVER = "server";
    static final String EXTRA_SURAHS = "surahs";
    static final String EXTRA_START_INDEX = "startIndex";

    private static final float[] SPEEDS = {1.0f, 1.25f, 1.5f, 0.75f};
    private static final int[] SLEEP_MINUTES = {0, 15, 30, 60};
    private static final String PREFS = "mushafi_audio";

    private final Handler main = new Handler(Looper.getMainLooper());
    private final Runnable ticker = new Runnable() {
        @Override
        public void run() {
            refreshProgress();
            main.postDelayed(this, 500);
        }
    };

    private ListenableFuture<MediaController> future;
    private MediaController controller;
    private String reciter;
    private String moshaf;
    private String server;
    private int[] surahs;
    private int startIndex;
    private int speedIndex;
    private int sleepIndex;

    private TextView title;
    private TextView subtitle;
    private TextView times;
    private SeekBar seekBar;
    private Button playPause;
    private Button speedButton;
    private Button repeatButton;
    private Button sleepButton;
    private boolean repeatOne;
    private boolean seeking;

    private final ActivityResultLauncher<String> notificationPermission =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> { });

    private final Player.Listener listener = new Player.Listener() {
        @Override
        public void onIsPlayingChanged(boolean isPlaying) {
            playPause.setText(isPlaying ? R.string.player_pause : R.string.player_play);
            if (isPlaying) {
                main.removeCallbacks(ticker);
                main.post(ticker);
            }
        }

        @Override
        public void onMediaItemTransition(MediaItem mediaItem, int reason) {
            updateTitle();
        }

        @Override
        public void onPlaybackStateChanged(int playbackState) {
            refreshProgress();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_player);

        reciter = getIntent().getStringExtra(EXTRA_RECITER);
        moshaf = getIntent().getStringExtra(EXTRA_MOSHAF);
        server = getIntent().getStringExtra(EXTRA_SERVER);
        surahs = getIntent().getIntArrayExtra(EXTRA_SURAHS);
        startIndex = getIntent().getIntExtra(EXTRA_START_INDEX, 0);
        if (reciter == null || server == null || surahs == null || surahs.length == 0) {
            finish();
            return;
        }

        title = findViewById(R.id.player_title);
        subtitle = findViewById(R.id.player_subtitle);
        times = findViewById(R.id.player_times);
        seekBar = findViewById(R.id.player_seek);
        playPause = findViewById(R.id.btn_play_pause);
        speedButton = findViewById(R.id.btn_speed);
        repeatButton = findViewById(R.id.btn_repeat);
        sleepButton = findViewById(R.id.btn_sleep);

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());
        playPause.setOnClickListener(v -> {
            if (controller == null) {
                return;
            }
            if (controller.isPlaying()) {
                controller.pause();
            } else {
                controller.play();
            }
        });
        findViewById(R.id.btn_prev).setOnClickListener(v -> {
            if (controller != null) {
                controller.seekToPrevious();
            }
        });
        findViewById(R.id.btn_next).setOnClickListener(v -> {
            if (controller != null) {
                controller.seekToNext();
            }
        });
        findViewById(R.id.btn_back10).setOnClickListener(v -> seekBy(-10_000));
        findViewById(R.id.btn_forward10).setOnClickListener(v -> seekBy(10_000));
        speedButton.setOnClickListener(v -> {
            speedIndex = (speedIndex + 1) % SPEEDS.length;
            if (controller != null) {
                controller.setPlaybackParameters(new PlaybackParameters(SPEEDS[speedIndex]));
            }
            renderSpeed();
        });
        repeatButton.setOnClickListener(v -> {
            repeatOne = !repeatOne;
            if (controller != null) {
                controller.setRepeatMode(repeatOne ? Player.REPEAT_MODE_ONE : Player.REPEAT_MODE_OFF);
            }
            renderRepeat();
        });
        sleepButton.setOnClickListener(v -> {
            sleepIndex = (sleepIndex + 1) % SLEEP_MINUTES.length;
            PlaybackService.scheduleSleep(SLEEP_MINUTES[sleepIndex] * 60_000L);
            renderSleep();
        });
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar s, int progress, boolean fromUser) {
            }

            @Override
            public void onStartTrackingTouch(SeekBar s) {
                seeking = true;
            }

            @Override
            public void onStopTrackingTouch(SeekBar s) {
                seeking = false;
                if (controller != null && controller.getDuration() > 0) {
                    controller.seekTo(controller.getDuration() * s.getProgress() / 1000L);
                }
            }
        });

        renderSpeed();
        renderRepeat();
        renderSleep();

        if (Build.VERSION.SDK_INT >= 33
                && ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS);
        }

        SessionToken token = new SessionToken(this, new ComponentName(this, PlaybackService.class));
        future = new MediaController.Builder(this, token).buildAsync();
        future.addListener(() -> {
            try {
                controller = future.get();
                controller.addListener(listener);
                loadQueueIfNeeded();
                updateTitle();
                refreshProgress();
            } catch (Exception e) {
                title.setText(R.string.player_error);
            }
        }, ContextCompat.getMainExecutor(this));
    }

    @Override
    protected void onStop() {
        saveResumePoint();
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        main.removeCallbacks(ticker);
        if (controller != null) {
            controller.removeListener(listener);
        }
        if (future != null) {
            MediaController.releaseFuture(future);
        }
        super.onDestroy();
    }

    private void loadQueueIfNeeded() {
        if (controller.getMediaItemCount() == surahs.length
                && server.equals(currentServer())) {
            return;
        }
        List<MediaItem> items = new ArrayList<>(surahs.length);
        for (int surah : surahs) {
            items.add(new MediaItem.Builder()
                    .setMediaId(AudioUrls.surahUrl(server, surah))
                    .setUri(AudioUrls.surahUrl(server, surah))
                    .setMediaMetadata(new MediaMetadata.Builder()
                            .setTitle(getString(R.string.surah_number_format, ArabicDigits.format(surah)))
                            .setArtist(reciter)
                            .setAlbumTitle(moshaf)
                            .build())
                    .build());
        }
        int start = startIndex;
        long position = 0;
        SharedPreferences p = getSharedPreferences(PREFS, MODE_PRIVATE);
        if (server.equals(p.getString("server", "")) && p.getInt("surah", -1) == surahs[startIndex]) {
            position = p.getLong("position", 0);
        }
        controller.setMediaItems(items, start, position);
        controller.setRepeatMode(repeatOne ? Player.REPEAT_MODE_ONE : Player.REPEAT_MODE_OFF);
        controller.setPlaybackParameters(new PlaybackParameters(SPEEDS[speedIndex]));
        controller.prepare();
        controller.play();
    }

    private String currentServer() {
        if (controller.getMediaItemCount() == 0) {
            return "";
        }
        MediaItem item = controller.getMediaItemAt(0);
        String uri = item.localConfiguration == null ? "" : item.localConfiguration.uri.toString();
        return uri.isEmpty() ? "" : uri.substring(0, uri.lastIndexOf('/') + 1);
    }

    private void seekBy(long deltaMs) {
        if (controller == null) {
            return;
        }
        long target = Math.max(0, controller.getCurrentPosition() + deltaMs);
        controller.seekTo(target);
    }

    private void updateTitle() {
        if (controller == null || controller.getMediaItemCount() == 0) {
            return;
        }
        MediaMetadata md = controller.getMediaMetadata();
        title.setText(md.title == null ? "" : md.title);
        subtitle.setText(reciter + " · " + moshaf);
    }

    private void refreshProgress() {
        if (controller == null || seeking) {
            return;
        }
        long pos = controller.getCurrentPosition();
        long dur = controller.getDuration();
        if (dur > 0) {
            seekBar.setProgress((int) (1000L * pos / dur));
        }
        times.setText(formatTime(pos) + " / " + formatTime(Math.max(0, dur)));
        playPause.setText(controller.isPlaying() ? R.string.player_pause : R.string.player_play);
    }

    private void saveResumePoint() {
        if (controller == null || controller.getMediaItemCount() == 0) {
            return;
        }
        int index = controller.getCurrentMediaItemIndex();
        if (index < 0 || index >= surahs.length) {
            return;
        }
        getSharedPreferences(PREFS, MODE_PRIVATE).edit()
                .putString("server", server)
                .putInt("surah", surahs[index])
                .putLong("position", controller.getCurrentPosition())
                .apply();
    }

    private void renderSpeed() {
        speedButton.setText(getString(R.string.player_speed_format,
                String.format(Locale.ROOT, "%.2f", SPEEDS[speedIndex])));
    }

    private void renderRepeat() {
        repeatButton.setText(repeatOne ? R.string.player_repeat_one : R.string.player_repeat_off);
    }

    private void renderSleep() {
        sleepButton.setText(sleepIndex == 0
                ? getString(R.string.player_sleep_off)
                : getString(R.string.player_sleep_format, ArabicDigits.format(SLEEP_MINUTES[sleepIndex])));
    }

    static String formatTime(long ms) {
        long totalSec = ms / 1000;
        long m = totalSec / 60;
        long s = totalSec % 60;
        return ArabicDigits.format((int) m) + ":" + (s < 10 ? "٠" : "") + ArabicDigits.format((int) s);
    }
}
