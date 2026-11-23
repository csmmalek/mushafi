package com.csm.mushafi;

import androidx.test.platform.app.InstrumentationRegistry;

import java.io.BufferedReader;
import java.io.InputStreamReader;

/** تشغيل أوامر shell عبر UiAutomation لتفعيل وضع الطيران في الاختبارات فقط. */
final class UiAutomationShell {
    private UiAutomationShell() {
    }

    static void setAirplaneMode(boolean on) throws Exception {
        String value = on ? "1" : "0";
        run("settings put global airplane_mode_on " + value);
        run("am broadcast -a android.intent.action.AIRPLANE_MODE --ez state " + (on ? "true" : "false"));
        Thread.sleep(3000);
    }

    private static void run(String command) throws Exception {
        android.app.UiAutomation ua = InstrumentationRegistry.getInstrumentation().getUiAutomation();
        android.os.ParcelFileDescriptor pfd = ua.executeShellCommand(command);
        try (BufferedReader r = new BufferedReader(new InputStreamReader(
                new android.os.ParcelFileDescriptor.AutoCloseInputStream(pfd)))) {
            while (r.readLine() != null) {
                // drain output
            }
        }
    }
}
