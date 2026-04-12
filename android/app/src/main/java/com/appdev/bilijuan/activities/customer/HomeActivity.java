package com.appdev.bilijuan.activities.customer;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.appdev.bilijuan.R;
import com.appdev.bilijuan.adapters.FoodListingAdapter;
import com.appdev.bilijuan.adapters.StoreAdapter;
import com.appdev.bilijuan.databinding.ActivityHomeBinding;
import com.appdev.bilijuan.models.Product;
import com.appdev.bilijuan.models.User;
import com.appdev.bilijuan.utils.FirebaseHelper;
import com.appdev.bilijuan.utils.LocationHelper;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class HomeActivity extends AppCompatActivity {

    private ActivityHomeBinding binding;
    private FoodListingAdapter popularAdapter;
    private FoodListingAdapter listingsAdapter;
    private StoreAdapter storeAdapter;
    private ListenerRegistration popularListener, listingsListener;
    private String activeTab = "Food";
    private String activeCategory = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityHomeBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // ── AUTO GPS: silently save customer's location to Firestore ──────────
        LocationHelper.autoSaveLocation(this);

        setupBottomNav();
        setupTopTabs();
        setupCategoryChips();
        setupRecyclerViews();
        loadUserGreeting();
        switchTab("Food");
    }

    // ── Permission result — needed for LocationHelper ─────────────────────────
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        LocationHelper.onPermissionGranted(this, requestCode, grantResults);
    }

    // ── Bottom Nav ────────────────────────────────────────────────────────────

    private void setupBottomNav() {
        binding.bottomNav.setSelectedItemId(R.id.nav_home);
        binding.bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home)      return true;
            if (id == R.id.nav_orders) {
                startActivity(new Intent(this, MyOrdersActivity.class));
                overridePendingTransition(0, 0); return true;
            }
            if (id == R.id.nav_category) {
                startActivity(new Intent(this, CategoryActivity.class));
                overridePendingTransition(0, 0);
                return true;
            }
            if (id == R.id.nav_profile) {
                startActivity(new Intent(this, AccountActivity.class));
                overridePendingTransition(0, 0);
                return true;
            }
            return false;
        });

        binding.searchBar.setOnClickListener(v ->
                startActivity(new Intent(this, SearchActivity.class)));
        binding.btnNotification.setOnClickListener(v ->
                startActivity(new Intent(this, MyOrdersActivity.class)));
    }

    // ── Top Tabs ──────────────────────────────────────────────────────────────

    private void setupTopTabs() {
        binding.tabFood.setOnClickListener(v  -> switchTab("Food"));
        binding.tabStore.setOnClickListener(v -> switchTab("Store"));
    }

    private void switchTab(String tab) {
        activeTab = tab;

        binding.tabFood.setBackgroundResource(android.R.color.transparent);
        binding.tabStore.setBackgroundResource(android.R.color.transparent);
        binding.tabFood.setTextColor(0xCCFFFFFF);
        binding.tabStore.setTextColor(0xCCFFFFFF);

        if ("Food".equals(tab)) {
            binding.tabFood.setBackgroundResource(R.drawable.bg_tab_active_white);
            binding.tabFood.setTextColor(getColor(R.color.primary));
            binding.sectionFood.setVisibility(View.VISIBLE);
            binding.sectionStore.setVisibility(View.GONE);
            binding.tvListingsLabel.setText("Fresh for you");
            showShimmer(true);
            loadPopular();
            loadFood(activeCategory);
        } else {
            binding.tabStore.setBackgroundResource(R.drawable.bg_tab_active_white);
            binding.tabStore.setTextColor(getColor(R.color.primary));
            binding.sectionFood.setVisibility(View.GONE);
            binding.sectionStore.setVisibility(View.VISIBLE);
            showShimmer(false);
            loadStores();
        }
    }

    // ── Category Chips ────────────────────────────────────────────────────────

    private void setupCategoryChips() {
        String[] categories = getResources().getStringArray(R.array.product_categories);
        binding.categoryChipsContainer.removeAllViews();
        binding.categoryChipsContainer.addView(makeChip("Lahat", null, true));
        for (String cat : categories) {
            binding.categoryChipsContainer.addView(makeChip(cat, cat, false));
        }
    }

    private TextView makeChip(String label, String category, boolean active) {
        TextView chip = new TextView(this);
        int px16 = dp(16), px8 = dp(8);
        chip.setText(label);
        chip.setTextSize(12f);
        chip.setTypeface(null, android.graphics.Typeface.BOLD);
        chip.setPadding(px16, px8, px16, px8);
        chip.setBackgroundResource(active ? R.drawable.bg_chip_active : R.drawable.bg_chip_inactive);
        chip.setTextColor(getColor(active ? R.color.on_chip_active : R.color.on_chip_inactive));
        android.widget.LinearLayout.LayoutParams lp =
                new android.widget.LinearLayout.LayoutParams(
                        android.widget.LinearLayout.LayoutParams.WRAP_CONTENT,
                        android.widget.LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMarginEnd(dp(8));
        chip.setLayoutParams(lp);
        chip.setOnClickListener(v -> {
            activeCategory = category;
            refreshChips(chip);
            if ("Food".equals(activeTab)) {
                showShimmer(true);
                loadFood(category);
            }
        });
        return chip;
    }

    private void refreshChips(TextView selected) {
        for (int i = 0; i < binding.categoryChipsContainer.getChildCount(); i++) {
            TextView chip = (TextView) binding.categoryChipsContainer.getChildAt(i);
            boolean sel = chip == selected;
            chip.setBackgroundResource(sel ? R.drawable.bg_chip_active : R.drawable.bg_chip_inactive);
            chip.setTextColor(getColor(sel ? R.color.on_chip_active : R.color.on_chip_inactive));
        }
    }

    // ── RecyclerViews ─────────────────────────────────────────────────────────

    private void setupRecyclerViews() {
        popularAdapter = new FoodListingAdapter(new ArrayList<>(), this::onProductClick);
        binding.rvPopular.setLayoutManager(
                new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        binding.rvPopular.setAdapter(popularAdapter);

        listingsAdapter = new FoodListingAdapter(new ArrayList<>(), this::onProductClick);
        binding.rvListings.setLayoutManager(new LinearLayoutManager(this));
        binding.rvListings.setAdapter(listingsAdapter);

        storeAdapter = new StoreAdapter(new ArrayList<>(), this::onStoreClick);
        binding.rvStores.setLayoutManager(new LinearLayoutManager(this));
        binding.rvStores.setAdapter(storeAdapter);
    }

    // ── Data Loading ──────────────────────────────────────────────────────────

    private void loadUserGreeting() {
        String uid = FirebaseHelper.getCurrentUid();
        if (uid == null) return;
        FirebaseHelper.getDb().collection("users").document(uid).get()
                .addOnSuccessListener(doc -> {
                    if (!doc.exists()) return;
                    User u = doc.toObject(User.class);
                    if (u != null) {
                        String first = u.getName().split(" ")[0];
                        binding.tvGreeting.setText("Kumain na, " + first + "!");
                    }
                });
    }

    private void loadPopular() {
        if (popularListener != null) popularListener.remove();
        popularListener = FirebaseHelper.getDb().collection("products")
                .whereEqualTo("available", true)
                .orderBy("shopScore", Query.Direction.DESCENDING)
                .limit(10)
                .addSnapshotListener((snap, e) -> {
                    if (e != null || snap == null) return;
                    List<Product> list = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : snap) {
                        Product p = doc.toObject(Product.class);
                        p.setProductId(doc.getId());
                        list.add(p);
                    }
                    popularAdapter.setProducts(list);
                    binding.shimmerPopular.setVisibility(View.GONE);
                    binding.rvPopular.setVisibility(View.VISIBLE);
                });
    }

    private void loadFood(String category) {
        if (listingsListener != null) listingsListener.remove();
        binding.rvListings.setAdapter(listingsAdapter);
        binding.rvListings.setLayoutManager(new LinearLayoutManager(this));

        Query q = FirebaseHelper.getDb().collection("products")
                .whereEqualTo("available", true)
                .orderBy("createdAt", Query.Direction.DESCENDING);
        if (category != null) q = q.whereEqualTo("category", category);

        listingsListener = q.addSnapshotListener((snap, e) -> {
            showShimmer(false);
            if (e != null || snap == null) return;
            List<Product> list = new ArrayList<>();
            for (QueryDocumentSnapshot doc : snap) {
                Product p = doc.toObject(Product.class);
                p.setProductId(doc.getId());
                list.add(p);
            }
            listingsAdapter.setProducts(list);
            binding.emptyListings.setVisibility(list.isEmpty() ? View.VISIBLE : View.GONE);
        });
    }

    private void loadStores() {
        if (listingsListener != null) listingsListener.remove();
        listingsListener = FirebaseHelper.getDb().collection("products")
                .whereEqualTo("available", true)
                .addSnapshotListener((snap, e) -> {
                    if (e != null || snap == null) return;
                    Map<String, StoreAdapter.StoreItem> map = new LinkedHashMap<>();
                    for (QueryDocumentSnapshot doc : snap) {
                        Product p = doc.toObject(Product.class);
                        String sid = p.getSellerId();
                        if (!map.containsKey(sid)) {
                            map.put(sid, new StoreAdapter.StoreItem(
                                    sid, p.getSellerName(),
                                    p.getImageBase64(), p.getCategory(),
                                    p.getStars(), 1));
                        } else {
                            map.get(sid).itemCount++;
                        }
                    }
                    List<StoreAdapter.StoreItem> list = new ArrayList<>(map.values());
                    storeAdapter.setStores(list);
                    binding.emptyStores.setVisibility(list.isEmpty() ? View.VISIBLE : View.GONE);
                });
    }

    // ── Shimmer ───────────────────────────────────────────────────────────────

    private void showShimmer(boolean show) {
        binding.shimmerListings.setVisibility(show ? View.VISIBLE : View.GONE);
        binding.rvListings.setVisibility(show ? View.GONE : View.VISIBLE);
    }

    // ── Click Handlers ────────────────────────────────────────────────────────

    private void onProductClick(Product product) {
        Intent intent = new Intent(this, ProductDetailActivity.class);
        intent.putExtra("productId", product.getProductId());
        startActivity(intent);
    }

    private void onStoreClick(StoreAdapter.StoreItem store) {
        Intent intent = new Intent(this, StoreDetailActivity.class);
        intent.putExtra("sellerId", store.sellerId);
        intent.putExtra("storeName", store.sellerName);
        startActivity(intent);
    }

    private int dp(int dp) {
        return Math.round(dp * getResources().getDisplayMetrics().density);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (popularListener  != null) popularListener.remove();
        if (listingsListener != null) listingsListener.remove();
    }
}