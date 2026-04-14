package com.appdev.bilijuan.adapters;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.appdev.bilijuan.R;
import com.appdev.bilijuan.models.User;
import com.google.android.material.imageview.ShapeableImageView;

import java.util.List;

public class StoreCircleAdapter extends RecyclerView.Adapter<StoreCircleAdapter.VH> {

    public interface OnStoreClickListener {
        void onStoreClick(User store);
    }

    private final List<User> stores;
    private final OnStoreClickListener listener;

    public StoreCircleAdapter(List<User> stores, OnStoreClickListener listener) {
        this.stores = stores;
        this.listener = listener;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_store_circle, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        User store = stores.get(position);
        holder.tvName.setText(store.getName());

        if (store.getStoreImageBase64() != null && !store.getStoreImageBase64().isEmpty()) {
            try {
                byte[] bytes = Base64.decode(store.getStoreImageBase64(), Base64.DEFAULT);
                Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                holder.ivStore.setImageBitmap(bitmap);
            } catch (Exception e) {
                holder.ivStore.setImageResource(R.drawable.ic_person);
            }
        } else {
            holder.ivStore.setImageResource(R.drawable.ic_person);
        }

        holder.itemView.setOnClickListener(v -> listener.onStoreClick(store));
    }

    @Override
    public int getItemCount() {
        return stores.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        ShapeableImageView ivStore;
        TextView tvName;

        VH(@NonNull View v) {
            super(v);
            ivStore = v.findViewById(R.id.ivStore);
            tvName = v.findViewById(R.id.tvStoreName);
        }
    }
}