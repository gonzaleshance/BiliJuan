package com.appdev.bilijuan.activities;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.appdev.bilijuan.databinding.ActivityDisabledAccountBinding;
import com.appdev.bilijuan.utils.FirebaseHelper;

public class DisabledAccountActivity extends AppCompatActivity {

    private ActivityDisabledAccountBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityDisabledAccountBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        String type   = getIntent().getStringExtra("type");
        String reason = getIntent().getStringExtra("reason");
        String note   = getIntent().getStringExtra("note");

        if ("suspended".equals(type)) {
            binding.tvTitle.setText("Account Suspended");
            binding.tvMessage.setText("Your account has been suspended for violating platform policies.");
            binding.tvReason.setText(reason);
            binding.tvNote.setText(note);
            binding.tvReason.setVisibility(android.view.View.VISIBLE);
            binding.tvNote.setVisibility(android.view.View.VISIBLE);
        } else if ("archived".equals(type)) {
            binding.tvTitle.setText("Account Permanently Closed");
            binding.tvMessage.setText(
                    "This account has been permanently removed from BiliJuan. " +
                            "If you believe this is a mistake, please contact support.");
            binding.tvReason.setVisibility(android.view.View.GONE);
            binding.tvNote.setVisibility(android.view.View.GONE);
        } else {
            binding.tvTitle.setText("Account Disabled");
            binding.tvMessage.setText(
                    "Your account has been temporarily disabled by our admin team.");
            if (reason != null && !reason.isEmpty()) {
                binding.tvReason.setText("Reason: " + reason);
                binding.tvReason.setVisibility(android.view.View.VISIBLE);
            }
            if (note != null && !note.isEmpty()) {
                binding.tvNote.setText(note);
                binding.tvNote.setVisibility(android.view.View.VISIBLE);
            }
        }

        binding.btnBackToLogin.setOnClickListener(v -> {
            FirebaseHelper.signOut(this);
            Intent intent = new Intent(this, LoginActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        });
    }

    @Override
    public void onBackPressed() {
        // Prevent going back — force them to tap the button
    }
}
