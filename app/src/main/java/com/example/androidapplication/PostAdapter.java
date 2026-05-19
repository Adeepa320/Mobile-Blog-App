package com.example.androidapplication;

import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.BackgroundColorSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.card.MaterialCardView;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class PostAdapter extends RecyclerView.Adapter<PostAdapter.PostViewHolder> {

    public interface OnPostClickListener {
        void onPostClick(BlogPost post);
        void onSelectionChanged(int count);
    }

    private final List<BlogPost> posts;
    private final OnPostClickListener listener;
    private final Set<Integer> selectedIds = new HashSet<>();
    private boolean selectionMode = false;
    private String highlightQuery = "";

    public PostAdapter(List<BlogPost> posts, OnPostClickListener listener) {
        this.posts = new ArrayList<>(posts);
        this.listener = listener;
    }

    /** Replace the current list with fresh data from SQLite and redraw. */
    public void refreshData(List<BlogPost> newPosts) {
        posts.clear();
        posts.addAll(newPosts);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public PostViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_post_card, parent, false);
        return new PostViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull PostViewHolder h, int position) {
        BlogPost post = posts.get(position);
        h.bind(post, highlightQuery, selectedIds.contains(post.id),
                selectionMode, listener, this);
    }

    @Override
    public int getItemCount() { return posts.size(); }

    public void setHighlightQuery(String q) {
        this.highlightQuery = q == null ? "" : q;
        notifyDataSetChanged();
    }

    public void toggleSelection(int postId) {
        if (selectedIds.contains(postId)) selectedIds.remove(postId);
        else selectedIds.add(postId);
        selectionMode = !selectedIds.isEmpty();
        listener.onSelectionChanged(selectedIds.size());
        notifyDataSetChanged();
    }

    public void selectAll() {
        for (BlogPost p : posts) selectedIds.add(p.id);
        selectionMode = true;
        listener.onSelectionChanged(selectedIds.size());
        notifyDataSetChanged();
    }

    public void clearSelection() {
        selectedIds.clear();
        selectionMode = false;
        listener.onSelectionChanged(0);
        notifyDataSetChanged();
    }

    public List<Integer> getSelectedIds() { return new ArrayList<>(selectedIds); }
    public boolean isInSelectionMode()    { return selectionMode; }

    // ── ViewHolder ───────────────────────────────────────────────────────────

    static class PostViewHolder extends RecyclerView.ViewHolder {
        MaterialCardView card;
        ImageView ivThumb;
        TextView tvTitle, tvPreview, tvDate, tvStatus;

        PostViewHolder(View v) {
            super(v);
            card      = v.findViewById(R.id.card);
            ivThumb   = v.findViewById(R.id.iv_thumb);
            tvTitle   = v.findViewById(R.id.tv_title);
            tvPreview = v.findViewById(R.id.tv_preview);
            tvDate    = v.findViewById(R.id.tv_date);
            tvStatus  = v.findViewById(R.id.tv_status);
        }

        void bind(BlogPost post, String query, boolean selected, boolean selectionMode,
                  OnPostClickListener listener, PostAdapter adapter) {

            tvTitle.setText(highlight(post.title, query));
            tvPreview.setText(highlight(post.body, query));
            tvDate.setText(post.date);

            if (post.imageUri != null && !post.imageUri.isEmpty()) {
                try {
                    ivThumb.setImageURI(android.net.Uri.parse(post.imageUri));
                    if (ivThumb.getDrawable() == null) throw new Exception("null drawable");
                } catch (Exception e) {
                    ivThumb.setImageDrawable(null);
                    ivThumb.setBackgroundColor(0xFFEEEEEE);
                }
            } else {
                ivThumb.setImageDrawable(null);
                ivThumb.setBackgroundColor(0xFFEEEEEE);
            }

            if (selected) {
                card.setStrokeColor(0xFF00897B);
                card.setStrokeWidth(3);
                card.setCardBackgroundColor(0xFFE0F2F1);
            } else {
                card.setStrokeColor(0xFFE0E0E0);
                card.setStrokeWidth(1);
                card.setCardBackgroundColor(0xFFFFFFFF);
            }

            itemView.setOnClickListener(v -> {
                if (selectionMode) adapter.toggleSelection(post.id);
                else listener.onPostClick(post);
            });
            itemView.setOnLongClickListener(v -> {
                adapter.toggleSelection(post.id);
                return true;
            });
        }

        private SpannableString highlight(String text, String query) {
            SpannableString s = new SpannableString(text);
            if (query == null || query.isEmpty()) return s;
            String lower  = text.toLowerCase();
            String lowerQ = query.toLowerCase();
            int idx = 0;
            while ((idx = lower.indexOf(lowerQ, idx)) != -1) {
                s.setSpan(new BackgroundColorSpan(0xFFB2DFDB),
                        idx, idx + query.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                idx += query.length();
            }
            return s;
        }
    }
}
