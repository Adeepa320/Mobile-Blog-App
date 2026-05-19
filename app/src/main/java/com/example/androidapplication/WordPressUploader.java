package com.example.androidapplication;

import android.content.Context;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import org.json.JSONObject;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * Uploads posts to WordPress.com using the public REST API v1.1.
 * Works on FREE WordPress.com plans.
 *
 * Endpoint: https://public-api.wordpress.com/rest/v1.1/sites/<site>/posts/new
 */
public class WordPressUploader {

    public interface Callback {
        void onDone(int succeeded, int failed);
    }

    private static final OkHttpClient CLIENT = new OkHttpClient();
    private static final ExecutorService EXEC = Executors.newSingleThreadExecutor();
    private static final Handler MAIN = new Handler(Looper.getMainLooper());

    public static void upload(BlogPost post, String siteUrl,
                              String username, String password, Callback cb) {
        EXEC.execute(() -> {
            boolean ok = doUpload(null, post, siteUrl, username, password);
            MAIN.post(() -> cb.onDone(ok ? 1 : 0, ok ? 0 : 1));
        });
    }

    public static void uploadWithContext(Context ctx, BlogPost post, String siteUrl,
                                         String username, String password, Callback cb) {
        EXEC.execute(() -> {
            boolean ok = doUpload(ctx, post, siteUrl, username, password);
            MAIN.post(() -> cb.onDone(ok ? 1 : 0, ok ? 0 : 1));
        });
    }

    public static void uploadAll(List<BlogPost> posts, String siteUrl,
                                 String username, String password, Callback cb) {
        EXEC.execute(() -> {
            int ok = 0, fail = 0;
            for (BlogPost p : posts) {
                if (doUpload(null, p, siteUrl, username, password)) ok++; else fail++;
            }
            final int s = ok, f = fail;
            MAIN.post(() -> cb.onDone(s, f));
        });
    }

    // ── internal ─────────────────────────────────────────────────────────────

    private static boolean doUpload(Context ctx, BlogPost post,
                                    String siteUrl, String username, String password) {
        // username param is now the OAuth token
        String site  = siteUrl.replaceAll("https?://", "").replaceAll("/$", "");
        String token = username; // getSavedCredentials returns [url, token]
        String apiBase = "https://public-api.wordpress.com/rest/v1.1/sites/" + site;

        try {
            // 1. Upload image if present
            int mediaId = 0;
            if (ctx != null && post.imageUri != null && !post.imageUri.isEmpty()) {
                mediaId = uploadMedia(ctx, Uri.parse(post.imageUri), apiBase, "Bearer " + token);
            }

            // 2. Create post
            JSONObject body = new JSONObject();
            body.put("title", post.title);
            body.put("content", post.body);
            body.put("status", "publish");
            if (mediaId > 0) body.put("featured_image", mediaId);

            Request req = new Request.Builder()
                    .url(apiBase + "/posts/new")
                    .header("Authorization", "Bearer " + token)
                    .post(RequestBody.create(body.toString(),
                            MediaType.parse("application/json; charset=utf-8")))
                    .build();

            try (Response resp = CLIENT.newCall(req).execute()) {
                String respBody = resp.body() != null ? resp.body().string() : "";
                android.util.Log.d("WPUpload", "HTTP " + resp.code() + " | " + respBody);
                return resp.isSuccessful() && !respBody.contains("\"error\"");
            }
        } catch (Exception e) {
            android.util.Log.e("WPUpload", "Upload error: " + e.getMessage());
            return false;
        }
    }

    private static int uploadMedia(Context ctx, Uri uri, String apiBase, String cred) {
        try {
            InputStream is = ctx.getContentResolver().openInputStream(uri);
            if (is == null) return 0;
            byte[] bytes = readAllBytes(is);
            is.close();

            String mimeType = ctx.getContentResolver().getType(uri);
            if (mimeType == null) mimeType = "image/jpeg";
            String filename = "photo_" + System.currentTimeMillis() + ".jpg";

            RequestBody fileBody = RequestBody.create(bytes, MediaType.parse(mimeType));
            RequestBody multipart = new MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("media[]", filename, fileBody)
                    .build();

            Request req = new Request.Builder()
                    .url(apiBase + "/media/new")
                    .header("Authorization", cred)
                    .post(multipart)
                    .build();

            try (Response resp = CLIENT.newCall(req).execute()) {
                if (!resp.isSuccessful() || resp.body() == null) return 0;
                String respBody = resp.body().string();
                android.util.Log.d("WPUpload", "Media HTTP " + resp.code() + " | " + respBody);
                JSONObject json = new JSONObject(respBody);
                // Response: {"media": [{"ID": 123, ...}]}
                if (json.has("media")) {
                    return json.getJSONArray("media").getJSONObject(0).optInt("ID", 0);
                }
                return 0;
            }
        } catch (Exception e) {
            android.util.Log.e("WPUpload", "Media upload error: " + e.getMessage());
            return 0;
        }
    }

    private static byte[] readAllBytes(InputStream is) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        byte[] chunk = new byte[8192];
        int n;
        while ((n = is.read(chunk)) != -1) buffer.write(chunk, 0, n);
        return buffer.toByteArray();
    }
}
