package com.appdev.bilijuan.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.appdev.bilijuan.R;
import com.appdev.bilijuan.models.Notification;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class NotificationAdapter extends RecyclerView.Adapter<NotificationAdapter.VH> {

    public interface OnNotificationClickListener {
        void onNotificationClick(Notification notification);
        void onMarkAsReadClick(Notification notification);
    }

    private final List<Notification> notifications;
    private final OnNotificationClickListener listener;
    private final SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, hh:mm a", Locale.getDefault());

    public NotificationAdapter(List<Notification> notifications, OnNotificationClickListener listener) {
        this.notifications = notifications;
        this.listener = listener;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_notification, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        Notification n = notifications.get(position);
        holder.tvTitle.setText(n.getTitle());
        holder.tvMessage.setText(n.getMessage());
        holder.tvTime.setText(n.getTimestamp() != null ? sdf.format(n.getTimestamp()) : "Just now");

        // UI for read/unread
        holder.itemView.setAlpha(n.isRead() ? 0.6f : 1.0f);
        holder.btnMarkRead.setVisibility(n.isRead() ? View.GONE : View.VISIBLE);

        // Icon based on type
        int iconRes = R.drawable.ic_notifications;
        if (Notification.TYPE_ORDER.equals(n.getType())) iconRes = R.drawable.ic_bag;
        else if (Notification.TYPE_TRACKING.equals(n.getType())) iconRes = R.drawable.ic_delivery;
        holder.ivIcon.setImageResource(iconRes);

        holder.itemView.setOnClickListener(v -> listener.onNotificationClick(n));
        holder.btnMarkRead.setOnClickListener(v -> listener.onMarkAsReadClick(n));
    }

    @Override
    public int getItemCount() {
        return notifications.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        ImageView ivIcon;
        TextView tvTitle, tvMessage, tvTime;
        View btnMarkRead;

        VH(@NonNull View v) {
            super(v);
            ivIcon = v.findViewById(R.id.ivIcon);
            tvTitle = v.findViewById(R.id.tvTitle);
            tvMessage = v.findViewById(R.id.tvMessage);
            tvTime = v.findViewById(R.id.tvTime);
            btnMarkRead = v.findViewById(R.id.btnMarkRead);
        }
    }
}
