package com.csm.mushafi;

import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

/** أساس كل الشاشات: يطبّق ثيم التطبيق المختار (زمردي، ليلي، ياقوتي، كهرماني، لؤلؤي). */
public abstract class BaseActivity extends AppCompatActivity {

    private int appliedAppTheme;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        appliedAppTheme = new ReadingPrefs(this).appTheme();
        setTheme(AppThemes.style(appliedAppTheme));
        super.onCreate(savedInstanceState);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (new ReadingPrefs(this).appTheme() != appliedAppTheme) {
            recreate();
        }
    }
}
