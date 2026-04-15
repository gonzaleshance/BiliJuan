package com.appdev.bilijuan.activities.admin;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
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

    private final List<Order> allOrders       = new ArrayList<>();
    private final List<Order> displayedOrders = new ArrayList<>();

    private ListenerRegistration ordersListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAdminOrdersBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setupBottomNav();
        setupRecyclerView();
        setupSearch();
        loadOrders();
    }

    // ── Bottom nav ────────────────────────────────────────────────────────────

    private void setupBottomNav() {
        AdminNavHelper.setup(this, binding.adminNav.getRoot(),
                AdminNavHelper.Tab.ORDERS, null);
    }

    // ── RecyclerView ──────────────────────────────────────────────────────────

    private void setupRecyclerView() {
        ordersAdapter = new AdminOrdersAdapter(displayedOrders);
        binding.rvOrders.setLayoutManager(new LinearLayoutManager(this));
        binding.rvOrders.setAdapter(ordersAdapter);
    }

    // ── Search ────────────────────────────────────────────────────────────────

    private void setupSearch() {
        binding.etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
            @Override public void onTextChanged(CharSequence s, int st, int b, int c) {}
            @Override public void afterTextChanged(Editable s) {
                filterOrders(s.toString().trim());
            }
        });
    }

    private void filterOrders(String query) {
        displayedOrders.clear();
        if (query.isEmpty()) {
            displayedOrders.addAll(allOrders);
        } else {
            String lower = query.toLowerCase();
            for (Order o : allOrders) {
                boolean customerMatch = o.getCustomerName() != null
                        && o.getCustomerName().toLowerCase().contains(lower);
                boolean sellerMatch   = o.getSellerName()   != null
                        && o.getSellerName().toLowerCase().contains(lower);
                boolean statusMatch   = o.getStatus()       != null
                        && o.getStatus().toLowerCase().contains(lower);
                boolean productMatch  = o.getProductName()  != null
                        && o.getProductName().toLowerCase().contains(lower);
                if (customerMatch || sellerMatch || statusMatch || productMatch)
                    displayedOrders.add(o);
            }
        }
        ordersAdapter.notifyDataSetChanged();
        binding.tvEmpty.setVisibility(displayedOrders.isEmpty() ? View.VISIBLE : View.GONE);
    }

    // ── Load orders ───────────────────────────────────────────────────────────

    private void loadOrders() {
        ordersListener = FirebaseHelper.getDb().collection("orders")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .addSnapshotListener((snap, e) -> {
                    if (e != null || snap == null) return;

                    allOrders.clear();
                    for (QueryDocumentSnapshot d : snap) {
                        Order o = d.toObject(Order.class);
                        o.setOrderId(d.getId());
                        allOrders.add(o);
                    }

                    String currentQuery = binding.etSearch.getText() != null
                            ? binding.etSearch.getText().toString().trim() : "";
                    filterOrders(currentQuery);

                    binding.tvEmpty.setVisibility(
                            displayedOrders.isEmpty() ? View.VISIBLE : View.GONE);
                });
    }

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (ordersListener != null) ordersListener.remove();
    }
}