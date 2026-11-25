package com.csm.mushafi;

import android.graphics.Color;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.csm.mushafi.data.AyahEntity;
import com.csm.mushafi.quran.ArabicDigits;

import java.util.ArrayList;
import java.util.List;

/** يعرض نص الآية كما هو من قاعدة البيانات، مع رقم الآية في النهاية للعرض فقط. */
final class AyahAdapter extends RecyclerView.Adapter<AyahAdapter.Holder> {

    private final List<AyahEntity> ayahs = new ArrayList<>();
    private int fontSp;
    private int selectedAyah;
    private OnAyahClick clickListener;

    /** يُستدعى عند الضغط على آية، برقمها داخل السورة. */
    interface OnAyahClick {
        void onAyahClick(int ayahNumber);
    }

    void setOnAyahClick(OnAyahClick listener) {
        clickListener = listener;
    }

    void setSelectedAyah(int ayahNumber) {
        selectedAyah = ayahNumber;
        notifyDataSetChanged();
    }

    /** نص الآية الخام من قاعدة البيانات (بدون رقم العرض)، أو null إن لم تكن موجودة. */
    String rawTextOf(int ayahNumber) {
        for (AyahEntity a : ayahs) {
            if (a.ayah == ayahNumber) {
                return a.text;
            }
        }
        return null;
    }

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
        holder.text.setText(a.text + "\u00A0﴿" + ArabicDigits.format(a.ayah) + "﴾");
        boolean selected = a.ayah == selectedAyah;
        holder.text.setBackgroundColor(selected
                ? ContextCompat.getColor(holder.text.getContext(), R.color.gold_soft)
                : Color.TRANSPARENT);
        holder.text.setOnClickListener(v -> {
            if (clickListener != null) {
                clickListener.onAyahClick(a.ayah);
            }
        });
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
