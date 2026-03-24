package com.appdev.bilijuan.activities;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseUser;
import com.appdev.bilijuan.activities.admin.AdminDashboardActivity;
import com.appdev.bilijuan.activities.customer.HomeActivity;
import com.appdev.bilijuan.activities.seller.SellerDashboardActivity;
import com.appdev.bilijuan.utils.FirebaseHelper;
import com.appdev.bilijuan.R;

public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        FirebaseUser currentUser = FirebaseHelper.getAuth().getCurrentUser();

        if (currentUser == null) {
            goTo(LoginActivity.class);
        } else {
            fetchRoleAndRoute(currentUser.getUid());
        }
    }

    private void fetchRoleAndRoute(String uid) {
        FirebaseHelper.getDb().collection("users").document(uid).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        routeByRole(doc.getString("role"));
                    } else {
                        FirebaseHelper.getAuth().signOut();
                        goTo(LoginActivity.class);
                    }
                })
                .addOnFailureListener(e -> {
                    FirebaseHelper.getAuth().signOut();
                    goTo(LoginActivity.class);
                });
    }

    private void routeByRole(String role) {
        if (role == null) { goTo(LoginActivity.class); return; }
        switch (role) {
            case "seller": goTo(SellerDashboardActivity.class); break;
            case "admin":  goTo(AdminDashboardActivity.class);  break;
            default:       goTo(HomeActivity.class);            break;
        }
    }

    private void goTo(Class<?> destination) {
        startActivity(new Intent(this, destination));
        finish();
    }
}