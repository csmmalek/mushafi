package com.csm.mushafi.data;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/** آية كما وردت من ملف النص المعتمد، بلا أي تعديل. */
@Entity(tableName = "ayahs", indices = {@Index(value = {"surah", "ayah"}, unique = true)})
public class AyahEntity {
    @PrimaryKey(autoGenerate = true)
    public long id;

    public int surah;
    public int ayah;

    @NonNull
    public String text = "";

    public AyahEntity() {
    }

    public AyahEntity(int surah, int ayah, @NonNull String text) {
        this.surah = surah;
        this.ayah = ayah;
        this.text = text;
    }
}
