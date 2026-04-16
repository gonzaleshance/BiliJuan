package com.appdev.bilijuan.activities.seller;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.appdev.bilijuan.R;
import com.appdev.bilijuan.databinding.ActivitySellerReportsBinding;
import com.appdev.bilijuan.models.CartItem;
import com.appdev.bilijuan.models.Order;
import com.appdev.bilijuan.models.Product;
import com.appdev.bilijuan.utils.FirebaseHelper;
import com.appdev.bilijuan.utils.StoreNavHelper;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class SellerReportsActivity extends AppCompatActivity {

    private ActivitySellerReportsBinding binding;
    private String sellerId;
    private SalesAdapter salesAdapter;
    private final List<Order> salesHistory = new ArrayList<>();
    private String currentRange = "Today";

    private final String[] DATE_RANGES = {
            "Today", "This Week", "This Month", "This Year", "All Time"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySellerReportsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        sellerId = FirebaseHelper.getCurrentUid();
        if (sellerId == null) { finish(); return; }

        StoreNavHelper.setup(this, binding.storeNav.getRoot(), StoreNavHelper.Tab.REPORTS);
        
        setupRecyclerView();
        setupDateRangePicker();

        binding.btnGenerateReport.setOnClickListener(v -> shareReport());
    }

    private void setupRecyclerView() {
        salesAdapter = new SalesAdapter(salesHistory);
        binding.rvSales.setLayoutManager(new LinearLayoutManager(this));
        binding.rvSales.setAdapter(salesAdapter);
    }

    private void setupDateRangePicker() {
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(
                this, android.R.layout.simple_spinner_item, DATE_RANGES) {
            @NonNull
            @Override
            public View getView(int position, View convertView, @NonNull ViewGroup parent) {
                View v = super.getView(position, convertView, parent);
                if (v instanceof TextView) {
                    TextView tv = (TextView) v;
                    tv.setText(DATE_RANGES[position]);
                    tv.setTextColor(0xFF27AE60); // Changed to green for white background
                    tv.setPadding(0, 0, 0, 0);
                }
                return v;
            }
        };
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.spinnerRange.setAdapter(adapter);
        
        binding.spinnerRange.post(() -> {
            if (binding != null) binding.spinnerRange.setSelection(1);
        });

        binding.spinnerRange.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
                currentRange = DATE_RANGES[pos];
                loadReport(currentRange);
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void loadReport(String range) {
        if (binding == null) return;
        binding.progressReport.setVisibility(View.VISIBLE);
        binding.tvEmptySales.setVisibility(View.GONE);
        binding.tvRangeLabel.setText("REVENUE: " + range.toUpperCase());

        FirebaseHelper.getDb().collection("orders")
                .whereEqualTo("sellerId", sellerId)
                .whereEqualTo("status", Order.STATUS_DELIVERED)
                .get()
                .addOnSuccessListener(snap -> {
                    if (isFinishing() || binding == null) return;
                    binding.progressReport.setVisibility(View.GONE);
                    salesHistory.clear();

                    Calendar start = getStartOf(range);
                    double revenue = 0;
                    int orderCount = 0;
                    Map<String, Integer> productStats = new HashMap<>();

                    for (QueryDocumentSnapshot doc : snap) {
                        Order o = doc.toObject(Order.class);
                        if (o.getCreatedAt() == null) continue;

                        Calendar oCal = Calendar.getInstance();
                        oCal.setTime(o.getCreatedAt());

                        if (range.equals("All Time") || oCal.after(start)) {
                            revenue += o.getTotalAmount();
                            orderCount++;
                            
                            // Accurate Best Seller Stats
                            if (o.getItems() != null && !o.getItems().isEmpty()) {
                                for (CartItem item : o.getItems()) {
                                    String name = item.getProductName();
                                    if (name != null) {
                                        Integer count = productStats.get(name);
                                        productStats.put(name, (count == null ? 0 : count) + item.getQuantity());
                                    }
                                }
                            } else if (o.getProductName() != null) {
                                String name = o.getProductName();
                                Integer count = productStats.get(name);
                                productStats.put(name, (count == null ? 0 : count) + o.getQuantity());
                            }
                            salesHistory.add(o);
                        }
                    }

                    Collections.sort(salesHistory, (a, b) -> {
                        if (a.getCreatedAt() == null || b.getCreatedAt() == null) return 0;
                        return b.getCreatedAt().compareTo(a.getCreatedAt());
                    });
                    
                    salesAdapter.notifyDataSetChanged();
                    binding.tvEmptySales.setVisibility(salesHistory.isEmpty() ? View.VISIBLE : View.GONE);

                    String bestSeller = "None";
                    int max = 0;
                    for (Map.Entry<String, Integer> entry : productStats.entrySet()) {
                        if (entry.getValue() > max) {
                            max = entry.getValue();
                            bestSeller = entry.getKey();
                        }
                    }

                    final double finalRev = revenue;
                    final int finalCount = orderCount;
                    final String finalBest = bestSeller;

                    FirebaseHelper.getDb().collection("orders")
                            .whereEqualTo("sellerId", sellerId)
                            .whereEqualTo("status", Order.STATUS_CANCELLED)
                            .get()
                            .addOnSuccessListener(cancelledSnap -> {
                                if (isFinishing() || binding == null) return;
                                int cancelCount = 0;
                                for (QueryDocumentSnapshot d : cancelledSnap) {
                                    Order o = d.toObject(Order.class);
                                    if (o.getCreatedAt() == null) continue;
                                    Calendar c = Calendar.getInstance();
                                    c.setTime(o.getCreatedAt());
                                    if (range.equals("All Time") || c.after(start)) cancelCount++;
                                }
                                updateStatsUI(finalRev, finalCount, cancelCount, finalBest);
                            });
                });

        FirebaseHelper.getDb().collection("products")
                .whereEqualTo("sellerId", sellerId)
                .get()
                .addOnSuccessListener(snap -> {
                    if (isFinishing() || binding == null) return;
                    float sum = 0; int count = 0;
                    for (QueryDocumentSnapshot doc : snap) {
                        Product p = doc.toObject(Product.class);
                        if (p.getRatingCount() > 0) {
                            sum += (p.getStars() / p.getRatingCount());
                            count++;
                        }
                    }
                    binding.tvAvgRating.setText(count > 0 ? String.format(Locale.US, "%.1f", sum / count) : "0.0");
                });
    }

    private void updateStatsUI(double revenue, int orders, int cancelled, String bestSeller) {
        if (binding == null) return;
        binding.tvRevenue.setText(String.format(Locale.US, "₱%.0f", revenue));
        binding.tvOrderCount.setText(String.valueOf(orders));
        binding.tvCancelledCount.setText(String.valueOf(cancelled));
        binding.tvBestSeller.setText(bestSeller);

        int total = orders + cancelled;
        int rate = total > 0 ? (orders * 100 / total) : 0;
        binding.tvCompletionRate.setText(rate + "%");
        binding.progressCompletion.setProgress(rate);
    }

    private Calendar getStartOf(String range) {
        Calendar c = Calendar.getInstance();
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);

        if (range.equals("Today")) return c;

        switch (range) {
            case "This Week":
                c.set(Calendar.DAY_OF_WEEK, c.getFirstDayOfWeek());
                break;
            case "This Month":
                c.set(Calendar.DAY_OF_MONTH, 1);
                break;
            case "This Year":
                c.set(Calendar.DAY_OF_YEAR, 1);
                break;
        }
        return c;
    }

    private void shareReport() {
        if (salesHistory.isEmpty()) {
            Toast.makeText(this, "No data to export", Toast.LENGTH_SHORT).show();
            return;
        }

        StringBuilder report = new StringBuilder();
        report.append("BiliJuan Business Report - ").append(currentRange).append("\n");
        report.append("==============================\n\n");
        report.append("Total Revenue: ").append(binding.tvRevenue.getText()).append("\n");
        report.append("Completed Orders: ").append(binding.tvOrderCount.getText()).append("\n");
        report.append("Cancelled Orders: ").append(binding.tvCancelledCount.getText()).append("\n");
        report.append("Completion Rate: ").append(binding.tvCompletionRate.getText()).append("\n");
        report.append("Average Rating: ").append(binding.tvAvgRating.getText()).append("\n");
        report.append("Best Selling Item: ").append(binding.tvBestSeller.getText()).append("\n\n");
        
        report.append("Sales Breakdown:\n");
        SimpleDateFormat df = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
        for (Order o : salesHistory) {
            String date = o.getCreatedAt() != null ? df.format(o.getCreatedAt()) : "N/A";
            String product = o.getProductName() != null ? o.getProductName() : "Bulk Order";
            report.append("- ").append(date).append(": ").append(product)
                    .append(" (₱").append(o.getTotalAmount()).append(")\n");
        }

        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_SUBJECT, "BiliJuan Sales Report (" + currentRange + ")");
        intent.putExtra(Intent.EXTRA_TEXT, report.toString());
        startActivity(Intent.createChooser(intent, "Share Report via"));
    }

    private class SalesAdapter extends RecyclerView.Adapter<SalesAdapter.VH> {
        private final List<Order> list;
        private final SimpleDateFormat df = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());

        SalesAdapter(List<Order> list) { this.list = list; }

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_seller_sale, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH holder, int position) {
            Order o = list.get(position);
            holder.name.setText(o.getProductName() != null ? o.getProductName() : "Order #" + o.getOrderId().substring(0, 5));
            holder.amount.setText(String.format(Locale.US, "₱%.0f", o.getTotalAmount()));
            holder.date.setText(o.getCreatedAt() != null ? df.format(o.getCreatedAt()) : "N/A");
        }

        @Override
        public int getItemCount() { return list.size(); }

        class VH extends RecyclerView.ViewHolder {
            TextView name, date, amount;
            VH(View v) {
                super(v);
                name = v.findViewById(R.id.tvProductName);
                date = v.findViewById(R.id.tvDate);
                amount = v.findViewById(R.id.tvAmount);
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}