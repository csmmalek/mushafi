package com.csm.mushafi;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.csm.mushafi.data.DownloadEntity;
import com.csm.mushafi.quran.ArabicDigits;

import java.util.List;
import java.util.Locale;

final class DownloadAdapter extends RecyclerView.Adapter<DownloadAdapter.Holder> {

    interface OnItem {
        void onClick(DownloadEntity item);
    }

    private final List<DownloadEntity> items;
    private final OnItem listener;

    DownloadAdapter(List<DownloadEntity> items, OnItem listener) {
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
        DownloadEntity d = items.get(position);
        holder.title.setText(holder.itemView.getContext().getString(R.string.download_row_format,
                ArabicDigits.format(d.surah), d.reciterName));
        holder.subtitle.setText(describe(holder, d));
        holder.itemView.setOnClickListener(v -> listener.onClick(d));
    }

    private String describe(Holder holder, DownloadEntity d) {
        String state = holder.itemView.getContext().getString(stateLabel(d.state));
        if (d.totalBytes > 0 && !DownloadEntity.COMPLETED.equals(d.state)) {
            int pct = (int) (100L * d.bytesDone / d.totalBytes);
            state += " · " + String.format(Locale.ROOT, "%d%%", pct);
        }
        if (!d.lastError.isEmpty() && DownloadEntity.FAILED.equals(d.state)) {
            state += "\n" + d.lastError;
        }
        return state;
    }

    private static int stateLabel(String state) {
        switch (state) {
            case DownloadEntity.COMPLETED:
                return R.string.download_state_completed;
            case DownloadEntity.RUNNING:
                return R.string.download_state_running;
            case DownloadEntity.PAUSED:
                return R.string.download_state_paused;
            case DownloadEntity.FAILED:
                return R.string.download_state_failed;
            default:
                return R.string.download_state_queued;
        }
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
            subtitle.setMaxLines(3);
        }
    }
}
