package com.appdev.bilijuan.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Patterns;
import android.view.View;
import android.widget.ArrayAdapter;
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
    private String selectedRole = "customer"; // default
    private boolean isGoogleSignUp = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityRegisterBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        isGoogleSignUp = getIntent().getBooleanExtra("is_google_signup", false);
        if (isGoogleSignUp) {
            handleGoogleSignUpData();
        }

        setupSubdivisionSpinner();
        setupRoleToggle();
        setupAddressAutoFill();

        binding.btnRegister.setOnClickListener(v -> attemptRegister());
        binding.tvLogin.setOnClickListener(v -> finish());
    }

    private void handleGoogleSignUpData() {
        String email = getIntent().getStringExtra("email");
        String name = getIntent().getStringExtra("name");

        if (email != null) {
            binding.etEmail.setText(email);
            binding.etEmail.setEnabled(false); // Email from Google is verified
        }
        if (name != null) {
            binding.etName.setText(name);
        }

        // Hide password field for Google users using its ID
        binding.tilPassword.setVisibility(View.GONE);
    }

    // ── UI setup ──────────────────────────────────────────────────────────────

    private void setupSubdivisionSpinner() {
        String[] subdivisions = getResources().getStringArray(R.array.saranay_subdivisions);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, 
                android.R.layout.simple_dropdown_item_1line, subdivisions);
        
        binding.spinnerSubdivision.setAdapter(adapter);
        binding.spinnerSubdivision.setOnItemClickListener((parent, view, position, id) -> rebuildAddress());
    }

    private void setupRoleToggle() {
        binding.btnRoleCustomer.setOnClickListener(v -> {
            selectedRole = "customer";
            updateRoleUI();
        });
        binding.btnRoleSeller.setOnClickListener(v -> {
            selectedRole = "seller";
            updateRoleUI();
        });
    }

    private void updateRoleUI() {
        if ("customer".equals(selectedRole)) {
            binding.btnRoleCustomer.setStrokeColor(android.content.res.ColorStateList.valueOf(getResources().getColor(R.color.primary)));
            binding.btnRoleCustomer.setCardBackgroundColor(getResources().getColor(R.color.primary_light));
            
            binding.btnRoleSeller.setStrokeColor(android.content.res.ColorStateList.valueOf(getResources().getColor(R.color.surface_container)));
            binding.btnRoleSeller.setCardBackgroundColor(getResources().getColor(R.color.surface));
        } else {
            binding.btnRoleSeller.setStrokeColor(android.content.res.ColorStateList.valueOf(getResources().getColor(R.color.primary)));
            binding.btnRoleSeller.setCardBackgroundColor(getResources().getColor(R.color.primary_light));
            
            binding.btnRoleCustomer.setStrokeColor(android.content.res.ColorStateList.valueOf(getResources().getColor(R.color.surface_container)));
            binding.btnRoleCustomer.setCardBackgroundColor(getResources().getColor(R.color.surface));
        }
    }

    private void setupAddressAutoFill() {
        TextWatcher watcher = new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
            @Override public void onTextChanged(CharSequence s, int st, int b, int c) {}
            @Override public void afterTextChanged(Editable s) { rebuildAddress(); }
        };
        binding.etBlock.addTextChangedListener(watcher);
        binding.etLot.addTextChangedListener(watcher);
        binding.etStreet.addTextChangedListener(watcher);
        binding.etLandmark.addTextChangedListener(watcher);
    }

    private void rebuildAddress() {
        String block       = getText(binding.etBlock);
        String lot         = getText(binding.etLot);
        String street      = getText(binding.etStreet);
        String landmark    = getText(binding.etLandmark);
        String subdivision = binding.spinnerSubdivision.getText().toString();

        StringBuilder sb = new StringBuilder();
        if (!block.isEmpty())  sb.append("Block ").append(block);
        if (!lot.isEmpty())    sb.append(sb.length() > 0 ? ", Lot " : "Lot ").append(lot);
        if (!street.isEmpty()) sb.append(sb.length() > 0 ? ", " : "").append(street);
        if (!subdivision.isEmpty())
            sb.append(sb.length() > 0 ? ", " : "").append(subdivision);
        
        sb.append(sb.length() > 0 ? ", " : "")
                .append("Saranay Rd, Barangay 171, Caloocan, Metro Manila");
        
        if (!landmark.isEmpty())
            sb.append(" (near ").append(landmark).append(")");

        binding.etAddress.setText(sb.toString());
    }

    // ── Register flow ─────────────────────────────────────────────────────────

    private void attemptRegister() {
        String name        = getText(binding.etName);
        String email       = getText(binding.etEmail);
        String phone       = getText(binding.etPhone);
        String pass        = getText(binding.etPassword);
        String subdivision = binding.spinnerSubdivision.getText().toString();

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
        if (!isGoogleSignUp && (TextUtils.isEmpty(pass) || pass.length() < 6)) {
            binding.etPassword.setError("Password must be at least 6 characters");
            binding.etPassword.requestFocus(); return;
        }
        if (TextUtils.isEmpty(subdivision)) {
            binding.spinnerSubdivision.setError("Select a subdivision");
            binding.spinnerSubdivision.requestFocus(); return;
        }

        setLoading(true);

        if (isGoogleSignUp) {
            // User already authenticated via Google
            saveUserToFirestore(
                    FirebaseHelper.getCurrentUser().getUid(), name, email, phone,
                    subdivision, getText(binding.etBlock), getText(binding.etLot), 
                    getText(binding.etStreet), getText(binding.etLandmark), getText(binding.etAddress)
            );
        } else {
            FirebaseHelper.getAuth()
                    .createUserWithEmailAndPassword(email, pass)
                    .addOnSuccessListener(result -> {
                        String uid = result.getUser().getUid();
                        saveUserToFirestore(
                                uid, name, email, phone,
                                subdivision, getText(binding.etBlock), getText(binding.etLot), 
                                getText(binding.etStreet), getText(binding.etLandmark), getText(binding.etAddress)
                        );
                    })
                    .addOnFailureListener(e -> {
                        setLoading(false);
                        Toast.makeText(this, "Registration failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    });
        }
    }

    private void saveUserToFirestore(String uid, String name, String email,
                                     String phone, String subdivision,
                                     String block, String lot,
                                     String street, String landmark,
                                     String address) {
        User user = new User(
                uid, name, email, selectedRole,
                phone, subdivision, block, lot, street, landmark, address
        );

        FirebaseHelper.getDb()
                .collection("users")
                .document(uid)
                .set(user)
                .addOnSuccessListener(unused -> {
                    setLoading(false);
                    Toast.makeText(this, "Welcome to BiliJuan, " + name + "!", Toast.LENGTH_SHORT).show();
                    Intent intent = "seller".equals(selectedRole) 
                            ? new Intent(this, SellerDashboardActivity.class) 
                            : new Intent(this, HomeActivity.class);
                    startActivity(intent);
                    finish();
                })
                .addOnFailureListener(e -> {
                    setLoading(false);
                    // For non-Google signups, delete auth account if Firestore fails
                    if (!isGoogleSignUp && FirebaseHelper.getCurrentUser() != null) {
                        FirebaseHelper.getCurrentUser().delete();
                    }
                    Toast.makeText(this, "Account setup failed. Please try again.", Toast.LENGTH_LONG).show();
                });
    }

    private String getText(com.google.android.material.textfield.TextInputEditText field) {
        return field.getText() != null ? field.getText().toString().trim() : "";
    }

    private void setLoading(boolean loading) {
        binding.btnRegister.setEnabled(!loading);
        binding.progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
    }
}
