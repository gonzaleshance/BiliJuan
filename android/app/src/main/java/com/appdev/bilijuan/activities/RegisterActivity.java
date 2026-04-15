package com.appdev.bilijuan.activities;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Patterns;
import android.view.View;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.appdev.bilijuan.R;
import com.appdev.bilijuan.activities.customer.HomeActivity;
import com.appdev.bilijuan.activities.seller.SellerDashboardActivity;
import com.appdev.bilijuan.activities.seller.SellerPinRegistrationActivity;
import com.appdev.bilijuan.databinding.ActivityRegisterBinding;
import com.appdev.bilijuan.models.User;
import com.appdev.bilijuan.utils.FirebaseHelper;
import com.appdev.bilijuan.utils.SimpleTextWatcher;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;

public class RegisterActivity extends AppCompatActivity {

    // ── Request codes ─────────────────────────────────────────────────────────
    private static final int REQUEST_LOCATION_PERMISSION = 4001;
    private static final int REQUEST_PIN_LOCATION        = 4002;

    // ── State ─────────────────────────────────────────────────────────────────
    private ActivityRegisterBinding binding;
    private String selectedRole = "";
    private int    currentStep  = 1;
    private String base64Image  = "";

    // ── Google Sign-In State ───────────────────────────────────────────────────
    private boolean isGoogleSignIn = false;
    private String googleUid = "";
    private String googleEmail = "";
    private String googleDisplayName = "";

    // Seller store pin — must be set before registration is allowed
    private double storeLat = 0;
    private double storeLng = 0;

