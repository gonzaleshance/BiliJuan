package com.appdev.bilijuan.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.View;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;


import com.appdev.bilijuan.activities.admin.AdminDashboardActivity;
import com.appdev.bilijuan.activities.customer.HomeActivity;
import com.appdev.bilijuan.activities.seller.SellerDashboardActivity;
import com.appdev.bilijuan.databinding.ActivityLoginBinding;
import com.appdev.bilijuan.models.User;
import com.appdev.bilijuan.utils.FirebaseHelper;
import com.appdev.bilijuan.utils.NetworkHelper;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuthException;

public class LoginActivity extends AppCompatActivity {

    private ActivityLoginBinding binding;

    // ── Google Sign-In Activity Result Launcher ──────────────────────────────
    private final ActivityResultLauncher<Intent> googleSignInLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(),
                    result -> {
                        if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                            handleGoogleSignInResult(result.getData());
                        }
                    });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.btnLogin.setOnClickListener(v -> attemptLogin());
        binding.tvRegister.setOnClickListener(v ->
                startActivity(new Intent(this, RegisterActivity.class)));
        binding.tvForgotPassword.setOnClickListener(v -> handleForgotPassword());
        binding.btnGoogle.setOnClickListener(v -> initiateGoogleSignIn());
    }

    // ── Google Sign-In Flow ───────────────────────────────────────────────────

    private void initiateGoogleSignIn() {
        if (!NetworkHelper.isOnline(this)) {
            NetworkHelper.showOfflineToast(this);
            return;
        }

        setLoading(true);
        Intent signInIntent = FirebaseHelper.getGoogleSignInClient(this).getSignInIntent();
        googleSignInLauncher.launch(signInIntent);
    }

    private void handleGoogleSignInResult(Intent data) {
        try {
            // Get the Google account from the result
            GoogleSignInAccount account = GoogleSignIn.getSignedInAccountFromIntent(data).getResult();
            if (account != null && account.getIdToken() != null) {
                authenticateWithFirebase(account);
            } else {
                setLoading(false);
                Toast.makeText(this, "Google Sign-In failed. Please try again.",
                        Toast.LENGTH_LONG).show();
            }
        } catch (Exception e) {
            setLoading(false);
            Toast.makeText(this, "Error: " + e.getMessage(),
                    Toast.LENGTH_LONG).show();
        }
    }

    private void authenticateWithFirebase(GoogleSignInAccount account) {
        AuthCredential credential = FirebaseHelper.getGoogleAuthCredential(account.getIdToken());

        FirebaseHelper.getAuth().signInWithCredential(credential)
                .addOnSuccessListener(authResult -> {
                    String uid = authResult.getUser().getUid();
                    String email = authResult.getUser().getEmail();
                    String displayName = authResult.getUser().getDisplayName();

                    // Check if user already exists
                    checkAndCreateGoogleUser(uid, email, displayName);
                })
                .addOnFailureListener(e -> {
                    setLoading(false);
                    Toast.makeText(this, "Authentication failed: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                });
    }

    private void checkAndCreateGoogleUser(String uid, String email, String displayName) {
        // Check if user document exists in Firestore
        FirebaseHelper.getDb().collection("users").document(uid).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        // User already exists, proceed to login
                        setLoading(false);
                        fetchUserAndRoute(uid);
                    } else {
                        // New user from Google Sign-In, proceed to role selection
                        navigateToGoogleRegister(uid, email, displayName);
                    }
                })
                .addOnFailureListener(e -> {
                    setLoading(false);
                    Toast.makeText(this, "Could not verify account. Please try again.",
                            Toast.LENGTH_SHORT).show();
                });
    }

    private void navigateToGoogleRegister(String uid, String email, String displayName) {
        // Pass user data to RegisterActivity for role selection
        Intent intent = new Intent(this, RegisterActivity.class);
        intent.putExtra("isGoogleSignIn", true);
        intent.putExtra("googleUid", uid);
        intent.putExtra("googleEmail", email);
        intent.putExtra("googleDisplayName", displayName != null ? displayName : "User");
        startActivity(intent);
        finish();
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

    // ── Email/Password Login ──────────────────────────────────────────────────

    private void attemptLogin() {

        if (!NetworkHelper.isOnline(this)) {
            NetworkHelper.showOfflineToast(this);
            return;
        }

        String email = getText(binding.etEmail).trim();
        String password = getText(binding.etPassword).trim();

        // Clear old errors
        binding.etEmail.setError(null);
        binding.etPassword.setError(null);

        boolean isValid = true;

        if (TextUtils.isEmpty(email)) {
            binding.etEmail.setError("Email is required");
            binding.etEmail.requestFocus();
            isValid = false;
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.etEmail.setError("Invalid email format");
            binding.etEmail.requestFocus();
            isValid = false;
        }

        if (TextUtils.isEmpty(password)) {
            binding.etPassword.setError("Password is required");
            binding.etPassword.requestFocus();
            isValid = false;
        } else if (password.length() < 6) {
            binding.etPassword.setError("Minimum 6 characters");
            binding.etPassword.requestFocus();
            isValid = false;
        }

        if (!isValid) return;

        setLoading(true);

        FirebaseHelper.getAuth()
                .signInWithEmailAndPassword(email, password)
                .addOnSuccessListener(result -> {
                    String uid = result.getUser().getUid();
                    fetchUserAndRoute(uid);
                })
                .addOnFailureListener(e -> {
                    setLoading(false);
                    String msg = e.getMessage() != null ? e.getMessage().toLowerCase() : "";

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
        binding.btnGoogle.setEnabled(!loading);
        binding.progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
    }
}