package com.appdev.bilijuan.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.appdev.bilijuan.R;
import com.appdev.bilijuan.models.Order;
import com.appdev.bilijuan.utils.DeliveryUtils;
import com.google.android.material.button.MaterialButton;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class SellerOrdersAdapter extends RecyclerView.Adapter<SellerOrdersAdapter.VH> {

    public interface ActionListener { void onAdvance(Order order); }

    private final List<Order> orders;
    private final ActionListener listener;
    private static final SimpleDateFormat SDF =
            new SimpleDateFormat("MMM d, h:mm a", Locale.getDefault());

    public SellerOrdersAdapter(List<Order> orders, ActionListener listener) {
        this.orders   = orders;
        this.listener = listener;
    }

    @NonNull @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_seller_order_row, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        Order o = orders.get(position);

        h.tvCustomer.setText(o.getCustomerName());
        h.tvProduct.setText(String.format(Locale.getDefault(), "%s × %d", o.getProductName(), o.getQuantity()));
        h.tvAmount.setText(String.format(Locale.getDefault(), "₱%.2f", o.getTotalAmount()));
        h.tvPayment.setText(o.getPaymentMethod());
        h.tvAddress.setText(o.getCustomerAddress());
        h.tvDistance.setText(DeliveryUtils.formatDistance(o.getDistanceKm()));
        
        h.chipStatus.setText(o.getStatus().toUpperCase());

        if (o.getCreatedAt() != null)
            h.tvTime.setText(SDF.format(o.getCreatedAt()));

        String label = actionLabel(o.getStatus());
        if (label != null) {
            h.btnAdvance.setVisibility(View.VISIBLE);
            h.btnAdvance.setText(label);
            h.btnAdvance.setOnClickListener(v -> listener.onAdvance(o));
        } else {
            h.btnAdvance.setVisibility(View.GONE);
        }
    }

    private String actionLabel(String status) {
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
        TextView tvCustomer, tvProduct, tvAmount, tvPayment, tvAddress, tvDistance, tvTime, chipStatus;
        MaterialButton btnAdvance;

        VH(@NonNull View v) {
            super(v);
            tvCustomer = v.findViewById(R.id.tvCustomer);
            tvProduct  = v.findViewById(R.id.tvProduct);
            tvAmount   = v.findViewById(R.id.tvAmount);
            tvPayment  = v.findViewById(R.id.tvPayment);
            tvAddress  = v.findViewById(R.id.tvAddress);
            tvDistance = v.findViewById(R.id.tvDistance);
            tvTime     = v.findViewById(R.id.tvTime);
            chipStatus = v.findViewById(R.id.chipStatus);
            btnAdvance = v.findViewById(R.id.btnAdvance);
        }
    }
}
