package com.appdev.bilijuan.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.appdev.bilijuan.R;
import com.appdev.bilijuan.activities.customer.HomeActivity;
import com.appdev.bilijuan.activities.seller.SellerDashboardActivity;
import com.appdev.bilijuan.databinding.ActivityRegisterBinding;
import com.appdev.bilijuan.models.User;
import com.appdev.bilijuan.utils.FirebaseHelper;

public class RegisterActivity extends AppCompatActivity {

    private ActivityRegisterBinding binding;
    private String selectedRole = "customer";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityRegisterBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setupRoleToggle();
        binding.btnRegister.setOnClickListener(v -> attemptRegister());
        binding.tvLogin.setOnClickListener(v -> finish());
    }

    private void setupRoleToggle() {
        binding.btnRoleCustomer.setOnClickListener(v -> {
            selectedRole = "customer";
            binding.btnRoleCustomer.setBackgroundResource(R.drawable.bg_role_selected);
            binding.btnRoleSeller.setBackgroundResource(R.drawable.bg_role_unselected);
        });
        binding.btnRoleSeller.setOnClickListener(v -> {
            selectedRole = "seller";
            binding.btnRoleSeller.setBackgroundResource(R.drawable.bg_role_selected);
            binding.btnRoleCustomer.setBackgroundResource(R.drawable.bg_role_unselected);
        });
    }

    private void attemptRegister() {
        String name    = getText(binding.etName);
        String email   = getText(binding.etEmail);
        String phone   = getText(binding.etPhone);
        String pass    = getText(binding.etPassword);
        String address = getText(binding.etAddress);

        if (TextUtils.isEmpty(name)) {
            binding.etName.setError("Full name is required");
            binding.etName.requestFocus(); return;
        }
        if (TextUtils.isEmpty(email) || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.etEmail.setError("Enter a valid email address");
            binding.etEmail.requestFocus(); return;
        }
        if (TextUtils.isEmpty(phone) || phone.length() < 10) {
            binding.etPhone.setError("Enter a valid phone number");
            binding.etPhone.requestFocus(); return;
        }
        if (TextUtils.isEmpty(pass) || pass.length() < 6) {
            binding.etPassword.setError("Password must be at least 6 characters");
            binding.etPassword.requestFocus(); return;
        }
        if (TextUtils.isEmpty(address)) {
            binding.etAddress.setError("Address is required");
            binding.etAddress.requestFocus(); return;
        }

        setLoading(true);

        FirebaseHelper.getAuth()
                .createUserWithEmailAndPassword(email, pass)
                .addOnSuccessListener(result -> {
                    String uid = result.getUser().getUid();
                    saveUserToFirestore(uid, name, email, phone, address);
                })
                .addOnFailureListener(e -> {
                    setLoading(false);
                    String msg = e.getMessage() != null ? e.getMessage() : "";
                    if (msg.contains("already")) {
                        Toast.makeText(this, "An account with this email already exists.",
                                Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(this, "Registration failed. Please try again.",
                                Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void saveUserToFirestore(String uid, String name, String email,
                                     String phone, String address) {
        User user = new User(uid, name, email, selectedRole, phone, address);

        FirebaseHelper.getDb().collection("users").document(uid)
                .set(user)
                .addOnSuccessListener(unused -> {
                    setLoading(false);
                    Toast.makeText(this, "Welcome to BiliJuan, " + name + "!",
                            Toast.LENGTH_SHORT).show();
                    if ("seller".equals(selectedRole)) {
                        startActivity(new Intent(this, SellerDashboardActivity.class));
                    } else {
                        startActivity(new Intent(this, HomeActivity.class));
                    }
                    finish();
                })
                .addOnFailureListener(e -> {
                    setLoading(false);
                    if (FirebaseHelper.getCurrentUser() != null)
                        FirebaseHelper.getCurrentUser().delete();
                    Toast.makeText(this, "Account setup failed. Please try again.",
                            Toast.LENGTH_LONG).show();
                });
    }

    private String getText(com.google.android.material.textfield.TextInputEditText f) {
        return f.getText() != null ? f.getText().toString().trim() : "";
    }

    private void setLoading(boolean loading) {
        binding.btnRegister.setEnabled(!loading);
        binding.progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
    }
}