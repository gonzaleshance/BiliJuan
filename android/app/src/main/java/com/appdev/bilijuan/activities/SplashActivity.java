package com.appdev.bilijuan.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;

import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;

import com.appdev.bilijuan.activities.DisabledAccountActivity;
import com.appdev.bilijuan.models.User;
import com.appdev.bilijuan.activities.admin.AdminDashboardActivity;
import com.appdev.bilijuan.activities.customer.HomeActivity;
import com.appdev.bilijuan.activities.seller.SellerDashboardActivity;
import com.appdev.bilijuan.adapters.OnboardingAdapter;
import com.appdev.bilijuan.databinding.ActivitySplashBinding;
import com.appdev.bilijuan.utils.FirebaseHelper;
import com.google.firebase.auth.FirebaseUser;

import java.util.ArrayList;
import java.util.List;

public class SplashActivity extends AppCompatActivity {

    private ActivitySplashBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Already logged in — skip onboarding, go straight to dashboard
        FirebaseUser currentUser = FirebaseHelper.getAuth().getCurrentUser();
        if (currentUser != null) {
            fetchRoleAndRoute(currentUser.getUid());
            return;
        }

        // Inflate layout via ViewBinding
        binding = ActivitySplashBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setupOnboarding();

        binding.btnGetStarted.setOnClickListener(v ->
                startActivity(new Intent(this, RegisterActivity.class))
        );

        binding.tvLogin.setOnClickListener(v ->
                startActivity(new Intent(this, LoginActivity.class))
        );
    }

    // ── Onboarding setup ─────────────────────────────────────────────────────

    private void setupOnboarding() {
        List<OnboardingAdapter.Slide> slides = new ArrayList<>();
        slides.add(new OnboardingAdapter.Slide(
                "🛒",
                "Bilijuan sa Saranay",
                "Order fresh homemade goods from your neighbors — delivered right to your door."
        ));
        slides.add(new OnboardingAdapter.Slide(
                "🍳",
                "Support Local Sellers",
                "From kakanin to gulayan, discover everyday staples made with love inside Saranay."
        ));
        slides.add(new OnboardingAdapter.Slide(
                "📍",
                "Track Every Delivery",
                "Watch your order move from the seller's kitchen all the way to your gate in real time."
        ));

        OnboardingAdapter adapter = new OnboardingAdapter(slides);
        binding.viewPagerOnboarding.setAdapter(adapter);

        buildDots(slides.size());
        updateDots(0, slides.size());

        binding.viewPagerOnboarding.registerOnPageChangeCallback(
                new ViewPager2.OnPageChangeCallback() {
                    @Override
                    public void onPageSelected(int position) {
                        updateDots(position, slides.size());
                        boolean isLast = position == slides.size() - 1;
                        binding.btnGetStarted.setVisibility(
                                isLast ? View.VISIBLE : View.INVISIBLE
                        );
                    }
                }
        );
    }

    // ── Dot indicators ────────────────────────────────────────────────────────

    private void buildDots(int count) {
        binding.dotsContainer.removeAllViews();
        for (int i = 0; i < count; i++) {
            View dot = new View(this);
            int sizePx = dpToPx(10);
            LinearLayout.LayoutParams params =
                    new LinearLayout.LayoutParams(sizePx, sizePx);
            params.setMargins(dpToPx(5), 0, dpToPx(5), 0);
            dot.setLayoutParams(params);
            dot.setBackgroundResource(com.appdev.bilijuan.R.drawable.bg_dot_inactive);
            binding.dotsContainer.addView(dot);
        }
    }

    private void updateDots(int activeIndex, int count) {
        for (int i = 0; i < count; i++) {
            View dot = binding.dotsContainer.getChildAt(i);
            if (dot == null) continue;
            dot.setBackgroundResource(
                    i == activeIndex
                            ? com.appdev.bilijuan.R.drawable.bg_dot_active
                            : com.appdev.bilijuan.R.drawable.bg_dot_inactive
            );
        }
    }

    private int dpToPx(int dp) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }

    // ── Role routing ──────────────────────────────────────────────────────────

    private void fetchRoleAndRoute(String uid) {
        FirebaseHelper.getDb().collection("users").document(uid).get()
                .addOnSuccessListener(doc -> {
                    if (!doc.exists()) {
                        FirebaseHelper.signOut();
                        goTo(LoginActivity.class);
                        return;
                    }
                    User user = doc.toObject(User.class);
                    if (user == null) { goTo(LoginActivity.class); return; }

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

                    routeByRole(user.getRole());
                })
                .addOnFailureListener(e -> goTo(LoginActivity.class));
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

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}