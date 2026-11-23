package com.csm.mushafi.data;

import java.util.List;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

@Dao
public interface DownloadDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    long insertIfAbsent(DownloadEntity item);

    @Update
    void update(DownloadEntity item);

    @Query("SELECT * FROM downloads WHERE server = :server AND surah = :surah LIMIT 1")
    DownloadEntity find(String server, int surah);

    @Query("SELECT * FROM downloads WHERE id = :id LIMIT 1")
    DownloadEntity byId(long id);

    @Query("SELECT state FROM downloads WHERE id = :id")
    String stateOf(long id);

    @Query("SELECT * FROM downloads ORDER BY createdAt, surah")
    List<DownloadEntity> all();

    @Query("SELECT * FROM downloads WHERE server = :server AND state = 'COMPLETED'")
    List<DownloadEntity> completedFor(String server);

    @Query("SELECT * FROM downloads WHERE state = 'QUEUED' OR (state = 'FAILED' AND attempts < 3) "
            + "ORDER BY createdAt, surah LIMIT 1")
    DownloadEntity nextPending();

    @Query("DELETE FROM downloads WHERE id = :id")
    void delete(long id);
}
