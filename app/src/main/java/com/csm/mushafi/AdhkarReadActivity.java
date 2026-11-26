package com.csm.mushafi;

import android.os.Bundle;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.csm.mushafi.quran.ArabicDigits;

import java.util.List;

/** قراءة باب من الأذكار: لمسة على البطاقة تُنقص عدّاد التكرار حتى ✓. */
public class AdhkarReadActivity extends BaseActivity {

    static final String EXTRA_INDEX = "index";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_adhkar_read);
        findViewById(R.id.btn_back).setOnClickListener(v -> finish());
        List<AdhkarActivity.Category> all = AdhkarActivity.load(this);
        int index = Math.max(0, Math.min(all.size() - 1, getIntent().getIntExtra(EXTRA_INDEX, 0)));
        if (all.isEmpty()) {
            finish();
            return;
        }
        AdhkarActivity.Category cat = all.get(index);
        ((TextView) findViewById(R.id.adhkar_title)).setText(cat.t);
        int[] left = new int[cat.a.size()];
        for (int i = 0; i < left.length; i++) {
            left[i] = Math.max(1, cat.a.get(i).n);
        }
        RecyclerView list = findViewById(R.id.adhkar_items);
        list.setLayoutManager(new LinearLayoutManager(this));
        list.setAdapter(new RecyclerView.Adapter<RecyclerView.ViewHolder>() {
            @NonNull
            @Override
            public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                float d = getResources().getDisplayMetrics().density;
                LinearLayout box = new LinearLayout(parent.getContext());
                box.setOrientation(LinearLayout.VERTICAL);
                RecyclerView.LayoutParams lp = new RecyclerView.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                lp.setMargins((int) (14 * d), (int) (6 * d), (int) (14 * d), (int) (6 * d));
                box.setLayoutParams(lp);
                box.setBackgroundResource(R.drawable.bg_feature_card);
                box.setPadding((int) (18 * d), (int) (16 * d), (int) (18 * d), (int) (14 * d));
                TextView text = new TextView(parent.getContext());
                text.setId(android.R.id.text1);
                text.setTextSize(19);
                text.setLineSpacing(0, 1.35f);
                text.setTextColor(Themes.color(parent.getContext(), R.attr.textPrimary));
                TextView counter = new TextView(parent.getContext());
                counter.setId(android.R.id.text2);
                counter.setTextSize(15);
                counter.setGravity(Gravity.CENTER);
                counter.setTextColor(Themes.color(parent.getContext(), R.attr.accent));
                counter.setPadding(0, (int) (10 * d), 0, 0);
                box.addView(text);
                box.addView(counter);
                return new RecyclerView.ViewHolder(box) {
                };
            }

            @Override
            public void onBindViewHolder(@NonNull RecyclerView.ViewHolder h, int position) {
                TextView text = h.itemView.findViewById(android.R.id.text1);
                TextView counter = h.itemView.findViewById(android.R.id.text2);
                AdhkarActivity.Item item = cat.a.get(position);
                text.setText(item.x);
                bindCounter(counter, item.n, left[position]);
                h.itemView.setOnClickListener(v -> {
                    int p = h.getBindingAdapterPosition();
                    if (p == RecyclerView.NO_POSITION || left[p] <= 0) {
                        return;
                    }
                    left[p]--;
                    bindCounter(counter, cat.a.get(p).n, left[p]);
                });
            }

            private void bindCounter(TextView counter, int total, int remaining) {
                counter.setText(remaining <= 0 ? "✓" : getString(R.string.adhkar_left,
                        ArabicDigits.format(remaining), ArabicDigits.format(Math.max(1, total))));
            }

            @Override
            public int getItemCount() {
                return cat.a.size();
            }
        });
    }
}
