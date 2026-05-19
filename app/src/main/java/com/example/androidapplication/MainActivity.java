package com.example.androidapplication;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import java.util.List;

public class MainActivity extends AppCompatActivity implements PostAdapter.OnPostClickListener {

    private PostAdapter adapter;
    private LinearLayout selectionToolbar;
    private TextView tvSelectedCount;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Initialise SQLite repository (seeds sample data on first run)
        PostRepository.init(this);
        setContentView(R.layout.activity_main);

        RecyclerView recycler = findViewById(R.id.recycler_posts);
        recycler.setLayoutManager(new LinearLayoutManager(this));
        adapter = new PostAdapter(PostRepository.getAll(), this);
        recycler.setAdapter(adapter);

        selectionToolbar = findViewById(R.id.selection_toolbar);
        tvSelectedCount  = findViewById(R.id.tv_selected_count);

        findViewById(R.id.btn_select_all).setOnClickListener(v -> adapter.selectAll());
        findViewById(R.id.btn_cancel_selection).setOnClickListener(v -> adapter.clearSelection());
        findViewById(R.id.btn_delete_selected).setOnClickListener(v -> confirmDeleteSelected());

        // Search bar — tap to open search screen
        findViewById(R.id.search_bar).setOnClickListener(v ->
                startActivity(new Intent(this, SearchResultsActivity.class)));
        findViewById(R.id.et_search_main).setOnClickListener(v ->
                startActivity(new Intent(this, SearchResultsActivity.class)));

FloatingActionButton fab = findViewById(R.id.fab_new_post);
        fab.setOnClickListener(v -> startActivity(new Intent(this, CreatePostActivity.class)));

        BottomNavigationView bottomNav = findViewById(R.id.bottom_nav);
        bottomNav.setSelectedItemId(R.id.nav_home);
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_new_post) {
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
        // Reload from SQLite so edits/deletes from other screens are reflected
        adapter.refreshData(PostRepository.getAll());
        adapter.clearSelection();
    }

    @Override
    public void onPostClick(BlogPost post) {
        Intent intent = new Intent(this, PostDetailActivity.class);
        intent.putExtra("post", post);
        startActivity(intent);
    }

    @Override
    public void onSelectionChanged(int count) {
        if (count == 0) {
            selectionToolbar.setVisibility(View.GONE);
        } else {
            selectionToolbar.setVisibility(View.VISIBLE);
            tvSelectedCount.setText(count + " selected");
        }
    }

    private void confirmDeleteSelected() {
        new AlertDialog.Builder(this)
            .setMessage(R.string.confirm_delete)
            .setPositiveButton(R.string.yes, (d, w) -> {
                PostRepository.deleteAll(adapter.getSelectedIds());
                adapter.refreshData(PostRepository.getAll());
                adapter.clearSelection();
            })
            .setNegativeButton(R.string.cancel, null)
            .show();
    }

}
