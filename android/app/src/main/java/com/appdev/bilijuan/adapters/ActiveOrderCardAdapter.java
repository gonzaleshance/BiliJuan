package com.appdev.bilijuan.adapters;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.appdev.bilijuan.R;
import com.appdev.bilijuan.models.Order;

import java.util.List;

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

        // Order number — use short orderId
        String shortId = o.getOrderId() != null && o.getOrderId().length() >= 6
                ? "Order #" + o.getOrderId().substring(0, 6).toUpperCase()
                : "Order";
        h.tvOrderNumber.setText(shortId);
        h.tvTimeAgo.setText(timeAgo(o));
        h.tvProductName.setText("• " + o.getProductName() + " ×" + o.getQuantity());
        h.tvCustomerName.setText("• " + o.getCustomerName());
        h.tvTotal.setText(String.format("Total: ₱%.0f", o.getTotalAmount()));

        // Status badge
        h.tvStatusBadge.setText(o.getStatus());
        h.tvStatusBadge.setBackgroundTintList(
                ColorStateList.valueOf(statusColor(o.getStatus())));
        h.tvStatusBadge.setTextColor(statusTextColor(o.getStatus()));

        // Action — advance status
        String next = nextStatus(o.getStatus());
        h.btnAction.setText(next != null ? "Advance →" : "View Details");
        h.btnAction.setOnClickListener(v -> listener.onAction(o));
    }

    private String timeAgo(Order o) {
        if (o.getCreatedAt() == null) return "";
        long diff = System.currentTimeMillis() - o.getCreatedAt().getTime();
        long mins  = diff / 60000;
        long hours = mins / 60;
        if (hours >= 1) return hours + "h ago";
        if (mins  >= 1) return mins  + "m ago";
        return "Just now";
    }

    private int statusColor(String status) {
        switch (status) {
            case Order.STATUS_PENDING:    return Color.parseColor("#FFF3CD");
            case Order.STATUS_CONFIRMED:
            case Order.STATUS_PREPARING:  return Color.parseColor("#D0EDFF");
            case Order.STATUS_ON_THE_WAY: return Color.parseColor("#FFE0CC");
            case Order.STATUS_DELIVERED:  return Color.parseColor("#D4EDDA");
            default:                      return Color.parseColor("#F1F2F6");
        }
    }

    private int statusTextColor(String status) {
        switch (status) {
            case Order.STATUS_PENDING:    return Color.parseColor("#856404");
            case Order.STATUS_CONFIRMED:
            case Order.STATUS_PREPARING:  return Color.parseColor("#004085");
            case Order.STATUS_ON_THE_WAY: return Color.parseColor("#833C00");
            case Order.STATUS_DELIVERED:  return Color.parseColor("#155724");
            default:                      return Color.parseColor("#636E72");
        }
    }

    private String nextStatus(String status) {
        switch (status) {
            case Order.STATUS_PENDING:    return Order.STATUS_CONFIRMED;
            case Order.STATUS_CONFIRMED:  return Order.STATUS_PREPARING;
            case Order.STATUS_PREPARING:  return Order.STATUS_ON_THE_WAY;
            case Order.STATUS_ON_THE_WAY: return Order.STATUS_DELIVERED;
            default: return null;
        }
    }

    @Override public int getItemCount() { return orders.size(); }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvOrderNumber, tvTimeAgo, tvProductName,
                tvCustomerName, tvTotal, tvStatusBadge, btnAction;

        VH(@NonNull View v) {
            super(v);
            tvOrderNumber  = v.findViewById(R.id.tvOrderNumber);
            tvTimeAgo      = v.findViewById(R.id.tvTimeAgo);
            tvProductName  = v.findViewById(R.id.tvProductName);
            tvCustomerName = v.findViewById(R.id.tvCustomerName);
            tvTotal        = v.findViewById(R.id.tvTotal);
            tvStatusBadge  = v.findViewById(R.id.tvStatusBadge);
            btnAction      = v.findViewById(R.id.btnAction);
        }
    }
}