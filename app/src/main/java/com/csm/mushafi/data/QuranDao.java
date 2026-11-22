package com.csm.mushafi.data;

import java.util.List;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

@Dao
public interface QuranDao {

    @Insert
    void insertSurahs(List<SurahEntity> surahs);

    @Insert
    void insertAyahs(List<AyahEntity> ayahs);

    @Insert
    void insertSearch(List<SearchEntity> rows);

    @Query("DELETE FROM ayahs")
    void clearAyahs();

    @Query("DELETE FROM surahs")
    void clearSurahs();

    @Query("DELETE FROM search_ayahs")
    void clearSearch();

    @Query("SELECT COUNT(*) FROM surahs")
    int countSurahs();

    @Query("SELECT COUNT(*) FROM ayahs")
    int countAyahs();

    @Query("SELECT COUNT(*) FROM search_ayahs")
    int countSearch();

    @Query("SELECT * FROM surahs ORDER BY number")
    List<SurahEntity> allSurahs();

    @Query("SELECT * FROM ayahs WHERE surah = :surah ORDER BY ayah")
    List<AyahEntity> ayahsOfSurah(int surah);

    @Query("SELECT a.surah AS surah, a.ayah AS ayah, a.text AS text FROM search_ayahs s "
            + "JOIN ayahs a ON a.surah = s.surah AND a.ayah = s.ayah "
            + "WHERE s.normalized LIKE '%' || :needle || '%' ORDER BY a.surah, a.ayah LIMIT 200")
    List<SearchHit> search(String needle);

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    long insertBookmark(BookmarkEntity bookmark);

    @Query("DELETE FROM bookmarks WHERE id = :id")
    void deleteBookmark(long id);

    @Query("UPDATE bookmarks SET label = :label WHERE id = :id")
    void renameBookmark(long id, String label);

    @Query("SELECT * FROM bookmarks ORDER BY surah, ayah")
    List<BookmarkEntity> allBookmarks();

    @Query("SELECT * FROM bookmarks WHERE surah = :surah AND ayah = :ayah LIMIT 1")
    BookmarkEntity findBookmark(int surah, int ayah);
}
