package com.example.androidapplication;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.textfield.TextInputEditText;
import java.util.ArrayList;
import java.util.List;

public class SearchResultsActivity extends AppCompatActivity implements PostAdapter.OnPostClickListener {

    private PostAdapter adapter;
    private List<BlogPost> resultList = new ArrayList<>();
    private TextView tvNoResults;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search_results);

        tvNoResults = findViewById(R.id.tv_no_results);
        RecyclerView recycler = findViewById(R.id.recycler_results);
        recycler.setLayoutManager(new LinearLayoutManager(this));
        adapter = new PostAdapter(resultList, this);
        recycler.setAdapter(adapter);

        ((ImageButton) findViewById(R.id.btn_back)).setOnClickListener(v -> finish());

        TextInputEditText etSearch = findViewById(R.id.et_search);
        etSearch.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
            public void onTextChanged(CharSequence s, int st, int b, int c) { runSearch(s.toString()); }
            public void afterTextChanged(Editable s) {}
        });
        etSearch.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) runSearch(v.getText().toString());
            return false;
        });
        etSearch.requestFocus();
    }

    private void runSearch(String query) {
        resultList.clear();
        if (!query.trim().isEmpty()) resultList.addAll(PostRepository.search(query.trim()));
        adapter.setHighlightQuery(query.trim());
        tvNoResults.setVisibility(resultList.isEmpty() && !query.trim().isEmpty() ? View.VISIBLE : View.GONE);
        adapter.notifyDataSetChanged();
    }

    @Override
    public void onPostClick(BlogPost post) {
        Intent intent = new Intent(this, PostDetailActivity.class);
        intent.putExtra("post", post);
        startActivity(intent);
    }

    @Override
    public void onSelectionChanged(int count) {}
}
