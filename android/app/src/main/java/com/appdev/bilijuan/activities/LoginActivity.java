package com.appdev.bilijuan.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.appdev.bilijuan.R;
import com.appdev.bilijuan.activities.admin.AdminDashboardActivity;
import com.appdev.bilijuan.activities.customer.HomeActivity;
import com.appdev.bilijuan.activities.seller.SellerDashboardActivity;
import com.appdev.bilijuan.databinding.ActivityLoginBinding;
import com.appdev.bilijuan.utils.FirebaseHelper;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.GoogleAuthProvider;

public class LoginActivity extends AppCompatActivity {

    private ActivityLoginBinding binding;
    private GoogleSignInClient googleSignInClient;
    private ActivityResultLauncher<Intent> googleSignInLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setupGoogleSignIn();

        binding.btnLogin.setOnClickListener(v -> attemptLogin());
        binding.btnGoogle.setOnClickListener(v -> startGoogleSignIn());
        
        binding.tvRegister.setOnClickListener(v ->
                startActivity(new Intent(this, RegisterActivity.class))
        );

        binding.tvForgotPassword.setOnClickListener(v -> handleForgotPassword());
    }

    // ── Google Sign-In Setup ──────────────────────────────────────────────────

    private void setupGoogleSignIn() {
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();

        googleSignInClient = GoogleSignIn.getClient(this, gso);

        googleSignInLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK) {
                        Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(result.getData());
                        try {
                            GoogleSignInAccount account = task.getResult(ApiException.class);
                            if (account != null) {
                                firebaseAuthWithGoogle(account.getIdToken());
                            }
                        } catch (ApiException e) {
                            Log.e("GoogleSignIn", "Google sign in failed", e);
                            Toast.makeText(this, "Google sign in failed", Toast.LENGTH_SHORT).show();
                        }
                    }
                }
        );
    }

    private void startGoogleSignIn() {
        setLoading(true);
        googleSignInLauncher.launch(googleSignInClient.getSignInIntent());
    }

    private void firebaseAuthWithGoogle(String idToken) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        FirebaseHelper.getAuth().signInWithCredential(credential)
                .addOnSuccessListener(result -> {
                    String uid = result.getUser().getUid();
                    boolean isNewUser = result.getAdditionalUserInfo() != null && result.getAdditionalUserInfo().isNewUser();
                    
                    if (isNewUser) {
                        // For new users, we still need them to complete the registration (Address, Role)
                        // Redirect to Register with a flag or pre-fill
                        Intent intent = new Intent(this, RegisterActivity.class);
                        intent.putExtra("is_google_signup", true);
                        intent.putExtra("email", result.getUser().getEmail());
                        intent.putExtra("name", result.getUser().getDisplayName());
                        startActivity(intent);
                        finish();
                    } else {
                        fetchRoleAndRoute(uid);
                    }
                })
                .addOnFailureListener(e -> {
                    setLoading(false);
                    Toast.makeText(this, "Authentication failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    // ── Email/Password Login ──────────────────────────────────────────────────

    private void attemptLogin() {
        String email    = getText(binding.etEmail);
        String password = getText(binding.etPassword);

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

        setLoading(true);

        FirebaseHelper.getAuth()
                .signInWithEmailAndPassword(email, password)
                .addOnSuccessListener(result -> {
                    String uid = result.getUser().getUid();
                    fetchRoleAndRoute(uid);
                })
                .addOnFailureListener(e -> {
                    setLoading(false);
                    Toast.makeText(this, "Login failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void fetchRoleAndRoute(String uid) {
        FirebaseHelper.getDb().collection("users").document(uid).get()
                .addOnSuccessListener(doc -> {
                    setLoading(false);
                    if (doc.exists()) {
                        routeByRole(doc.getString("role"));
                    } else {
                        // User exists in Auth but not in Firestore (e.g. Google user who didn't finish registration)
                        Intent intent = new Intent(this, RegisterActivity.class);
                        intent.putExtra("is_google_signup", true);
                        startActivity(intent);
                        finish();
                    }
                })
                .addOnFailureListener(e -> {
                    setLoading(false);
                    Toast.makeText(this, "Error fetching account details.", Toast.LENGTH_SHORT).show();
                });
    }

    private void routeByRole(String role) {
        Class<?> dest;
        if ("seller".equals(role)) {
            dest = SellerDashboardActivity.class;
        } else if ("admin".equals(role)) {
            dest = AdminDashboardActivity.class;
        } else {
            dest = HomeActivity.class;
        }
        startActivity(new Intent(this, dest));
        finish();
    }

    private void handleForgotPassword() {
        String email = getText(binding.etEmail);
        if (TextUtils.isEmpty(email) || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.etEmail.setError("Enter your email first");
            binding.etEmail.requestFocus();
            return;
        }
        FirebaseHelper.getAuth().sendPasswordResetEmail(email)
                .addOnSuccessListener(unused ->
                        Toast.makeText(this, "Reset link sent to " + email, Toast.LENGTH_LONG).show()
                )
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to send reset email.", Toast.LENGTH_LONG).show()
                );
    }

    private String getText(com.google.android.material.textfield.TextInputEditText field) {
        return field.getText() != null ? field.getText().toString().trim() : "";
    }

    private void setLoading(boolean loading) {
        binding.btnLogin.setEnabled(!loading);
        binding.btnGoogle.setEnabled(!loading);
        binding.progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
    }
}
