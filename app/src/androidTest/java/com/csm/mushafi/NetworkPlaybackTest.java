package com.csm.mushafi;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.os.Looper;

import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.csm.mushafi.audio.AudioUrls;
import com.csm.mushafi.audio.CatalogueClient;
import com.csm.mushafi.audio.ReciterCatalogue;

import androidx.media3.common.MediaItem;
import androidx.media3.common.Player;
import androidx.media3.exoplayer.ExoPlayer;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * يعمل على محاكٍ Android 10 داخل CI. يتحقق من الشبكة الفعلية: جلب الفهرس، ثم تحميل سورة من خادم القارئ وتشغيلها.
 */
@RunWith(AndroidJUnit4.class)
public class NetworkPlaybackTest {

    @Test
    public void liveCatalogueParsesWithValidServers() throws Exception {
        String body = CatalogueClient.fetch();
        ReciterCatalogue.Result result = ReciterCatalogue.parse(body);
        assertTrue("catalogue must contain reciters", result.reciters.size() > 0);
        ReciterCatalogue.Moshaf m = result.reciters.get(0).moshafs.get(0);
        assertTrue(AudioUrls.isAcceptableServer(m.server));
        assertFalse(m.surahs.isEmpty());
    }

    @Test
    public void networkSurahStartsPlaying() throws Exception {
        String body = CatalogueClient.fetch();
        ReciterCatalogue.Result result = ReciterCatalogue.parse(body);
        ReciterCatalogue.Moshaf m = result.reciters.get(0).moshafs.get(0);
        String url = AudioUrls.surahUrl(m.server, 1);

        AtomicReference<ExoPlayer> ref = new AtomicReference<>();
        CountDownLatch ready = new CountDownLatch(1);
        AtomicReference<Long> duration = new AtomicReference<>(0L);
        AtomicReference<Boolean> playing = new AtomicReference<>(false);

        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            ExoPlayer p = new ExoPlayer.Builder(InstrumentationRegistry.getInstrumentation().getTargetContext())
                    .build();
            ref.set(p);
            p.addListener(new Player.Listener() {
                @Override
                public void onPlaybackStateChanged(int state) {
                    if (state == Player.STATE_READY) {
                        duration.set(p.getDuration());
                        playing.set(true);
                        ready.countDown();
                    }
                }
            });
            p.setMediaItem(MediaItem.fromUri(url));
            p.prepare();
            p.setPlayWhenReady(true);
        });

        boolean ok = ready.await(90, TimeUnit.SECONDS);
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> ref.get().release());
        assertTrue("surah must reach STATE_READY from the network within 90 seconds", ok);
        assertTrue("duration must be known", duration.get() > 0);
        assertTrue(playing.get());
    }
}
