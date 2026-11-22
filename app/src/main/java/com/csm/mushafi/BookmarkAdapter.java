package com.csm.mushafi;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.csm.mushafi.data.BookmarkEntity;
import com.csm.mushafi.quran.ArabicDigits;

import java.util.List;

final class BookmarkAdapter extends RecyclerView.Adapter<BookmarkAdapter.Holder> {

    interface Listener {
        void onOpen(BookmarkEntity b);

        void onLongPress(BookmarkEntity b);
    }

    private final List<BookmarkEntity> items;
    private final Listener listener;

    BookmarkAdapter(List<BookmarkEntity> items, Listener listener) {
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
        BookmarkEntity b = items.get(position);
        String place = holder.itemView.getContext().getString(R.string.bookmark_place_format,
                ArabicDigits.format(b.surah), ArabicDigits.format(b.ayah));
        holder.title.setText(b.label.isEmpty() ? place : b.label);
        holder.subtitle.setText(place);
        holder.itemView.setOnClickListener(v -> listener.onOpen(b));
        holder.itemView.setOnLongClickListener(v -> {
            listener.onLongPress(b);
            return true;
        });
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
