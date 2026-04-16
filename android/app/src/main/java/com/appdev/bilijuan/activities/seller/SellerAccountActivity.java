package com.appdev.bilijuan.activities.seller;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.appdev.bilijuan.R;
import com.appdev.bilijuan.adapters.SellerMenuAdapter;
import com.appdev.bilijuan.activities.LoginActivity;
import com.appdev.bilijuan.databinding.ActivitySellerAccountBinding;
import com.appdev.bilijuan.models.Product;
import com.appdev.bilijuan.models.User;
import com.appdev.bilijuan.utils.FirebaseHelper;
import com.appdev.bilijuan.utils.ImageHelper;
import com.appdev.bilijuan.utils.StoreNavHelper;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SellerAccountActivity extends AppCompatActivity {

    private ActivitySellerAccountBinding binding;
    private SellerMenuAdapter menuAdapter;
    private final List<Product> menuItems = new ArrayList<>();
    private ListenerRegistration menuListener;
    private String sellerId;
    private User currentUser;

    private String encodedImage;
    private ImageView ivEditAvatar;
    private TextInputEditText etEditAddress;
    
    private double updatedLat = 0;
    private double updatedLng = 0;
    private String updatedAddress = "";

    private final ActivityResultLauncher<String> pickerLauncher = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                if (uri != null && ivEditAvatar != null) {
                    encodedImage = ImageHelper.uriToBase64(this, uri);
                    ivEditAvatar.setImageURI(uri);
                    ivEditAvatar.setPadding(0, 0, 0, 0);
                    ivEditAvatar.setScaleType(ImageView.ScaleType.CENTER_CROP);
                    ivEditAvatar.setImageTintList(null);
                }
            }
    );

    private final ActivityResultLauncher<Intent> pinMapLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    updatedLat = result.getData().getDoubleExtra("lat", 0);
                    updatedLng = result.getData().getDoubleExtra("lng", 0);
                    updatedAddress = result.getData().getStringExtra("address");
                    
                    if (etEditAddress != null && !TextUtils.isEmpty(updatedAddress)) {
                        etEditAddress.setText(updatedAddress);
                    }
                    Toast.makeText(this, "Location updated. Don't forget to save!", Toast.LENGTH_LONG).show();
                }
            }
    );

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
        StoreNavHelper.setup(this, binding.storeNav.getRoot(), StoreNavHelper.Tab.STORE);
        binding.storeNav.fabPost.setOnClickListener(v -> showPostBottomSheet());
    }

    private void setupClickListeners() {
        binding.btnSettings.setOnClickListener(v -> showEditProfileModal());
        binding.btnAddItem.setOnClickListener(v -> startActivity(new Intent(this, AddProductActivity.class)));
        binding.btnLogout.setOnClickListener(v -> showLogoutConfirmation());
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
            finish();
        });

        view.findViewById(R.id.btnCancelLogout).setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    private void loadProfile() {
        FirebaseHelper.getDb().collection("users").document(sellerId).get()
                .addOnSuccessListener(doc -> {
                    if (!doc.exists()) return;
                    currentUser = doc.toObject(User.class);
                    updateUI();
                });
    }

    private void updateUI() {
        if (currentUser == null) return;
        binding.tvStoreName.setText(currentUser.getName());
        binding.tvPhone.setText(!TextUtils.isEmpty(currentUser.getPhone()) ? currentUser.getPhone() : "Not set");
        binding.tvAddress.setText(!TextUtils.isEmpty(currentUser.getAddress()) ? currentUser.getAddress() : "Address not set");

        if (!TextUtils.isEmpty(currentUser.getStoreImageBase64())) {
            Bitmap bitmap = ImageHelper.base64ToBitmap(currentUser.getStoreImageBase64());
            if (bitmap != null) {
                binding.ivStoreAvatar.setImageBitmap(bitmap);
                binding.ivStoreAvatar.setPadding(0, 0, 0, 0);
                binding.ivStoreAvatar.setScaleType(ImageView.ScaleType.CENTER_CROP);
                binding.ivStoreAvatar.setImageTintList(null);
            }
        }
    }

    private void showEditProfileModal() {
        if (currentUser == null) return;

        BottomSheetDialog dialog = new BottomSheetDialog(this, R.style.BottomSheetDialogTheme);
        View view = getLayoutInflater().inflate(R.layout.bottom_sheet_edit_profile, null);

        ivEditAvatar = view.findViewById(R.id.ivEditAvatar);
        TextView btnChangePhoto = view.findViewById(R.id.btnChangePhoto);
        TextInputEditText etName = view.findViewById(R.id.etEditName);
        TextInputEditText etPhone = view.findViewById(R.id.etEditPhone);
        etEditAddress = view.findViewById(R.id.etEditAddress);
        View btnUpdatePin = view.findViewById(R.id.btnUpdateLocation);
        View btnSave = view.findViewById(R.id.btnSaveProfile);

        // Reset temporary update state
        updatedLat = currentUser.getLatitude();
        updatedLng = currentUser.getLongitude();
        updatedAddress = currentUser.getAddress();

        // Populate
        etName.setText(currentUser.getName());
        etPhone.setText(currentUser.getPhone());
        etEditAddress.setText(currentUser.getAddress());
        encodedImage = currentUser.getStoreImageBase64();

        if (!TextUtils.isEmpty(encodedImage)) {
            Bitmap bitmap = ImageHelper.base64ToBitmap(encodedImage);
            if (bitmap != null) {
                ivEditAvatar.setImageBitmap(bitmap);
                ivEditAvatar.setPadding(0, 0, 0, 0);
                ivEditAvatar.setScaleType(ImageView.ScaleType.CENTER_CROP);
                ivEditAvatar.setImageTintList(null);
            }
        }

        btnChangePhoto.setOnClickListener(v -> pickerLauncher.launch("image/*"));

        btnUpdatePin.setOnClickListener(v -> {
            showRepinConfirmSheet();
        });

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
            updates.put("address", updatedAddress);
            updates.put("latitude", updatedLat);
            updates.put("longitude", updatedLng);
            updates.put("storeImageBase64", encodedImage);

            FirebaseHelper.getDb().collection("users").document(sellerId)
                    .update(updates)
                    .addOnSuccessListener(aVoid -> {
                        currentUser.setName(newName);
                        currentUser.setPhone(newPhone);
                        currentUser.setAddress(updatedAddress);
                        currentUser.setLatitude(updatedLat);
                        currentUser.setLongitude(updatedLng);
                        currentUser.setStoreImageBase64(encodedImage);
                        updateUI();
                        dialog.dismiss();
                        Toast.makeText(this, "Store profile updated!", Toast.LENGTH_SHORT).show();
                    })
                    .addOnFailureListener(e -> {
                        btnSave.setEnabled(true);
                        Toast.makeText(this, "Update failed.", Toast.LENGTH_SHORT).show();
                    });
        });

        dialog.setContentView(view);
        dialog.show();
    }

    private void showRepinConfirmSheet() {
        BottomSheetDialog confirmSheet = new BottomSheetDialog(this, R.style.BottomSheetStyle);
        View v = getLayoutInflater().inflate(R.layout.bottom_sheet_repin_confirm, null);
        confirmSheet.setContentView(v);

        v.findViewById(R.id.btnConfirm).setOnClickListener(view -> {
            confirmSheet.dismiss();
            Intent intent = new Intent(this, SellerPinRegistrationActivity.class);
            pinMapLauncher.launch(intent);
        });

        v.findViewById(R.id.btnCancel).setOnClickListener(view -> confirmSheet.dismiss());
        confirmSheet.show();
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
            binding.scrollContent.smoothScrollTo(0, binding.tvMyMenuLabel.getTop());
        });
        sheet.show();
    }

    private void listenMenu() {
        menuListener = FirebaseHelper.getDb().collection("products").whereEqualTo("sellerId", sellerId)
                .addSnapshotListener((snap, e) -> {
                    if (e != null || snap == null) return;
                    menuItems.clear();
                    for (QueryDocumentSnapshot doc : snap) {
                        Product p = doc.toObject(Product.class);
                        p.setProductId(doc.getId());
                        menuItems.add(p);
                    }
                    menuAdapter.notifyDataSetChanged();
                    binding.emptyMenu.setVisibility(menuItems.isEmpty() ? View.VISIBLE : View.GONE);
                });
    }

    private void onToggle(Product product, boolean available) {
        FirebaseHelper.getDb().collection("products").document(product.getProductId()).update("available", available);
    }

    private void onEdit(Product product) {
        startActivity(new Intent(this, AddProductActivity.class).putExtra("productId", product.getProductId()));
    }

    private void onDelete(Product product) {
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Delete Item").setMessage("Remove \"" + product.getName() + "\"?")
                .setPositiveButton("Delete", (d, w) -> FirebaseHelper.getDb().collection("products").document(product.getProductId()).delete())
                .setNegativeButton("Cancel", null).show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (menuListener != null) menuListener.remove();
    }
}