package com.appdev.bilijuan.activities.customer;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.appcompat.app.AppCompatActivity;

import com.appdev.bilijuan.databinding.ActivityOrderSuccessBinding;

public class OrderSuccessActivity extends AppCompatActivity {

    private ActivityOrderSuccessBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityOrderSuccessBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        String orderId      = getIntent().getStringExtra("orderId");
        String productName  = getIntent().getStringExtra("productName");
        String total        = getIntent().getStringExtra("total");
        String storeName    = getIntent().getStringExtra("storeName");

        binding.tvProductName.setText(productName != null ? productName : "Your order");
        binding.tvStoreName.setText("from " + (storeName != null ? storeName : "the store"));
        binding.tvTotal.setText(total != null ? total : "");
        binding.tvOrderId.setText(orderId != null
                ? "#" + orderId.substring(0, Math.min(6, orderId.length())).toUpperCase()
                : "");

        // REMOVED: Auto-redirect to PinLocation since pinning is now done BEFORE summary.
        // Instead, point to Tracking or My Orders.
        binding.btnTrackOrder.setText("Track My Order");
        binding.btnTrackOrder.setOnClickListener(v -> {
            Intent intent = new Intent(this, OrderTrackingActivity.class);
            intent.putExtra("orderId", orderId);
            startActivity(intent);
            finish();
        });

        binding.btnBackHome.setOnClickListener(v -> {
            startActivity(new Intent(this, HomeActivity.class)
                    .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
            finish();
        });

        // Redirect to My Orders after 5 seconds instead of PinLocation
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            if (!isFinishing()) {
                Intent intent = new Intent(this, MyOrdersActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
                finish();
            }
        }, 5000);
    }
}
