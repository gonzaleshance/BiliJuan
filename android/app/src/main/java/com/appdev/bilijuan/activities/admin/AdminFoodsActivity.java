package com.appdev.bilijuan.activities.admin;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.appdev.bilijuan.adapters.FoodListingAdapter;
import com.appdev.bilijuan.databinding.ActivityAdminFoodsBinding;
import com.appdev.bilijuan.models.Product;
import com.appdev.bilijuan.utils.AdminNavHelper;
import com.appdev.bilijuan.utils.FirebaseHelper;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class AdminFoodsActivity extends AppCompatActivity {

    private ActivityAdminFoodsBinding binding;
    private FoodListingAdapter productsAdapter;

    private final List<Product> allProducts       = new ArrayList<>();
    private final List<Product> displayedProducts = new ArrayList<>();

    private ListenerRegistration productsListener;

    private String filterSellerId   = null;
    private String filterSellerName = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAdminFoodsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        filterSellerId   = getIntent().getStringExtra("sellerId");
        filterSellerName = getIntent().getStringExtra("sellerName");

        setupUI();
        setupBottomNav();
        setupRecyclerView();
        setupSearch();
        loadProducts();
    }

    // ── UI ────────────────────────────────────────────────────────────────────

    private void setupUI() {
        if (filterSellerId != null) {
            binding.tvHeaderTitle.setText(filterSellerName);
            binding.tvHeaderSubtitle.setText("Food items from this store");
            binding.chipSellerFilter.setVisibility(View.VISIBLE);
            binding.chipSellerFilter.setText("Store: " + filterSellerName);
            binding.chipSellerFilter.setOnCloseIconClickListener(v -> {
                filterSellerId   = null;
                filterSellerName = null;
                binding.chipSellerFilter.setVisibility(View.GONE);
                binding.tvHeaderTitle.setText("Platform Foods");
                binding.tvHeaderSubtitle.setText("Manage all listed food items");
                loadProducts();
            });
        } else {
            binding.chipSellerFilter.setVisibility(View.GONE);
        }
    }

    // ── Bottom nav ────────────────────────────────────────────────────────────

    private void setupBottomNav() {
        AdminNavHelper.setup(this, binding.adminNav.getRoot(),
                AdminNavHelper.Tab.FOODS);
    }

    // ── RecyclerView ──────────────────────────────────────────────────────────

    private void setupRecyclerView() {
        productsAdapter = new FoodListingAdapter(displayedProducts, product ->
                Toast.makeText(this,
                        product.getName() + " by " + product.getSellerName(),
                        Toast.LENGTH_SHORT).show());
        binding.rvProducts.setLayoutManager(new LinearLayoutManager(this));
        binding.rvProducts.setAdapter(productsAdapter);
    }

    // ── Search ────────────────────────────────────────────────────────────────

    private void setupSearch() {
        binding.etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
            @Override public void onTextChanged(CharSequence s, int st, int b, int c) {}
            @Override public void afterTextChanged(Editable s) {
                filterProducts(s.toString().trim());
            }
        });
    }

    private void filterProducts(String query) {
        displayedProducts.clear();
        if (query.isEmpty()) {
            displayedProducts.addAll(allProducts);
        } else {
            String lower = query.toLowerCase();
            for (Product p : allProducts) {
                boolean nameMatch     = p.getName()       != null
                        && p.getName().toLowerCase().contains(lower);
                boolean sellerMatch   = p.getSellerName() != null
                        && p.getSellerName().toLowerCase().contains(lower);
                boolean categoryMatch = p.getCategory()   != null
                        && p.getCategory().toLowerCase().contains(lower);
                if (nameMatch || sellerMatch || categoryMatch)
                    displayedProducts.add(p);
            }
        }
        productsAdapter.setProducts(displayedProducts);
        binding.tvEmpty.setVisibility(
                displayedProducts.isEmpty() ? View.VISIBLE : View.GONE);
    }

    // ── Load products ─────────────────────────────────────────────────────────

    private void loadProducts() {
        if (productsListener != null) productsListener.remove();

        Query query = FirebaseHelper.getDb().collection("products");
        if (filterSellerId != null) {
            query = query.whereEqualTo("sellerId", filterSellerId);
        }
        query = query.orderBy("createdAt", Query.Direction.DESCENDING);

        productsListener = query.addSnapshotListener((snap, e) -> {
            if (e != null || snap == null) return;

            allProducts.clear();
            int availableCount = 0;
            for (QueryDocumentSnapshot d : snap) {
                Product p = d.toObject(Product.class);
                p.setProductId(d.getId());
                allProducts.add(p);
                if (p.isAvailable()) availableCount++;
            }

            String currentQuery = binding.etSearch.getText() != null
                    ? binding.etSearch.getText().toString().trim() : "";
            filterProducts(currentQuery);

            binding.tvTotalProducts.setText(String.valueOf(allProducts.size()));
            binding.tvAvailableProducts.setText(String.valueOf(availableCount));
            binding.tvEmpty.setVisibility(
                    displayedProducts.isEmpty() ? View.VISIBLE : View.GONE);
        });
    }

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (productsListener != null) productsListener.remove();
    }
}