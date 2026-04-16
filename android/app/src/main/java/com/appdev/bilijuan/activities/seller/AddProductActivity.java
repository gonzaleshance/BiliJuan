package com.appdev.bilijuan.activities.seller;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.appdev.bilijuan.R;
import com.appdev.bilijuan.databinding.ActivityAddProductBinding;
import com.appdev.bilijuan.models.Product;
import com.appdev.bilijuan.models.User;
import com.appdev.bilijuan.utils.FirebaseHelper;
import com.appdev.bilijuan.utils.ImageHelper;
import com.google.android.material.bottomsheet.BottomSheetDialog;

public class AddProductActivity extends AppCompatActivity {

    private ActivityAddProductBinding binding;
    private String sellerId;
    private String sellerName;
    private String selectedImageBase64 = "";
    private String editProductId = null; // non-null when editing

    private final ActivityResultLauncher<String> imagePicker =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri == null) return;
                loadImage(uri);
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAddProductBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        sellerId = FirebaseHelper.getCurrentUid();
        if (sellerId == null) { finish(); return; }

        editProductId = getIntent().getStringExtra("productId");

        setupCategoryDropdown();
        setupClickListeners();
        loadSellerName();

        if (editProductId != null) {
            binding.tvTitle.setText("Edit Item");
            binding.btnPost.setText("Save Changes");
            loadProductForEdit();
        }
    }

    private void setupCategoryDropdown() {
        String[] categories = getResources().getStringArray(
                com.appdev.bilijuan.R.array.product_categories);
        
        // Using a custom layout for the dropdown items to ensure they are light-themed
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this, R.layout.item_dropdown_category, categories);
        
        binding.spinnerCategory.setAdapter(adapter);
        binding.spinnerCategory.setThreshold(0);
        binding.spinnerCategory.setOnClickListener(v -> binding.spinnerCategory.showDropDown());
    }

    private void setupClickListeners() {
        binding.btnBack.setOnClickListener(v -> finish());

        binding.cardImage.setOnClickListener(v ->
                imagePicker.launch("image/*"));

        binding.btnPost.setOnClickListener(v -> saveProduct());
    }

    private void loadSellerName() {
        FirebaseHelper.getDb().collection("users").document(sellerId).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        User u = doc.toObject(User.class);
                        if (u != null) sellerName = u.getName();
                    }
                });
    }

    private void loadProductForEdit() {
        FirebaseHelper.getDb().collection("products").document(editProductId).get()
                .addOnSuccessListener(doc -> {
                    if (!doc.exists()) return;
                    Product p = doc.toObject(Product.class);
                    if (p == null) return;

                    binding.etName.setText(p.getName());
                    binding.etDescription.setText(p.getDescription());
                    binding.etPrice.setText(String.valueOf(p.getPrice()));
                    binding.spinnerCategory.setText(p.getCategory(), false);

                    if (!TextUtils.isEmpty(p.getImageBase64())) {
                        selectedImageBase64 = p.getImageBase64();
                        Bitmap bm = ImageHelper.base64ToBitmap(selectedImageBase64);
                        if (bm != null) {
                            binding.ivProductImage.setImageBitmap(bm);
                            binding.ivProductImage.setVisibility(View.VISIBLE);
                            binding.layoutImagePlaceholder.setVisibility(View.GONE);
                        }
                    }
                });
    }

    private void loadImage(Uri uri) {
        binding.progressImage.setVisibility(View.VISIBLE);
        new Thread(() -> {
            String base64 = ImageHelper.uriToBase64(this, uri);
            runOnUiThread(() -> {
                binding.progressImage.setVisibility(View.GONE);
                if (base64 == null) {
                    Toast.makeText(this, "Failed to load image", Toast.LENGTH_SHORT).show();
                    return;
                }
                selectedImageBase64 = base64;
                Bitmap bm = ImageHelper.base64ToBitmap(base64);
                binding.ivProductImage.setImageBitmap(bm);
                binding.ivProductImage.setVisibility(View.VISIBLE);
                binding.layoutImagePlaceholder.setVisibility(View.GONE);
            });
        }).start();
    }

    private void saveProduct() {
        String name     = binding.etName.getText().toString().trim();
        String desc     = binding.etDescription.getText().toString().trim();
        String priceStr = binding.etPrice.getText().toString().trim();
        String category = binding.spinnerCategory.getText().toString().trim();

        if (TextUtils.isEmpty(name)) {
            binding.etName.setError("Name is required"); binding.etName.requestFocus(); return;
        }
        if (TextUtils.isEmpty(priceStr)) {
            binding.etPrice.setError("Price is required"); binding.etPrice.requestFocus(); return;
        }
        if (TextUtils.isEmpty(category)) {
            Toast.makeText(this, "Please select a category", Toast.LENGTH_SHORT).show(); return;
        }
        if (TextUtils.isEmpty(selectedImageBase64)) {
            Toast.makeText(this, "Please add a photo of your item", Toast.LENGTH_SHORT).show(); return;
        }

        double price;
        try { price = Double.parseDouble(priceStr); }
        catch (NumberFormatException ex) {
            binding.etPrice.setError("Invalid price"); return;
        }

        setLoading(true);

        if (editProductId != null) {
            // Update existing
            FirebaseHelper.getDb().collection("products").document(editProductId)
                    .update("name", name,
                            "description", desc,
                            "price", price,
                            "category", category,
                            "imageBase64", selectedImageBase64)
                    .addOnSuccessListener(v -> {
                        setLoading(false);
                        showSuccessSheet(true);
                    })
                    .addOnFailureListener(e -> { setLoading(false);
                        Toast.makeText(this, "Save failed", Toast.LENGTH_SHORT).show(); });
        } else {
            // Create new
            Product product = new Product(sellerId, sellerName != null ? sellerName : "",
                    name, desc, price, category);
            product.setImageBase64(selectedImageBase64);

            FirebaseHelper.getDb().collection("products").add(product)
                    .addOnSuccessListener(ref -> {
                        ref.update("productId", ref.getId());
                        setLoading(false);
                        showSuccessSheet(false);
                    })
                    .addOnFailureListener(e -> { setLoading(false);
                        Toast.makeText(this, "Post failed. Try again.", Toast.LENGTH_SHORT).show(); });
        }
    }

    private void showSuccessSheet(boolean isEdit) {
        BottomSheetDialog sheet = new BottomSheetDialog(this, R.style.BottomSheetStyle);
        View v = LayoutInflater.from(this).inflate(R.layout.layout_post_success, null);
        sheet.setContentView(v);
        sheet.setCancelable(false);

        if (isEdit) {
            ((TextView) v.findViewById(R.id.tvSuccessTitle)).setText("Product Updated!");
            ((TextView) v.findViewById(R.id.tvSuccessMessage)).setText("Your changes have been saved successfully.");
        }

        v.findViewById(R.id.btnDone).setOnClickListener(btn -> {
            sheet.dismiss();
            finish();
        });
        sheet.show();
    }

    private void setLoading(boolean loading) {
        binding.btnPost.setEnabled(!loading);
        binding.progressPost.setVisibility(loading ? View.VISIBLE : View.GONE);
        binding.btnPost.setText(loading ? "" : (editProductId != null ? "Save Changes" : "Post Item"));
    }
}