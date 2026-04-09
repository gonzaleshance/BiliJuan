package com.appdev.bilijuan.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.appdev.bilijuan.R;
import com.appdev.bilijuan.models.Comment;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class CommentAdapter extends RecyclerView.Adapter<CommentAdapter.VH> {

    private final List<Comment> comments;
    private static final SimpleDateFormat SDF =
            new SimpleDateFormat("MMM d, yyyy", Locale.getDefault());

    public CommentAdapter(List<Comment> comments) { this.comments = comments; }

    @NonNull @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_comment, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        Comment c = comments.get(position);
        h.tvUserName.setText(c.getUserName());
        h.tvText.setText(c.getText());
        h.tvDate.setText(c.getTimestamp() != null ? SDF.format(c.getTimestamp()) : "");
        // Star display — fill stars up to rating
        ImageView[] stars = {h.star1, h.star2, h.star3, h.star4, h.star5};
        for (int i = 0; i < 5; i++) {
            stars[i].setAlpha(i < c.getStars() ? 1.0f : 0.2f);
        }
    }

    @Override public int getItemCount() { return comments.size(); }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvUserName, tvText, tvDate;
        ImageView star1, star2, star3, star4, star5;

        VH(@NonNull View v) {
            super(v);
            tvUserName = v.findViewById(R.id.tvUserName);
            tvText     = v.findViewById(R.id.tvText);
            tvDate     = v.findViewById(R.id.tvDate);
            star1      = v.findViewById(R.id.star1);
            star2      = v.findViewById(R.id.star2);
            star3      = v.findViewById(R.id.star3);
            star4      = v.findViewById(R.id.star4);
            star5      = v.findViewById(R.id.star5);
        }
    }
}