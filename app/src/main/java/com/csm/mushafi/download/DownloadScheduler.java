package com.csm.mushafi.download;

import android.content.Context;

import androidx.work.BackoffPolicy;
import androidx.work.Constraints;
import androidx.work.ExistingWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import com.csm.mushafi.audio.AudioUrls;
import com.csm.mushafi.data.AppDatabase;
import com.csm.mushafi.data.DownloadDao;
import com.csm.mushafi.data.DownloadEntity;

import java.util.List;
import java.util.concurrent.TimeUnit;

/** يضيف التنزيلات إلى قاعدة البيانات ويجدول العامل. العامل يستمر بعد إعادة التشغيل. */
public final class DownloadScheduler {

    static final String WORK_NAME = "mushafi-downloads";

    private DownloadScheduler() {
    }

    /** يضيف السور ويتجاهل ما هو موجود أو مكتمل. يعيد عدد ما أُضيف فعلاً. */
    public static int enqueue(Context context, String server, String reciterName, String moshafName,
                              List<Integer> surahs) {
        Context app = context.getApplicationContext();
        DownloadDao dao = AppDatabase.get(app).downloadDao();
        int added = 0;
        long now = System.currentTimeMillis();
        for (int surah : surahs) {
            DownloadEntity item = new DownloadEntity();
            item.server = server;
            item.surah = surah;
            item.reciterName = reciterName;
            item.moshafName = moshafName;
            item.url = AudioUrls.surahUrl(server, surah);
            item.path = DownloadStore.fileFor(app, server, surah).getAbsolutePath();
            item.state = DownloadEntity.QUEUED;
            item.createdAt = now + surah;
            if (dao.insertIfAbsent(item) != -1) {
                added++;
            }
        }
        if (added > 0) {
            schedule(app);
        }
        return added;
    }

    /** يعيد التنزيل من الحالة الفاشلة أو المتوقفة. */
    public static void requeue(Context context, DownloadEntity item) {
        Context app = context.getApplicationContext();
        item.state = DownloadEntity.QUEUED;
        item.attempts = 0;
        item.lastError = "";
        AppDatabase.get(app).downloadDao().update(item);
        schedule(app);
    }

    public static void schedule(Context app) {
        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();
        OneTimeWorkRequest request = new OneTimeWorkRequest.Builder(DownloadWorker.class)
                .setConstraints(constraints)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
                .build();
        WorkManager.getInstance(app).enqueueUniqueWork(WORK_NAME, ExistingWorkPolicy.KEEP, request);
    }
}
