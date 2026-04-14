package com.appdev.bilijuan.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;

import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;

import com.appdev.bilijuan.R;
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

        FirebaseUser currentUser = FirebaseHelper.getAuth().getCurrentUser();
        if (currentUser != null) {
            fetchRoleAndRoute(currentUser.getUid());
            return;
        }

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

    private void setupOnboarding() {
        List<OnboardingAdapter.Slide> slides = new ArrayList<>();
        slides.add(new OnboardingAdapter.Slide(
                R.drawable.ic_onboarding_1,
                "Bilijuan Marketplace",
                "Order fresh homemade goods from your neighbors — delivered right to your door."
        ));
        slides.add(new OnboardingAdapter.Slide(
                R.drawable.ic_onboarding_2,
                "Support Local Sellers",
                "From kakanin to gulayan, discover everyday staples made with love by local cooks."
        ));
        slides.add(new OnboardingAdapter.Slide(
                R.drawable.ic_onboarding_3,
                "Track Every Delivery",
                "Watch your order move from the seller's kitchen all the way to your gate in real time."
        ));

        OnboardingAdapter adapter = new OnboardingAdapter(slides);
        binding.viewPagerOnboarding.setAdapter(adapter);

        // Page Transformer for smooth slide animations
        binding.viewPagerOnboarding.setPageTransformer((page, position) -> {
            page.setAlpha(0.5f + (1 - Math.abs(position)) * 0.5f);
            page.setScaleX(0.85f + (1 - Math.abs(position)) * 0.15f);
            page.setScaleY(0.85f + (1 - Math.abs(position)) * 0.15f);
        });

        buildDots(slides.size());
        updateDots(0, slides.size());

        binding.viewPagerOnboarding.registerOnPageChangeCallback(
                new ViewPager2.OnPageChangeCallback() {
                    @Override
                    public void onPageSelected(int position) {
                        updateDots(position, slides.size());
                        
                        // Animate Progress Bar
                        int progress = ((position + 1) * 100) / slides.size();
                        binding.onboardingProgress.setProgress(progress, true);

                        boolean isLast = position == slides.size() - 1;
                        if (isLast) {
                            binding.btnGetStarted.setAlpha(0f);
                            binding.btnGetStarted.setVisibility(View.VISIBLE);
                            binding.btnGetStarted.animate().alpha(1f).setDuration(400).start();
                        } else {
                            binding.btnGetStarted.setVisibility(View.INVISIBLE);
                        }
                    }
                }
        );
    }

    private void buildDots(int count) {
        binding.dotsContainer.removeAllViews();
        for (int i = 0; i < count; i++) {
            View dot = new View(this);
            int sizePx = dpToPx(8);
            LinearLayout.LayoutParams params =
                    new LinearLayout.LayoutParams(sizePx, sizePx);
            params.setMargins(dpToPx(6), 0, dpToPx(6), 0);
            dot.setLayoutParams(params);
            dot.setBackgroundResource(R.drawable.bg_dot_inactive);
            binding.dotsContainer.addView(dot);
        }
    }

    private void updateDots(int activeIndex, int count) {
        for (int i = 0; i < count; i++) {
            View dot = binding.dotsContainer.getChildAt(i);
            if (dot == null) continue;
            
            boolean isActive = i == activeIndex;
            dot.setBackgroundResource(isActive ? R.drawable.bg_dot_active : R.drawable.bg_dot_inactive);
            
            // Animate dot size
            float scale = isActive ? 1.2f : 1.0f;
            dot.animate().scaleX(scale).scaleY(scale).setDuration(200).start();
        }
    }

    private int dpToPx(int dp) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }

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
