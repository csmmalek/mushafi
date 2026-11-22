package com.csm.mushafi.data;

import androidx.annotation.NonNull;
import androidx.room.Entity;

/** فهرس بحث مشتق من نص الآيات. منفصل عن جدول النص الأصلي ولا يستبدله. */
@Entity(tableName = "search_ayahs", primaryKeys = {"surah", "ayah"})
public class SearchEntity {
    public int surah;
    public int ayah;

    @NonNull
    public String normalized = "";

    public SearchEntity() {
    }

    public SearchEntity(int surah, int ayah, @NonNull String normalized) {
        this.surah = surah;
        this.ayah = ayah;
        this.normalized = normalized;
    }
}
