package com.csm.mushafi;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.csm.mushafi.audio.ReciterCatalogue;
import com.csm.mushafi.quran.ArabicDigits;

import java.util.List;

final class ReciterAdapter extends RecyclerView.Adapter<ReciterAdapter.Holder> {

    interface OnReciter {
        void onClick(ReciterCatalogue.Reciter reciter);
    }

    private final List<ReciterCatalogue.Reciter> items;
    private final OnReciter listener;

    ReciterAdapter(List<ReciterCatalogue.Reciter> items, OnReciter listener) {
        this.items = items;
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
        ReciterCatalogue.Reciter r = items.get(position);
        holder.title.setText(r.name);
        holder.subtitle.setText(holder.itemView.getContext().getString(
                R.string.reciter_moshaf_count_format, ArabicDigits.format(r.moshafs.size())));
        holder.itemView.setOnClickListener(v -> listener.onClick(r));
    }

    @Override
    public int getItemCount() {
        return items.size();
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
