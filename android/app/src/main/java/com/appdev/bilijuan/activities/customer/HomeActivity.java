package com.appdev.bilijuan.activities.customer;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.appdev.bilijuan.R;
import com.appdev.bilijuan.adapters.FoodListingAdapter;
import com.appdev.bilijuan.adapters.StoreAdapter;
import com.appdev.bilijuan.databinding.ActivityHomeBinding;
import com.appdev.bilijuan.models.Product;
import com.appdev.bilijuan.models.User;
import com.appdev.bilijuan.utils.CartBottomSheet;
import com.appdev.bilijuan.utils.CartHelper;
import com.appdev.bilijuan.utils.CustomerNavHelper;
import com.appdev.bilijuan.utils.FirebaseHelper;
import com.appdev.bilijuan.utils.LocationHelper;
import com.appdev.bilijuan.utils.NetworkHelper;
import com.appdev.bilijuan.utils.NotificationUIHelper;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class HomeActivity extends AppCompatActivity {

    private ActivityHomeBinding binding;
    private FoodListingAdapter popularAdapter;
    private FoodListingAdapter listingsAdapter;
    private FoodListingAdapter searchResultsAdapter;
    private StoreAdapter storeAdapter;
    private ListenerRegistration popularListener, listingsListener, storesListener, notifListener;
    private String activeTab = "Food";
    private String activeCategory = null;
    private List<Product> allProductsForSearch = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityHomeBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        LocationHelper.autoSaveLocation(this);

        setupBottomNav();
        setupTopTabs();
        setupCategoryChips();
        setupRecyclerViews();
        setupSearch();
        setupBackNavigation();
        listenForNotifications();
        loadUserGreeting();
        updateCartUI();
        switchTab("Food");
    }

    private void setupBackNavigation() {
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (binding.layoutSearchResults.getVisibility() == View.VISIBLE) {
                    binding.etSearch.setText("");
                    binding.layoutSearchResults.setVisibility(View.GONE);
                    binding.swipeRefresh.setVisibility(View.VISIBLE);
                } else {
                    setEnabled(false);
                    getOnBackPressedDispatcher().onBackPressed();
                }
            }
        });
    }

    private void listenForNotifications() {
        String uid = FirebaseHelper.getCurrentUid();
        if (uid == null) return;

        notifListener = FirebaseHelper.getDb().collection("notifications")
                .whereEqualTo("userId", uid)
                .whereEqualTo("read", false)
                .addSnapshotListener((snap, e) -> {
                    if (e != null || snap == null) return;
                    binding.notifDot.setVisibility(snap.isEmpty() ? View.GONE : View.VISIBLE);
                });
    }

    private void setupBottomNav() {
        CustomerNavHelper.setup(this, binding.customerNav.getRoot(), CustomerNavHelper.Tab.HOME);
        binding.btnNotification.setOnClickListener(v -> NotificationUIHelper.showNotificationSheet(this));
        
        binding.btnCart.setOnClickListener(v -> CartBottomSheet.show(this, this::updateCartUI));
    }

    private void updateCartUI() {
        int count = CartHelper.getCartCount(this);
        if (count > 0) {
            binding.btnCart.setVisibility(View.VISIBLE);
            binding.tvCartBadge.setText(String.valueOf(count));
        } else {
            binding.btnCart.setVisibility(View.GONE);
        }
    }

    private void setupTopTabs() {
        binding.tabFood.setOnClickListener(v  -> switchTab("Food"));
        binding.tabStore.setOnClickListener(v -> switchTab("Store"));
    }

    private void switchTab(String tab) {
        activeTab = tab;
        binding.etSearch.setText(""); 
        
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
        android.widget.LinearLayout.LayoutParams lp = new android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT,
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMarginEnd(dp(8));
        chip.setLayoutParams(lp);
        chip.setOnClickListener(v -> {
            activeCategory = category;
            refreshChips(chip);
            if ("Food".equals(activeTab)) {
                updatePopularVisibility(category != null);
                showShimmer(true);
                loadFood(category);
            }
        });
        return chip;
    }

    private void updatePopularVisibility(boolean isFiltering) {
        View popularHeader = binding.tvSeeAllPopular.getParent() instanceof View ? (View) binding.tvSeeAllPopular.getParent() : null;
        if (popularHeader != null) popularHeader.setVisibility(isFiltering ? View.GONE : View.VISIBLE);
        binding.rvPopular.setVisibility(isFiltering ? View.GONE : View.VISIBLE);
        binding.shimmerPopular.setVisibility(isFiltering ? View.GONE : View.VISIBLE);
    }

    private void refreshChips(TextView selected) {
        for (int i = 0; i < binding.categoryChipsContainer.getChildCount(); i++) {
            TextView chip = (TextView) binding.categoryChipsContainer.getChildAt(i);
            boolean sel = chip == selected;
            chip.setBackgroundResource(sel ? R.drawable.bg_chip_active : R.drawable.bg_chip_inactive);
            chip.setTextColor(getColor(sel ? R.color.on_chip_active : R.color.on_chip_inactive));
        }
    }

    private void setupRecyclerViews() {
        binding.swipeRefresh.setColorSchemeColors(getColor(R.color.primary));
        binding.swipeRefresh.setOnRefreshListener(() -> {
            activeCategory = null;
            refreshChips((TextView) binding.categoryChipsContainer.getChildAt(0));
            updatePopularVisibility(false);
            loadPopular();
            loadFood(null);
            loadStores();
            binding.swipeRefresh.postDelayed(() -> binding.swipeRefresh.setRefreshing(false), 1500);
        });

        popularAdapter = new FoodListingAdapter(new ArrayList<>(), this::onProductClick);
        binding.rvPopular.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        binding.rvPopular.setAdapter(popularAdapter);

        listingsAdapter = new FoodListingAdapter(new ArrayList<>(), this::onProductClick);
        binding.rvListings.setLayoutManager(new LinearLayoutManager(this));
        binding.rvListings.setAdapter(listingsAdapter);

        searchResultsAdapter = new FoodListingAdapter(new ArrayList<>(), this::onProductClick);
        binding.rvSearchResults.setLayoutManager(new LinearLayoutManager(this));
        binding.rvSearchResults.setAdapter(searchResultsAdapter);

        storeAdapter = new StoreAdapter(new ArrayList<>(), this::onStoreClick);
        binding.rvStores.setLayoutManager(new LinearLayoutManager(this));
        binding.rvStores.setAdapter(storeAdapter);
    }

    private void setupSearch() {
        binding.etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                performSearch(s.toString());
            }
            @Override public void afterTextChanged(Editable s) {}
        });
    }

    private void performSearch(String query) {
        if (query.isEmpty()) {
            binding.layoutSearchResults.setVisibility(View.GONE);
            binding.swipeRefresh.setVisibility(View.VISIBLE);
            return;
        }

        binding.layoutSearchResults.setVisibility(View.VISIBLE);
        binding.swipeRefresh.setVisibility(View.GONE);

        List<Product> filtered = new ArrayList<>();
        for (Product p : allProductsForSearch) {
            if (p.getName().toLowerCase().contains(query.toLowerCase()) ||
                p.getSellerName().toLowerCase().contains(query.toLowerCase())) {
                filtered.add(p);
            }
        }
        searchResultsAdapter.setProducts(filtered);
        binding.tvSearchCount.setText(filtered.size() + " results found");
    }

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
                .limit(10)
                .addSnapshotListener((snap, e) -> {
                    binding.shimmerPopular.setVisibility(View.GONE);
                    if (activeCategory == null) binding.rvPopular.setVisibility(View.VISIBLE);
                    if (e != null || snap == null) return;
                    List<Product> list = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : snap) {
                        Product p = doc.toObject(Product.class);
                        p.setProductId(doc.getId());
                        list.add(p);
                    }
                    Collections.sort(list, (a, b) -> Double.compare(b.getStars(), a.getStars()));
                    popularAdapter.setProducts(list);
                });
    }

    private void loadFood(String category) {
        if (listingsListener != null) listingsListener.remove();
        if (!NetworkHelper.isOnline(this)) {
            NetworkHelper.showOfflineToast(this);
            return;
        }

        Query q = FirebaseHelper.getDb().collection("products").whereEqualTo("available", true);
        if (category != null) q = q.whereEqualTo("category", category);

        listingsListener = q.addSnapshotListener((snap, e) -> {
            showShimmer(false);
            binding.rvListings.setVisibility(View.VISIBLE);
            if (e != null || snap == null) return;
            
            List<Product> list = new ArrayList<>();
            for (QueryDocumentSnapshot doc : snap) {
                Product p = doc.toObject(Product.class);
                p.setProductId(doc.getId());
                list.add(p);
            }
            
            Collections.sort(list, (a, b) -> {
                if (a.getCreatedAt() == null || b.getCreatedAt() == null) return 0;
                return b.getCreatedAt().compareTo(a.getCreatedAt());
            });

            listingsAdapter.setProducts(list);
            if (category == null) allProductsForSearch = new ArrayList<>(list);
            binding.emptyListings.setVisibility(list.isEmpty() ? View.VISIBLE : View.GONE);
        });
    }

    private void loadStores() {
        if (storesListener != null) storesListener.remove();
        storesListener = FirebaseHelper.getDb().collection("users")
                .whereEqualTo("role", "seller")
                .whereEqualTo("status", "active")
                .addSnapshotListener((snap, e) -> {
                    if (e != null || snap == null) return;
                    List<StoreAdapter.StoreItem> list = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : snap) {
                        User u = doc.toObject(User.class);
                        u.setUid(doc.getId());
                        list.add(new StoreAdapter.StoreItem(
                                u.getUid(), u.getName(),
                                null, u.getStoreImageBase64(),
                                "Available", 0, 0)); // Rating/count handled separately or later
                    }
                    storeAdapter.setStores(list);
                    binding.emptyStores.setVisibility(list.isEmpty() ? View.VISIBLE : View.GONE);

                    // Optional: Fetch item count and average rating for each store
                    for (StoreAdapter.StoreItem item : list) {
                        FirebaseHelper.getDb().collection("products")
                                .whereEqualTo("sellerId", item.sellerId)
                                .whereEqualTo("available", true)
                                .get()
                                .addOnSuccessListener(pSnap -> {
                                    if (pSnap.isEmpty()) return;
                                    item.itemCount = pSnap.size();
                                    float totalStars = 0;
                                    for (QueryDocumentSnapshot pDoc : pSnap) {
                                        totalStars += pDoc.getDouble("stars");
                                    }
                                    item.rating = totalStars / pSnap.size();
                                    storeAdapter.notifyDataSetChanged();
                                });
                    }
                });
    }

    private void showShimmer(boolean show) {
        binding.shimmerListings.setVisibility(show ? View.VISIBLE : View.GONE);
        binding.shimmerPopular.setVisibility(show && activeCategory == null ? View.VISIBLE : View.GONE);
        binding.scrollViewMain.setVisibility(show ? View.GONE : View.VISIBLE);
    }

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

    private int dp(int dp) { return Math.round(dp * getResources().getDisplayMetrics().density); }

    @Override
    protected void onResume() {
        super.onResume();
        updateCartUI();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (popularListener != null) popularListener.remove();
        if (listingsListener != null) listingsListener.remove();
        if (storesListener != null) storesListener.remove();
        if (notifListener != null) notifListener.remove();
    }
}