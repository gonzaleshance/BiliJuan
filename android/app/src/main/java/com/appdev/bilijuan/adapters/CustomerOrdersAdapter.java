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
import com.google.android.material.progressindicator.LinearProgressIndicator;

import java.util.List;

public class CustomerOrdersAdapter extends RecyclerView.Adapter<CustomerOrdersAdapter.VH> {

    public interface OnOrderClickListener {
        void onTrack(Order order);
        void onReorder(Order order);
        void onCancel(Order order);
        void onClick(Order order);
    }

    private final List<Order> orders;
    private final OnOrderClickListener listener;

    public CustomerOrdersAdapter(List<Order> orders, OnOrderClickListener listener) {
        this.orders = orders;
        this.listener = listener;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_customer_order, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        Order o = orders.get(position);

        // Header info
        String shortId = o.getOrderId().length() > 6 ? o.getOrderId().substring(0, 6).toUpperCase() : o.getOrderId();
        holder.tvOrderId.setText("Order #" + shortId);

        // Build product list string for multi-item orders
        StringBuilder productList = new StringBuilder();
        if (o.getItems() != null && !o.getItems().isEmpty()) {
            for (int i = 0; i < o.getItems().size(); i++) {
                CartItem item = o.getItems().get(i);
                productList.append(item.getProductName()).append(" x").append(item.getQuantity());
                if (i < o.getItems().size() - 1) productList.append(", ");
            }
            holder.tvProductName.setText(productList.toString());
        } else {
            // Fallback to legacy fields if items list is empty
            holder.tvProductName.setText(o.getProductName() + " x" + o.getQuantity());
        }

        holder.tvStatus.setText(o.getStatus());

        // Price and ETA
        holder.tvTotal.setText(String.format("₱%.0f", o.getTotalAmount()));
        
        // Dynamic ETA based on status
        String eta = "40-45 mins";
        int progress = 10;
        String secondaryMsg = "Waiting...";

        switch (o.getStatus()) {
            case Order.STATUS_CONFIRMED:
                eta = "30-40 mins";
                progress = 25;
                secondaryMsg = "Order accepted!";
                break;
            case Order.STATUS_PREPARING:
                eta = "20-30 mins";
                progress = 50;
                secondaryMsg = "Cooking now!";
                break;
            case Order.STATUS_ON_THE_WAY:
                eta = "10-15 mins";
                progress = 75;
                secondaryMsg = "On the way!";
                break;
            case Order.STATUS_DELIVERED:
                eta = "Arrived";
                progress = 100;
                secondaryMsg = "Enjoy your meal!";
                break;
        }
        
        holder.tvEta.setText("ETA: " + eta);
        holder.indicatorProgress.setProgress(progress);
        holder.tvSecondaryStatus.setText(secondaryMsg);

        // Visibility logic
        boolean isActive = o.isActive();
        holder.layoutProgress.setVisibility(isActive ? View.VISIBLE : View.GONE);
        holder.layoutActiveActions.setVisibility(isActive ? View.VISIBLE : View.GONE);
        holder.btnTrack.setVisibility(isActive ? View.VISIBLE : View.GONE);
        holder.layoutHistoryActions.setVisibility(isActive ? View.GONE : View.VISIBLE);

        // Show cancel button only if it can be cancelled
        holder.btnCancel.setVisibility(o.canCustomerCancel() ? View.VISIBLE : View.GONE);

        // Listeners
        holder.itemView.setOnClickListener(v -> listener.onClick(o));
        holder.btnTrack.setOnClickListener(v -> listener.onTrack(o));
        holder.btnReorder.setOnClickListener(v -> listener.onReorder(o));
        holder.btnCancel.setOnClickListener(v -> listener.onCancel(o));
    }

    @Override
    public int getItemCount() {
        return orders.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvOrderId, tvProductName, tvStatus, tvTotal, tvEta, tvSecondaryStatus;
        LinearProgressIndicator indicatorProgress;
        View layoutProgress, btnTrack, layoutHistoryActions, btnReorder, btnCancel, layoutActiveActions;

        VH(@NonNull View v) {
            super(v);
            tvOrderId = v.findViewById(R.id.tvOrderId);
            tvProductName = v.findViewById(R.id.tvProductName);
            tvStatus = v.findViewById(R.id.tvStatus);
            tvTotal = v.findViewById(R.id.tvTotal);
            tvEta = v.findViewById(R.id.tvEta);
            tvSecondaryStatus = v.findViewById(R.id.tvSecondaryStatus);
            indicatorProgress = v.findViewById(R.id.indicatorProgress);
            layoutProgress = v.findViewById(R.id.layoutProgress);
            btnTrack = v.findViewById(R.id.btnTrack);
            layoutHistoryActions = v.findViewById(R.id.layoutHistoryActions);
            btnReorder = v.findViewById(R.id.btnReorder);
            btnCancel = v.findViewById(R.id.btnCancel);
            layoutActiveActions = v.findViewById(R.id.layoutActiveActions);
        }
    }
}