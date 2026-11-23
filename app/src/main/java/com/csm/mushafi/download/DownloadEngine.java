package com.csm.mushafi.download;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * ينزّل ملفاً واحداً إلى ملف مؤقت ‎.part، ويستأنف بـ Range إذا دعم الخادم ذلك.
 * لا يعلن الاكتمال إلا بعد مطابقة الحجم المتوقع ورأس MP3.
 */
public final class DownloadEngine {

    public interface Control {
        /** true إذا طلب المستخدم الإيقاف أو حُذف التنزيل. */
        boolean shouldStop();

        void progress(long bytesDone, long totalBytes);
    }

    public enum Outcome {COMPLETED, STOPPED}

    private static final int BUFFER = 64 * 1024;

    private DownloadEngine() {
    }

    public static Outcome download(String url, File dest, Control control) throws IOException {
        File part = new File(dest.getParentFile(), dest.getName() + ".part");
        long existing = part.exists() ? part.length() : 0;

        HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
        try {
            conn.setConnectTimeout(15000);
            conn.setReadTimeout(30000);
            conn.setInstanceFollowRedirects(true);
            String range = Mp3Check.rangeHeader(existing);
            if (range != null) {
                conn.setRequestProperty("Range", range);
            }
            int code = conn.getResponseCode();
            if (code == 416) {
                // المؤقت أكبر من الملف الأصلي: نبدأ من جديد.
                part.delete();
                throw new IOException("الملف المؤقت غير صالح، أعد المحاولة.");
            }
            boolean resumed = code == HttpURLConnection.HTTP_PARTIAL;
            if (!resumed && code != HttpURLConnection.HTTP_OK) {
                throw new IOException("الخادم أعاد الرمز " + code + ".");
            }
            if (!resumed && existing > 0) {
                part.delete();
                existing = 0;
            }
            long remaining = conn.getContentLengthLong();
            long total = remaining > 0 ? existing + remaining : -1;

            long written = existing;
            try (InputStream in = conn.getInputStream();
                 FileOutputStream out = new FileOutputStream(part, existing > 0)) {
                byte[] buf = new byte[BUFFER];
                int n;
                while ((n = in.read(buf)) != -1) {
                    if (control.shouldStop()) {
                        return Outcome.STOPPED;
                    }
                    out.write(buf, 0, n);
                    written += n;
                    control.progress(written, total);
                }
            }
            if (total > 0 && part.length() != total) {
                throw new IOException("اكتمل التنزيل بحجم غير متوقع.");
            }
            byte[] head = new byte[4];
            try (InputStream in = new java.io.FileInputStream(part)) {
                int read = in.read(head);
                if (read < 3 || !Mp3Check.looksLikeMp3(java.util.Arrays.copyOf(head, read))) {
                    part.delete();
                    throw new IOException("الملف المحمّل ليس صوت MP3 صالحاً.");
                }
            }
            if (dest.exists() && !dest.delete()) {
                throw new IOException("تعذّر استبدال الملف القديم.");
            }
            if (!part.renameTo(dest)) {
                throw new IOException("تعذّر حفظ الملف النهائي.");
            }
            return Outcome.COMPLETED;
        } finally {
            conn.disconnect();
        }
    }
}
