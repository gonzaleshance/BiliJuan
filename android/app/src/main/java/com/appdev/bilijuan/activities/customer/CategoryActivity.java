package com.appdev.bilijuan.activities.customer;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.appdev.bilijuan.R;
import com.appdev.bilijuan.adapters.CategoryAdapter;
import com.appdev.bilijuan.adapters.FoodListingAdapter;
import com.appdev.bilijuan.databinding.ActivityCategoryBinding;
import com.appdev.bilijuan.models.Product;
import com.appdev.bilijuan.utils.CustomerNavHelper;
import com.appdev.bilijuan.utils.FirebaseHelper;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class CategoryActivity extends AppCompatActivity {

    private ActivityCategoryBinding binding;
    private FoodListingAdapter foodAdapter;
    private ListenerRegistration foodListener;
    private String selectedCategory = null;

    private static final String[] CATEGORY_EMOJIS = {
            "🍱", "🍚", "🥟", "🍻", "🍰", "🍢", "🎈"
    };

    private static final String[] CATEGORY_SUBTITLES = {
            "Freshly cooked local viands",
            "Heavy and filling complete meals",
            "Perfect afternoon Filipino snacks",
            "Best paired with cold drinks",
            "Sweet treats and native desserts",
            "Quick, tasty, and affordable bites",
            "Made for your special occasions"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityCategoryBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setupUI();
        setupBottomNav();
        setupCategories();
        setupFoodList();
    }

    private void setupUI() {
        binding.btnBack.setOnClickListener(v -> closeCategory());
    }

    private void setupBottomNav() {
        CustomerNavHelper.setup(this, binding.customerNav.getRoot(), CustomerNavHelper.Tab.CATEGORY);
    }

    private void setupCategories() {
        String[] categories = getResources().getStringArray(R.array.product_categories);
        List<CategoryAdapter.CategoryItem> items = new ArrayList<>();
        
        for (int i = 0; i < categories.length; i++) {
            items.add(new CategoryAdapter.CategoryItem(
                    categories[i],
                    CATEGORY_EMOJIS[Math.min(i, CATEGORY_EMOJIS.length - 1)],
                    CATEGORY_SUBTITLES[Math.min(i, CATEGORY_SUBTITLES.length - 1)],
                    "#FFFFFF"
            ));
        }

        CategoryAdapter adapter = new CategoryAdapter(items, item -> openCategory(item.name));
        binding.rvCategories.setLayoutManager(new LinearLayoutManager(this));
        binding.rvCategories.setAdapter(adapter);
    }

    private void setupFoodList() {
        foodAdapter = new FoodListingAdapter(new ArrayList<>(), product -> {
            Intent intent = new Intent(this, ProductDetailActivity.class);
            intent.putExtra("productId", product.getProductId());
            startActivity(intent);
        });
        binding.rvFoodList.setLayoutManager(new LinearLayoutManager(this));
        binding.rvFoodList.setAdapter(foodAdapter);
    }

    private void openCategory(String category) {
        selectedCategory = category;
        binding.tvHeaderTitle.setText(category);
        binding.btnBack.setVisibility(View.VISIBLE);
        binding.rvCategories.setVisibility(View.GONE);
        binding.rvFoodList.setVisibility(View.VISIBLE);
        binding.shimmer.setVisibility(View.VISIBLE);
        loadCategoryFood(category);
    }

    private void closeCategory() {
        selectedCategory = null;
        binding.tvHeaderTitle.setText("Explore Categories");
        binding.btnBack.setVisibility(View.GONE);
        binding.rvCategories.setVisibility(View.VISIBLE);
        binding.rvFoodList.setVisibility(View.GONE);
        binding.tvEmpty.setVisibility(View.GONE);
        if (foodListener != null) foodListener.remove();
    }

    private void loadCategoryFood(String category) {
        if (foodListener != null) foodListener.remove();
        
        Query q = FirebaseHelper.getDb().collection("products")
                .whereEqualTo("category", category)
                .whereEqualTo("available", true);

        foodListener = q.addSnapshotListener((snap, e) -> {
            binding.shimmer.setVisibility(View.GONE);
            if (e != null || snap == null) return;
            
            List<Product> list = new ArrayList<>();
            for (QueryDocumentSnapshot doc : snap) {
                Product p = doc.toObject(Product.class);
                p.setProductId(doc.getId());
                list.add(p);
            }
            foodAdapter.setProducts(list);
            binding.tvEmpty.setVisibility(list.isEmpty() ? View.VISIBLE : View.GONE);
        });
    }

    @Override
    public void onBackPressed() {
        if (selectedCategory != null) {
            closeCategory();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (foodListener != null) foodListener.remove();
    }
}
