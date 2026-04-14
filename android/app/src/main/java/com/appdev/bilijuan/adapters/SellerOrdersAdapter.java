package com.appdev.bilijuan.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.appdev.bilijuan.R;
import com.appdev.bilijuan.models.CartItem;
import com.appdev.bilijuan.models.Order;
import com.google.android.material.button.MaterialButton;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class SellerOrdersAdapter extends RecyclerView.Adapter<SellerOrdersAdapter.VH> {

    public interface ActionListener {
        void onAdvance(Order order);
        void onViewMap(Order order);
        void onViewDetails(Order order);
    }

    private final List<Order>    orders;
    private final ActionListener listener;
    private static final SimpleDateFormat SDF = new SimpleDateFormat("MMM d, h:mm a", Locale.getDefault());

    public SellerOrdersAdapter(List<Order> orders, ActionListener listener) {
        this.orders   = orders;
        this.listener = listener;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_seller_order_row, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        Order o = orders.get(position);

        h.tvCustomer.setText(o.getCustomerName());
        
        // Handle Multi-item display
        StringBuilder productDetails = new StringBuilder();
        if (o.getItems() != null && !o.getItems().isEmpty()) {
            for (int i = 0; i < o.getItems().size(); i++) {
                CartItem item = o.getItems().get(i);
                productDetails.append(item.getProductName()).append(" x").append(item.getQuantity());
                if (i < o.getItems().size() - 1) productDetails.append(", ");
            }
        } else {
            // Fallback to legacy fields
            productDetails.append(o.getProductName()).append(" x").append(o.getQuantity());
        }
        h.tvProduct.setText(productDetails.toString());
        
        h.tvAmount.setText(String.format(Locale.getDefault(), "₱%.0f", o.getTotalAmount()));
        h.tvAddress.setText(o.getCustomerAddress());
        
        if (o.getDistanceKm() > 0) {
            h.tvDistance.setVisibility(View.VISIBLE);
            h.tvDistance.setText(String.format(Locale.getDefault(), "%.1f km", o.getDistanceKm()));
        } else {
            h.tvDistance.setVisibility(View.GONE);
        }

        h.chipStatus.setText(o.getStatus());
        
        if (o.getCreatedAt() != null) {
            h.tvTime.setText(formatTimeAgo(o.getCreatedAt()));
        }

        // Action Label (e.g. Accept Order, Start Cooking)
        String label = actionLabel(o.getStatus());
        if (label != null && o.isActive()) {
            h.btnAdvance.setVisibility(View.VISIBLE);
            h.btnAdvance.setText(label);
            h.btnAdvance.setOnClickListener(v -> listener.onAdvance(o));
        } else {
            h.btnAdvance.setVisibility(View.GONE);
        }

        // Track Location button (matches customer Track Order style)
        boolean hasLocation = o.getCustomerLat() != 0 || o.getCustomerLng() != 0;
        if (o.isActive() && hasLocation) {
            h.btnViewMap.setVisibility(View.VISIBLE);
            h.btnViewMap.setOnClickListener(v -> listener.onViewMap(o));
        } else {
            h.btnViewMap.setVisibility(View.GONE);
        }

        // View Details click listener on the whole card
        h.itemView.setOnClickListener(v -> listener.onViewDetails(o));
    }

    private String formatTimeAgo(Date date) {
        long diff = System.currentTimeMillis() - date.getTime();
        long minutes = diff / (1000 * 60);
        if (minutes < 1) return "Just now";
        if (minutes < 60) return minutes + " mins ago";
        long hours = minutes / 60;
        if (hours < 24) return hours + " hrs ago";
        return SDF.format(date);
    }

    private String actionLabel(String status) {
        if (status == null) return null;
        switch (status) {
            case Order.STATUS_PENDING:    return "Accept Order";
            case Order.STATUS_CONFIRMED:  return "Start Cooking";
            case Order.STATUS_PREPARING:  return "Out for Delivery";
            case Order.STATUS_ON_THE_WAY: return "Mark Delivered";
            default: return null;
        }
    }

    @Override
    public int getItemCount() { return orders.size(); }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvCustomer, tvProduct, tvAmount, tvAddress, tvDistance, tvTime, chipStatus;
        MaterialButton btnAdvance;
        View btnViewMap;

        VH(@NonNull View v) {
            super(v);
            tvCustomer = v.findViewById(R.id.tvCustomer);
            tvProduct  = v.findViewById(R.id.tvProduct);
            tvAmount   = v.findViewById(R.id.tvAmount);
            tvAddress  = v.findViewById(R.id.tvAddress);
            tvDistance = v.findViewById(R.id.tvDistance);
            tvTime     = v.findViewById(R.id.tvTime);
            chipStatus = v.findViewById(R.id.chipStatus);
            btnAdvance = v.findViewById(R.id.btnAdvance);
            btnViewMap = v.findViewById(R.id.btnViewMap);
        }
    }
}