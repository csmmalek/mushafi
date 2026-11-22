package com.csm.mushafi.data;

import java.util.List;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

@Dao
public interface QuranDao {

    @Insert
    void insertSurahs(List<SurahEntity> surahs);

    @Insert
    void insertAyahs(List<AyahEntity> ayahs);

    @Query("DELETE FROM ayahs")
    void clearAyahs();

    @Query("DELETE FROM surahs")
    void clearSurahs();

    @Query("SELECT COUNT(*) FROM surahs")
    int countSurahs();

    @Query("SELECT COUNT(*) FROM ayahs")
    int countAyahs();

    @Query("SELECT * FROM surahs ORDER BY number")
    List<SurahEntity> allSurahs();

    @Query("SELECT * FROM ayahs WHERE surah = :surah ORDER BY ayah")
    List<AyahEntity> ayahsOfSurah(int surah);
}
