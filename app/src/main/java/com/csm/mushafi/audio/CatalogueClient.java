package com.csm.mushafi.audio;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/** جلب الفهرس عبر HTTPS مع مهلة ومحاولتين. الفشل يُعرض للمستخدم دون تعطيل بقية التطبيق. */
public final class CatalogueClient {

    public static final String RECITERS_URL = "https://mp3quran.net/api/v3/reciters?language=ar";
    private static final int TIMEOUT_MS = 15000;
    private static final int ATTEMPTS = 2;
    private static final int MAX_BYTES = 4 * 1024 * 1024;

    public static final class NetworkException extends Exception {
        public NetworkException(String message) {
            super(message);
        }
    }

    private CatalogueClient() {
    }

    public static String fetch() throws NetworkException {
        NetworkException last = null;
        for (int attempt = 0; attempt < ATTEMPTS; attempt++) {
            try {
                return get(RECITERS_URL);
            } catch (NetworkException e) {
                last = e;
                try {
                    Thread.sleep(800L * (attempt + 1));
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
        throw last != null ? last : new NetworkException("تعذّر تحميل الفهرس.");
    }

    private static String get(String url) throws NetworkException {
        HttpURLConnection conn = null;
        try {
            conn = (HttpURLConnection) new URL(url).openConnection();
            conn.setConnectTimeout(TIMEOUT_MS);
            conn.setReadTimeout(TIMEOUT_MS);
            conn.setInstanceFollowRedirects(true);
            int code = conn.getResponseCode();
            if (code != HttpURLConnection.HTTP_OK) {
                throw new NetworkException("الخادم أعاد الرمز " + code + ".");
            }
            try (InputStream in = conn.getInputStream()) {
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                byte[] buf = new byte[8192];
                int n;
                while ((n = in.read(buf)) != -1) {
                    out.write(buf, 0, n);
                    if (out.size() > MAX_BYTES) {
                        throw new NetworkException("حجم الفهرس أكبر من المتوقع.");
                    }
                }
                return new String(out.toByteArray(), "UTF-8");
            }
        } catch (IOException e) {
            throw new NetworkException("تعذّر الوصول إلى الفهرس. تحقق من الاتصال بالإنترنت.");
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }
}
