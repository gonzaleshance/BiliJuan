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

import java.util.List;
import java.util.Locale;

public class FoodListingAdapter extends RecyclerView.Adapter<FoodListingAdapter.VH> {

    public interface ClickListener { void onClick(Product product); }

    private List<Product> products;
    private final ClickListener listener;

    public FoodListingAdapter(List<Product> products, ClickListener listener) {
        this.products = products;
        this.listener = listener;
    }

    public void setProducts(List<Product> products) {
        this.products = products;
        notifyDataSetChanged();
    }

    @NonNull @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_listing_food, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        Product p = products.get(position);
        h.tvName.setText(p.getName());
        h.tvSellerName.setText(p.getSellerName());
        h.tvPrice.setText(String.format(Locale.getDefault(), "₱%.0f", p.getPrice()));
        h.tvRating.setText(String.format(Locale.getDefault(),
                "%.1f (%d)", p.getStars(), p.getRatingCount()));

        // Clear the image view first to prevent wrong images appearing during recycling
        h.ivProduct.setImageDrawable(null);

        if (p.getImageBase64() != null && !p.getImageBase64().isEmpty()) {
            try {
                byte[] bytes = Base64.decode(p.getImageBase64(), Base64.DEFAULT);
                Glide.with(h.itemView.getContext())
                        .asBitmap().load(bytes)
                        .diskCacheStrategy(DiskCacheStrategy.ALL)
                        .placeholder(R.color.surface_variant)
                        .error(R.color.surface_variant)
                        .into(h.ivProduct);
            } catch (Exception e) {
                h.ivProduct.setImageResource(R.color.surface_variant);
            }
        } else {
            h.ivProduct.setImageResource(R.color.surface_variant);
        }

        h.itemView.setOnClickListener(v -> listener.onClick(p));
    }

    @Override public int getItemCount() { return products.size(); }

    static class VH extends RecyclerView.ViewHolder {
        ImageView ivProduct;
        TextView tvName, tvSellerName, tvPrice, tvRating;

        VH(@NonNull View v) {
            super(v);
            ivProduct    = v.findViewById(R.id.ivProduct);
            tvName       = v.findViewById(R.id.tvName);
            tvSellerName = v.findViewById(R.id.tvSellerName);
            tvPrice      = v.findViewById(R.id.tvPrice);
            tvRating     = v.findViewById(R.id.tvRating);
        }
    }
}