package com.appdev.bilijuan.activities.customer;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.appdev.bilijuan.R;
import com.appdev.bilijuan.activities.LoginActivity;
import com.appdev.bilijuan.adapters.CustomerOrdersAdapter;
import com.appdev.bilijuan.databinding.ActivityAccountBinding;
import com.appdev.bilijuan.models.CartItem;
import com.appdev.bilijuan.models.Order;
import com.appdev.bilijuan.models.User;
import com.appdev.bilijuan.utils.CustomerNavHelper;
import com.appdev.bilijuan.utils.FirebaseHelper;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class AccountActivity extends AppCompatActivity {

    private ActivityAccountBinding binding;
    private String currentUid;
    private User currentUser;
    private CustomerOrdersAdapter ordersAdapter;
    private final List<Order> activeOrdersList = new ArrayList<>();
    private ListenerRegistration ordersListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAccountBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        currentUid = FirebaseHelper.getCurrentUid();
        if (currentUid == null) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        setupBottomNav();
        setupClickListeners();
        setupRecyclerView();
        loadProfile();
        listenToOrders();
    }

    private void setupBottomNav() {
        CustomerNavHelper.setup(this, binding.customerNav.getRoot(), CustomerNavHelper.Tab.PROFILE);
    }

    private void setupClickListeners() {
        binding.btnEdit.setOnClickListener(v -> showEditProfileModal());
        binding.btnLogout.setOnClickListener(v -> showLogoutConfirmation());
    }

    private void setupRecyclerView() {
        ordersAdapter = new CustomerOrdersAdapter(activeOrdersList, new CustomerOrdersAdapter.OnOrderClickListener() {
            @Override
            public void onTrack(Order order) {
                Intent intent = new Intent(AccountActivity.this, OrderTrackingActivity.class);
                intent.putExtra("orderId", order.getOrderId());
                startActivity(intent);
            }

            @Override
            public void onReorder(Order order) { }

            @Override
            public void onCancel(Order order) { }

            @Override
            public void onClick(Order order) {
                showOrderDetails(order);
            }
        });
        binding.rvActiveOrders.setLayoutManager(new LinearLayoutManager(this));
        binding.rvActiveOrders.setAdapter(ordersAdapter);
    }

    private void showOrderDetails(Order order) {
        BottomSheetDialog dialog = new BottomSheetDialog(this, R.style.BottomSheetDialogTheme);
        View view = getLayoutInflater().inflate(R.layout.bottom_sheet_order_details, null);
        dialog.setContentView(view);

        TextView tvOrderId = view.findViewById(R.id.tvOrderId);
        TextView tvOrderDate = view.findViewById(R.id.tvOrderDate);
        TextView tvCustomerName = view.findViewById(R.id.tvCustomerName);
        TextView tvCustomerPhone = view.findViewById(R.id.tvCustomerPhone);
        TextView tvCustomerAddress = view.findViewById(R.id.tvCustomerAddress);
        LinearLayout layoutOrderItems = view.findViewById(R.id.layoutOrderItems);
        TextView tvSubtotal = view.findViewById(R.id.tvSubtotal);
        TextView tvDeliveryFee = view.findViewById(R.id.tvDeliveryFee);
        TextView tvTotalAmount = view.findViewById(R.id.tvTotalAmount);
        View btnClose = view.findViewById(R.id.btnClose);

        String shortId = order.getOrderId().length() > 8 ? order.getOrderId().substring(0, 8).toUpperCase() : order.getOrderId();
        tvOrderId.setText("Order #" + shortId);

        if (order.getCreatedAt() != null) {
            SimpleDateFormat sdf = new SimpleDateFormat("MMM d, yyyy • h:mm a", Locale.getDefault());
            tvOrderDate.setText("Placed on " + sdf.format(order.getCreatedAt()));
        }

        tvCustomerName.setText(order.getCustomerName());
        tvCustomerPhone.setText(order.getCustomerPhone());
        tvCustomerAddress.setText(order.getCustomerAddress());

        layoutOrderItems.removeAllViews();
        double subtotal = 0;

        if (order.getItems() != null && !order.getItems().isEmpty()) {
            for (CartItem item : order.getItems()) {
                View itemView = getLayoutInflater().inflate(R.layout.item_order_detail, layoutOrderItems, false);
                ((TextView) itemView.findViewById(R.id.tvItemName)).setText(item.getProductName());
                ((TextView) itemView.findViewById(R.id.tvItemQty)).setText("x" + item.getQuantity());
                ((TextView) itemView.findViewById(R.id.tvItemPrice)).setText(String.format("₱%.0f", item.getPrice() * item.getQuantity()));
                layoutOrderItems.addView(itemView);
                subtotal += item.getPrice() * item.getQuantity();
            }
        } else {
            // Fallback for single item orders
            View itemView = getLayoutInflater().inflate(R.layout.item_order_detail, layoutOrderItems, false);
            ((TextView) itemView.findViewById(R.id.tvItemName)).setText(order.getProductName());
            ((TextView) itemView.findViewById(R.id.tvItemQty)).setText("x" + order.getQuantity());
            ((TextView) itemView.findViewById(R.id.tvItemPrice)).setText(String.format("₱%.0f", order.getProductPrice() * order.getQuantity()));
            layoutOrderItems.addView(itemView);
            subtotal = order.getProductPrice() * order.getQuantity();
        }

        tvSubtotal.setText(String.format("₱%.0f", subtotal));
        tvDeliveryFee.setText(String.format("₱%.0f", order.getDeliveryFee()));
        tvTotalAmount.setText(String.format("₱%.0f", order.getTotalAmount()));

        btnClose.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    private void showLogoutConfirmation() {
        BottomSheetDialog dialog = new BottomSheetDialog(this, R.style.BottomSheetDialogTheme);
        View view = LayoutInflater.from(this).inflate(R.layout.bottom_sheet_logout_confirm, null);
        dialog.setContentView(view);

        view.findViewById(R.id.btnConfirmLogout).setOnClickListener(v -> {
            dialog.dismiss();
            FirebaseHelper.signOut(this);
            Intent intent = new Intent(this, LoginActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        });

        view.findViewById(R.id.btnCancelLogout).setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    private void loadProfile() {
        FirebaseHelper.getDb().collection("users").document(currentUid).get()
                .addOnSuccessListener(doc -> {
                    if (!doc.exists()) return;
                    currentUser = doc.toObject(User.class);
                    if (currentUser == null) return;
                    updateUI();
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Failed to load profile.", Toast.LENGTH_SHORT).show());
    }

    /**
     * More robust listener that doesn't rely on the "active" boolean field in Firestore,
     * which might be missing or inconsistent. Uses the Order.isActive() method instead.
     */
    private void listenToOrders() {
        if (ordersListener != null) ordersListener.remove();

        ordersListener = FirebaseHelper.getDb().collection("orders")
                .whereEqualTo("customerId", currentUid)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .addSnapshotListener((snap, e) -> {
                    if (e != null || snap == null) return;

                    activeOrdersList.clear();
                    int totalCount = snap.size();
                    int activeCount = 0;

                    for (QueryDocumentSnapshot doc : snap) {
                        Order o = doc.toObject(Order.class);
                        o.setOrderId(doc.getId());

                        // Use the robust isActive() method which checks the status field
                        if (o.isActive()) {
                            activeCount++;
                            // Only show up to 3 in the profile summary
                            if (activeOrdersList.size() < 3) {
                                activeOrdersList.add(o);
                            }
                        }
                    }

                    // Update UI stats
                    binding.tvTotalOrders.setText(String.valueOf(totalCount));
                    binding.tvActiveOrders.setText(String.valueOf(activeCount));

                    // Update list visibility
                    ordersAdapter.notifyDataSetChanged();
                    boolean hasActive = !activeOrdersList.isEmpty();
                    binding.tvEmptyActiveOrders.setVisibility(hasActive ? View.GONE : View.VISIBLE);
                    binding.rvActiveOrders.setVisibility(hasActive ? View.VISIBLE : View.GONE);
                });
    }

    private void updateUI() {
        if (currentUser == null) return;
        binding.tvName.setText(currentUser.getName());
        binding.tvEmail.setText(currentUser.getEmail());
        binding.tvPhone.setText(!TextUtils.isEmpty(currentUser.getPhone()) ? currentUser.getPhone() : "Not set");
        
        // Update initials
        if (!TextUtils.isEmpty(currentUser.getName())) {
            String[] parts = currentUser.getName().trim().split("\\s+");
            String initials = "";
            if (parts.length > 0 && !parts[0].isEmpty()) {
                initials += parts[0].substring(0, 1).toUpperCase();
                if (parts.length > 1 && !parts[parts.length - 1].isEmpty()) {
                    initials += parts[parts.length - 1].substring(0, 1).toUpperCase();
                }
            }
            binding.tvInitials.setText(initials);
        }

        if (currentUser.getCreatedAt() != null) {
            SimpleDateFormat sdf = new SimpleDateFormat("MMMM d, yyyy", Locale.getDefault());
            binding.tvJoinedDate.setText(sdf.format(currentUser.getCreatedAt()));
        } else {
            binding.tvJoinedDate.setText("Early Member");
        }
    }

    private void showEditProfileModal() {
        if (currentUser == null) return;

        BottomSheetDialog dialog = new BottomSheetDialog(this, R.style.BottomSheetDialogTheme);
        View view = getLayoutInflater().inflate(R.layout.bottom_sheet_edit_profile, null);

        TextInputEditText etName = view.findViewById(R.id.etEditName);
        TextInputEditText etPhone = view.findViewById(R.id.etEditPhone);
        View btnSave = view.findViewById(R.id.btnSaveProfile);

        etName.setText(currentUser.getName());
        etPhone.setText(currentUser.getPhone());

        btnSave.setOnClickListener(v -> {
            String newName = etName.getText().toString().trim();
            String newPhone = etPhone.getText().toString().trim();

            if (TextUtils.isEmpty(newName)) {
                etName.setError("Name is required");
                return;
            }

            btnSave.setEnabled(false);
            
            Map<String, Object> updates = new HashMap<>();
            updates.put("name", newName);
            updates.put("phone", newPhone);

            FirebaseHelper.getDb().collection("users").document(currentUid)
                    .update(updates)
                    .addOnSuccessListener(aVoid -> {
                        currentUser.setName(newName);
                        currentUser.setPhone(newPhone);
                        updateUI();
                        dialog.dismiss();
                        Toast.makeText(this, "Profile updated!", Toast.LENGTH_SHORT).show();
                    })
                    .addOnFailureListener(e -> {
                        btnSave.setEnabled(true);
                        Toast.makeText(this, "Update failed.", Toast.LENGTH_SHORT).show();
                    });
        });

        dialog.setContentView(view);
        dialog.show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (ordersListener != null) ordersListener.remove();
    }
}