    // ── Image picker ──────────────────────────────────────────────────────────
    private final ActivityResultLauncher<Intent> imagePickerLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(),
                    result -> {
                        if (result.getResultCode() == RESULT_OK
                                && result.getData() != null) {
                            Uri imageUri = result.getData().getData();
                            try {
                                InputStream is = getContentResolver()
                                        .openInputStream(imageUri);
                                Bitmap bitmap = BitmapFactory.decodeStream(is);
                                binding.ivStoreLogo.setImageBitmap(bitmap);
                                binding.ivStoreLogo.setPadding(0, 0, 0, 0);
                                // Clear the tint so the actual image colors show
                                binding.ivStoreLogo.setImageTintList(null);
                                base64Image = encodeImage(bitmap);
                            } catch (Exception e) {
                                Toast.makeText(this, "Failed to load image",
                                        Toast.LENGTH_SHORT).show();
                            }
                        }
                    });

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityRegisterBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Handle Google Sign-In data if present
        handleGoogleSignInData();
        binding.etEmail.addTextChangedListener(new SimpleTextWatcher(() -> binding.etEmail.setError(null)));
        binding.etPassword.addTextChangedListener(new SimpleTextWatcher(() -> binding.etPassword.setError(null)));
        setupListeners();
        updateStepUI();
    }

    // ── Google Sign-In Data Handler ────────────────────────────────────────────

    private void handleGoogleSignInData() {
        isGoogleSignIn = getIntent().getBooleanExtra("isGoogleSignIn", false);
        if (isGoogleSignIn) {
            googleUid = getIntent().getStringExtra("googleUid");
            googleEmail = getIntent().getStringExtra("googleEmail");
            googleDisplayName = getIntent().getStringExtra("googleDisplayName");

            // Pre-fill the email from Google Sign-In
            binding.etEmail.setText(googleEmail);
            binding.etEmail.setEnabled(false); // Disable email editing for Google users
        }
    }

    // ── Listeners ─────────────────────────────────────────────────────────────

    private void setupListeners() {
        binding.btnBack.setOnClickListener(v -> handleBack());
        binding.tvLogin.setOnClickListener(v -> finish());

        binding.cardCustomer.setOnClickListener(v -> selectRole("customer"));
        binding.cardSeller.setOnClickListener(v -> selectRole("seller"));

        binding.btnNext.setOnClickListener(v -> {
            if (currentStep == 1) {
                if (!selectedRole.isEmpty()) nextStep();
                else Toast.makeText(this, "Please select a role",
                        Toast.LENGTH_SHORT).show();
            } else if (currentStep == 2) {
                if (validateStep2()) nextStep();
            } else if (currentStep == 3) {
                if (validateStep3()) attemptRegister();
            }
        });

        binding.btnSelectLogo.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK,
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            imagePickerLauncher.launch(intent);
        });

        // Seller map pin button
        binding.btnPinStoreLocation.setOnClickListener(v -> openPinMap());
    }

    // ── Role selection ────────────────────────────────────────────────────────

    private void selectRole(String role) {
        selectedRole = role;

        int primary = Color.parseColor("#27AE60");
        int grey    = Color.parseColor("#F1F2F6");
        boolean isCustomer = "customer".equals(role);

        binding.rbCustomer.setChecked(isCustomer);
        binding.rbSeller.setChecked(!isCustomer);

        binding.cardCustomer.setStrokeColor(isCustomer ? primary : grey);
        binding.cardSeller.setStrokeColor(!isCustomer ? primary : grey);

        binding.rbCustomer.setButtonTintList(ColorStateList.valueOf(primary));
        binding.rbSeller.setButtonTintList(ColorStateList.valueOf(primary));

        binding.btnNext.setVisibility(View.VISIBLE);
    }

    // ── Step navigation ───────────────────────────────────────────────────────

    private void handleBack() {
        if (currentStep > 1) { currentStep--; updateStepUI(); }
        else finish();
    }

    private void nextStep() {
        currentStep++;
        updateStepUI();
    }

    private void updateStepUI() {
        binding.btnBack.setVisibility(currentStep > 1 ? View.VISIBLE : View.GONE);
        binding.layoutLogin.setVisibility(currentStep == 1 ? View.VISIBLE : View.GONE);

        if (currentStep == 1) {
            binding.btnNext.setVisibility(
                    selectedRole.isEmpty() ? View.GONE : View.VISIBLE);
        } else {
            binding.btnNext.setVisibility(View.VISIBLE);
        }

        binding.step1Role.setVisibility(currentStep == 1 ? View.VISIBLE : View.GONE);
        binding.step2Credentials.setVisibility(currentStep == 2 ? View.VISIBLE : View.GONE);
        binding.step3Customer.setVisibility(
                (currentStep == 3 && "customer".equals(selectedRole))
                        ? View.VISIBLE : View.GONE);
        binding.step3Seller.setVisibility(
                (currentStep == 3 && "seller".equals(selectedRole))
                        ? View.VISIBLE : View.GONE);

        int progress = (currentStep * 100) / 3;
        binding.stepProgress.setProgress(progress, true);
        binding.stepProgress.setIndicatorColor(Color.parseColor("#27AE60"));

        switch (currentStep) {
            case 1:
                binding.tvStepTitle.setText("Choose your role");
                binding.tvStepSub.setText("Step 1 of 3");
                binding.btnNext.setText("Continue");
                break;
            case 2:
                binding.tvStepTitle.setText("Account Details");
                binding.tvStepSub.setText("Step 2 of 3");
                binding.btnNext.setText("Continue");

                // If Google Sign-In, hide password fields
                if (isGoogleSignIn) {
                    binding.tilPassword.setVisibility(View.GONE);
                    binding.tilConfirmPassword.setVisibility(View.GONE);
                } else {
                    binding.tilPassword.setVisibility(View.VISIBLE);
                    binding.tilConfirmPassword.setVisibility(View.VISIBLE);
                }
                break;
            case 3:
                binding.tvStepTitle.setText(
                        "customer".equals(selectedRole) ? "Your Profile" : "Store Details");
                binding.tvStepSub.setText("Step 3 of 3");
                binding.btnNext.setText("Create Account");
                break;
        }
    }

    // ── Pin Map Handler ───────────────────────────────────────────────────────

    private void openPinMap() {
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    REQUEST_LOCATION_PERMISSION);
        } else {
            checkGpsEnabledAndLaunchMap();
        }
    }

    private void checkGpsEnabledAndLaunchMap() {
        startActivityForResult(
                new Intent(this, SellerPinRegistrationActivity.class),
                REQUEST_PIN_LOCATION);
    }

    private void enforceSellerPinState() {
        binding.btnPinStoreLocation.setEnabled(true);
        binding.tvPinStoreStatus.setText("✓ Location Pinned");
        binding.tvPinStoreStatus.setTextColor(Color.parseColor("#27AE60"));
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_LOCATION_PERMISSION
                && grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            checkGpsEnabledAndLaunchMap();
        } else {
            Toast.makeText(this,
                    "Location permission is required to set your store location.",
                    Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode,
                                    @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_PIN_LOCATION
                && resultCode == Activity.RESULT_OK
                && data != null) {
            storeLat = data.getDoubleExtra("lat", 0);
            storeLng = data.getDoubleExtra("lng", 0);
            if (storeLat != 0 && storeLng != 0) {
                enforceSellerPinState();
                Toast.makeText(this, "Store location pinned ✓",
                        Toast.LENGTH_SHORT).show();
            }
        }
    }

    // ── Validation ────────────────────────────────────────────────────────────

    private boolean validateStep2() {

        String email = getText(binding.etEmail).trim();

        binding.etEmail.setError(null);
        binding.etPassword.setError(null);
        binding.etConfirmPassword.setError(null);

        if (TextUtils.isEmpty(email)) {
            binding.etEmail.setError("Email is required");
            binding.etEmail.requestFocus();
            return false;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.etEmail.setError("Enter a valid email address");
            binding.etEmail.requestFocus();
            return false;
        }

        if (!isGoogleSignIn) {
            String pass = getText(binding.etPassword);
            String confirmPass = getText(binding.etConfirmPassword);

            if (TextUtils.isEmpty(pass)) {
                binding.etPassword.setError("Password is required");
                binding.etPassword.requestFocus();
                return false;
            }

            if (pass.length() < 6) {
                binding.etPassword.setError("Minimum 6 characters");
                binding.etPassword.requestFocus();
                return false;
            }

            if (!pass.matches(".*[A-Z].*")) {
                binding.etPassword.setError("Must contain at least 1 uppercase letter");
                binding.etPassword.requestFocus();
                return false;
            }

            if (!pass.matches(".*[0-9].*")) {
                binding.etPassword.setError("Must contain at least 1 number");
                binding.etPassword.requestFocus();
                return false;
            }

            if (!pass.equals(confirmPass)) {
                binding.etConfirmPassword.setError("Passwords do not match");
                binding.etConfirmPassword.requestFocus();
                return false;
            }
        }

        return true;
    }

    private boolean validateStep3() {

        if ("customer".equals(selectedRole)) {

            String name = getText(binding.etName).trim();
            String phone = getText(binding.etPhone).trim();

            binding.etName.setError(null);
            binding.etPhone.setError(null);

            if (TextUtils.isEmpty(name)) {
                binding.etName.setError("Full name is required");
                binding.etName.requestFocus();
                return false;
            }

            if (!name.matches("^[a-zA-Z ]+$")) {
                binding.etName.setError("Only letters and spaces allowed");
                binding.etName.requestFocus();
                return false;
            }

            if (!phone.matches("^09\\d{9}$")) {
                binding.etPhone.setError("Invalid PH number (09123456789)");
                binding.etPhone.requestFocus();
                return false;
            }

        } else {

            String storeName = getText(binding.etStoreName).trim();
            String phone = getText(binding.etSellerPhone).trim();

            binding.etStoreName.setError(null);
            binding.etSellerPhone.setError(null);

            if (TextUtils.isEmpty(storeName)) {
                binding.etStoreName.setError("Store name is required");
                binding.etStoreName.requestFocus();
                return false;
            }

            if (!phone.matches("^09\\d{9}$")) {
                binding.etSellerPhone.setError("Invalid PH number");
                binding.etSellerPhone.requestFocus();
                return false;
            }

            if (TextUtils.isEmpty(base64Image)) {
                Toast.makeText(this, "Please upload a store logo",
                        Toast.LENGTH_SHORT).show();
                return false;
            }

            if (storeLat == 0 || storeLng == 0) {
                Toast.makeText(this,
                        "Please pin your store location first.",
                        Toast.LENGTH_LONG).show();
                return false;
            }
        }

        return true;
    }

    // ── Registration ──────────────────────────────────────────────────────────

    private void attemptRegister() {
        setLoading(true);

        if (isGoogleSignIn) {
            // Google Sign-In user: skip Firebase auth creation
            saveGoogleUserToFirestore(googleUid, googleEmail);
        } else {
            // Traditional email/password registration
            String email = getText(binding.etEmail);
            String pass  = getText(binding.etPassword);

            FirebaseHelper.getAuth()
                    .createUserWithEmailAndPassword(email, pass)
                    .addOnSuccessListener(result -> {
                        String uid = result.getUser().getUid();
                        saveUserToFirestore(uid, email);
                    })
                    .addOnFailureListener(e -> {
                        setLoading(false);
                        String msg = e.getMessage() != null ? e.getMessage() : "";
                        if (msg.contains("already")) {
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
    }

    private void saveGoogleUserToFirestore(String uid, String email) {
        String name, phone;
        if ("customer".equals(selectedRole)) {
            name  = getText(binding.etName);
            phone = getText(binding.etPhone);
        } else {
            name  = getText(binding.etStoreName);
            phone = getText(binding.etSellerPhone);
        }

        User user = new User(uid, name, email, selectedRole, phone, "");

        if ("seller".equals(selectedRole)) {
            user.setStoreImageBase64(base64Image);
            user.setLatitude(storeLat);
            user.setLongitude(storeLng);
        }

        FirebaseHelper.getDb().collection("users").document(uid)
                .set(user)
                .addOnSuccessListener(unused -> {
                    setLoading(false);
                    Toast.makeText(this,
                            "Welcome to BiliJuan, " + name + "!",
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
                    Toast.makeText(this,
                            "Account setup failed. Please try again.",
                            Toast.LENGTH_LONG).show();
                });
    }

    private void saveUserToFirestore(String uid, String email) {
        String name, phone;
        if ("customer".equals(selectedRole)) {
            name  = getText(binding.etName);
            phone = getText(binding.etPhone);
        } else {
            name  = getText(binding.etStoreName);
            phone = getText(binding.etSellerPhone);
        }

        User user = new User(uid, name, email, selectedRole, phone, "");

        if ("seller".equals(selectedRole)) {
            user.setStoreImageBase64(base64Image);
            user.setLatitude(storeLat);
            user.setLongitude(storeLng);
        }

        FirebaseHelper.getDb().collection("users").document(uid)
                .set(user)
                .addOnSuccessListener(unused -> {
                    setLoading(false);
                    Toast.makeText(this,
                            "Welcome to BiliJuan, " + name + "!",
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
                    Toast.makeText(this,
                            "Account setup failed. Please try again.",
                            Toast.LENGTH_LONG).show();
                });
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private String getText(
            com.google.android.material.textfield.TextInputEditText f) {
        return f.getText() != null ? f.getText().toString().trim() : "";
    }

    private void setLoading(boolean loading) {
        binding.btnNext.setEnabled(!loading);
        binding.btnBack.setEnabled(!loading);
        binding.progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
    }

    private String encodeImage(Bitmap bitmap) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 70, baos);
        byte[] bytes = baos.toByteArray();
        return Base64.encodeToString(bytes, Base64.DEFAULT);
    }
}