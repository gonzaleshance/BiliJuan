package com.appdev.bilijuan.activities.admin;

import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.appdev.bilijuan.databinding.ActivityAdminSettingsBinding;
import com.appdev.bilijuan.models.GlobalConfig;
import com.appdev.bilijuan.utils.FirebaseHelper;
import com.google.firebase.firestore.DocumentReference;

public class AdminSettingsActivity extends AppCompatActivity {

    private ActivityAdminSettingsBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAdminSettingsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbar);
        binding.toolbar.setNavigationOnClickListener(v -> finish());

        loadGlobalConfig();

        binding.btnUpdateConfig.setOnClickListener(v -> updateGlobalConfig());
    }

    private void loadGlobalConfig() {
        FirebaseHelper.getDb().collection("settings").document("global_config")
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        GlobalConfig config = doc.toObject(GlobalConfig.class);
                        if (config != null) {
                            binding.etBaseFee.setText(String.valueOf(config.getBase_delivery_fee()));
                            binding.etPricePerKm.setText(String.valueOf(config.getPrice_per_km()));
                        }
                    } else {
                        // If document doesn't exist, create it with default values
                        createDefaultConfig();
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Failed to load settings: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void createDefaultConfig() {
        GlobalConfig defaultConfig = new GlobalConfig(20.0, 10.0);
        FirebaseHelper.getDb().collection("settings").document("global_config")
                .set(defaultConfig)
                .addOnSuccessListener(aVoid -> {
                    binding.etBaseFee.setText("20.0");
                    binding.etPricePerKm.setText("10.0");
                });
    }

    private void updateGlobalConfig() {
        String baseFeeStr = binding.etBaseFee.getText().toString();
        String pricePerKmStr = binding.etPricePerKm.getText().toString();

        if (baseFeeStr.isEmpty() || pricePerKmStr.isEmpty()) {
            Toast.makeText(this, "Please enter both fees", Toast.LENGTH_SHORT).show();
            return;
        }

        double baseFee = Double.parseDouble(baseFeeStr);
        double pricePerKm = Double.parseDouble(pricePerKmStr);

        GlobalConfig config = new GlobalConfig(baseFee, pricePerKm);

        FirebaseHelper.getDb().collection("settings").document("global_config")
                .set(config)
                .addOnSuccessListener(aVoid -> Toast.makeText(this, "Settings updated", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e -> Toast.makeText(this, "Failed to update settings", Toast.LENGTH_SHORT).show());
    }
}
