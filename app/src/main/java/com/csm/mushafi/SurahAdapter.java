package com.csm.mushafi;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.csm.mushafi.data.SurahEntity;
import com.csm.mushafi.quran.ArabicDigits;

import java.util.List;

final class SurahAdapter extends RecyclerView.Adapter<SurahAdapter.Holder> {

    interface OnSurahClick {
        void onClick(int number);
    }

    private final List<SurahEntity> surahs;
    private final OnSurahClick listener;

    SurahAdapter(List<SurahEntity> surahs, OnSurahClick listener) {
        this.surahs = surahs;
        this.listener = listener;
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_surah, parent, false);
        return new Holder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        SurahEntity s = surahs.get(position);
        holder.title.setText(SurahNames.title(holder.itemView.getContext(), s.number));
        holder.subtitle.setText(holder.itemView.getContext()
                .getString(R.string.ayah_count_format, ArabicDigits.format(s.ayahCount)));
        holder.itemView.setOnClickListener(v -> listener.onClick(s.number));
    }

    @Override
    public int getItemCount() {
        return surahs.size();
    }

    static final class Holder extends RecyclerView.ViewHolder {
        final TextView title;
        final TextView subtitle;

        Holder(View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.surah_title);
            subtitle = itemView.findViewById(R.id.surah_subtitle);
        }
    }
}
