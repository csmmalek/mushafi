package com.csm.mushafi;

import android.app.Instrumentation;
import android.content.Context;
import android.content.Intent;
import android.os.ParcelFileDescriptor;

import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.csm.mushafi.data.QuranRepository;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.InputStream;

/** يلتقط لقطات شاشة لشاشات التطبيق إلى /sdcard/shots ليراجعها المطوّر في CI. لا يتحقق من شيء. */
@RunWith(AndroidJUnit4.class)
public class ScreenshotTest {

    @Test
    public void captureMainReaderSettings() throws Exception {
        Context ctx = ApplicationProvider.getApplicationContext();
        new QuranRepository(ctx).verifyAndImportIfNeeded();
        shell("mkdir -p /sdcard/shots");

        try (ActivityScenario<MainActivity> s = ActivityScenario.launch(MainActivity.class)) {
            Thread.sleep(3000);
            shell("screencap -p /sdcard/shots/main.png");
        }

        Intent reader = new Intent(ctx, ReaderActivity.class);
        reader.putExtra(ReaderActivity.EXTRA_SURAH, 2);
        reader.putExtra(ReaderActivity.EXTRA_AYAH, 1);
        try (ActivityScenario<ReaderActivity> s = ActivityScenario.launch(reader)) {
            Thread.sleep(3000);
            shell("screencap -p /sdcard/shots/reader.png");
        }

        int[][] pagesToShoot = {{1, 1}, {2, 1}, {2, 142}, {36, 1}, {18, 1}, {114, 1}};
        for (int[] sa : pagesToShoot) {
            Intent page = new Intent(ctx, PageReaderActivity.class);
            page.putExtra(ReaderActivity.EXTRA_SURAH, sa[0]);
            page.putExtra(ReaderActivity.EXTRA_AYAH, sa[1]);
            try (ActivityScenario<PageReaderActivity> s = ActivityScenario.launch(page)) {
                Thread.sleep(3500);
                shell("screencap -p /sdcard/shots/page_" + sa[0] + ".png");
            }
        }

        Intent listen = new Intent(ctx, PageReaderActivity.class);
        listen.putExtra(ReaderActivity.EXTRA_SURAH, 36);
        listen.putExtra(ReaderActivity.EXTRA_AYAH, 1);
        try (ActivityScenario<PageReaderActivity> s = ActivityScenario.launch(listen)) {
            Thread.sleep(3500);
            s.onActivity(a -> a.findViewById(com.csm.mushafi.R.id.btn_listen).performClick());
            Thread.sleep(9000);
            shell("screencap -p /sdcard/shots/page_listen.png");
        }

        Intent list = new Intent(ctx, SurahListActivity.class);
        try (ActivityScenario<SurahListActivity> s = ActivityScenario.launch(list)) {
            Thread.sleep(2500);
            shell("screencap -p /sdcard/shots/surah_list.png");
        }

        Intent quizIntent = new Intent(ctx, PageReaderActivity.class);
        quizIntent.putExtra(ReaderActivity.EXTRA_SURAH, 112);
        quizIntent.putExtra(ReaderActivity.EXTRA_AYAH, 1);
        try (ActivityScenario<PageReaderActivity> s = ActivityScenario.launch(quizIntent)) {
            Thread.sleep(3500);
            s.onActivity(a -> a.findViewById(com.csm.mushafi.R.id.btn_quiz).performClick());
            Thread.sleep(2500);
            shell("screencap -p /sdcard/shots/quiz_hidden.png");
        }

        ReadingPrefs reading = new ReadingPrefs(ctx);
        for (int theme = 0; theme < 5; theme++) {
            reading.saveAppTheme(theme);
            try (ActivityScenario<MainActivity> sc = ActivityScenario.launch(new Intent(ctx, MainActivity.class))) {
                Thread.sleep(2000);
                shell("screencap -p /sdcard/shots/app_theme_" + theme + ".png");
            }
        }
        reading.saveAppTheme(1);
        try (ActivityScenario<SettingsActivity> sc = ActivityScenario.launch(new Intent(ctx, SettingsActivity.class))) {
            Thread.sleep(2000);
            shell("screencap -p /sdcard/shots/settings_night.png");
        }
        reading.saveAppTheme(0);
        for (int pct : new int[]{80, 130}) {
            reading.savePageFontPercent(pct);
            Intent sized = new Intent(ctx, PageReaderActivity.class);
            sized.putExtra(ReaderActivity.EXTRA_SURAH, 36);
            sized.putExtra(ReaderActivity.EXTRA_AYAH, 1);
            try (ActivityScenario<PageReaderActivity> s = ActivityScenario.launch(sized)) {
                Thread.sleep(3500);
                shell("screencap -p /sdcard/shots/page_font_" + pct + ".png");
            }
        }
        reading.savePageFontPercent(100);
        reading.savePageTheme(3);
        Intent night = new Intent(ctx, PageReaderActivity.class);
        night.putExtra(ReaderActivity.EXTRA_SURAH, 36);
        night.putExtra(ReaderActivity.EXTRA_AYAH, 1);
        try (ActivityScenario<PageReaderActivity> s = ActivityScenario.launch(night)) {
            Thread.sleep(3500);
            shell("screencap -p /sdcard/shots/page_night.png");
        }
        reading.savePageTheme(0);

        try (ActivityScenario<SettingsActivity> s =
                     ActivityScenario.launch(new Intent(ctx, SettingsActivity.class))) {
            Thread.sleep(2000);
            shell("screencap -p /sdcard/shots/settings.png");
        }
    }

