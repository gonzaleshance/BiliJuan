package com.appdev.bilijuan.activities;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Patterns;
import android.view.View;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.appdev.bilijuan.R;
import com.appdev.bilijuan.activities.customer.HomeActivity;
import com.appdev.bilijuan.activities.seller.SellerDashboardActivity;
import com.appdev.bilijuan.databinding.ActivityRegisterBinding;
import com.appdev.bilijuan.models.User;
import com.appdev.bilijuan.utils.FirebaseHelper;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;

public class RegisterActivity extends AppCompatActivity {

    private ActivityRegisterBinding binding;
    private String selectedRole = "";
    private int currentStep = 1;
    private String base64Image = "";

    private final ActivityResultLauncher<Intent> imagePickerLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Uri imageUri = result.getData().getData();
                    try {
                        InputStream is = getContentResolver().openInputStream(imageUri);
                        Bitmap bitmap = BitmapFactory.decodeStream(is);
                        binding.ivStoreLogo.setImageBitmap(bitmap);
                        binding.ivStoreLogo.setPadding(0, 0, 0, 0); 
                        base64Image = encodeImage(bitmap);
                    } catch (Exception e) {
                        e.printStackTrace();
                        Toast.makeText(this, "Failed to load image", Toast.LENGTH_SHORT).show();
                    }
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityRegisterBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setupListeners();
        updateStepUI();
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
    }

    private void selectRole(String role) {
        selectedRole = role;
        
        // Hardcoded green color to prevent system dynamic color overrides
        int primary = Color.parseColor("#27AE60");
        int grey = Color.parseColor("#F1F2F6");

        boolean isCustomer = "customer".equals(role);
        
        // Update selection UI
        binding.rbCustomer.setChecked(isCustomer);
        binding.rbSeller.setChecked(!isCustomer);

        binding.cardCustomer.setStrokeColor(isCustomer ? primary : grey);
        binding.cardSeller.setStrokeColor(!isCustomer ? primary : grey);
        
        // Ensure radio buttons also use the hardcoded green
        binding.rbCustomer.setButtonTintList(ColorStateList.valueOf(primary));
        binding.rbSeller.setButtonTintList(ColorStateList.valueOf(primary));
        
        binding.btnNext.setVisibility(View.VISIBLE);
    }

    private void handleBack() {
        if (currentStep > 1) {
            currentStep--;
            updateStepUI();
        } else {
            finish();
        }
    }

    private void nextStep() {
        currentStep++;
        updateStepUI();
    }

    private void updateStepUI() {
        binding.btnBack.setVisibility(currentStep > 1 ? View.VISIBLE : View.GONE);
        binding.layoutLogin.setVisibility(currentStep == 1 ? View.VISIBLE : View.GONE);
        
        if (currentStep == 1) {
            binding.btnNext.setVisibility(selectedRole.isEmpty() ? View.GONE : View.VISIBLE);
        } else {
            binding.btnNext.setVisibility(View.VISIBLE);
        }

        binding.step1Role.setVisibility(currentStep == 1 ? View.VISIBLE : View.GONE);
        binding.step2Credentials.setVisibility(currentStep == 2 ? View.VISIBLE : View.GONE);
        binding.step3Customer.setVisibility((currentStep == 3 && "customer".equals(selectedRole)) ? View.VISIBLE : View.GONE);
        binding.step3Seller.setVisibility((currentStep == 3 && "seller".equals(selectedRole)) ? View.VISIBLE : View.GONE);

        // Update Progress Bar
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
                break;
            case 3:
                binding.tvStepTitle.setText("Profile Information");
                binding.tvStepSub.setText("Final Step");
                binding.btnNext.setText("Create Account");
                break;
        }
        
        // Hardcode Next button color in all steps
        binding.btnNext.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#27AE60")));
    }

    private boolean validateStep2() {
        String email = getText(binding.etEmail);
        String pass = getText(binding.etPassword);
        String confirmPass = getText(binding.etConfirmPassword);

        if (TextUtils.isEmpty(email) || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.etEmail.setError("Enter a valid email address");
            binding.etEmail.requestFocus();
            return false;
        }
        if (TextUtils.isEmpty(pass) || pass.length() < 6) {
            binding.etPassword.setError("Password must be at least 6 characters");
            binding.etPassword.requestFocus();
            return false;
        }
        if (!pass.equals(confirmPass)) {
            binding.etConfirmPassword.setError("Passwords do not match");
            binding.etConfirmPassword.requestFocus();
            return false;
        }
        return true;
    }

    private boolean validateStep3() {
        if ("customer".equals(selectedRole)) {
            String name = getText(binding.etName);
            String phone = getText(binding.etPhone);
            if (TextUtils.isEmpty(name)) {
                binding.etName.setError("Full name is required");
                binding.etName.requestFocus();
                return false;
            }
            if (TextUtils.isEmpty(phone) || phone.length() < 10) {
                binding.etPhone.setError("Enter a valid phone number");
                binding.etPhone.requestFocus();
                return false;
            }
        } else {
            String storeName = getText(binding.etStoreName);
            String phone = getText(binding.etSellerPhone);
            if (TextUtils.isEmpty(storeName)) {
                binding.etStoreName.setError("Store name is required");
                binding.etStoreName.requestFocus();
                return false;
            }
            if (TextUtils.isEmpty(phone) || phone.length() < 10) {
                binding.etSellerPhone.setError("Enter a valid phone number");
                binding.etSellerPhone.requestFocus();
                return false;
            }
            if (TextUtils.isEmpty(base64Image)) {
                Toast.makeText(this, "Please upload a store logo", Toast.LENGTH_SHORT).show();
                return false;
            }
        }
        return true;
    }

    private void attemptRegister() {
        String email = getText(binding.etEmail);
        String pass = getText(binding.etPassword);

        setLoading(true);

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
                        Toast.makeText(this, "An account with this email already exists.",
                                Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(this, "Registration failed. Please try again.",
                                Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void saveUserToFirestore(String uid, String email) {
        String name, phone;
        if ("customer".equals(selectedRole)) {
            name = getText(binding.etName);
            phone = getText(binding.etPhone);
        } else {
            name = getText(binding.etStoreName);
            phone = getText(binding.etSellerPhone);
        }

        User user = new User(uid, name, email, selectedRole, phone, "");
        if ("seller".equals(selectedRole)) {
            user.setStoreImageBase64(base64Image);
        }

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
