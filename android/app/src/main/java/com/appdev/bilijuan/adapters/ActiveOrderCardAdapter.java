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
import com.appdev.bilijuan.models.Order;
import com.appdev.bilijuan.utils.DeliveryUtils;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.google.android.material.button.MaterialButton;

import java.util.List;
import java.util.Locale;

public class ActiveOrderCardAdapter extends RecyclerView.Adapter<ActiveOrderCardAdapter.VH> {

    public interface ActionListener { void onAction(Order order); }

    private final List<Order> orders;
    private final ActionListener listener;

    public ActiveOrderCardAdapter(List<Order> orders, ActionListener listener) {
        this.orders   = orders;
        this.listener = listener;
    }

    @NonNull @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_active_order_card, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        Order o = orders.get(position);

        h.tvCustomerName.setText(o.getCustomerName());
        h.tvProductName.setText(String.format(Locale.getDefault(), "%s × %d", o.getProductName(), o.getQuantity()));
        h.tvTotal.setText(String.format(Locale.getDefault(), "₱%.2f", o.getTotalAmount()));
        h.tvPayment.setText(o.getPaymentMethod());

        double dist = DeliveryUtils.haversineKm(
                o.getSellerLat(), o.getSellerLng(),
                o.getCustomerLat(), o.getCustomerLng());
        h.tvDistance.setText(DeliveryUtils.formatDistance(dist));
        h.tvAddress.setText(o.getCustomerAddress());

        h.tvStatusLabel.setText(o.getStatus().toUpperCase());

        String nextLabel = nextActionLabel(o.getStatus());
        if (nextLabel != null) {
            h.btnAction.setVisibility(View.VISIBLE);
            h.btnAction.setText(nextLabel);
            h.btnAction.setOnClickListener(v -> listener.onAction(o));
        } else {
            h.btnAction.setVisibility(View.GONE);
        }

        // Optimized Image Loading
        if (o.getProductImageBase64() != null && !o.getProductImageBase64().isEmpty()) {
            try {
                byte[] imageByteArray = Base64.decode(o.getProductImageBase64(), Base64.DEFAULT);
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

    private String nextActionLabel(String status) {
        if (status == null) return null;
        switch (status) {
            case "PENDING":    return "Confirm Order";
            case "CONFIRMED":  return "Start Preparing";
            case "PREPARING":  return "Mark On the Way";
            case "ON_THE_WAY": return "Mark Delivered";
            default: return null;
        }
    }

    @Override public int getItemCount() { return orders.size(); }

    static class VH extends RecyclerView.ViewHolder {
        ImageView ivProduct;
        TextView tvCustomerName, tvProductName, tvTotal, tvPayment, tvDistance, tvAddress, tvStatusLabel;
        MaterialButton btnAction;

        VH(@NonNull View v) {
            super(v);
            ivProduct      = v.findViewById(R.id.ivProduct);
            tvCustomerName = v.findViewById(R.id.tvCustomerName);
            tvProductName  = v.findViewById(R.id.tvProductName);
            tvTotal        = v.findViewById(R.id.tvTotal);
            tvPayment      = v.findViewById(R.id.tvPayment);
            tvDistance     = v.findViewById(R.id.tvDistance);
            tvAddress      = v.findViewById(R.id.tvAddress);
            tvStatusLabel  = v.findViewById(R.id.tvStatusLabel);
            btnAction      = v.findViewById(R.id.btnAction);
        }
    }
}
