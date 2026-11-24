package com.csm.mushafi;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.content.Intent;
import android.widget.TextView;

import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

/** يتحقق من أن شاشة الإعدادات تعرض حالة التخزين، وأن زر الحذف يعمل حسب وجود ملفات. */
@RunWith(AndroidJUnit4.class)
public class SettingsScreenTest {

    @Test
    public void storageLineIsShownAndClearButtonMatchesState() throws Exception {
        Intent intent = new Intent(ApplicationProvider.getApplicationContext(), SettingsActivity.class);
        try (ActivityScenario<SettingsActivity> scenario = ActivityScenario.launch(intent)) {
            long end = System.currentTimeMillis() + 10_000;
            final String[] text = {""};
            while (System.currentTimeMillis() < end) {
                scenario.onActivity(a -> text[0] = ((TextView) a.findViewById(R.id.settings_storage)).getText().toString());
                if (text[0].contains("ملفات محمّلة")) {
                    break;
                }
                Thread.sleep(200);
            }
            assertTrue("storage line not shown: " + text[0], text[0].contains("ملفات محمّلة"));

            final boolean[] enabled = {true};
            scenario.onActivity(a -> enabled[0] = a.findViewById(R.id.btn_clear_audio).isEnabled());
            // إن لم تكن هناك ملفات محمّلة يجب أن يكون زر الحذف معطّلاً (لا زر وهمي).
            if (text[0].startsWith("ملفات محمّلة: ٠")) {
                assertFalse("clear button must be disabled with no files", enabled[0]);
            }
        }
    }
}
