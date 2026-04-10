package com.appdev.bilijuan.adapters;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.Space;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.appdev.bilijuan.R;
import com.appdev.bilijuan.models.Order;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.List;

public class ActiveOrderCardAdapter extends RecyclerView.Adapter<ActiveOrderCardAdapter.VH> {

    // ── Listener ──────────────────────────────────────────────────────────────
    public interface ActionListener {
        void onAction(Order order);
        void onViewMap(Order order);
    }

    private final List<Order>    orders;
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

        // Order number
        String shortId = o.getOrderId() != null && o.getOrderId().length() >= 6
                ? "Order #" + o.getOrderId().substring(0, 6).toUpperCase()
                : "Order";
        h.tvOrderNumber.setText(shortId);
        h.tvTimeAgo.setText(timeAgo(o));
        h.tvProductName.setText("• " + o.getProductName() + " ×" + o.getQuantity());
        h.tvTotal.setText(String.format("Total: ₱%.0f", o.getTotalAmount()));

        // Customer info
        h.tvCustomerName.setText(o.getCustomerName());
        h.tvCustomerAddress.setText(o.getCustomerAddress());

        // Call button
        h.btnCallCustomer.setOnClickListener(v -> {
            if (o.getCustomerPhone() == null) return;
            Intent dial = new Intent(Intent.ACTION_DIAL,
                    Uri.parse("tel:" + o.getCustomerPhone()));
            v.getContext().startActivity(dial);
        });

        // Status badge
        h.tvStatusBadge.setText(o.getStatus());
        h.tvStatusBadge.setBackgroundTintList(
                ColorStateList.valueOf(statusColor(o.getStatus())));
        h.tvStatusBadge.setTextColor(statusTextColor(o.getStatus()));

        // ── Advance button ────────────────────────────────────────────────────
        String next = nextStatus(o.getStatus());
        boolean showAdvance = next != null;
        if (showAdvance) {
            h.btnAction.setVisibility(View.VISIBLE);
            h.btnAction.setText(advanceLabel(o.getStatus()));
            h.btnAction.setOnClickListener(v -> listener.onAction(o));
        } else {
            h.btnAction.setVisibility(View.GONE);
        }

        // ── View Map button ───────────────────────────────────────────────────
        boolean hasLocation = o.getCustomerLat() != 0 || o.getCustomerLng() != 0;
        boolean showMap     = o.isActive() && hasLocation;
        if (showMap) {
            h.btnViewMap.setVisibility(View.VISIBLE);
            h.btnViewMap.setOnClickListener(v -> listener.onViewMap(o));
        } else {
            h.btnViewMap.setVisibility(View.GONE);
        }

        // Spacer — only when both buttons visible
        h.spaceBetweenButtons.setVisibility(
                (showAdvance && showMap) ? View.VISIBLE : View.GONE);

        // ── "You are delivering" banner ───────────────────────────────────────
        boolean onTheWay = Order.STATUS_ON_THE_WAY.equals(o.getStatus());
        h.bannerDelivering.setVisibility(onTheWay ? View.VISIBLE : View.GONE);
    }

    @Override public int getItemCount() { return orders.size(); }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private String timeAgo(Order o) {
        if (o.getCreatedAt() == null) return "";
        long diff  = System.currentTimeMillis() - o.getCreatedAt().getTime();
        long mins  = diff / 60000;
        long hours = mins / 60;
        if (hours >= 1) return hours + "h ago";
        if (mins  >= 1) return mins  + "m ago";
        return "Just now";
    }

    private String advanceLabel(String status) {
        switch (status) {
            case Order.STATUS_PENDING:    return "Confirm Order";
            case Order.STATUS_CONFIRMED:  return "Start Preparing";
            case Order.STATUS_PREPARING:  return "Mark On the Way";
            case Order.STATUS_ON_THE_WAY: return "Mark Delivered";
            default: return "Advance";
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

    // ── ViewHolder ────────────────────────────────────────────────────────────

    static class VH extends RecyclerView.ViewHolder {
        TextView               tvOrderNumber, tvTimeAgo, tvProductName,
                tvCustomerName, tvCustomerAddress, tvTotal, tvStatusBadge;
        MaterialButton         btnAction, btnViewMap;
        Space                  spaceBetweenButtons;
        FloatingActionButton   btnCallCustomer;
        LinearLayout           bannerDelivering;

        VH(@NonNull View v) {
            super(v);
            tvOrderNumber      = v.findViewById(R.id.tvOrderNumber);
            tvTimeAgo          = v.findViewById(R.id.tvTimeAgo);
            tvProductName      = v.findViewById(R.id.tvProductName);
            tvCustomerName     = v.findViewById(R.id.tvCustomerName);
            tvCustomerAddress  = v.findViewById(R.id.tvCustomerAddress);
            tvTotal            = v.findViewById(R.id.tvTotal);
            tvStatusBadge      = v.findViewById(R.id.tvStatusBadge);
            btnAction          = v.findViewById(R.id.btnAction);
            btnViewMap         = v.findViewById(R.id.btnViewMap);
            spaceBetweenButtons= v.findViewById(R.id.spaceBetweenButtons);
            btnCallCustomer    = v.findViewById(R.id.btnCallCustomer);
            bannerDelivering   = v.findViewById(R.id.bannerDelivering);
        }
    }
}
