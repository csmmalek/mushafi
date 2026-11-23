package com.csm.mushafi.playback;

import android.os.Handler;
import android.os.Looper;

import androidx.annotation.Nullable;
import androidx.media3.common.AudioAttributes;
import androidx.media3.common.C;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.session.MediaSession;
import androidx.media3.session.MediaSessionService;

/**
 * خدمة التشغيل في الخلفية: ExoPlayer داخل MediaSession، مع إشعار التحكم تلقائياً.
 * مؤقت النوم يعيش هنا حتى لا يتوقف بإغلاق شاشة المشغل.
 */
public class PlaybackService extends MediaSessionService {

    private static volatile PlaybackService instance;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable sleepRunnable = () -> {
        if (player() != null) {
            player().pause();
        }
    };
    private MediaSession session;

    @Override
    public void onCreate() {
        super.onCreate();
        AudioAttributes attrs = new AudioAttributes.Builder()
                .setUsage(C.USAGE_MEDIA)
                .setContentType(C.AUDIO_CONTENT_TYPE_SPEECH)
                .build();
        ExoPlayer player = new ExoPlayer.Builder(this)
                .setAudioAttributes(attrs, true)
                .setHandleAudioBecomingNoisy(true)
                .build();
        session = new MediaSession.Builder(this, player).build();
        instance = this;
    }

    @Nullable
    @Override
    public MediaSession onGetSession(MediaSession.ControllerInfo controllerInfo) {
        return session;
    }

    @Override
    public void onDestroy() {
        instance = null;
        handler.removeCallbacksAndMessages(null);
        if (session != null) {
            ExoPlayer p = (ExoPlayer) session.getPlayer();
            session.release();
            p.release();
            session = null;
        }
        super.onDestroy();
    }

    /** يوقف التشغيل بعد المدة المحددة؛ إذا كانت 0 يلغي المؤقت. */
    public static void scheduleSleep(long delayMs) {
        PlaybackService s = instance;
        if (s == null) {
            return;
        }
        s.handler.removeCallbacks(s.sleepRunnable);
        if (delayMs > 0) {
            s.handler.postDelayed(s.sleepRunnable, delayMs);
        }
    }

    private ExoPlayer player() {
        return session == null ? null : (ExoPlayer) session.getPlayer();
    }
}
