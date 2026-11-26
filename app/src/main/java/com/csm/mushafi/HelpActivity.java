package com.csm.mushafi;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;

import java.io.InputStream;
import java.io.InputStreamReader;

/** المساعدة والدعم: شرح الاستخدام وروابط التواصل من assets/support.json (الفارغ منها لا يظهر). */
public class HelpActivity extends BaseActivity {

    static final class Link {
        String label;
        String url;
    }

    static final class Support {
        Link[] links;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_help);
        findViewById(R.id.btn_back).setOnClickListener(v -> finish());
        LinearLayout box = findViewById(R.id.support_links);
        Link[] links = new Link[0];
        try (InputStream in = getAssets().open("support.json")) {
            Support s = new Gson().fromJson(new InputStreamReader(in, "UTF-8"), Support.class);
            if (s != null && s.links != null) {
                links = s.links;
            }
        } catch (Exception ignored) {
            // بلا روابط
        }
        float d = getResources().getDisplayMetrics().density;
        int shown = 0;
        for (Link link : links) {
            if (link.url == null || link.url.trim().isEmpty()) {
                continue;
            }
            shown++;
            Button b = new Button(this);
            b.setBackgroundResource(R.drawable.bg_row);
            b.setText(link.label);
            b.setAllCaps(false);
            b.setTextSize(16);
            b.setTextColor(Themes.color(this, R.attr.textPrimary));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, (int) (54 * d));
            lp.topMargin = (int) (8 * d);
            b.setLayoutParams(lp);
            final String url = link.url.trim();
            b.setOnClickListener(v -> open(url));
            box.addView(b);
        }
        if (shown == 0) {
            TextView none = new TextView(this);
            none.setText(R.string.support_empty);
            none.setTextColor(Themes.color(this, R.attr.textMuted));
            none.setTextSize(14);
            box.addView(none);
        }
    }

    private void open(String url) {
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
        } catch (Exception e) {
            Toast.makeText(this, R.string.support_open_failed, Toast.LENGTH_SHORT).show();
        }
    }
}
