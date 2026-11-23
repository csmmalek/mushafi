package com.csm.mushafi.download;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.csm.mushafi.data.AppDatabase;
import com.csm.mushafi.data.DownloadDao;
import com.csm.mushafi.data.DownloadEntity;

import java.io.File;
import java.io.IOException;

/** يعالج قائمة التنزيلات واحداً بعد الآخر. الفشل المؤقت يعيد المحاولة بتأخير متزايد. */
public class DownloadWorker extends Worker {

    public DownloadWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        DownloadDao dao = AppDatabase.get(getApplicationContext()).downloadDao();
        while (true) {
            if (isStopped()) {
                return Result.retry();
            }
            DownloadEntity item = dao.nextPending();
            if (item == null) {
                return Result.success();
            }
            if (!DownloadStore.hasSpace(getApplicationContext())) {
                item.state = DownloadEntity.FAILED;
                item.lastError = "المساحة المتاحة غير كافية";
                item.attempts = 3;
                dao.update(item);
                continue;
            }
            item.state = DownloadEntity.RUNNING;
            dao.update(item);

            File dest = DownloadStore.fileFor(getApplicationContext(), item.server, item.surah);
            final long id = item.id;
            try {
                DownloadEngine.Outcome outcome = DownloadEngine.download(item.url, dest,
                        new DownloadEngine.Control() {
                            @Override
                            public boolean shouldStop() {
                                String s = dao.stateOf(id);
                                return s == null || DownloadEntity.PAUSED.equals(s);
                            }

                            @Override
                            public void progress(long bytesDone, long totalBytes) {
                                DownloadEntity current = dao.byId(id);
                                if (current != null) {
                                    current.bytesDone = bytesDone;
                                    current.totalBytes = totalBytes;
                                    dao.update(current);
                                }
                            }
                        });
                DownloadEntity latest = dao.byId(id);
                if (latest == null) {
                    continue;
                }
                if (outcome == DownloadEngine.Outcome.COMPLETED) {
                    latest.state = DownloadEntity.COMPLETED;
                    latest.path = dest.getAbsolutePath();
                    latest.bytesDone = dest.length();
                    latest.totalBytes = dest.length();
                    latest.lastError = "";
                } else if (!DownloadEntity.PAUSED.equals(latest.state)) {
                    latest.state = DownloadEntity.PAUSED;
                }
                dao.update(latest);
            } catch (IOException e) {
                DownloadEntity latest = dao.byId(id);
                if (latest != null && !DownloadEntity.PAUSED.equals(latest.state)) {
                    latest.state = DownloadEntity.FAILED;
                    latest.attempts = latest.attempts + 1;
                    latest.lastError = e.getMessage() == null ? "تعذّر التنزيل" : e.getMessage();
                    dao.update(latest);
                }
                return Result.retry();
            }
        }
    }
}
