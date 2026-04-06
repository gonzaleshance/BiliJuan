package com.appdev.bilijuan.adapters;

import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.SwitchCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.appdev.bilijuan.R;
import com.appdev.bilijuan.models.Product;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.google.android.material.button.MaterialButton;

import java.util.List;
import java.util.Locale;

public class SellerMenuAdapter extends RecyclerView.Adapter<SellerMenuAdapter.VH> {

    public interface ToggleListener { void onToggle(Product product, boolean available); }
    public interface EditListener   { void onEdit(Product product); }

    private final List<Product> items;
    private final ToggleListener toggleListener;
    private final EditListener   editListener;

    public SellerMenuAdapter(List<Product> items, ToggleListener t, EditListener e) {
        this.items          = items;
        this.toggleListener = t;
        this.editListener   = e;
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
        h.tvPrice.setText(String.format(Locale.getDefault(), "₱%.2f", p.getPrice()));
        h.tvCategory.setText(p.getCategory());

        h.switchAvailable.setOnCheckedChangeListener(null);
        h.switchAvailable.setChecked(p.isAvailable());
        h.switchAvailable.setOnCheckedChangeListener((btn, checked) ->
                toggleListener.onToggle(p, checked));

        h.btnEdit.setOnClickListener(v -> editListener.onEdit(p));

        // Optimized Image Loading with Glide
        if (p.getImageBase64() != null && !p.getImageBase64().isEmpty()) {
            try {
                byte[] imageByteArray = Base64.decode(p.getImageBase64(), Base64.DEFAULT);
                Glide.with(h.itemView.getContext())
                        .asBitmap()
                        .load(imageByteArray)
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
        ImageView ivProduct;
        TextView tvName, tvPrice, tvCategory;
        SwitchCompat switchAvailable;
        MaterialButton btnEdit;

        VH(@NonNull View v) {
            super(v);
            ivProduct       = v.findViewById(R.id.ivProduct);
            tvName          = v.findViewById(R.id.tvName);
            tvPrice         = v.findViewById(R.id.tvPrice);
            tvCategory      = v.findViewById(R.id.tvCategory);
            switchAvailable = v.findViewById(R.id.switchAvailable);
            btnEdit         = v.findViewById(R.id.btnEdit);
        }
    }
}
