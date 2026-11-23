package com.csm.mushafi;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import androidx.media3.common.MediaItem;
import androidx.media3.common.Player;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.csm.mushafi.audio.AudioUrls;
import com.csm.mushafi.audio.CatalogueClient;
import com.csm.mushafi.audio.ReciterCatalogue;
import com.csm.mushafi.download.DownloadEngine;
import com.csm.mushafi.download.DownloadStore;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * يحمّل سورة الأولى فعلياً من خادم القارئ، ثم يطفئ الشبكة (وضع الطيران) ويشغّل الملف المحلي.
 */
@RunWith(AndroidJUnit4.class)
public class DownloadAndOfflinePlaybackTest {

    @Test
    public void downloadThenPlayOffline() throws Exception {
        String body = CatalogueClient.fetch();
        ReciterCatalogue.Result result = ReciterCatalogue.parse(body);
        ReciterCatalogue.Moshaf m = result.reciters.get(0).moshafs.get(0);
        String url = AudioUrls.surahUrl(m.server, 1);

        File dest = DownloadStore.fileFor(InstrumentationRegistry.getInstrumentation().getTargetContext(),
                m.server, 1);
        dest.delete();
        DownloadEngine.Outcome outcome = DownloadEngine.download(url, dest, new DownloadEngine.Control() {
            @Override
            public boolean shouldStop() {
                return false;
            }

            @Override
            public void progress(long bytesDone, long totalBytes) {
            }
        });
        assertEquals(DownloadEngine.Outcome.COMPLETED, outcome);
        assertTrue("downloaded file must exist", dest.exists());
        assertTrue("downloaded file must not be empty", dest.length() > 0);

        UiAutomationShell.setAirplaneMode(true);
        try {
            boolean offline = false;
            try {
                CatalogueClient.fetch();
            } catch (CatalogueClient.NetworkException expected) {
                offline = true;
            }
            assertTrue("network must be unavailable in airplane mode for this test to mean anything", offline);

            AtomicReference<ExoPlayer> ref = new AtomicReference<>();
            CountDownLatch ready = new CountDownLatch(1);
            InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
                ExoPlayer p = new ExoPlayer.Builder(InstrumentationRegistry.getInstrumentation().getTargetContext())
                        .build();
                ref.set(p);
                p.addListener(new Player.Listener() {
                    @Override
                    public void onPlaybackStateChanged(int state) {
                        if (state == Player.STATE_READY) {
                            ready.countDown();
                        }
                    }
                });
                p.setMediaItem(MediaItem.fromUri(dest.toURI().toString()));
                p.prepare();
            });
            boolean ok = ready.await(60, TimeUnit.SECONDS);
            InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> ref.get().release());
            assertTrue("local file must play in airplane mode", ok);
        } finally {
            UiAutomationShell.setAirplaneMode(false);
        }
    }
}
