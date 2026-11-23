package com.csm.mushafi;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.csm.mushafi.audio.CatalogueCache;
import com.csm.mushafi.audio.ReciterCatalogue;
import com.csm.mushafi.playback.PlayerActivity;
import com.csm.mushafi.quran.ArabicDigits;

import java.util.List;

/** تفاصيل القارئ: السور المتاحة لهذه الرواية. التشغيل يبدأ من الشبكة. التنزيل في المرحلة السادسة. */
public class ReciterActivity extends AppCompatActivity {

    static final String EXTRA_RECITER_INDEX = "reciterIndex";
    static final String EXTRA_MOSHAF_INDEX = "moshafIndex";

    private ReciterCatalogue.Reciter reciter;
    private ReciterCatalogue.Moshaf moshaf;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reciter);
        findViewById(R.id.btn_back).setOnClickListener(v -> finish());

        ReciterCatalogue.Result result = CatalogueCache.get();
        int ri = getIntent().getIntExtra(EXTRA_RECITER_INDEX, -1);
        int mi = getIntent().getIntExtra(EXTRA_MOSHAF_INDEX, 0);
        if (result == null || ri < 0 || ri >= result.reciters.size()) {
            finish();
            return;
        }
        reciter = result.reciters.get(ri);
        if (mi < 0 || mi >= reciter.moshafs.size()) {
            mi = 0;
        }
        moshaf = reciter.moshafs.get(mi);

        ((TextView) findViewById(R.id.reciter_name)).setText(reciter.name);
        ((TextView) findViewById(R.id.reciter_moshaf)).setText(moshaf.name);
        ((TextView) findViewById(R.id.reciter_source)).setText(getString(R.string.reciter_source_format,
                ArabicDigits.format(moshaf.surahs.size())));

        RecyclerView list = findViewById(R.id.reciter_surahs);
        list.setLayoutManager(new LinearLayoutManager(this));
        list.setAdapter(new SurahAudioAdapter(moshaf.surahs, index -> {
            Intent intent = new Intent(this, PlayerActivity.class);
            intent.putExtra(PlayerActivity.EXTRA_RECITER, reciter.name);
            intent.putExtra(PlayerActivity.EXTRA_MOSHAF, moshaf.name);
            intent.putExtra(PlayerActivity.EXTRA_SERVER, moshaf.server);
            intent.putExtra(PlayerActivity.EXTRA_SURAHS, toArray(moshaf.surahs));
            intent.putExtra(PlayerActivity.EXTRA_START_INDEX, index);
            startActivity(intent);
        }));
    }

    private static int[] toArray(List<Integer> values) {
        int[] out = new int[values.size()];
        for (int i = 0; i < out.length; i++) {
            out[i] = values.get(i);
        }
        return out;
    }
}
