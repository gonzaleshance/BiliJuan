package com.appdev.bilijuan.activities.admin;

import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.appdev.bilijuan.adapters.AdminOrdersAdapter;
import com.appdev.bilijuan.databinding.ActivityAdminOrdersBinding;
import com.appdev.bilijuan.models.Order;
import com.appdev.bilijuan.utils.AdminNavHelper;
import com.appdev.bilijuan.utils.FirebaseHelper;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class AdminOrdersActivity extends AppCompatActivity {

    private ActivityAdminOrdersBinding binding;
    private AdminOrdersAdapter ordersAdapter;
    private final List<Order> orders = new ArrayList<>();
    private ListenerRegistration ordersListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAdminOrdersBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setupBottomNav();
        setupRecyclerView();
        loadOrders();
    }

    private void setupBottomNav() {
        AdminNavHelper.setup(this, binding.adminNav.getRoot(), AdminNavHelper.Tab.ORDERS, null);
    }

    private void setupRecyclerView() {
        ordersAdapter = new AdminOrdersAdapter(orders);
        binding.rvOrders.setLayoutManager(new LinearLayoutManager(this));
        binding.rvOrders.setAdapter(ordersAdapter);
    }

    private void loadOrders() {
        ordersListener = FirebaseHelper.getDb().collection("orders")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .addSnapshotListener((snap, e) -> {
                    if (e != null || snap == null) return;
                    orders.clear();
                    for (QueryDocumentSnapshot d : snap) {
                        Order o = d.toObject(Order.class);
                        o.setOrderId(d.getId());
                        orders.add(o);
                    }
                    ordersAdapter.notifyDataSetChanged();
                    binding.tvEmpty.setVisibility(orders.isEmpty() ? View.VISIBLE : View.GONE);
                });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (ordersListener != null) ordersListener.remove();
    }
}
