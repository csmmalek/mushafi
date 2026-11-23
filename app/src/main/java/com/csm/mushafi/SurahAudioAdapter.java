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
import java.util.Map;

/**
 * قائمة سور القارئ. السورة المحمّلة تعمل دون إنترنت، وغير المحمّلة تُعطَّل في وضع الطيران.
 */
final class SurahAudioAdapter extends RecyclerView.Adapter<SurahAudioAdapter.Holder> {

    interface Listener {
        void onPlay(int index);

        void onLongPress(int surah);

        void onBlocked(int surah);
    }

    private final List<Integer> surahs;
    private final Map<Integer, DownloadEntity> completed;
    private final boolean online;
    private final Listener listener;

    SurahAudioAdapter(List<Integer> surahs, Map<Integer, DownloadEntity> completed, boolean online,
                      Listener listener) {
        this.surahs = surahs;
        this.completed = completed;
        this.online = online;
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
        boolean local = completed.containsKey(surah);
        boolean playable = local || online;
        holder.title.setText(holder.itemView.getContext()
                .getString(R.string.surah_number_format, ArabicDigits.format(surah)));
        int hint = local ? R.string.play_surah_local : (online ? R.string.play_surah_hint : R.string.play_needs_network);
        holder.subtitle.setText(hint);
        holder.itemView.setAlpha(playable ? 1f : 0.5f);
        holder.itemView.setOnClickListener(v -> {
            if (playable) {
                listener.onPlay(position);
            } else {
                listener.onBlocked(surah);
            }
        });
        holder.itemView.setOnLongClickListener(v -> {
            listener.onLongPress(surah);
            return true;
        });
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
