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
import com.appdev.bilijuan.models.Product;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ProductAdapter extends RecyclerView.Adapter<ProductAdapter.VH> {

    public interface ClickListener { void onClick(Product product); }

    private List<Product> products = new ArrayList<>();
    private final boolean isHorizontal;
    private final ClickListener listener;

    public ProductAdapter(boolean isHorizontal, ClickListener listener) {
        this.isHorizontal = isHorizontal;
        this.listener      = listener;
    }

    public void setProducts(List<Product> products) {
        this.products = products;
        notifyDataSetChanged();
    }

    @NonNull @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        int layout = isHorizontal
                ? R.layout.item_listing_horizontal
                : R.layout.item_listing_grid;
        View v = LayoutInflater.from(parent.getContext()).inflate(layout, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        Product p = products.get(position);
        h.tvName.setText(p.getName());
        h.tvRating.setText(String.format(Locale.getDefault(),
                "%.1f (%d)", p.getStars(), p.getRatingCount()));

        if (isHorizontal) {
            // horizontal card uses tvPriceRange
            if (h.tvPriceRange != null)
                h.tvPriceRange.setText(String.format(Locale.getDefault(), "₱%.0f", p.getPrice()));
            if (h.tvDescription != null)
                h.tvDescription.setText(p.getCategory());
            if (h.tvStatus != null)
                h.tvStatus.setVisibility(p.isAvailable() ? View.VISIBLE : View.GONE);
        } else {
            // grid card uses tvPrice and tvSeller
            if (h.tvPrice != null)
                h.tvPrice.setText(String.format(Locale.getDefault(), "₱%.0f", p.getPrice()));
            if (h.tvSeller != null)
                h.tvSeller.setText(p.getSellerName());
        }

        if (p.getImageBase64() != null && !p.getImageBase64().isEmpty()) {
            try {
                byte[] bytes = Base64.decode(p.getImageBase64(), Base64.DEFAULT);
                Glide.with(h.itemView.getContext())
                        .asBitmap().load(bytes)
                        .diskCacheStrategy(DiskCacheStrategy.ALL)
                        .placeholder(R.color.surface_variant)
                        .into(h.ivProduct);
            } catch (Exception e) {
                h.ivProduct.setImageResource(R.color.surface_variant);
            }
        } else {
            h.ivProduct.setImageResource(R.color.surface_variant);
        }

        h.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onClick(p);
        });
    }

    @Override public int getItemCount() { return products.size(); }

    static class VH extends RecyclerView.ViewHolder {
        ImageView ivProduct;
        TextView tvName, tvRating;
        // horizontal only
        TextView tvPriceRange, tvDescription, tvStatus;
        // grid only
        TextView tvPrice, tvSeller;

        VH(@NonNull View v) {
            super(v);
            ivProduct     = v.findViewById(R.id.ivProduct);
            tvName        = v.findViewById(R.id.tvName);
            tvRating      = v.findViewById(R.id.tvRating);
            tvPriceRange  = v.findViewById(R.id.tvPriceRange);
            tvDescription = v.findViewById(R.id.tvDescription);
            tvStatus      = v.findViewById(R.id.tvStatus);
            tvPrice       = v.findViewById(R.id.tvPrice);
            tvSeller      = v.findViewById(R.id.tvSeller);
        }
    }
}