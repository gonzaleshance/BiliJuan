package com.appdev.bilijuan.adapters;

import android.graphics.Bitmap;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.appdev.bilijuan.R;
import com.appdev.bilijuan.models.CartItem;
import com.appdev.bilijuan.utils.ImageHelper;
import com.google.android.material.button.MaterialButton;

import java.util.List;

public class CartAdapter extends RecyclerView.Adapter<CartAdapter.VH> {

    public interface OnCartActionListener {
        void onQuantityChanged(int position, int newQuantity);
        void onRemoveItem(int position);
    }

    private final List<CartItem> items;
    private final OnCartActionListener listener;

    public CartAdapter(List<CartItem> items, OnCartActionListener listener) {
        this.items = items;
        this.listener = listener;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_cart, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        CartItem item = items.get(position);

        holder.tvName.setText(item.getProductName());
        holder.tvPrice.setText(String.format("₱%.0f", item.getPrice()));
        holder.tvQuantity.setText(String.valueOf(item.getQuantity()));

        if (item.getImageBase64() != null && !item.getImageBase64().isEmpty()) {
            Bitmap bm = ImageHelper.base64ToBitmap(item.getImageBase64());
            if (bm != null) holder.ivProduct.setImageBitmap(bm);
        }

        holder.btnMinus.setOnClickListener(v -> {
            if (item.getQuantity() > 1) {
                listener.onQuantityChanged(position, item.getQuantity() - 1);
            }
        });

        holder.btnPlus.setOnClickListener(v -> {
            listener.onQuantityChanged(position, item.getQuantity() + 1);
        });

        holder.btnRemove.setOnClickListener(v -> {
            listener.onRemoveItem(position);
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        ImageView ivProduct;
        TextView tvName, tvPrice, tvQuantity;
        MaterialButton btnMinus, btnPlus;
        ImageButton btnRemove;

        VH(@NonNull View v) {
            super(v);
            ivProduct = v.findViewById(R.id.ivCartProduct);
            tvName = v.findViewById(R.id.tvCartProductName);
            tvPrice = v.findViewById(R.id.tvCartProductPrice);
            tvQuantity = v.findViewById(R.id.tvCartQuantity);
            btnMinus = v.findViewById(R.id.btnCartMinus);
            btnPlus = v.findViewById(R.id.btnCartPlus);
            btnRemove = v.findViewById(R.id.btnRemoveItem);
        }
    }
}
