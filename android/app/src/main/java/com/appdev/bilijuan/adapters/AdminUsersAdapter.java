package com.appdev.bilijuan.adapters;

import android.graphics.Color;
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

    public interface ActionListener { void onAction(User user); }
    public interface DetailListener { void onDetail(User user); }

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
        h.tvEmail.setText(u.getEmail()  != null ? u.getEmail()  : "");
        h.tvPhone.setText(u.getPhone()  != null ? u.getPhone()  : "—");
        h.tvRole.setText(u.getRole()    != null ? u.getRole().toUpperCase() : "");

        // ── Status badge ──────────────────────────────────────────────────────
        String status = u.getStatus();
        h.tvStatus.setText(status != null ? status : "active");
        int statusColor;
        switch (status != null ? status : "active") {
            case "disabled": statusColor = R.color.error;     break;
            case "archived": statusColor = R.color.text_hint; break;
            default:         statusColor = R.color.primary;   break;
        }
        h.tvStatus.setTextColor(h.itemView.getContext().getColor(statusColor));

        // ── Report count flag ─────────────────────────────────────────────────
        if (u.getReportCount() > 0) {
            h.tvFlagCount.setVisibility(View.VISIBLE);
            h.tvFlagCount.setText(u.getReportCount() + " reports");
        } else {
            h.tvFlagCount.setVisibility(View.GONE);
        }

        // ── No-location warning (sellers only) ────────────────────────────────
        // If a seller's latitude is 0.0, they registered before mandatory pinning
        // was enforced. Highlight the card in amber and show a warning label so
        // the admin can identify and contact them.
        boolean isSeller       = "seller".equals(u.getRole());
        boolean missingLocation = isSeller && !u.hasLocation();

        if (missingLocation) {
            // Amber left border tint + warning label
            h.itemView.setBackgroundColor(Color.parseColor("#FFFBF0"));
            if (h.tvFlagCount.getVisibility() == View.GONE) {
                h.tvFlagCount.setVisibility(View.VISIBLE);
            }
            // Append location warning to flag text (or set it if no reports)
            String existingFlag = h.tvFlagCount.getText().toString();
            if (existingFlag.isEmpty() || existingFlag.equals("0 reports")) {
                h.tvFlagCount.setText("⚠️ No location set");
            } else {
                h.tvFlagCount.setText(existingFlag + " · ⚠️ No location");
            }
            h.tvFlagCount.setTextColor(Color.parseColor("#E67E22")); // amber
        } else {
            // Reset to default card background
            h.itemView.setBackgroundColor(Color.WHITE);
            // Reset flag text color
            h.tvFlagCount.setTextColor(Color.parseColor("#EB4D4B")); // red for reports
        }

        // ── Tap handlers ──────────────────────────────────────────────────────
        h.itemView.setOnClickListener(v -> detailListener.onDetail(u));
        h.btnAction.setOnClickListener(v -> actionListener.onAction(u));
    }

    @Override public int getItemCount() { return users.size(); }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvName, tvEmail, tvPhone, tvRole, tvStatus, tvFlagCount;
        View     btnAction;

        VH(@NonNull View v) {
            super(v);
            tvName      = v.findViewById(R.id.tvName);
            tvEmail     = v.findViewById(R.id.tvEmail);
            tvPhone     = v.findViewById(R.id.tvPhone);
            tvRole      = v.findViewById(R.id.tvRole);
            tvStatus    = v.findViewById(R.id.tvStatus);
            tvFlagCount = v.findViewById(R.id.tvFlagCount);
            btnAction   = v.findViewById(R.id.btnAction);
        }
    }
}