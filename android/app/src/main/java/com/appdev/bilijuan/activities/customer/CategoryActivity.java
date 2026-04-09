package com.appdev.bilijuan.activities.customer;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;

import com.appdev.bilijuan.R;
import com.appdev.bilijuan.adapters.CategoryAdapter;
import com.appdev.bilijuan.databinding.ActivityCategoryBinding;

import java.util.Arrays;
import java.util.List;

public class CategoryActivity extends AppCompatActivity {

    private ActivityCategoryBinding binding;

    // Category icons — matched to arrays.xml order
    private static final int[] CATEGORY_ICONS = {
            R.drawable.ic_restaurant,   // Ulam
            R.drawable.ic_restaurant,   // Rice Meals
            R.drawable.ic_star,         // Meryenda
            R.drawable.ic_receipt,      // Pulutan
            R.drawable.ic_star,         // Panghimagas
            R.drawable.ic_location,     // Street Food
            R.drawable.ic_check_circle  // Specialty/Celebration
    };

    // Category colors (light green tints alternating)
    private static final String[] CATEGORY_COLORS = {
            "#E8F5E9", "#F1F8E9", "#E8F5E9", "#F1F8E9",
            "#E8F5E9", "#F1F8E9", "#E8F5E9"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityCategoryBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.btnBack.setOnClickListener(v -> finish());
        setupBottomNav();
        setupCategories();
    }

    private void setupBottomNav() {
        binding.bottomNav.setSelectedItemId(R.id.nav_category);
        binding.bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_category) return true;
            if (id == R.id.nav_home) {
                startActivity(new Intent(this, HomeActivity.class));
                overridePendingTransition(0, 0);
                finish(); return true;
            }
            if (id == R.id.nav_orders) {
                startActivity(new Intent(this, MyOrdersActivity.class));
                overridePendingTransition(0, 0);
                finish(); return true;
            }
            if (id == R.id.nav_profile) {
                startActivity(new Intent(this, AccountActivity.class));
                overridePendingTransition(0, 0);
                finish(); return true;
            }
            return false;
        });
    }

    private void setupCategories() {
        String[] categories = getResources().getStringArray(R.array.product_categories);

        List<CategoryAdapter.CategoryItem> items = new java.util.ArrayList<>();
        for (int i = 0; i < categories.length; i++) {
            items.add(new CategoryAdapter.CategoryItem(
                    categories[i],
                    CATEGORY_ICONS[i],
                    CATEGORY_COLORS[i]
            ));
        }

        CategoryAdapter adapter = new CategoryAdapter(items, item -> {
            Intent intent = new Intent(this, CategoryFoodListActivity.class);
            intent.putExtra("category", item.name);
            startActivity(intent);
        });

        binding.rvCategories.setLayoutManager(new GridLayoutManager(this, 2));
        binding.rvCategories.setAdapter(adapter);
    }
}