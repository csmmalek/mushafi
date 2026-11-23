package com.csm.mushafi;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.csm.mushafi.quran.ArabicDigits;

import java.util.List;

/** قائمة سور القارئ المتاحة. النقر يبدأ التشغيل من تلك السورة. */
final class SurahAudioAdapter extends RecyclerView.Adapter<SurahAudioAdapter.Holder> {

    interface OnIndex {
        void onClick(int index);
    }

    private final List<Integer> surahs;
    private final OnIndex listener;

    SurahAudioAdapter(List<Integer> surahs, OnIndex listener) {
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
        int surah = surahs.get(position);
        holder.title.setText(holder.itemView.getContext()
                .getString(R.string.surah_number_format, ArabicDigits.format(surah)));
        holder.subtitle.setText(holder.itemView.getContext().getString(R.string.play_surah_hint));
        holder.itemView.setOnClickListener(v -> listener.onClick(position));
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
