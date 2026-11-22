package com.csm.mushafi;

import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.csm.mushafi.data.AyahEntity;
import com.csm.mushafi.quran.ArabicDigits;

import java.util.ArrayList;
import java.util.List;

/** يعرض نص الآية كما هو من قاعدة البيانات، مع رقم الآية في النهاية للعرض فقط. */
final class AyahAdapter extends RecyclerView.Adapter<AyahAdapter.Holder> {

    private final List<AyahEntity> ayahs = new ArrayList<>();
    private int fontSp;

    AyahAdapter(int fontSp) {
        this.fontSp = fontSp;
    }

    void setAyahs(List<AyahEntity> items) {
        ayahs.clear();
        ayahs.addAll(items);
        notifyDataSetChanged();
    }

    void setFontSp(int sp) {
        fontSp = sp;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_ayah, parent, false);
        return new Holder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        AyahEntity a = ayahs.get(position);
        holder.text.setTextSize(TypedValue.COMPLEX_UNIT_SP, fontSp);
        holder.text.setText(a.text + "  ﴿" + ArabicDigits.format(a.ayah) + "﴾");
    }

    @Override
    public int getItemCount() {
        return ayahs.size();
    }

    static final class Holder extends RecyclerView.ViewHolder {
        final TextView text;

        Holder(View itemView) {
            super(itemView);
            text = itemView.findViewById(R.id.ayah_text);
        }
    }
}
