package com.appdev.bilijuan.activities;

import android.content.Intent;
import android.os.Bundle;
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
    }

    private void attemptLogin() {
        String email    = binding.etEmail.getText().toString().trim();
        String password = binding.etPassword.getText().toString().trim();

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please fill in all fields",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        setLoading(true);

        FirebaseHelper.getAuth()
                .signInWithEmailAndPassword(email, password)
                .addOnSuccessListener(result -> {
                    fetchRoleAndRoute(result.getUser().getUid());
                })
                .addOnFailureListener(e -> {
                    setLoading(false);
                    Toast.makeText(this, "Login failed: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                });
    }

    private void fetchRoleAndRoute(String uid) {
        FirebaseHelper.getDb().collection("users").document(uid).get()
                .addOnSuccessListener(doc -> {
                    setLoading(false);
                    routeByRole(doc.getString("role"));
                })
                .addOnFailureListener(e -> {
                    setLoading(false);
                    Toast.makeText(this, "Could not fetch user data.",
                            Toast.LENGTH_SHORT).show();
                });
    }

    private void routeByRole(String role) {
        Class<?> dest;
        if (role == null) dest = HomeActivity.class;
        else switch (role) {
            case "seller": dest = SellerDashboardActivity.class; break;
            case "admin":  dest = AdminDashboardActivity.class;  break;
            default:       dest = HomeActivity.class;            break;
        }
        startActivity(new Intent(this, dest));
        finish();
    }

    private void setLoading(boolean loading) {
        binding.btnLogin.setEnabled(!loading);
        binding.progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
    }
}