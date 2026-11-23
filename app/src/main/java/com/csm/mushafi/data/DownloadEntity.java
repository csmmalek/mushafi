package com.csm.mushafi.data;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/** تنزيل سورة من خادم قارئ. الحالة: QUEUED، RUNNING، PAUSED، COMPLETED، FAILED. */
@Entity(tableName = "downloads", indices = {@Index(value = {"server", "surah"}, unique = true)})
public class DownloadEntity {
    public static final String QUEUED = "QUEUED";
    public static final String RUNNING = "RUNNING";
    public static final String PAUSED = "PAUSED";
    public static final String COMPLETED = "COMPLETED";
    public static final String FAILED = "FAILED";

    @PrimaryKey(autoGenerate = true)
    public long id;

    @NonNull
    public String server = "";
    public int surah;

    @NonNull
    public String reciterName = "";
    @NonNull
    public String moshafName = "";
    @NonNull
    public String url = "";
    @NonNull
    public String path = "";
    @NonNull
    public String state = QUEUED;

    public long bytesDone;
    public long totalBytes;
    public int attempts;
    @NonNull
    public String lastError = "";
    public long createdAt;

    public DownloadEntity() {
    }
}
