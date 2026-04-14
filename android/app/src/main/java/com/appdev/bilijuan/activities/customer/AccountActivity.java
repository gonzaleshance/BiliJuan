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
import com.appdev.bilijuan.utils.CustomerNavHelper;
import com.appdev.bilijuan.utils.FirebaseHelper;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.textfield.TextInputEditText;

import java.util.HashMap;
import java.util.Map;

public class AccountActivity extends AppCompatActivity {

    private ActivityAccountBinding binding;
    private String currentUid;
    private User currentUser;

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

    private void setupBottomNav() {
        CustomerNavHelper.setup(this, binding.customerNav.getRoot(), CustomerNavHelper.Tab.PROFILE);
    }

    private void setupClickListeners() {
        binding.btnEdit.setOnClickListener(v -> showEditProfileModal());

        binding.btnLogout.setOnClickListener(v -> {
            FirebaseHelper.signOut();
            Intent intent = new Intent(this, LoginActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        });
    }

    private void loadProfile() {
        binding.progressProfile.setVisibility(View.VISIBLE);
        binding.contentProfile.setVisibility(View.GONE);

        FirebaseHelper.getDb().collection("users").document(currentUid).get()
                .addOnSuccessListener(doc -> {
                    binding.progressProfile.setVisibility(View.GONE);
                    binding.contentProfile.setVisibility(View.VISIBLE);

                    if (!doc.exists()) return;

                    currentUser = doc.toObject(User.class);
                    if (currentUser == null) return;

                    updateUI();
                })
                .addOnFailureListener(e -> {
                    binding.progressProfile.setVisibility(View.GONE);
                    Toast.makeText(this, "Failed to load profile.", Toast.LENGTH_SHORT).show();
                });
    }

    private void updateUI() {
        if (currentUser == null) return;
        binding.tvName.setText(currentUser.getName());
        binding.tvEmail.setText(currentUser.getEmail());
        binding.tvPhone.setText(!TextUtils.isEmpty(currentUser.getPhone()) ? currentUser.getPhone() : "Not set");
        binding.tvRole.setText(currentUser.getRole());
        binding.tvMemberSince.setText("2024"); // Static for now or parse from createdAt
    }

    private void showEditProfileModal() {
        if (currentUser == null) return;

        BottomSheetDialog dialog = new BottomSheetDialog(this, R.style.BottomSheetDialogTheme);
        View view = getLayoutInflater().inflate(R.layout.bottom_sheet_edit_profile, null);

        TextInputEditText etName = view.findViewById(R.id.etEditName);
        TextInputEditText etPhone = view.findViewById(R.id.etEditPhone);
        View btnSave = view.findViewById(R.id.btnSaveProfile);

        etName.setText(currentUser.getName());
        etPhone.setText(currentUser.getPhone());

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

            FirebaseHelper.getDb().collection("users").document(currentUid)
                    .update(updates)
                    .addOnSuccessListener(aVoid -> {
                        currentUser.setName(newName);
                        currentUser.setPhone(newPhone);
                        updateUI();
                        dialog.dismiss();
                        Toast.makeText(this, "Profile updated!", Toast.LENGTH_SHORT).show();
                    })
                    .addOnFailureListener(e -> {
                        btnSave.setEnabled(true);
                        Toast.makeText(this, "Update failed.", Toast.LENGTH_SHORT).show();
                    });
        });

        dialog.setContentView(view);
        dialog.show();
    }
}
