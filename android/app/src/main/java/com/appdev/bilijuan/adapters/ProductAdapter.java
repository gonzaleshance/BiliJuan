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

public class ProductAdapter extends RecyclerView.Adapter<ProductAdapter.ProductViewHolder> {

    private List<Product> products = new ArrayList<>();
    private final boolean isHorizontal;
    private OnProductClickListener listener;

    public interface OnProductClickListener {
        void onProductClick(Product product);
    }

    public ProductAdapter(boolean isHorizontal) {
        this.isHorizontal = isHorizontal;
    }

    public void setOnProductClickListener(OnProductClickListener listener) {
        this.listener = listener;
    }

    public void setProducts(List<Product> products) {
        this.products = products;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ProductViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        int layoutRes = isHorizontal ? R.layout.item_listing_horizontal : R.layout.item_listing_grid;
        View view = LayoutInflater.from(parent.getContext()).inflate(layoutRes, parent, false);
        return new ProductViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ProductViewHolder holder, int position) {
        holder.bind(products.get(position));
    }

    @Override
    public int getItemCount() {
        return products.size();
    }

    class ProductViewHolder extends RecyclerView.ViewHolder {
        ImageView ivProduct;
        TextView tvName, tvPrice, tvRating, tvSeller, tvDescription, tvStatus;

        ProductViewHolder(@NonNull View itemView) {
            super(itemView);
            ivProduct = itemView.findViewById(R.id.ivProduct);
            tvName = itemView.findViewById(R.id.tvName);
            tvPrice = itemView.findViewById(R.id.tvPrice);
            tvRating = itemView.findViewById(R.id.tvRating);
            
            if (isHorizontal) {
                tvDescription = itemView.findViewById(R.id.tvDescription);
                tvStatus = itemView.findViewById(R.id.tvStatus);
            } else {
                tvSeller = itemView.findViewById(R.id.tvSeller);
            }

            itemView.setOnClickListener(v -> {
                int pos = getAdapterPosition();
                if (listener != null && pos != RecyclerView.NO_POSITION) {
                    listener.onProductClick(products.get(pos));
                }
            });
        }

        void bind(Product product) {
            tvName.setText(product.getName());
            tvPrice.setText(String.format(Locale.getDefault(), "₱%.2f", product.getPrice()));
            tvRating.setText(String.format(Locale.getDefault(), "%.1f (%d)", product.getStars(), product.getRatingCount()));

            if (isHorizontal) {
                if (tvDescription != null) tvDescription.setText(product.getCategory());
                if (tvStatus != null) {
                    tvStatus.setVisibility(product.isAvailable() ? View.VISIBLE : View.GONE);
                }
            } else {
                if (tvSeller != null) tvSeller.setText(product.getSellerName());
            }

            // Performance: Load image efficiently
            if (product.getImageBase64() != null && !product.getImageBase64().isEmpty()) {
                try {
                    byte[] imageByteArray = Base64.decode(product.getImageBase64(), Base64.DEFAULT);
                    Glide.with(itemView.getContext())
                            .asBitmap()
                            .load(imageByteArray)
                            .diskCacheStrategy(DiskCacheStrategy.ALL)
                            .placeholder(R.color.surface_variant)
                            .into(ivProduct);
                } catch (Exception e) {
                    ivProduct.setImageResource(R.color.surface_variant);
                }
            } else {
                ivProduct.setImageResource(R.color.surface_variant);
            }
        }
    }
}
