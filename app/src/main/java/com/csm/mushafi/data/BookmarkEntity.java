package com.csm.mushafi.data;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/** علامة مرجعية محلية على آية. */
@Entity(tableName = "bookmarks", indices = {@Index(value = {"surah", "ayah"}, unique = true)})
public class BookmarkEntity {
    @PrimaryKey(autoGenerate = true)
    public long id;

    public int surah;
    public int ayah;

    @NonNull
    public String label = "";

    public long createdAt;

    public BookmarkEntity() {
    }

    public BookmarkEntity(int surah, int ayah, @NonNull String label, long createdAt) {
        this.surah = surah;
        this.ayah = ayah;
        this.label = label;
        this.createdAt = createdAt;
    }
}
