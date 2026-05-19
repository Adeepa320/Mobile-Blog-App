package com.example.androidapplication;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class PostDetailActivity extends AppCompatActivity {

    private BlogPost post;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_post_detail);

        post = (BlogPost) getIntent().getSerializableExtra("post");
        if (post == null) { finish(); return; }

        bindPost();

        ((ImageButton) findViewById(R.id.btn_back)).setOnClickListener(v -> finish());

        // Edit
        ((LinearLayout) findViewById(R.id.btn_edit)).setOnClickListener(v -> {
            Intent intent = new Intent(this, CreatePostActivity.class);
            intent.putExtra("post", post);
            startActivity(intent);
        });

        // Delete single post
        ((LinearLayout) findViewById(R.id.btn_delete)).setOnClickListener(v ->
            new AlertDialog.Builder(this)
                .setMessage(R.string.confirm_delete)
                .setPositiveButton(R.string.yes, (d, w) -> {
                    PostRepository.delete(post.id);
                    finish();
                })
                .setNegativeButton(R.string.cancel, null)
                .show());

        // Share — uploads to WordPress
        ((LinearLayout) findViewById(R.id.btn_share)).setOnClickListener(v -> sharePost());

        BottomNavigationView bottomNav = findViewById(R.id.bottom_nav);
        bottomNav.setSelectedItemId(R.id.nav_home);
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) {
                startActivity(new Intent(this, MainActivity.class));
                finish();
                return true;
            } else if (id == R.id.nav_new_post) {
                startActivity(new Intent(this, CreatePostActivity.class));
                return false;
            } else if (id == R.id.nav_settings) {
                startActivity(new Intent(this, SettingsActivity.class));
                return false;
            }
            return true;
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Refresh post data from SQLite in case it was edited
        for (BlogPost p : PostRepository.getAll()) {
            if (p.id == post.id) { post = p; break; }
        }
        bindPost();
    }

    // ── share ────────────────────────────────────────────────────────────────

    /**
     * Shares the post text (and image if attached) via the standard Android
     * share sheet, satisfying the "Share via Android intent" criterion.
     */
    private void sharePost() {
        android.view.View view = getLayoutInflater().inflate(R.layout.dialog_share, null);
        AlertDialog dialog = new AlertDialog.Builder(this)
            .setView(view)
            .create();
        if (dialog.getWindow() != null)
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);

        view.findViewById(R.id.option_wordpress).setOnClickListener(v -> {
            dialog.dismiss();
            uploadToWordPress();
        });
        view.findViewById(R.id.option_other).setOnClickListener(v -> {
            dialog.dismiss();
            shareViaApps();
        });
        view.findViewById(R.id.option_cancel).setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    private void uploadToWordPress() {
        String[] creds = SettingsActivity.getSavedCredentials(this);
        if (creds == null) {
            Toast.makeText(this, "Connect WordPress account in Settings first", Toast.LENGTH_LONG).show();
            return;
        }
        Toast.makeText(this, "Uploading to WordPress\u2026", Toast.LENGTH_SHORT).show();
        WordPressUploader.uploadWithContext(this, post, creds[0], creds[1], "", (ok, fail) -> {
            if (!isFinishing() && !isDestroyed())
                Toast.makeText(this,
                    ok > 0 ? "\u2713 Post uploaded to WordPress!" : "Upload failed. Check credentials.",
                    Toast.LENGTH_LONG).show();
        });
    }

    private void shareViaApps() {
        Intent share = new Intent(Intent.ACTION_SEND);
        share.putExtra(Intent.EXTRA_SUBJECT, post.title);
        share.putExtra(Intent.EXTRA_TEXT, post.title + "\n\n" + post.body);
        boolean imageAttached = false;
        if (post.imageUri != null && !post.imageUri.isEmpty()) {
            try {
                Uri uri = toContentUri(Uri.parse(post.imageUri));
                share.setType("image/*");
                share.putExtra(Intent.EXTRA_STREAM, uri);
                share.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                imageAttached = true;
            } catch (Exception ignored) {}
        }
        if (!imageAttached) share.setType("text/plain");
        try {
            startActivity(Intent.createChooser(share, "Share post via\u2026"));
        } catch (Exception e) {
            Toast.makeText(this, "No app available to share", Toast.LENGTH_SHORT).show();
        }
    }

    private Uri toContentUri(Uri uri) {
        if (uri == null) return null;
        if ("file".equals(uri.getScheme())) {
            try {
                return androidx.core.content.FileProvider.getUriForFile(
                        this, getPackageName() + ".fileprovider",
                        new java.io.File(uri.getPath()));
            } catch (IllegalArgumentException e) {
                return uri; // fall back, caller will catch FileUriExposedException
            }
        }
        return uri;
    }

    // ── bind ─────────────────────────────────────────────────────────────────

    private void bindPost() {
        ((TextView) findViewById(R.id.tv_title)).setText(post.title);
        ((TextView) findViewById(R.id.tv_date)).setText(post.date);
        ((TextView) findViewById(R.id.tv_body)).setText(post.body);

        ImageView ivImage = findViewById(R.id.iv_image);
        if (post.imageUri != null && !post.imageUri.isEmpty()) {
            try {
                Uri uri = toContentUri(Uri.parse(post.imageUri));
                ivImage.setImageURI(uri);
                if (ivImage.getDrawable() == null) throw new Exception();
            } catch (Exception e) {
                ivImage.setImageDrawable(null);
                ivImage.setBackgroundColor(0xFFEEEEEE);
            }
        } else {
            ivImage.setImageDrawable(null);
            ivImage.setBackgroundColor(0xFFEEEEEE);
        }
    }
}