    @Test
    public void captureNewScreens() throws Exception {
        Context ctx = ApplicationProvider.getApplicationContext();
        new QuranRepository(ctx).verifyAndImportIfNeeded();
        shell("mkdir -p /sdcard/shots");
        Class<?>[] screens = {TasbihActivity.class, AdhkarActivity.class, HelpActivity.class};
        String[] names = {"tasbih", "adhkar", "help"};
        for (int i = 0; i < screens.length; i++) {
            try (ActivityScenario<?> s = ActivityScenario.launch(new Intent(ctx, screens[i]))) {
                Thread.sleep(1500);
                shell("screencap -p /sdcard/shots/" + names[i] + ".png");
            }
        }
        Intent read = new Intent(ctx, AdhkarReadActivity.class);
        read.putExtra(AdhkarReadActivity.EXTRA_INDEX, 0);
        try (ActivityScenario<AdhkarReadActivity> s = ActivityScenario.launch(read)) {
            Thread.sleep(1500);
            shell("screencap -p /sdcard/shots/adhkar_read.png");
        }
        Intent page = new Intent(ctx, PageReaderActivity.class);
        page.putExtra(ReaderActivity.EXTRA_SURAH, 18);
        page.putExtra(ReaderActivity.EXTRA_AYAH, 1);
        try (ActivityScenario<PageReaderActivity> s = ActivityScenario.launch(page)) {
            Thread.sleep(3500);
            s.onActivity(a -> {
                a.findViewById(R.id.btn_menu).setVisibility(android.view.View.VISIBLE);
                a.findViewById(R.id.btn_menu).performClick();
            });
            Thread.sleep(800);
            shell("screencap -p /sdcard/shots/reader_menu.png");
        }
    }

    private static void shell(String cmd) throws Exception {
        Instrumentation inst = InstrumentationRegistry.getInstrumentation();
        ParcelFileDescriptor pfd = inst.getUiAutomation().executeShellCommand(cmd);
        try (InputStream in = new ParcelFileDescriptor.AutoCloseInputStream(pfd)) {
            byte[] buf = new byte[4096];
            while (in.read(buf) != -1) {
                // اقرأ حتى النهاية لضمان اكتمال الأمر
            }
        }
    }
}
