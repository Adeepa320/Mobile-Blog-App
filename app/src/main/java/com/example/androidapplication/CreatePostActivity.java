package com.example.androidapplication;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class CreatePostActivity extends AppCompatActivity {

    private EditText etTitle, etBody;
    private ImageView ivPreview;
    private String imageUri = null;
    private Uri cameraUri;          // FileProvider URI for the current camera shot
    private BlogPost editPost = null;

    private final ActivityResultLauncher<String> permissionLauncher =
        registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
            if (granted) openGallery();
            else Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show();
        });

    // Gallery picker
    private final ActivityResultLauncher<Intent> galleryLauncher =
        registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                Uri uri = result.getData().getData();
                if (uri != null) {
                    try {
                        getContentResolver().takePersistableUriPermission(
                                uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    } catch (SecurityException ignored) {}
                    // Copy to app storage so URI stays valid after app restart
                    Uri localUri = copyImageToAppStorage(uri);
                    if (localUri != null) {
                        imageUri = localUri.toString();
                        showPreview(localUri);
                    } else {
                        imageUri = uri.toString();
                        showPreview(uri);
                    }
                }
            }
        });

    // Camera — uses FileProvider so we get the full-resolution image URI
    private final ActivityResultLauncher<Intent> cameraLauncher =
        registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getResultCode() == RESULT_OK && cameraUri != null) {
                // cameraUri is already a content:// FileProvider URI — store it directly
                imageUri = cameraUri.toString();
                showPreview(cameraUri);
            }
        });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_post);

        etTitle  = findViewById(R.id.et_title);
        etBody   = findViewById(R.id.et_body);
        ivPreview = findViewById(R.id.iv_preview);
        TextView tvScreenTitle = findViewById(R.id.tv_screen_title);

        editPost = (BlogPost) getIntent().getSerializableExtra("post");
        if (editPost != null) {
            tvScreenTitle.setText("Edit Post");
            etTitle.setText(editPost.title);
            etBody.setText(editPost.body);
            if (editPost.imageUri != null) {
                imageUri = editPost.imageUri;
                showPreview(Uri.parse(imageUri));
            }
        }

        ((ImageButton) findViewById(R.id.btn_back)).setOnClickListener(v -> finish());

        ((LinearLayout) findViewById(R.id.btn_gallery)).setOnClickListener(v -> checkAndOpenGallery());

        ((LinearLayout) findViewById(R.id.btn_camera)).setOnClickListener(v -> launchCamera());

        ((LinearLayout) findViewById(R.id.btn_save)).setOnClickListener(v -> savePost());

        BottomNavigationView bottomNav = findViewById(R.id.bottom_nav);
        bottomNav.setSelectedItemId(R.id.nav_new_post);
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) {
                startActivity(new Intent(this, MainActivity.class));
                finish();
                return true;
            } else if (id == R.id.nav_settings) {
                startActivity(new Intent(this, SettingsActivity.class));
                return false;
            }
            return true;
        });
    }

    // ── gallery ──────────────────────────────────────────────────────────────

    private void checkAndOpenGallery() {
        String permission = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                ? Manifest.permission.READ_MEDIA_IMAGES
                : Manifest.permission.READ_EXTERNAL_STORAGE;
        if (ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED) {
            openGallery();
        } else {
            permissionLauncher.launch(permission);
        }
    }

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        galleryLauncher.launch(intent);
    }

    // ── camera ───────────────────────────────────────────────────────────────

    private void launchCamera() {
        File photoFile;
        try {
            photoFile = createImageFile();
        } catch (IOException e) {
            Toast.makeText(this, "Cannot create image file", Toast.LENGTH_SHORT).show();
            return;
        }
        cameraUri = FileProvider.getUriForFile(this,
                getPackageName() + ".fileprovider", photoFile);
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, cameraUri);
        cameraLauncher.launch(intent);
    }

    /** Creates a uniquely-named JPEG file in the app's external pictures directory. */
    private File createImageFile() throws IOException {
        String stamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        File dir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        return File.createTempFile("IMG_" + stamp + "_", ".jpg", dir);
    }

    // ── save ─────────────────────────────────────────────────────────────────

    private void savePost() {
        String title = etTitle.getText().toString().trim();
        String body  = etBody.getText().toString().trim();
        if (title.isEmpty()) { etTitle.setError("Title required"); return; }
        if (body.isEmpty())  { etBody.setError("Content required"); return; }

        if (editPost != null) {
            editPost.title    = title;
            editPost.body     = body;
            editPost.imageUri = imageUri;
            PostRepository.update(editPost);
        } else {
            PostRepository.add(new BlogPost(0, title, body, imageUri, PostRepository.today()));
        }
        Toast.makeText(this, "Post saved", Toast.LENGTH_SHORT).show();
        finish();
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private void showPreview(Uri uri) {
        ivPreview.setImageURI(uri);
        ivPreview.setVisibility(android.view.View.VISIBLE);
    }

    /** Copies an external URI into app-private Pictures dir and returns a file:// URI. */
    private Uri copyImageToAppStorage(Uri source) {
        try {
            InputStream in = getContentResolver().openInputStream(source);
            if (in == null) return null;
            File dir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
            String name = "IMG_" + new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date()) + ".jpg";
            File dest = new File(dir, name);
            FileOutputStream out = new FileOutputStream(dest);
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) != -1) out.write(buf, 0, n);
            in.close();
            out.close();
            return androidx.core.content.FileProvider.getUriForFile(
                    this, getPackageName() + ".fileprovider", dest);
        } catch (IOException e) {
            return null;
        }
    }
}
