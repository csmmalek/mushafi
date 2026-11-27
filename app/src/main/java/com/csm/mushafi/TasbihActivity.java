package com.csm.mushafi;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.csm.mushafi.quran.ArabicDigits;

/** مسبحة إلكترونية: عدّاد بلمسة، أذكار جاهزة، هدف (33/99/100)، ويُحفظ مجموع كل ذكر. */
public class TasbihActivity extends BaseActivity {

    private static final String[] PHRASES = {
            "سُبْحَانَ اللَّهِ", "الْحَمْدُ لِلَّهِ", "اللَّهُ أَكْبَرُ",
            "لَا إِلَهَ إِلَّا اللَّهُ", "أَسْتَغْفِرُ اللَّهَ"};
    private static final int[] TARGETS = {33, 99, 100};

    private SharedPreferences store;
    private int phrase;
    private int targetIndex;
    private int count;
    private TextView phraseText;
    private TextView countText;
    private TextView totalText;
    private TextView targetText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tasbih);
        store = getSharedPreferences("tasbih", MODE_PRIVATE);
        phrase = store.getInt("phrase", 0);
        targetIndex = store.getInt("target", 0);
        phraseText = findViewById(R.id.tasbih_phrase);
        countText = findViewById(R.id.tasbih_tap);
        totalText = findViewById(R.id.tasbih_total);
        targetText = findViewById(R.id.tasbih_target);
        findViewById(R.id.btn_back).setOnClickListener(v -> finish());
        findViewById(R.id.tasbih_tap).setOnClickListener(v -> tap());
        findViewById(R.id.tasbih_reset).setOnClickListener(v -> {
            count = 0;
            render();
        });
        findViewById(R.id.tasbih_next).setOnClickListener(v -> {
            phrase = (phrase + 1) % PHRASES.length;
            count = 0;
            store.edit().putInt("phrase", phrase).apply();
            render();
        });
        targetText.setOnClickListener(v -> {
            targetIndex = (targetIndex + 1) % TARGETS.length;
            store.edit().putInt("target", targetIndex).apply();
            render();
        });
        render();
    }

    private void tap() {
        count++;
        store.edit().putInt("total_" + phrase, store.getInt("total_" + phrase, 0) + 1).apply();
        if (count == TARGETS[targetIndex]) {
            buzz(220);
        } else {
            buzz(12);
        }
        if (count > TARGETS[targetIndex]) {
            count = 1;
        }
        render();
    }

    @SuppressWarnings("deprecation")
    private void buzz(long ms) {
        try {
            Vibrator vib = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
            if (vib != null && vib.hasVibrator()) {
                vib.vibrate(VibrationEffect.createOneShot(ms, VibrationEffect.DEFAULT_AMPLITUDE));
            }
        } catch (RuntimeException ignored) {
            // الاهتزاز اختياري ولا يجب أن يوقف العدّ
        }
    }

    private void render() {
        phraseText.setText(PHRASES[phrase]);
        countText.setText(ArabicDigits.format(count));
        targetText.setText(getString(R.string.tasbih_target, ArabicDigits.format(TARGETS[targetIndex])));
        totalText.setText(getString(R.string.tasbih_total,
                ArabicDigits.format(store.getInt("total_" + phrase, 0))));
    }
}
