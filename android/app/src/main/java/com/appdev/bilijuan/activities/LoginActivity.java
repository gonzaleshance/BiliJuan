package com.appdev.bilijuan.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.appdev.bilijuan.activities.admin.AdminDashboardActivity;
import com.appdev.bilijuan.activities.customer.HomeActivity;
import com.appdev.bilijuan.activities.seller.SellerDashboardActivity;
import com.appdev.bilijuan.databinding.ActivityLoginBinding;
import com.appdev.bilijuan.utils.FirebaseHelper;

public class LoginActivity extends AppCompatActivity {

    private ActivityLoginBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.btnLogin.setOnClickListener(v -> attemptLogin());

        binding.tvRegister.setOnClickListener(v ->
                startActivity(new Intent(this, RegisterActivity.class))
        );

        // Forgot password — sends reset email
        binding.tvForgotPassword.setOnClickListener(v -> handleForgotPassword());

        // Google sign-in placeholder (wire up later with Credential Manager)
        binding.btnGoogle.setOnClickListener(v ->
                Toast.makeText(this, "Google sign-in coming soon!", Toast.LENGTH_SHORT).show()
        );
    }

    // ── Login flow ────────────────────────────────────────────────────────────

    private void attemptLogin() {
        String email    = getText(binding.etEmail);
        String password = getText(binding.etPassword);

        // Validate inputs
        if (TextUtils.isEmpty(email)) {
            binding.etEmail.setError("Email is required");
            binding.etEmail.requestFocus();
            return;
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.etEmail.setError("Enter a valid email address");
            binding.etEmail.requestFocus();
            return;
        }
        if (TextUtils.isEmpty(password)) {
            binding.etPassword.setError("Password is required");
            binding.etPassword.requestFocus();
            return;
        }
        if (password.length() < 6) {
            binding.etPassword.setError("Password must be at least 6 characters");
            binding.etPassword.requestFocus();
            return;
        }

        setLoading(true);

        FirebaseHelper.getAuth()
                .signInWithEmailAndPassword(email, password)
                .addOnSuccessListener(result -> {
                    String uid = result.getUser().getUid();
                    fetchRoleAndRoute(uid);
                })
                .addOnFailureListener(e -> {
                    setLoading(false);
                    String msg = e.getMessage();
                    if (msg != null && msg.contains("password")) {
                        Toast.makeText(this, "Incorrect password. Please try again.",
                                Toast.LENGTH_LONG).show();
                    } else if (msg != null && msg.contains("no user")) {
                        Toast.makeText(this, "No account found with that email.",
                                Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(this, "Login failed. Please try again.",
                                Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void fetchRoleAndRoute(String uid) {
        FirebaseHelper.getDb().collection("users").document(uid).get()
                .addOnSuccessListener(doc -> {
                    setLoading(false);
                    if (doc.exists()) {
                        routeByRole(doc.getString("role"));
                    } else {
                        // Account exists in Auth but not Firestore — sign out and warn
                        FirebaseHelper.signOut();
                        Toast.makeText(this,
                                "Account data not found. Please register again.",
                                Toast.LENGTH_LONG).show();
                    }
                })
                .addOnFailureListener(e -> {
                    setLoading(false);
                    Toast.makeText(this, "Could not fetch account. Check your connection.",
                            Toast.LENGTH_SHORT).show();
                });
    }

    private void routeByRole(String role) {
        Class<?> dest;
        if (role == null) {
            dest = HomeActivity.class;
        } else {
            switch (role) {
                case "seller": dest = SellerDashboardActivity.class; break;
                case "admin":  dest = AdminDashboardActivity.class;  break;
                default:       dest = HomeActivity.class;            break;
            }
        }
        startActivity(new Intent(this, dest));
        finish();
    }

    // ── Forgot password ───────────────────────────────────────────────────────

    private void handleForgotPassword() {
        String email = getText(binding.etEmail);
        if (TextUtils.isEmpty(email) || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.etEmail.setError("Enter your email first");
            binding.etEmail.requestFocus();
            return;
        }
        FirebaseHelper.getAuth().sendPasswordResetEmail(email)
                .addOnSuccessListener(unused ->
                        Toast.makeText(this,
                                "Reset link sent to " + email,
                                Toast.LENGTH_LONG).show()
                )
                .addOnFailureListener(e ->
                        Toast.makeText(this,
                                "Failed to send reset email. Check the address.",
                                Toast.LENGTH_LONG).show()
                );
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private String getText(com.google.android.material.textfield.TextInputEditText field) {
        return field.getText() != null ? field.getText().toString().trim() : "";
    }

    private void setLoading(boolean loading) {
        binding.btnLogin.setEnabled(!loading);
        binding.progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
    }
}