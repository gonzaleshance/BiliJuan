package com.appdev.bilijuan.adapters;

import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.appdev.bilijuan.R;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;

import java.util.List;
import java.util.Locale;

public class StoreAdapter extends RecyclerView.Adapter<StoreAdapter.VH> {

    public interface ClickListener { void onClick(StoreItem store); }

    public static class StoreItem {
        public String sellerId, sellerName, bannerBase64, logoBase64, category;
        public float rating;
        public int itemCount;

        public StoreItem(String sellerId, String sellerName, String bannerBase64, String logoBase64,
                         String category, float rating, int itemCount) {
            this.sellerId     = sellerId;
            this.sellerName   = sellerName;
            this.bannerBase64 = bannerBase64;
            this.logoBase64   = logoBase64;
            this.category     = category;
            this.rating       = rating;
            this.itemCount    = itemCount;
        }
    }

    private List<StoreItem> stores;
    private final ClickListener listener;

    public StoreAdapter(List<StoreItem> stores, ClickListener listener) {
        this.stores   = stores;
        this.listener = listener;
    }

    public void setStores(List<StoreItem> stores) {
        this.stores = stores;
        notifyDataSetChanged();
    }

    @NonNull @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_listing_store, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        StoreItem s = stores.get(position);
        h.tvStoreName.setText(s.sellerName);
        h.tvCategory.setText(s.category);
        h.tvRating.setText(String.format(Locale.getDefault(), "%.1f", s.rating));
        h.tvItemCount.setText(s.itemCount + " items");

        // Load Banner
        if (s.bannerBase64 != null && !s.bannerBase64.isEmpty()) {
            try {
                byte[] bytes = Base64.decode(s.bannerBase64, Base64.DEFAULT);
                Glide.with(h.itemView.getContext())
                        .asBitmap().load(bytes)
                        .diskCacheStrategy(DiskCacheStrategy.ALL)
                        .placeholder(R.color.surface_variant)
                        .into(h.ivStoreBanner);
            } catch (Exception e) {
                h.ivStoreBanner.setImageResource(R.color.surface_variant);
            }
        }

        // Load Logo
        if (s.logoBase64 != null && !s.logoBase64.isEmpty()) {
            try {
                byte[] bytes = Base64.decode(s.logoBase64, Base64.DEFAULT);
                Glide.with(h.itemView.getContext())
                        .asBitmap().load(bytes)
                        .circleCrop()
                        .diskCacheStrategy(DiskCacheStrategy.ALL)
                        .into(h.ivStoreLogo);
            } catch (Exception e) {
                h.ivStoreLogo.setImageResource(R.drawable.ic_store);
            }
        }

        h.itemView.setOnClickListener(v -> listener.onClick(s));
    }

    @Override public int getItemCount() { return stores.size(); }

    static class VH extends RecyclerView.ViewHolder {
        ImageView ivStoreBanner, ivStoreLogo;
        TextView tvStoreName, tvCategory, tvRating, tvItemCount;

        VH(@NonNull View v) {
            super(v);
            ivStoreBanner = v.findViewById(R.id.ivStoreBanner);
            ivStoreLogo   = v.findViewById(R.id.ivStoreLogo);
            tvStoreName   = v.findViewById(R.id.tvStoreName);
            tvCategory    = v.findViewById(R.id.tvCategory);
            tvRating      = v.findViewById(R.id.tvRating);
            tvItemCount   = v.findViewById(R.id.tvItemCount);
        }
    }
}