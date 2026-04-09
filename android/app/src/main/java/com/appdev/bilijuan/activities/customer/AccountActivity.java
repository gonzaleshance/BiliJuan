package com.appdev.bilijuan.activities.customer;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.appdev.bilijuan.R;
import com.appdev.bilijuan.activities.LoginActivity;
import com.appdev.bilijuan.databinding.ActivityAccountBinding;
import com.appdev.bilijuan.models.User;
import com.appdev.bilijuan.utils.FirebaseHelper;

import java.util.HashMap;
import java.util.Map;

public class AccountActivity extends AppCompatActivity {

    private ActivityAccountBinding binding;
    private String currentUid;
    private boolean isEditing = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAccountBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        currentUid = FirebaseHelper.getCurrentUid();
        if (currentUid == null) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        setupBottomNav();
        setupClickListeners();
        loadProfile();
    }

    // ── Bottom Nav ────────────────────────────────────────────────────────────

    private void setupBottomNav() {
        binding.bottomNav.setSelectedItemId(R.id.nav_profile);
        binding.bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_profile) return true;
            if (id == R.id.nav_home) {
                startActivity(new Intent(this, HomeActivity.class));
                overridePendingTransition(0, 0);
                finish(); return true;
            }
            if (id == R.id.nav_category) {
                startActivity(new Intent(this, CategoryActivity.class));
                overridePendingTransition(0, 0);
                finish(); return true;
            }
            if (id == R.id.nav_orders) {
                startActivity(new Intent(this, MyOrdersActivity.class));
                overridePendingTransition(0, 0);
                finish(); return true;
            }
            return false;
        });
    }

    // ── Click Listeners ───────────────────────────────────────────────────────

    private void setupClickListeners() {
        binding.btnBack.setOnClickListener(v -> finish());

        binding.btnEdit.setOnClickListener(v -> {
            if (isEditing) {
                saveProfile();
            } else {
                enterEditMode();
            }
        });

        binding.btnLogout.setOnClickListener(v -> {
            FirebaseHelper.signOut();
            Intent intent = new Intent(this, LoginActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        });
    }

    // ── Load Profile ──────────────────────────────────────────────────────────

    private void loadProfile() {
        binding.progressProfile.setVisibility(View.VISIBLE);
        binding.contentProfile.setVisibility(View.GONE);

        FirebaseHelper.getDb().collection("users").document(currentUid).get()
                .addOnSuccessListener(doc -> {
                    binding.progressProfile.setVisibility(View.GONE);
                    binding.contentProfile.setVisibility(View.VISIBLE);

                    if (!doc.exists()) {
                        Toast.makeText(this, "Profile not found.", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    User u = doc.toObject(User.class);
                    if (u == null) return;

                    binding.tvName.setText(u.getName());
                    binding.tvEmail.setText(u.getEmail());

                    // Editable fields
                    binding.etName.setText(u.getName());
                    binding.etPhone.setText(u.getPhone() != null ? u.getPhone() : "");
                    binding.etAddress.setText(u.getAddress() != null ? u.getAddress() : "");

                    // Display fields
                    binding.tvPhone.setText(
                            !TextUtils.isEmpty(u.getPhone()) ? u.getPhone() : "Not set");
                    binding.tvAddress.setText(
                            !TextUtils.isEmpty(u.getAddress()) ? u.getAddress() : "Not set");
                })
                .addOnFailureListener(e -> {
                    binding.progressProfile.setVisibility(View.GONE);
                    Toast.makeText(this, "Failed to load profile.", Toast.LENGTH_SHORT).show();
                });
    }

    // ── Edit Mode ─────────────────────────────────────────────────────────────

    private void enterEditMode() {
        isEditing = true;
        binding.viewMode.setVisibility(View.GONE);
        binding.editMode.setVisibility(View.VISIBLE);
        binding.btnEdit.setText("Save");
        binding.btnEdit.setBackgroundTintList(
                android.content.res.ColorStateList.valueOf(getColor(R.color.primary)));
        binding.btnEdit.setTextColor(getColor(R.color.on_primary));
    }

    private void exitEditMode() {
        isEditing = false;
        binding.viewMode.setVisibility(View.VISIBLE);
        binding.editMode.setVisibility(View.GONE);
        binding.btnEdit.setText("Edit");
        binding.btnEdit.setBackgroundTintList(
                android.content.res.ColorStateList.valueOf(getColor(R.color.surface)));
        binding.btnEdit.setTextColor(getColor(R.color.primary));
    }

    // ── Save Profile ──────────────────────────────────────────────────────────

    private void saveProfile() {
        String name    = binding.etName.getText() != null
                ? binding.etName.getText().toString().trim() : "";
        String phone   = binding.etPhone.getText() != null
                ? binding.etPhone.getText().toString().trim() : "";
        String address = binding.etAddress.getText() != null
                ? binding.etAddress.getText().toString().trim() : "";

        if (TextUtils.isEmpty(name)) {
            binding.etName.setError("Name is required");
            binding.etName.requestFocus();
            return;
        }

        binding.btnEdit.setEnabled(false);
        binding.btnEdit.setText("Saving...");

        Map<String, Object> updates = new HashMap<>();
        updates.put("name", name);
        updates.put("phone", phone);
        updates.put("address", address);

        FirebaseHelper.getDb().collection("users").document(currentUid)
                .update(updates)
                .addOnSuccessListener(v -> {
                    binding.btnEdit.setEnabled(true);
                    exitEditMode();
                    // Update display
                    binding.tvName.setText(name);
                    binding.tvPhone.setText(!TextUtils.isEmpty(phone) ? phone : "Not set");
                    binding.tvAddress.setText(!TextUtils.isEmpty(address) ? address : "Not set");
                    Toast.makeText(this, "Profile updated!", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    binding.btnEdit.setEnabled(true);
                    binding.btnEdit.setText("Save");
                    Toast.makeText(this, "Update failed. Try again.", Toast.LENGTH_SHORT).show();
                });
    }
}