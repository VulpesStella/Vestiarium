package io.github.vulpesstella.vestiarium.skin;

import io.github.vulpesstella.vestiarium.Vestiarium;
import io.github.vulpesstella.vestiarium.config.VestiariumConfig;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.Charset;

/**
 * Small HTTP helper used by the skin providers. All calls are blocking and
 * must run on SkinManager's worker pool.
 */
public final class SkinHttp {
    private static final Charset UTF_8 = Charset.forName("UTF-8");
    private static final int MAX_JSON_BYTES = 2 * 1024 * 1024;

    static {
        // Java 7 defaults to TLS 1.0 in some builds. Modern skin sites require
        // TLS 1.2, and HttpsURLConnection negotiates the best version offered.
        if (System.getProperty("https.protocols") == null) {
            System.setProperty("https.protocols", "TLSv1.2,TLSv1.1,TLSv1");
        }
    }

    private SkinHttp() {
    }

    public static String getJson(String url) throws IOException {
        byte[] data = request(url, "GET", null, MAX_JSON_BYTES, true);
        return new String(data, UTF_8);
    }

    public static String postJson(String url, String body) throws IOException {
        byte[] data = request(url, "POST", body, MAX_JSON_BYTES, true);
        return new String(data, UTF_8);
    }

    public static byte[] getImage(String url, boolean allowLocal) throws IOException {
        if (!allowLocal && url.regionMatches(true, 0, "file:", 0, 5)) {
            throw new IOException("Refusing to read a local file from a remote skin source");
        }
        return request(url, "GET", null, VestiariumConfig.maxImageBytes(), allowLocal);
    }

    /**
     * Returns a response header without downloading the body. Used for the
     * authlib-injector API location discovery.
     */
    public static String getHeader(String url, String header) {
        HttpURLConnection connection = null;
        try {
            connection = open(url, true);
            connection.setRequestMethod("GET");
            connection.getResponseCode();
            return connection.getHeaderField(header);
        } catch (Throwable t) {
            Vestiarium.debug("Header lookup failed for " + url + ": " + t);
            return null;
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    public static String urlEncode(String value) {
        try {
            return URLEncoder.encode(value, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new IllegalStateException(e);
        }
    }

    public static String escapeJson(String value) {
        StringBuilder out = new StringBuilder(value.length() + 8);
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '"':
                    out.append("\\\"");
                    break;
                case '\\':
                    out.append("\\\\");
                    break;
                case '\n':
                    out.append("\\n");
                    break;
                case '\r':
                    out.append("\\r");
                    break;
                case '\t':
                    out.append("\\t");
                    break;
                default:
                    if (c < 0x20) {
                        out.append(String.format("\\u%04x", (int) c));
                    } else {
                        out.append(c);
                    }
            }
        }
        return out.toString();
    }

    private static byte[] request(String url, String method, String body, int maxBytes, boolean allowLocal)
            throws IOException {
        if (url == null || url.length() == 0) {
            throw new IOException("Empty URL");
        }
        if (url.regionMatches(true, 0, "file:", 0, 5)) {
            if (!allowLocal) {
                throw new IOException("Local file access is not allowed here");
            }
            return readLocal(url, maxBytes);
        }

        HttpURLConnection connection = open(url, false);
        try {
            connection.setRequestMethod(method);
            if ("POST".equals(method)) {
                byte[] payload = body == null ? new byte[0] : body.getBytes(UTF_8);
                connection.setDoOutput(true);
                connection.setFixedLengthStreamingMode(payload.length);
                connection.setRequestProperty("Content-Type", "application/json; charset=utf-8");
                OutputStream out = connection.getOutputStream();
                try {
                    out.write(payload);
                } finally {
                    out.close();
                }
            }

            int code = connection.getResponseCode();
            if (code == 204) {
                return new byte[0];
            }
            if (code < 200 || code >= 300) {
                throw new IOException("HTTP " + code + " for " + url);
            }

            String contentType = connection.getContentType();
            if (contentType != null) {
                String lower = contentType.toLowerCase();
                if (!allowLocal && lower.startsWith("text/") && !looksLikeJson(url)) {
                    throw new IOException("Unexpected content type " + contentType + " for " + url);
                }
            }

            InputStream in = connection.getInputStream();
            try {
                return readFully(in, maxBytes);
            } finally {
                in.close();
            }
        } finally {
            connection.disconnect();
        }
    }

    private static boolean looksLikeJson(String url) {
        return url.indexOf("/session/minecraft/profile/") >= 0
                || url.indexOf("/profiles/minecraft") >= 0
                || url.endsWith("/api/yggdrasil")
                || url.endsWith("/api/yggdrasil/");
    }

    private static HttpURLConnection open(String url, boolean headerOnly) throws IOException {
        URL parsed = new URL(url);
        String protocol = parsed.getProtocol();
        if (!"http".equalsIgnoreCase(protocol) && !"https".equalsIgnoreCase(protocol)) {
            throw new IOException("Unsupported URL protocol " + protocol);
        }
        HttpURLConnection connection = (HttpURLConnection) parsed.openConnection();
        connection.setConnectTimeout(VestiariumConfig.connectTimeoutMs());
        connection.setReadTimeout(VestiariumConfig.readTimeoutMs());
        connection.setInstanceFollowRedirects(true);
        connection.setRequestProperty("User-Agent", "Vestiarium/" + Vestiarium.VERSION + " (Minecraft 1.6.4)");
        connection.setRequestProperty("Accept", headerOnly ? "*/*" : "application/json, image/png, image/*;q=0.8, */*;q=0.1");
        return connection;
    }

    private static byte[] readLocal(String url, int maxBytes) throws IOException {
        URL parsed = new URL(url);
        if (!"file".equalsIgnoreCase(parsed.getProtocol())) {
            throw new IOException("Not a file URL: " + url);
        }
        InputStream in = parsed.openStream();
        try {
            return readFully(in, maxBytes);
        } finally {
            in.close();
        }
    }

    private static byte[] readFully(InputStream in, int maxBytes) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream(8192);
        byte[] buffer = new byte[8192];
        int total = 0;
        int read;
        while ((read = in.read(buffer)) != -1) {
            total += read;
            if (total > maxBytes) {
                throw new IOException("Response is larger than " + maxBytes + " bytes");
            }
            out.write(buffer, 0, read);
        }
        return out.toByteArray();
    }

}
