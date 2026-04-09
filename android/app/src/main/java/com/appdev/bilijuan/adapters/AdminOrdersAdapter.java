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

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class AdminOrdersAdapter extends RecyclerView.Adapter<AdminOrdersAdapter.VH> {

    private final List<Order> orders;
    private static final SimpleDateFormat SDF =
            new SimpleDateFormat("MMM d, h:mm a", Locale.getDefault());

    public AdminOrdersAdapter(List<Order> orders) { this.orders = orders; }

    @NonNull @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_admin_order, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        Order o = orders.get(position);
        h.tvCustomer.setText(o.getCustomerName());
        h.tvSeller.setText("from " + o.getSellerName());
        h.tvProduct.setText(o.getProductName() + " × " + o.getQuantity());
        h.tvTotal.setText(String.format("₱%.0f", o.getTotalAmount()));
        h.tvStatus.setText(o.getStatus());
        h.tvStatus.setBackgroundTintList(
                ColorStateList.valueOf(statusColor(o.getStatus())));
        h.tvPayment.setText(o.getPaymentMethod());
        if (o.getCreatedAt() != null)
            h.tvDate.setText(SDF.format(o.getCreatedAt()));
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
        TextView tvCustomer, tvSeller, tvProduct, tvTotal, tvStatus, tvPayment, tvDate;

        VH(@NonNull View v) {
            super(v);
            tvCustomer = v.findViewById(R.id.tvCustomer);
            tvSeller   = v.findViewById(R.id.tvSeller);
            tvProduct  = v.findViewById(R.id.tvProduct);
            tvTotal    = v.findViewById(R.id.tvTotal);
            tvStatus   = v.findViewById(R.id.tvStatus);
            tvPayment  = v.findViewById(R.id.tvPayment);
            tvDate     = v.findViewById(R.id.tvDate);
        }
    }
}