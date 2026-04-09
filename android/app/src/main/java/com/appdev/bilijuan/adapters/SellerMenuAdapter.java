package com.appdev.bilijuan.adapters;

import android.graphics.Color;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.SwitchCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.appdev.bilijuan.R;
import com.appdev.bilijuan.models.Product;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.google.android.material.imageview.ShapeableImageView;

import java.util.List;
import java.util.Locale;

public class SellerMenuAdapter extends RecyclerView.Adapter<SellerMenuAdapter.VH> {

    public interface ToggleListener { void onToggle(Product product, boolean available); }
    public interface EditListener   { void onEdit(Product product); }
    public interface DeleteListener { void onDelete(Product product); }

    private final List<Product> items;
    private final ToggleListener toggleListener;
    private final EditListener   editListener;
    private final DeleteListener deleteListener;

    public SellerMenuAdapter(List<Product> items,
                             ToggleListener t, EditListener e, DeleteListener d) {
        this.items          = items;
        this.toggleListener = t;
        this.editListener   = e;
        this.deleteListener = d;
    }

    @NonNull @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_seller_menu, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        Product p = items.get(position);

        h.tvName.setText(p.getName());
        h.tvPrice.setText(String.format(Locale.getDefault(), "₱%.0f", p.getPrice()));
        h.tvCategory.setText(p.getCategory());

        // Availability badge
        if (p.isAvailable()) {
            h.tvAvailabilityBadge.setText("Active");
            h.tvAvailabilityBadge.setTextColor(
                    h.itemView.getContext().getColor(R.color.primary));
            h.tvAvailabilityBadge.setBackgroundResource(R.drawable.bg_badge_active);
        } else {
            h.tvAvailabilityBadge.setText("Out of Stock");
            h.tvAvailabilityBadge.setTextColor(
                    h.itemView.getContext().getColor(R.color.error));
            h.tvAvailabilityBadge.setBackgroundResource(R.drawable.bg_badge_inactive);
        }

        // Buttons
        h.btnView.setOnClickListener(v -> editListener.onEdit(p)); // View = open detail
        h.btnEdit.setOnClickListener(v -> editListener.onEdit(p));
        h.btnDelete.setOnClickListener(v -> deleteListener.onDelete(p));

        // Image
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
    }

    @Override public int getItemCount() { return items.size(); }

    static class VH extends RecyclerView.ViewHolder {
        ShapeableImageView ivProduct;
        TextView tvName, tvPrice, tvCategory, tvAvailabilityBadge;
        View btnView, btnEdit, btnDelete;

        VH(@NonNull View v) {
            super(v);
            ivProduct           = v.findViewById(R.id.ivProduct);
            tvName              = v.findViewById(R.id.tvName);
            tvPrice             = v.findViewById(R.id.tvPrice);
            tvCategory          = v.findViewById(R.id.tvCategory);
            tvAvailabilityBadge = v.findViewById(R.id.tvAvailabilityBadge);
            btnView             = v.findViewById(R.id.btnView);
            btnEdit             = v.findViewById(R.id.btnEdit);
            btnDelete           = v.findViewById(R.id.btnDelete);
        }
    }
}