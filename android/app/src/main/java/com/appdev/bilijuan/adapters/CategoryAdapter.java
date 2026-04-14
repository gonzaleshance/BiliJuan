package com.appdev.bilijuan.adapters;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.appdev.bilijuan.R;

import java.util.List;

public class CategoryAdapter extends RecyclerView.Adapter<CategoryAdapter.VH> {

    public interface ClickListener { void onClick(CategoryItem item); }

    public static class CategoryItem {
        public String name;
        public String emoji;
        public String subtitle;
        public String bgColor;

        public CategoryItem(String name, String emoji, String subtitle, String bgColor) {
            this.name = name;
            this.emoji = emoji;
            this.subtitle = subtitle;
            this.bgColor = bgColor;
        }
    }

    private final List<CategoryItem> items;
    private final ClickListener listener;

    public CategoryAdapter(List<CategoryItem> items, ClickListener listener) {
        this.items = items;
        this.listener = listener;
    }

    @NonNull @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_category_card, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        CategoryItem item = items.get(position);
        h.tvName.setText(item.name);
        h.tvEmoji.setText(item.emoji);
        h.tvSubtitle.setText(item.subtitle);
        h.itemView.setOnClickListener(v -> listener.onClick(item));
    }

    @Override public int getItemCount() { return items.size(); }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvName, tvEmoji, tvSubtitle;

        VH(@NonNull View v) {
            super(v);
            tvEmoji = v.findViewById(R.id.tvEmoji);
            tvName = v.findViewById(R.id.tvName);
            tvSubtitle = v.findViewById(R.id.tvSubtitle);
        }
    }
}