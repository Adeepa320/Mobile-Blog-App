package com.example.androidapplication;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class SettingsActivity extends AppCompatActivity {

    private static final String PREFS         = "wp_prefs";
    private static final String KEY_URL       = "wp_url";
    private static final String KEY_CLIENT_ID = "wp_client_id";
    private static final String KEY_SECRET    = "wp_client_secret";
    private static final String KEY_TOKEN     = "wp_token";

    // Redirect URI registered in developer.wordpress.com
    private static final String REDIRECT_URI  = "https://localhost";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        refreshPostCount();

        ((LinearLayout) findViewById(R.id.btn_upload)).setOnClickListener(v -> showCredentialDialog());

        BottomNavigationView bottomNav = findViewById(R.id.bottom_nav);
        bottomNav.setSelectedItemId(R.id.nav_settings);
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) {
                startActivity(new Intent(this, MainActivity.class));
                finish();
                return true;
            } else if (id == R.id.nav_new_post) {
                startActivity(new Intent(this, CreatePostActivity.class));
                return false;
            }
            return true;
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshPostCount();
        updateConnectionStatus();

        // Handle OAuth redirect — check if we came back from browser with a code
        Uri data = getIntent().getData();
        if (data != null && data.toString().startsWith(REDIRECT_URI)) {
            String code = data.getQueryParameter("code");
            if (code != null) exchangeCodeForToken(code);
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
    }

    // ── credential dialog ────────────────────────────────────────────────────

    private void showCredentialDialog() {
        View view = getLayoutInflater().inflate(R.layout.dialog_wp_credentials, null);
        EditText etUrl      = view.findViewById(R.id.et_wp_url);
        EditText etClientId = view.findViewById(R.id.et_client_id);
        EditText etSecret   = view.findViewById(R.id.et_client_secret);
        TextView tvStatus   = view.findViewById(R.id.tv_oauth_status);
        Button   btnLogin   = view.findViewById(R.id.btn_oauth_login);

        SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        etUrl.setText(prefs.getString(KEY_URL, ""));
        etClientId.setText(prefs.getString(KEY_CLIENT_ID, ""));

        String token = prefs.getString(KEY_TOKEN, "");
        tvStatus.setText(token.isEmpty() ? "Not connected" : "✓ Connected");

        AlertDialog dialog = new AlertDialog.Builder(this)
            .setTitle("WordPress.com Connection")
            .setView(view)
            .setPositiveButton("Save", (d, w) -> {
                String url      = etUrl.getText().toString().trim();
                String clientId = etClientId.getText().toString().trim();
                String secret   = etSecret.getText().toString().trim();
                if (url.isEmpty() || clientId.isEmpty()) {
                    Toast.makeText(this, "URL and Client ID required", Toast.LENGTH_SHORT).show();
                    return;
                }
                prefs.edit()
                    .putString(KEY_URL, url)
                    .putString(KEY_CLIENT_ID, clientId)
                    .putString(KEY_SECRET, secret)
                    .apply();
                Toast.makeText(this, "Saved. Now tap Login to authorize.", Toast.LENGTH_LONG).show();
                updateConnectionStatus();
            })
            .setNegativeButton("Cancel", null)
            .create();

        btnLogin.setOnClickListener(v -> {
            String clientId = etClientId.getText().toString().trim();
            String url      = etUrl.getText().toString().trim();
            if (clientId.isEmpty() || url.isEmpty()) {
                Toast.makeText(this, "Enter URL and Client ID first", Toast.LENGTH_SHORT).show();
                return;
            }
            // Save before opening browser
            prefs.edit()
                .putString(KEY_URL, url)
                .putString(KEY_CLIENT_ID, clientId)
                .putString(KEY_SECRET, etSecret.getText().toString().trim())
                .apply();
            dialog.dismiss();
            openOAuthBrowser(clientId);
        });

        dialog.show();
    }

    private void openOAuthBrowser(String clientId) {
        String authUrl = "https://public-api.wordpress.com/oauth2/authorize"
                + "?client_id=" + clientId
                + "&redirect_uri=" + Uri.encode(REDIRECT_URI)
                + "&response_type=code"
                + "&scope=global";
        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(authUrl)));
        Toast.makeText(this, "After login, copy the 'code' from the URL and paste it below", Toast.LENGTH_LONG).show();

        // Show manual code entry dialog since redirect to localhost won't auto-return
        showCodeEntryDialog();
    }

    private void showCodeEntryDialog() {
        EditText etCode = new EditText(this);
        etCode.setHint("Paste the 'code' from the redirect URL");

        new AlertDialog.Builder(this)
            .setTitle("Enter Authorization Code")
            .setMessage("After login, copy the 'code=XXXXX' value from the browser URL bar and paste it here.")
            .setView(etCode)
            .setPositiveButton("Submit", (d, w) -> {
                String code = etCode.getText().toString().trim();
                if (!code.isEmpty()) exchangeCodeForToken(code);
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    private void exchangeCodeForToken(String code) {
        SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        String clientId = prefs.getString(KEY_CLIENT_ID, "");
        String secret   = prefs.getString(KEY_SECRET, "");

        new Thread(() -> {
            try {
                okhttp3.OkHttpClient client = new okhttp3.OkHttpClient();
                okhttp3.RequestBody body = new okhttp3.FormBody.Builder()
                        .add("client_id", clientId)
                        .add("redirect_uri", REDIRECT_URI)
                        .add("client_secret", secret)
                        .add("code", code)
                        .add("grant_type", "authorization_code")
                        .build();
                okhttp3.Request req = new okhttp3.Request.Builder()
                        .url("https://public-api.wordpress.com/oauth2/token")
                        .post(body)
                        .build();
                try (okhttp3.Response resp = client.newCall(req).execute()) {
                    String respBody = resp.body() != null ? resp.body().string() : "";
                    org.json.JSONObject json = new org.json.JSONObject(respBody);
                    if (json.has("access_token")) {
                        String token = json.getString("access_token");
                        prefs.edit().putString(KEY_TOKEN, token).apply();
                        runOnUiThread(() -> {
                            updateConnectionStatus();
                            Toast.makeText(this, "✓ Connected to WordPress.com!", Toast.LENGTH_LONG).show();
                        });
                    } else {
                        runOnUiThread(() -> Toast.makeText(this,
                                "Auth failed: " + respBody, Toast.LENGTH_LONG).show());
                    }
                }
            } catch (Exception e) {
                runOnUiThread(() -> Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show());
            }
        }).start();
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private void refreshPostCount() {
        TextView tvCount = findViewById(R.id.tv_post_count);
        if (tvCount != null) tvCount.setText(String.valueOf(PostRepository.getAll().size()));
    }

    private void updateConnectionStatus() {
        TextView tvStatus = findViewById(R.id.tv_connection_status);
        if (tvStatus == null) return;
        SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        String token = prefs.getString(KEY_TOKEN, "");
        String url   = prefs.getString(KEY_URL, "");
        tvStatus.setText(token.isEmpty() ? "Not Connected" : "Connected: " + url);
    }

    /** Returns [siteUrl, token] if connected, or null. */
    public static String[] getSavedCredentials(Context ctx) {
        SharedPreferences prefs = ctx.getSharedPreferences(PREFS, MODE_PRIVATE);
        String url   = prefs.getString(KEY_URL, "");
        String token = prefs.getString(KEY_TOKEN, "");
        if (url.isEmpty() || token.isEmpty()) return null;
        return new String[]{url, token};
    }
}
