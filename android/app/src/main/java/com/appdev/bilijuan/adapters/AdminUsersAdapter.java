package com.appdev.bilijuan.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.appdev.bilijuan.R;
import com.appdev.bilijuan.models.User;

import java.util.List;

public class AdminUsersAdapter extends RecyclerView.Adapter<AdminUsersAdapter.VH> {

    public interface ActionListener  { void onAction(User user); }
    public interface DetailListener  { void onDetail(User user); }

    private final List<User>   users;
    private final ActionListener actionListener;
    private final DetailListener detailListener;

    public AdminUsersAdapter(List<User> users,
                             ActionListener actionListener,
                             DetailListener detailListener) {
        this.users          = users;
        this.actionListener = actionListener;
        this.detailListener = detailListener;
    }

    @NonNull @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_admin_user, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        User u = users.get(position);
        h.tvName.setText(u.getName());
        h.tvEmail.setText(u.getEmail() != null ? u.getEmail() : "");
        h.tvPhone.setText(u.getPhone() != null ? u.getPhone() : "—");
        h.tvRole.setText(u.getRole() != null ? u.getRole().toUpperCase() : "");

        // Status badge
        String status = u.getStatus();
        h.tvStatus.setText(status != null ? status : "active");
        int statusColor;
        switch (status != null ? status : "active") {
            case "disabled": statusColor = R.color.error;   break;
            case "archived": statusColor = R.color.text_hint; break;
            default:         statusColor = R.color.primary; break;
        }
        h.tvStatus.setTextColor(
                h.itemView.getContext().getColor(statusColor));

        // Flag count — only for stores
        if (u.getReportCount() > 0) {
            h.tvFlagCount.setVisibility(View.VISIBLE);
            h.tvFlagCount.setText(u.getReportCount() + " reports");
        } else {
            h.tvFlagCount.setVisibility(View.GONE);
        }

        // Tap card → detail
        h.itemView.setOnClickListener(v -> detailListener.onDetail(u));

        // Tap action button → action sheet
        h.btnAction.setOnClickListener(v -> actionListener.onAction(u));
    }

    @Override public int getItemCount() { return users.size(); }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvName, tvEmail, tvPhone, tvRole, tvStatus, tvFlagCount;
        View btnAction;

        VH(@NonNull View v) {
            super(v);
            tvName     = v.findViewById(R.id.tvName);
            tvEmail    = v.findViewById(R.id.tvEmail);
            tvPhone    = v.findViewById(R.id.tvPhone);
            tvRole     = v.findViewById(R.id.tvRole);
            tvStatus   = v.findViewById(R.id.tvStatus);
            tvFlagCount = v.findViewById(R.id.tvFlagCount);
            btnAction  = v.findViewById(R.id.btnAction);
        }
    }
}