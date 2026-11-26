package com.csm.mushafi;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.gson.Gson;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

/** قائمة أبواب الأذكار (حصن المسلم) ثم قراءة الباب مع عدّاد للتكرار. */
public class AdhkarActivity extends BaseActivity {

    static final class Item {
        String x;
        int n;
    }

    static final class Category {
        int id;
        String t;
        List<Item> a;
    }

    static List<Category> load(Context ctx) {
        try (InputStream in = ctx.getAssets().open("adhkar.json")) {
            Category[] all = new Gson().fromJson(new InputStreamReader(in, "UTF-8"), Category[].class);
            List<Category> out = new ArrayList<>();
            for (Category c : all) {
                out.add(c);
            }
            return out;
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_adhkar);
        findViewById(R.id.btn_back).setOnClickListener(v -> finish());
        List<Category> cats = load(this);
        RecyclerView list = findViewById(R.id.adhkar_list);
        list.setLayoutManager(new LinearLayoutManager(this));
        list.setAdapter(new RecyclerView.Adapter<RecyclerView.ViewHolder>() {
            @NonNull
            @Override
            public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                TextView t = new TextView(parent.getContext());
                RecyclerView.LayoutParams lp = new RecyclerView.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                float d = getResources().getDisplayMetrics().density;
                lp.setMargins((int) (16 * d), (int) (5 * d), (int) (16 * d), (int) (5 * d));
                t.setLayoutParams(lp);
                t.setBackgroundResource(R.drawable.bg_row);
                t.setMinHeight((int) (54 * d));
                t.setGravity(Gravity.CENTER_VERTICAL | Gravity.START);
                t.setPadding((int) (18 * d), (int) (12 * d), (int) (18 * d), (int) (12 * d));
                t.setTextSize(16);
                t.setTextColor(Themes.color(parent.getContext(), R.attr.textPrimary));
                return new RecyclerView.ViewHolder(t) {
                };
            }

            @Override
            public void onBindViewHolder(@NonNull RecyclerView.ViewHolder h, int position) {
                Category c = cats.get(position);
                ((TextView) h.itemView).setText(c.t);
                h.itemView.setOnClickListener(v -> {
                    Intent i = new Intent(AdhkarActivity.this, AdhkarReadActivity.class);
                    i.putExtra(AdhkarReadActivity.EXTRA_INDEX, position);
                    startActivity(i);
                });
            }

            @Override
            public int getItemCount() {
                return cats.size();
            }
        });
    }
}
