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
import com.appdev.bilijuan.models.User;
import com.appdev.bilijuan.utils.FirebaseHelper;
import com.appdev.bilijuan.utils.NetworkHelper;

public class LoginActivity extends AppCompatActivity {

    private ActivityLoginBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.btnLogin.setOnClickListener(v -> attemptLogin());
        binding.tvRegister.setOnClickListener(v ->
                startActivity(new Intent(this, RegisterActivity.class)));
        binding.tvForgotPassword.setOnClickListener(v -> handleForgotPassword());
    }

    private void attemptLogin() {

        // Example usage in any Activity:
        if (!NetworkHelper.isOnline(this)) {
            NetworkHelper.showOfflineToast(this);
            return;
        }

        String email    = getText(binding.etEmail);
        String password = getText(binding.etPassword);

        if (TextUtils.isEmpty(email)) {
            binding.etEmail.setError("Email is required");
            binding.etEmail.requestFocus(); return;
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.etEmail.setError("Enter a valid email address");
            binding.etEmail.requestFocus(); return;
        }
        if (TextUtils.isEmpty(password)) {
            binding.etPassword.setError("Password is required");
            binding.etPassword.requestFocus(); return;
        }
        if (password.length() < 6) {
            binding.etPassword.setError("Password must be at least 6 characters");
            binding.etPassword.requestFocus(); return;
        }

        setLoading(true);

        FirebaseHelper.getAuth()
                .signInWithEmailAndPassword(email, password)
                .addOnSuccessListener(result -> {
                    String uid = result.getUser().getUid();
                    fetchUserAndRoute(uid);
                })
                .addOnFailureListener(e -> {
                    setLoading(false);
                    String msg = e.getMessage() != null ? e.getMessage() : "";
                    if (msg.contains("password") || msg.contains("credential")) {
                        Toast.makeText(this, "Incorrect email or password.",
                                Toast.LENGTH_LONG).show();
                    } else if (msg.contains("no user")) {
                        Toast.makeText(this, "No account found with that email.",
                                Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(this, "Login failed. Please try again.",
                                Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void fetchUserAndRoute(String uid) {
        FirebaseHelper.getDb().collection("users").document(uid).get()
                .addOnSuccessListener(doc -> {
                    setLoading(false);
                    if (!doc.exists()) {
                        FirebaseHelper.signOut();
                        Toast.makeText(this, "Account data not found. Please register again.",
                                Toast.LENGTH_LONG).show();
                        return;
                    }

                    User user = doc.toObject(User.class);
                    if (user == null) return;

                    // ── Status check ──────────────────────────────────────────
                    String status = user.getStatus();
                    if ("disabled".equals(status)) {
                        FirebaseHelper.signOut();
                        Intent intent = new Intent(this, DisabledAccountActivity.class);
                        intent.putExtra("reason", user.getDisableReason());
                        intent.putExtra("note",   user.getDisableNote());
                        intent.putExtra("type",   "disabled");
                        startActivity(intent);
                        finish();
                        return;
                    }
                    if ("archived".equals(status)) {
                        FirebaseHelper.signOut();
                        Intent intent = new Intent(this, DisabledAccountActivity.class);
                        intent.putExtra("type", "archived");
                        startActivity(intent);
                        finish();
                        return;
                    }

                    // Save FCM token
                    saveFcmToken(uid);

                    // Route by role
                    routeByRole(user.getRole());
                })
                .addOnFailureListener(e -> {
                    setLoading(false);
                    Toast.makeText(this, "Could not fetch account. Check your connection.",
                            Toast.LENGTH_SHORT).show();
                });
    }

    private void routeByRole(String role) {
        Class<?> dest;
        if (role == null) { dest = HomeActivity.class; }
        else switch (role) {
            case "seller": dest = SellerDashboardActivity.class; break;
            case "admin":  dest = AdminDashboardActivity.class;  break;
            default:       dest = HomeActivity.class;            break;
        }
        startActivity(new Intent(this, dest));
        finish();
    }

    private void saveFcmToken(String uid) {
        com.google.firebase.messaging.FirebaseMessaging.getInstance().getToken()
                .addOnSuccessListener(token ->
                        FirebaseHelper.getDb().collection("users").document(uid)
                                .update("fcmToken", token));
    }

    private void handleForgotPassword() {
        String email = getText(binding.etEmail);
        if (TextUtils.isEmpty(email) || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.etEmail.setError("Enter your email first");
            binding.etEmail.requestFocus(); return;
        }
        FirebaseHelper.getAuth().sendPasswordResetEmail(email)
                .addOnSuccessListener(v ->
                        Toast.makeText(this, "Reset link sent to " + email,
                                Toast.LENGTH_LONG).show())
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to send reset email.",
                                Toast.LENGTH_LONG).show());
    }

    private String getText(com.google.android.material.textfield.TextInputEditText f) {
        return f.getText() != null ? f.getText().toString().trim() : "";
    }

    private void setLoading(boolean loading) {
        binding.btnLogin.setEnabled(!loading);
        binding.progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
    }
}