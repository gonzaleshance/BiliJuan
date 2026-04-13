package com.appdev.bilijuan.activities.seller;

import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;

import androidx.appcompat.app.AppCompatActivity;

import com.appdev.bilijuan.R;
import com.appdev.bilijuan.databinding.ActivitySellerReportsBinding;
import com.appdev.bilijuan.models.Order;
import com.appdev.bilijuan.models.Product;
import com.appdev.bilijuan.utils.FirebaseHelper;
import com.appdev.bilijuan.utils.StoreNavHelper;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

public class SellerReportsActivity extends AppCompatActivity {

    private ActivitySellerReportsBinding binding;
    private String sellerId;
    private String selectedRange = "This Week"; // default

    private final String[] DATE_RANGES = {
            "Today", "This Week", "This Month", "This Year"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySellerReportsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        sellerId = FirebaseHelper.getCurrentUid();
        if (sellerId == null) { finish(); return; }

        binding.btnBack.setOnClickListener(v -> finish());
        
        // FIXED: Use .getRoot() because binding.storeNav is an include binding object
        StoreNavHelper.setup(this, binding.storeNav.getRoot(), StoreNavHelper.Tab.REPORTS);
        
        setupDateRangePicker();
        loadReport("This Week");
    }

    // ── Date Range Picker ─────────────────────────────────────────────────────

    private void setupDateRangePicker() {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_dropdown_item, DATE_RANGES);
        binding.spinnerRange.setAdapter(adapter);
        binding.spinnerRange.setSelection(1); // "This Week" default

        binding.spinnerRange.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
                selectedRange = DATE_RANGES[pos];
                loadReport(selectedRange);
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    // ── Load Report ───────────────────────────────────────────────────────────

    private void loadReport(String range) {
        binding.progressReport.setVisibility(View.VISIBLE);

        FirebaseHelper.getDb().collection("orders")
                .whereEqualTo("sellerId", sellerId)
                .whereEqualTo("status", Order.STATUS_DELIVERED)
                .get()
                .addOnSuccessListener(snap -> {
                    binding.progressReport.setVisibility(View.GONE);

                    Calendar start = getStartOf(range);
                    Calendar end   = Calendar.getInstance();

                    double revenue = 0;
                    int    orderCount = 0;
                    Map<String, Integer> productCount = new HashMap<>();

                    for (QueryDocumentSnapshot doc : snap) {
                        Order o = doc.toObject(Order.class);
                        if (o.getCreatedAt() == null) continue;

                        Calendar oCal = Calendar.getInstance();
                        oCal.setTime(o.getCreatedAt());

                        if (oCal.after(start) && oCal.before(end)) {
                            revenue += o.getTotalAmount();
                            orderCount++;
                            productCount.merge(o.getProductName(), 1, Integer::sum);
                        }
                    }

                    // Best seller
                    String bestSeller = "—";
                    int bestCount = 0;
                    for (Map.Entry<String, Integer> e : productCount.entrySet()) {
                        if (e.getValue() > bestCount) {
                            bestCount  = e.getValue();
                            bestSeller = e.getKey() + " (" + bestCount + " orders)";
                        }
                    }

                    // Cancelled count
                    final double finalRevenue   = revenue;
                    final int    finalOrders    = orderCount;
                    final String finalBestSeller = bestSeller;

                    FirebaseHelper.getDb().collection("orders")
                            .whereEqualTo("sellerId", sellerId)
                            .whereEqualTo("status", Order.STATUS_CANCELLED)
                            .get()
                            .addOnSuccessListener(cancelled -> {
                                int cancelCount = 0;
                                for (QueryDocumentSnapshot d : cancelled) {
                                    Order o = d.toObject(Order.class);
                                    if (o.getCreatedAt() == null) continue;
                                    Calendar oCal = Calendar.getInstance();
                                    oCal.setTime(o.getCreatedAt());
                                    if (oCal.after(start) && oCal.before(end)) cancelCount++;
                                }
                                updateUI(finalRevenue, finalOrders, cancelCount, finalBestSeller, range);
                            });
                });

        // Average rating from products
        FirebaseHelper.getDb().collection("products")
                .whereEqualTo("sellerId", sellerId)
                .get()
                .addOnSuccessListener(snap -> {
                    float sum = 0; int count = 0;
                    for (QueryDocumentSnapshot doc : snap) {
                        Product p = doc.toObject(Product.class);
                        if (p.getRatingCount() > 0) { sum += p.getStars(); count++; }
                    }
                    if (count > 0)
                        binding.tvAvgRating.setText(String.format("%.1f / 5.0", sum / count));
                    else
                        binding.tvAvgRating.setText("No ratings yet");
                });
    }

    private void updateUI(double revenue, int orders, int cancelled,
                          String bestSeller, String range) {
        binding.tvRangeLabel.setText("Summary for: " + range);
        binding.tvRevenue.setText(String.format("₱%.0f", revenue));
        binding.tvOrderCount.setText(String.valueOf(orders));
        binding.tvCancelledCount.setText(String.valueOf(cancelled));
        binding.tvBestSeller.setText(bestSeller);

        // Completion rate
        int total = orders + cancelled;
        int rate  = total > 0 ? (int) ((orders / (float) total) * 100) : 0;
        binding.tvCompletionRate.setText(rate + "%");
        binding.progressCompletion.setProgress(rate);
    }

    private Calendar getStartOf(String range) {
        Calendar c = Calendar.getInstance();
        switch (range) {
            case "Today":
                c.set(Calendar.HOUR_OF_DAY, 0);
                c.set(Calendar.MINUTE, 0);
                c.set(Calendar.SECOND, 0);
                break;
            case "This Week":
                c.set(Calendar.DAY_OF_WEEK, c.getFirstDayOfWeek());
                c.set(Calendar.HOUR_OF_DAY, 0);
                break;
            case "This Month":
                c.set(Calendar.DAY_OF_MONTH, 1);
                c.set(Calendar.HOUR_OF_DAY, 0);
                break;
            case "This Year":
                c.set(Calendar.DAY_OF_YEAR, 1);
                c.set(Calendar.HOUR_OF_DAY, 0);
                break;
        }
        return c;
    }
}
