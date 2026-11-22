package com.csm.mushafi.data;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

/** سورة: الرقم وعدد آياتها فقط. أسماء السور لا تُكتب هنا حتى يتوفر مصدر معتمد. */
@Entity(tableName = "surahs")
public class SurahEntity {
    @PrimaryKey
    public int number;

    public int ayahCount;

    public SurahEntity() {
    }

    public SurahEntity(int number, int ayahCount) {
        this.number = number;
        this.ayahCount = ayahCount;
    }
}
