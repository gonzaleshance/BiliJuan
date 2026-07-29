package com.appdev.bilijuan.activities;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.style.UnderlineSpan;
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

    private static final int REQUEST_LOCATION_PERMISSION = 4001;
    private static final int REQUEST_PIN_LOCATION        = 4002;

    private ActivityRegisterBinding binding;
    private String selectedRole = "";
    private int    currentStep  = 1;
    private String base64Image  = "";

    private boolean isGoogleSignIn = false;
    private String googleUid = "";
    private String googleEmail = "";
    private String googleDisplayName = "";

    private double storeLat = 0;
    private double storeLng = 0;
    private String storeAddress = "";

    private final ActivityResultLauncher<Intent> imagePickerLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(),
                    result -> {
                        if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                            Uri imageUri = result.getData().getData();
                            try {
                                InputStream is = getContentResolver().openInputStream(imageUri);
                                Bitmap bitmap = BitmapFactory.decodeStream(is);
                                binding.ivStoreLogo.setImageBitmap(bitmap);
                                binding.ivStoreLogo.setPadding(0, 0, 0, 0);
                                binding.ivStoreLogo.setImageTintList(null);
                                base64Image = encodeImage(bitmap);
                            } catch (Exception e) {
                                Toast.makeText(this, "Failed to load image", Toast.LENGTH_SHORT).show();
                            }
                        }
                    });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityRegisterBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        handleGoogleSignInData();
        binding.etEmail.addTextChangedListener(new SimpleTextWatcher(() -> binding.etEmail.setError(null)));
        binding.etPassword.addTextChangedListener(new SimpleTextWatcher(() -> binding.etPassword.setError(null)));
        setupListeners();
        updateStepUI();
        
        // Make Terms text underlined to look like a link
        SpannableString content = new SpannableString("I agree to the Terms and Conditions");
        content.setSpan(new UnderlineSpan(), 15, content.length(), 0);
        binding.tvTerms.setText(content);
    }

    private void handleGoogleSignInData() {
        isGoogleSignIn = getIntent().getBooleanExtra("isGoogleSignIn", false);
        if (isGoogleSignIn) {
            googleUid = getIntent().getStringExtra("googleUid");
            googleEmail = getIntent().getStringExtra("googleEmail");
            googleDisplayName = getIntent().getStringExtra("googleDisplayName");
            binding.etEmail.setText(googleEmail);
            binding.etEmail.setEnabled(false);
        }
    }

    private void setupListeners() {
        binding.btnBack.setOnClickListener(v -> handleBack());
        binding.tvLogin.setOnClickListener(v -> finish());
        binding.cardCustomer.setOnClickListener(v -> selectRole("customer"));
        binding.cardSeller.setOnClickListener(v -> selectRole("seller"));

        binding.btnNext.setOnClickListener(v -> {
            if (currentStep == 1) {
                if (!selectedRole.isEmpty()) nextStep();
                else Toast.makeText(this, "Please select a role", Toast.LENGTH_SHORT).show();
            } else if (currentStep == 2) {
                if (validateStep2()) nextStep();
            } else if (currentStep == 3) {
                if (validateStep3()) attemptRegister();
            }
        });

        binding.btnSelectLogo.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            imagePickerLauncher.launch(intent);
        });

        binding.btnPinStoreLocation.setOnClickListener(v -> openPinMap());
        binding.tvTerms.setOnClickListener(v -> showTermsDialog());
    }

    private void showTermsDialog() {
        String terms = "TERMS AND CONDITIONS\n\n" +
                "Welcome to our platform. By using this system, you agree to the following terms:\n\n" +
                "1. User Responsibility\n" +
                "Users must provide accurate and truthful information when registering and using the platform.\n\n" +
                "2. Food Quality and Listings\n" +
                "All sellers are responsible for ensuring that the food they list is safe and properly described.\n\n" +
                "3. Reporting System\n" +
                "Users may report unsafe food or misleading descriptions. All reports are reviewed by Admin.\n\n" +
                "4. Account Suspension Policy\n" +
                "Receiving THREE (3) or more valid reports will result in automatic account suspension.\n\n" +
                "5. Admin Rights\n" +
                "Admin has the authority to review reports, suspend accounts, and remove violating listings.\n\n" +
                "6. Abuse of Reporting\n" +
                "False reporting may result in account suspension.";

        new AlertDialog.Builder(this)
                .setTitle("Terms and Conditions")
                .setMessage(terms)
                .setPositiveButton("I Accept", (dialog, which) -> binding.cbTerms.setChecked(true))
                .setNegativeButton("Close", null)
                .show();
    }

    private void selectRole(String role) {
        selectedRole = role;
        int primary = Color.parseColor("#27AE60");
        int grey    = Color.parseColor("#F1F2F6");
        boolean isCustomer = "customer".equals(role);

        binding.rbCustomer.setChecked(isCustomer);
        binding.rbSeller.setChecked(!isCustomer);
        binding.cardCustomer.setStrokeColor(isCustomer ? primary : grey);
        binding.cardSeller.setStrokeColor(!isCustomer ? primary : grey);
        binding.btnNext.setVisibility(View.VISIBLE);
    }

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
        binding.btnNext.setVisibility(currentStep == 1 && selectedRole.isEmpty() ? View.GONE : View.VISIBLE);

        binding.step1Role.setVisibility(currentStep == 1 ? View.VISIBLE : View.GONE);
        binding.step2Credentials.setVisibility(currentStep == 2 ? View.VISIBLE : View.GONE);
        binding.step3Customer.setVisibility((currentStep == 3 && "customer".equals(selectedRole)) ? View.VISIBLE : View.GONE);
        binding.step3Seller.setVisibility((currentStep == 3 && "seller".equals(selectedRole)) ? View.VISIBLE : View.GONE);

        int progress = (currentStep * 100) / 3;
        binding.stepProgress.setProgress(progress, true);

        switch (currentStep) {
            case 1: binding.btnNext.setText("Continue"); break;
            case 2: binding.btnNext.setText("Continue"); 
                    if (isGoogleSignIn) {
                        binding.tilPassword.setVisibility(View.GONE);
                        binding.tilConfirmPassword.setVisibility(View.GONE);
                    }
                    break;
            case 3: binding.btnNext.setText("Create Account"); break;
        }
    }

    private void openPinMap() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_LOCATION_PERMISSION);
        } else {
            startActivityForResult(new Intent(this, SellerPinRegistrationActivity.class), REQUEST_PIN_LOCATION);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_PIN_LOCATION && resultCode == Activity.RESULT_OK && data != null) {
            storeLat = data.getDoubleExtra("lat", 0);
            storeLng = data.getDoubleExtra("lng", 0);
            storeAddress = data.getStringExtra("address");
            binding.tvPinStoreStatus.setText("✓ Location Pinned");
            binding.tvPinCoords.setVisibility(View.VISIBLE);
            binding.tvPinCoords.setText(storeAddress);
        }
    }

    private boolean validateStep2() {
        String email = getText(binding.etEmail).trim();
        if (TextUtils.isEmpty(email) || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.etEmail.setError("Valid email required");
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

            if (pass.length() < 8) {
                binding.etPassword.setError("Minimum 8 characters");
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

            if (!pass.matches(".*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?].*")) {
                binding.etPassword.setError("Must contain at least 1 special character");
                binding.etPassword.requestFocus();
                return false;
            }

            if (!pass.equals(confirmPass)) {
                binding.etConfirmPassword.setError("Passwords do not match");
                return false;
            }
        }
        if (!binding.cbTerms.isChecked()) {
            Toast.makeText(this, "Please agree to the terms to continue", Toast.LENGTH_SHORT).show();
            return false;
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

            if (name.length() > 50) {
                binding.etName.setError("Name too long (max 50)");
                binding.etName.requestFocus();
                return false;
            }

            if (!name.matches("^[a-zA-Z\\s\\-'.]+$")) {
                binding.etName.setError("Name contains invalid characters");
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

            if (storeName.length() > 50) {
                binding.etStoreName.setError("Store name too long (max 50)");
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

    private void attemptRegister() {
        setLoading(true);
        if (isGoogleSignIn) saveUserToFirestore(googleUid, googleEmail);
        else {
            FirebaseHelper.getAuth().createUserWithEmailAndPassword(getText(binding.etEmail), getText(binding.etPassword))
                    .addOnSuccessListener(authResult -> saveUserToFirestore(authResult.getUser().getUid(), getText(binding.etEmail)))
                    .addOnFailureListener(e -> { setLoading(false); Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show(); });
        }
    }

    private void saveUserToFirestore(String uid, String email) {
        String name = "customer".equals(selectedRole) ? getText(binding.etName) : getText(binding.etStoreName);
        String phone = "customer".equals(selectedRole) ? getText(binding.etPhone) : getText(binding.etSellerPhone);
        User user = new User(uid, name, email, selectedRole, phone, storeAddress);
        if ("seller".equals(selectedRole)) {
            user.setStoreImageBase64(base64Image);
            user.setLatitude(storeLat);
            user.setLongitude(storeLng);
        }
        FirebaseHelper.getDb().collection("users").document(uid).set(user)
                .addOnSuccessListener(v -> {
                    startActivity(new Intent(this, "seller".equals(selectedRole) ? SellerDashboardActivity.class : HomeActivity.class));
                    finish();
                });
    }

    private String getText(com.google.android.material.textfield.TextInputEditText f) { return f.getText().toString().trim(); }
    private void setLoading(boolean loading) { binding.progressBar.setVisibility(loading ? View.VISIBLE : View.GONE); }
    private String encodeImage(Bitmap b) { ByteArrayOutputStream baos = new ByteArrayOutputStream(); b.compress(Bitmap.CompressFormat.JPEG, 70, baos); return Base64.encodeToString(baos.toByteArray(), Base64.DEFAULT); }
}
