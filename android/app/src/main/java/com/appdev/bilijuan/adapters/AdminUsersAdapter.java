package com.appdev.bilijuan.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.SwitchCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.appdev.bilijuan.R;
import com.appdev.bilijuan.models.User;

import java.util.List;

public class AdminUsersAdapter extends RecyclerView.Adapter<AdminUsersAdapter.VH> {

    public interface ToggleListener { void onToggle(User user); }

    private final List<User> users;
    private final ToggleListener listener;

    public AdminUsersAdapter(List<User> users, ToggleListener listener) {
        this.users    = users;
        this.listener = listener;
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
        h.tvEmail.setText(u.getEmail());
        h.tvRole.setText(u.getRole().toUpperCase());
        h.tvPhone.setText(u.getPhone());

        h.switchApproved.setOnCheckedChangeListener(null);
        h.switchApproved.setChecked(u.isApproved());
        h.switchApproved.setOnCheckedChangeListener((btn, checked) ->
                listener.onToggle(u));
    }

    @Override public int getItemCount() { return users.size(); }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvName, tvEmail, tvRole, tvPhone;
        SwitchCompat switchApproved;

        VH(@NonNull View v) {
            super(v);
            tvName        = v.findViewById(R.id.tvName);
            tvEmail       = v.findViewById(R.id.tvEmail);
            tvRole        = v.findViewById(R.id.tvRole);
            tvPhone       = v.findViewById(R.id.tvPhone);
            switchApproved = v.findViewById(R.id.switchApproved);
        }
    }
}