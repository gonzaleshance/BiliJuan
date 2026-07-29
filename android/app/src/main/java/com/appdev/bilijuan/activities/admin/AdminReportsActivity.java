package com.appdev.bilijuan.activities.admin;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.appdev.bilijuan.R;
import com.appdev.bilijuan.adapters.ReportAdapter;
import com.appdev.bilijuan.databinding.ActivityAdminReportsBinding;
import com.appdev.bilijuan.models.Notification;
import com.appdev.bilijuan.models.Report;
import com.appdev.bilijuan.utils.FirebaseHelper;
import com.appdev.bilijuan.utils.NotificationHelper;
import com.google.firebase.firestore.DocumentSnapshot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AdminReportsActivity extends AppCompatActivity implements ReportAdapter.OnReportClickListener {

    private ActivityAdminReportsBinding binding;
    private ReportAdapter adapter;
    private List<Report> reportList = new ArrayList<>();
    private List<StoreStat> storeStats = new ArrayList<>();
    private StoreStatAdapter statsAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAdminReportsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbar);
        binding.toolbar.setNavigationOnClickListener(v -> finish());

        setupRecyclerView();
        setupStatsPanel();
        loadReports();

        binding.btnOpenStats.setOnClickListener(v -> binding.drawerLayout.openDrawer(GravityCompat.END));
        binding.btnCloseDrawer.setOnClickListener(v -> binding.drawerLayout.closeDrawer(GravityCompat.END));
    }

    private void setupRecyclerView() {
        adapter = new ReportAdapter(reportList, this);
        binding.rvReports.setLayoutManager(new LinearLayoutManager(this));
        binding.rvReports.setAdapter(adapter);
    }

    private void setupStatsPanel() {
        statsAdapter = new StoreStatAdapter(storeStats);
        binding.rvStoreStats.setLayoutManager(new LinearLayoutManager(this));
        binding.rvStoreStats.setAdapter(statsAdapter);
    }

    private void loadReports() {
        FirebaseHelper.getDb().collection("reports")
                .whereEqualTo("status", Report.STATUS_PENDING)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Log.e("AdminReports", "Error loading reports", error);
                        return;
                    }
                    reportList.clear();
                    if (value != null) {
                        for (DocumentSnapshot doc : value.getDocuments()) {
                            Report report = doc.toObject(Report.class);
                            if (report != null) {
                                report.setReportId(doc.getId());
                                reportList.add(report);
                            }
                        }
                    }
                    adapter.notifyDataSetChanged();
                    binding.tvEmpty.setVisibility(reportList.isEmpty() ? View.VISIBLE : View.GONE);
                    calculateStats();
                });
    }

    private void calculateStats() {
        Map<String, StoreStat> statsMap = new HashMap<>();
        for (Report r : reportList) {
            String name = r.getStoreName();
            if (name == null || name.isEmpty()) name = r.getDisplayTargetName(); // Fallback
            
            if (statsMap.containsKey(name)) {
                statsMap.get(name).count++;
            } else {
                statsMap.put(name, new StoreStat(name, 1));
            }
        }
        
        storeStats.clear();
        storeStats.addAll(statsMap.values());
        Collections.sort(storeStats, (a, b) -> b.count - a.count); // Sort by count descending
        statsAdapter.notifyDataSetChanged();
    }

    @Override
    public void onReview(Report report) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Review Report");
        
        StringBuilder message = new StringBuilder();
        String type = report.getDisplayTargetType();
        
        if ("product".equalsIgnoreCase(type)) {
            message.append("Product: ").append(report.getDisplayTargetName()).append("\n");
            String store = report.getStoreName();
            if (store == null || store.isEmpty()) store = "Unknown Store";
            message.append("Store: ").append(store).append("\n");
        } else {
            message.append("User: ").append(report.getDisplayTargetName()).append("\n");
        }
        
        String displayReason = report.getReason() != null ? report.getReason() : "No reason provided";
        message.append("Reason: ").append(displayReason);
        
        builder.setMessage(message.toString());

        builder.setPositiveButton("Dismiss", (dialog, which) -> dismissReport(report));

        String suspendUid = "product".equalsIgnoreCase(type) ? report.getStoreId() : report.getTargetId();
        String suspendName = "product".equalsIgnoreCase(type) ? report.getStoreName() : report.getTargetName();

        if (suspendUid != null) {
            String btnText = "product".equalsIgnoreCase(type) ? "Suspend Seller" : "Suspend User";
            builder.setNegativeButton(btnText, (dialog, which) -> showSuspendDialog(report, suspendUid, suspendName));
        }

        builder.setNeutralButton("Cancel", null);
        builder.show();
    }

    private void dismissReport(Report report) {
        FirebaseHelper.getDb().collection("reports").document(report.getReportId())
                .update("status", Report.STATUS_DISMISSED)
                .addOnSuccessListener(aVoid -> Toast.makeText(this, "Report dismissed", Toast.LENGTH_SHORT).show());
    }

    private void showSuspendDialog(Report report, String uid, String name) {
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_suspend_user, null);
        EditText etMessage = view.findViewById(R.id.etSuspensionMessage);

        new AlertDialog.Builder(this)
                .setTitle("Suspend " + (name != null ? name : "Account"))
                .setView(view)
                .setPositiveButton("Confirm Suspension", (dialog, which) -> {
                    String message = etMessage.getText().toString().trim();
                    if (message.isEmpty()) message = "Your account has been suspended for violating platform policies.";
                    suspendUser(report, uid, message);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void suspendUser(Report report, String uid, String message) {
        FirebaseHelper.getDb().collection("users").document(uid)
                .update("isSuspended", true, "suspensionMessage", message)
                .addOnSuccessListener(aVoid -> {
                    FirebaseHelper.getDb().collection("reports").document(report.getReportId())
                            .update("status", Report.STATUS_RESOLVED);
                    
                    // Trigger "Direct-Code" notification explaining the suspension
                    NotificationHelper.sendNotification(uid, "Account Suspended", 
                        message, Notification.TYPE_NOTICE, report.getReportId());
                        
                    Toast.makeText(this, "Account suspended successfully", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Suspension failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    // --- Inner Classes for Statistics ---

    private static class StoreStat {
        String name;
        int count;
        StoreStat(String name, int count) { this.name = name; this.count = count; }
    }

    private static class StoreStatAdapter extends RecyclerView.Adapter<StoreStatAdapter.VH> {
        private List<StoreStat> stats;
        StoreStatAdapter(List<StoreStat> stats) { this.stats = stats; }

        @NonNull @Override
        public VH onCreateViewHolder(@NonNull ViewGroup p, int vt) {
            View v = LayoutInflater.from(p.getContext()).inflate(R.layout.item_store_report_stat, p, false);
            return new VH(v);
        }

        @Override public void onBindViewHolder(@NonNull VH h, int pos) {
            StoreStat s = stats.get(pos);
            h.tvName.setText(s.name);
            h.tvCount.setText(String.valueOf(s.count));
        }

        @Override public int getItemCount() { return stats.size(); }

        static class VH extends RecyclerView.ViewHolder {
            TextView tvName, tvCount;
            VH(View v) {
                super(v);
                tvName = v.findViewById(R.id.tvStoreName);
                tvCount = v.findViewById(R.id.tvReportCount);
            }
        }
    }
}
