package com.csm.mushafi;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.csm.mushafi.data.SearchHit;
import com.csm.mushafi.quran.ArabicDigits;

import java.util.List;

final class SearchAdapter extends RecyclerView.Adapter<SearchAdapter.Holder> {

    interface OnHit {
        void onClick(SearchHit hit);
    }

    private final List<SearchHit> hits;
    private final OnHit listener;

    SearchAdapter(List<SearchHit> hits, OnHit listener) {
        this.hits = hits;
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
        SearchHit h = hits.get(position);
        holder.title.setText(holder.itemView.getContext().getString(R.string.bookmark_place_format,
                ArabicDigits.format(h.surah), ArabicDigits.format(h.ayah)));
        holder.subtitle.setText(h.text);
        holder.subtitle.setMaxLines(2);
        holder.itemView.setOnClickListener(v -> listener.onClick(h));
    }

    @Override
    public int getItemCount() {
        return hits.size();
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
