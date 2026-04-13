package com.appdev.bilijuan.activities.seller;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.appdev.bilijuan.R;
import com.appdev.bilijuan.adapters.SellerMenuAdapter;
import com.appdev.bilijuan.activities.LoginActivity;
import com.appdev.bilijuan.databinding.ActivitySellerAccountBinding;
import com.appdev.bilijuan.models.Product;
import com.appdev.bilijuan.models.User;
import com.appdev.bilijuan.utils.FirebaseHelper;
import com.appdev.bilijuan.utils.StoreNavHelper;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class SellerAccountActivity extends AppCompatActivity {

    private ActivitySellerAccountBinding binding;
    private SellerMenuAdapter menuAdapter;
    private final List<Product> menuItems = new ArrayList<>();
    private ListenerRegistration menuListener;
    private String sellerId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySellerAccountBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        sellerId = FirebaseHelper.getCurrentUid();
        if (sellerId == null) { finish(); return; }

        setupRecyclerView();
        setupStoreNav();
        setupClickListeners();
        loadProfile();
        listenMenu();

        // If opened from Post sheet with openMenu=true, scroll to menu section
        if (getIntent().getBooleanExtra("openMenu", false)) {
            binding.scrollContent.post(() -> {
                if (binding.tvMyMenuLabel != null) {
                    binding.scrollContent.smoothScrollTo(0, binding.tvMyMenuLabel.getTop());
                }
            });
        }
    }

    private void setupRecyclerView() {
        menuAdapter = new SellerMenuAdapter(menuItems, this::onToggle, this::onEdit, this::onDelete);
        binding.rvMenu.setLayoutManager(new LinearLayoutManager(this));
        binding.rvMenu.setAdapter(menuAdapter);
        binding.rvMenu.setNestedScrollingEnabled(false);
    }

    private void setupStoreNav() {
        // FIXED: Use .getRoot() to pass the View, and use the Tab.STORE active state
        StoreNavHelper.setup(this, binding.storeNav.getRoot(), StoreNavHelper.Tab.STORE);
        
        // Custom FAB action for Store Account
        binding.storeNav.fabPost.setOnClickListener(v -> showPostBottomSheet());
    }

    private void setupClickListeners() {
        binding.btnBack.setOnClickListener(v -> finish());
        
        // Settings / Menu button in header
        binding.btnSettings.setOnClickListener(v -> showSettingsBottomSheet());

        binding.btnAddItem.setOnClickListener(v ->
                startActivity(new Intent(this, AddProductActivity.class)));
        
        binding.btnPinLocation.setOnClickListener(v ->
                startActivity(new Intent(this, SellerPinLocationActivity.class)));

        binding.btnLogout.setOnClickListener(v -> logout());
    }

    private void showSettingsBottomSheet() {
        BottomSheetDialog sheet = new BottomSheetDialog(this, R.style.BottomSheetStyle);
        View v = LayoutInflater.from(this).inflate(R.layout.bottom_sheet_admin_action, null);
        sheet.setContentView(v);
        
        TextView tvTitle = v.findViewById(R.id.tvUserName);
        if (tvTitle != null) tvTitle.setText("Account Settings");

        // Edit Profile using btnEnable slot
        View btnEdit = v.findViewById(R.id.btnEnable);
        if (btnEdit != null) {
            btnEdit.setVisibility(View.VISIBLE);
            // Access text view inside LinearLayout button
            TextView tvLabel = btnEdit.findViewById(R.id.tvUserName); // This ID is reused in include? No, let's look at XML again
            // In bottom_sheet_admin_action.xml, the buttons are LinearLayouts.
            // Inside them, the first TextView has no ID? No, wait.
            // I'll just find the first TextView in the button hierarchy.
        }
        
        btnEdit.setOnClickListener(btn -> {
            sheet.dismiss();
            startActivity(new Intent(this, EditSellerProfileActivity.class));
        });

        // Logout using btnArchive slot
        View btnLogout = v.findViewById(R.id.btnArchive);
        if (btnLogout != null) {
            btnLogout.setVisibility(View.VISIBLE);
            btnLogout.setOnClickListener(btn -> {
                sheet.dismiss();
                logout();
            });
        }

        // Hide unused buttons
        if (v.findViewById(R.id.btnDisable) != null) v.findViewById(R.id.btnDisable).setVisibility(View.GONE);

        sheet.show();
    }

    private void showPostBottomSheet() {
        BottomSheetDialog sheet = new BottomSheetDialog(this, R.style.BottomSheetStyle);
        View v = LayoutInflater.from(this).inflate(R.layout.bottom_sheet_post, null);
        sheet.setContentView(v);
        v.findViewById(R.id.btnNewItem).setOnClickListener(btn -> {
            sheet.dismiss();
            startActivity(new Intent(this, AddProductActivity.class));
        });
        v.findViewById(R.id.btnEditExisting).setOnClickListener(btn -> {
            sheet.dismiss();
            if (binding.tvMyMenuLabel != null) {
                binding.scrollContent.smoothScrollTo(0, binding.tvMyMenuLabel.getTop());
            }
        });
        sheet.show();
    }

    private void loadProfile() {
        FirebaseHelper.getDb().collection("users").document(sellerId).get()
                .addOnSuccessListener(doc -> {
                    if (!doc.exists()) return;
                    User u = doc.toObject(User.class);
                    if (u == null) return;
                    binding.tvStoreName.setText(u.getName());
                    binding.tvPhone.setText(u.getPhone());
                    binding.tvAddress.setText(u.getAddress());
                });
    }

    private void listenMenu() {
        menuListener = FirebaseHelper.getDb()
                .collection("products")
                .whereEqualTo("sellerId", sellerId)
                .addSnapshotListener((snap, e) -> {
                    if (e != null || snap == null) return;
                    menuItems.clear();
                    for (QueryDocumentSnapshot doc : snap) {
                        Product p = doc.toObject(Product.class);
                        p.setProductId(doc.getId());
                        menuItems.add(p);
                    }
                    menuAdapter.notifyDataSetChanged();
                    binding.emptyMenu.setVisibility(
                            menuItems.isEmpty() ? View.VISIBLE : View.GONE);
                });
    }

    private void onToggle(Product product, boolean available) {
        FirebaseHelper.getDb().collection("products")
                .document(product.getProductId())
                .update("available", available);
    }

    private void onEdit(Product product) {
        Intent intent = new Intent(this, AddProductActivity.class);
        intent.putExtra("productId", product.getProductId());
        startActivity(intent);
    }

    private void onDelete(Product product) {
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Delete Item")
                .setMessage("Remove \"" + product.getName() + "\" from your menu?")
                .setPositiveButton("Delete", (d, w) ->
                        FirebaseHelper.getDb().collection("products")
                                .document(product.getProductId()).delete())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void logout() {
        FirebaseHelper.signOut();
        Intent intent = new Intent(this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (menuListener != null) menuListener.remove();
    }
}
