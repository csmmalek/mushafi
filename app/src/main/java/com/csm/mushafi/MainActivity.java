package com.csm.mushafi;

import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

/**
 * Stage 2: home screen shell only.
 * Every feature button is disabled until its stage is implemented,
 * so no visible button performs a fake action.
 */
public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        int[] disabledControls = {
                R.id.btn_continue,
                R.id.btn_recitations,
                R.id.btn_quran,
                R.id.btn_bookmarks,
                R.id.btn_settings
        };
        for (int id : disabledControls) {
            View v = findViewById(id);
            v.setEnabled(false);
            v.setAlpha(0.55f);
        }
    }
}
