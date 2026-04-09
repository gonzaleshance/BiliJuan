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
import com.google.android.material.button.MaterialButton;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class CustomerOrdersAdapter extends RecyclerView.Adapter<CustomerOrdersAdapter.VH> {

    public interface TrackListener  { void onTrack(Order order); }
    public interface CancelListener { void onCancel(Order order); }

    private final List<Order> orders;
    private final TrackListener  trackListener;
    private final CancelListener cancelListener;
    private static final SimpleDateFormat SDF =
            new SimpleDateFormat("MMM d, h:mm a", Locale.getDefault());

    public CustomerOrdersAdapter(List<Order> orders,
                                 TrackListener t, CancelListener c) {
        this.orders         = orders;
        this.trackListener  = t;
        this.cancelListener = c;
    }

    @NonNull @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_customer_order, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        Order o = orders.get(position);
        h.tvProductName.setText(o.getProductName() + " × " + o.getQuantity());
        h.tvSellerName.setText("from " + o.getSellerName());
        h.tvTotal.setText(String.format("₱%.0f", o.getTotalAmount()));
        h.tvPayment.setText(o.getPaymentMethod());
        h.tvStatus.setText(o.getStatus());
        h.tvStatus.setBackgroundTintList(
                ColorStateList.valueOf(statusColor(o.getStatus())));
        if (o.getCreatedAt() != null)
            h.tvDate.setText(SDF.format(o.getCreatedAt()));

        // Track button — show for active orders
        if (o.isActive()) {
            h.btnTrack.setVisibility(View.VISIBLE);
            h.btnTrack.setOnClickListener(v -> trackListener.onTrack(o));
        } else {
            h.btnTrack.setVisibility(View.GONE);
        }

        // Cancel button — show only if cancellable
        if (o.canCustomerCancel()) {
            h.btnCancel.setVisibility(View.VISIBLE);
            h.btnCancel.setOnClickListener(v -> cancelListener.onCancel(o));
        } else {
            h.btnCancel.setVisibility(View.GONE);
        }
    }

    private int statusColor(String status) {
        switch (status) {
            case Order.STATUS_PENDING:    return Color.parseColor("#FFDBD0");
            case Order.STATUS_CONFIRMED:
            case Order.STATUS_PREPARING:  return Color.parseColor("#D0EDFF");
            case Order.STATUS_ON_THE_WAY: return Color.parseColor("#FFD6B0");
            case Order.STATUS_DELIVERED:  return Color.parseColor("#D0F0D0");
            default:                      return Color.parseColor("#E8E8E4");
        }
    }

    @Override public int getItemCount() { return orders.size(); }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvProductName, tvSellerName, tvTotal, tvPayment, tvStatus, tvDate;
        MaterialButton btnTrack, btnCancel;

        VH(@NonNull View v) {
            super(v);
            tvProductName = v.findViewById(R.id.tvProductName);
            tvSellerName  = v.findViewById(R.id.tvSellerName);
            tvTotal       = v.findViewById(R.id.tvTotal);
            tvPayment     = v.findViewById(R.id.tvPayment);
            tvStatus      = v.findViewById(R.id.tvStatus);
            tvDate        = v.findViewById(R.id.tvDate);
            btnTrack      = v.findViewById(R.id.btnTrack);
            btnCancel     = v.findViewById(R.id.btnCancel);
        }
    }
}