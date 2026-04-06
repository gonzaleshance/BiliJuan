package com.appdev.bilijuan.activities.customer;

import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.appdev.bilijuan.R;
import com.appdev.bilijuan.adapters.ProductAdapter;
import com.appdev.bilijuan.models.Product;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.List;

public class HomeActivity extends AppCompatActivity {

    private RecyclerView rvDeals, rvListings;
    private ProductAdapter dealsAdapter, listingsAdapter;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        db = FirebaseFirestore.getInstance();
        initViews();
        setupRecyclerViews();
        loadProducts();
    }

    private void initViews() {
        rvDeals = findViewById(R.id.rvDeals);
        rvListings = findViewById(R.id.rvListings);
        
        TextView tvGreeting = findViewById(R.id.tvGreeting);
        // In a real app, you'd get the user name from FirebaseAuth/Firestore
        tvGreeting.setText("Hey, Juan!");
    }

    private void setupRecyclerViews() {
        // Horizontal Deals (Popular)
        dealsAdapter = new ProductAdapter(true);
        rvDeals.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        rvDeals.setAdapter(dealsAdapter);

        // Grid Listings (All/Fresh)
        listingsAdapter = new ProductAdapter(false);
        rvListings.setLayoutManager(new GridLayoutManager(this, 2));
        rvListings.setAdapter(listingsAdapter);

        // Click Listeners
        ProductAdapter.OnProductClickListener listener = product -> {
            Toast.makeText(this, "Clicked: " + product.getName(), Toast.LENGTH_SHORT).show();
            // TODO: Navigate to Product Detail
        };

        dealsAdapter.setOnProductClickListener(listener);
        listingsAdapter.setOnProductClickListener(listener);
    }

    private void loadProducts() {
        // Load Popular Deals (Ordered by Shop Score or Rating)
        db.collection("products")
                .whereEqualTo("available", true)
                .orderBy("shopScore", Query.Direction.DESCENDING)
                .limit(5)
                .addSnapshotListener((value, error) -> {
                    if (error != null) return;
                    if (value != null) {
                        List<Product> deals = value.toObjects(Product.class);
                        dealsAdapter.setProducts(deals);
                    }
                });

        // Load All Listings
        db.collection("products")
                .whereEqualTo("available", true)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .addSnapshotListener((value, error) -> {
                    if (error != null) return;
                    if (value != null) {
                        List<Product> listings = value.toObjects(Product.class);
                        listingsAdapter.setProducts(listings);
                    }
                });
    }
}
