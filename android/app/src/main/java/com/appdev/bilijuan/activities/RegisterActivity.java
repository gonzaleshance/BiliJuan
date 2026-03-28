package com.appdev.bilijuan.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Patterns;
import android.view.View;
import android.widget.AdapterView;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityRegisterBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setupSubdivisionSpinner();
        setupRoleToggle();
        setupAddressAutoFill();

        binding.btnRegister.setOnClickListener(v -> attemptRegister());
        binding.tvLogin.setOnClickListener(v -> finish());
    }

    // ── UI setup ──────────────────────────────────────────────────────────────

    private void setupSubdivisionSpinner() {
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                this,
                R.array.saranay_subdivisions,
                android.R.layout.simple_spinner_item
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.spinnerSubdivision.setAdapter(adapter);

        binding.spinnerSubdivision.setOnItemSelectedListener(
                new AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(AdapterView<?> parent,
                                               View view, int position, long id) {
                        rebuildAddress();
                    }
                    @Override
                    public void onNothingSelected(AdapterView<?> parent) {}
                });
    }

    private void setupRoleToggle() {
        // Customer is selected by default
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

    /**
     * Watches Block, Lot, Street, Landmark fields and auto-builds
     * the Full Address field so users see a live preview.
     * Example: "Block 4, Lot 12, Ilang-Ilang St., Saranay Homes,
     *           Saranay Rd, Barangay 171, Caloocan"
     */
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
        String subdivision = binding.spinnerSubdivision.getSelectedItem() != null
                ? binding.spinnerSubdivision.getSelectedItem().toString() : "";

        StringBuilder sb = new StringBuilder();
        if (!block.isEmpty())  sb.append("Block ").append(block);
        if (!lot.isEmpty())    sb.append(sb.length() > 0 ? ", Lot " : "Lot ").append(lot);
        if (!street.isEmpty()) sb.append(sb.length() > 0 ? ", " : "").append(street);
        if (!subdivision.isEmpty() && !subdivision.equals("Other / Not Listed"))
            sb.append(sb.length() > 0 ? ", " : "").append(subdivision);
        sb.append(sb.length() > 0 ? ", " : "")
                .append("Saranay Rd, Barangay 171, Caloocan, Metro Manila");
        if (!landmark.isEmpty())
            sb.append(" (near ").append(landmark).append(")");

        binding.etAddress.setText(sb.toString());
        // Move cursor to end so user can still edit
        if (binding.etAddress.getText() != null)
            binding.etAddress.setSelection(binding.etAddress.getText().length());
    }

    // ── Register flow ─────────────────────────────────────────────────────────

    private void attemptRegister() {
        String name        = getText(binding.etName);
        String email       = getText(binding.etEmail);
        String phone       = getText(binding.etPhone);
        String pass        = getText(binding.etPassword);
        String block       = getText(binding.etBlock);
        String lot         = getText(binding.etLot);
        String street      = getText(binding.etStreet);
        String landmark    = getText(binding.etLandmark);
        String address     = getText(binding.etAddress);
        String subdivision = binding.spinnerSubdivision.getSelectedItem().toString();

        // ── Validation ──────────────────────────────────────────────────────
        if (TextUtils.isEmpty(name)) {
            binding.etName.setError("Full name is required");
            binding.etName.requestFocus(); return;
        }
        if (TextUtils.isEmpty(email) || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.etEmail.setError("Enter a valid email address");
            binding.etEmail.requestFocus(); return;
        }
        if (TextUtils.isEmpty(phone) || phone.length() < 10) {
            binding.etPhone.setError("Enter a valid Philippine phone number");
            binding.etPhone.requestFocus(); return;
        }
        if (TextUtils.isEmpty(pass)) {
            binding.etPassword.setError("Password is required");
            binding.etPassword.requestFocus(); return;
        }
        if (pass.length() < 6) {
            binding.etPassword.setError("Password must be at least 6 characters");
            binding.etPassword.requestFocus(); return;
        }
        if (TextUtils.isEmpty(block) && TextUtils.isEmpty(lot)) {
            binding.etBlock.setError("Enter at least a Block or Lot number");
            binding.etBlock.requestFocus(); return;
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
                    saveUserToFirestore(
                            uid, name, email, phone,
                            subdivision, block, lot, street, landmark, address
                    );
                })
                .addOnFailureListener(e -> {
                    setLoading(false);
                    String msg = e.getMessage() != null ? e.getMessage() : "";
                    if (msg.contains("email address is already")) {
                        Toast.makeText(this,
                                "An account with this email already exists.",
                                Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(this,
                                "Registration failed. Please try again.",
                                Toast.LENGTH_LONG).show();
                    }
                });
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
                    Toast.makeText(this,
                            "Welcome to BiliJuan, " + name + "! 🎉",
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
                    // Firestore failed — delete orphaned Auth account so user can retry
                    if (FirebaseHelper.getCurrentUser() != null) {
                        FirebaseHelper.getCurrentUser().delete();
                    }
                    Toast.makeText(this,
                            "Account setup failed. Please try again.",
                            Toast.LENGTH_LONG).show();
                });
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private String getText(com.google.android.material.textfield.TextInputEditText field) {
        return field.getText() != null ? field.getText().toString().trim() : "";
    }

    private void setLoading(boolean loading) {
        binding.btnRegister.setEnabled(!loading);
        binding.progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
    }
}