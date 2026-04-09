package com.appdev.bilijuan.activities.seller;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.appdev.bilijuan.databinding.ActivityEditProfileBinding;
import com.appdev.bilijuan.models.User;
import com.appdev.bilijuan.utils.FirebaseHelper;

import java.util.HashMap;
import java.util.Map;

public class EditSellerProfileActivity extends AppCompatActivity {

    private ActivityEditProfileBinding binding;
    private String currentUid;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityEditProfileBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        currentUid = FirebaseHelper.getCurrentUid();
        if (currentUid == null) { finish(); return; }

        binding.btnBack.setOnClickListener(v -> finish());
        binding.btnSave.setOnClickListener(v -> saveProfile());

        loadProfile();
    }

    private void loadProfile() {
        FirebaseHelper.getDb().collection("users").document(currentUid).get()
                .addOnSuccessListener(doc -> {
                    if (!doc.exists()) return;
                    User u = doc.toObject(User.class);
                    if (u == null) return;
                    binding.etName.setText(u.getName());
                    binding.etPhone.setText(u.getPhone());
                    binding.etAddress.setText(u.getAddress());
                });
    }

    private void saveProfile() {
        String name    = binding.etName.getText() != null
                ? binding.etName.getText().toString().trim() : "";
        String phone   = binding.etPhone.getText() != null
                ? binding.etPhone.getText().toString().trim() : "";
        String address = binding.etAddress.getText() != null
                ? binding.etAddress.getText().toString().trim() : "";

        if (TextUtils.isEmpty(name)) {
            binding.etName.setError("Name is required");
            binding.etName.requestFocus(); return;
        }
        if (TextUtils.isEmpty(phone)) {
            binding.etPhone.setError("Phone is required");
            binding.etPhone.requestFocus(); return;
        }

        setLoading(true);
        Map<String, Object> updates = new HashMap<>();
        updates.put("name", name);
        updates.put("phone", phone);
        updates.put("address", address);

        FirebaseHelper.getDb().collection("users").document(currentUid)
                .update(updates)
                .addOnSuccessListener(v -> {
                    setLoading(false);
                    Toast.makeText(this, "Store info updated!", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    setLoading(false);
                    Toast.makeText(this, "Update failed.", Toast.LENGTH_SHORT).show();
                });
    }

    private void setLoading(boolean loading) {
        binding.btnSave.setEnabled(!loading);
        binding.progressSave.setVisibility(loading ? View.VISIBLE : View.GONE);
    }
}