package com.appdev.bilijuan.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.appdev.bilijuan.R;
import com.appdev.bilijuan.models.Report;
import com.google.android.material.button.MaterialButton;

import java.util.List;

public class ReportAdapter extends RecyclerView.Adapter<ReportAdapter.ViewHolder> {

    private List<Report> reports;
    private OnReportClickListener listener;

    public interface OnReportClickListener {
        void onReview(Report report);
    }

    public ReportAdapter(List<Report> reports, OnReportClickListener listener) {
        this.reports = reports;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_report, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Report report = reports.get(position);
        
        // Use display helpers to avoid NullPointerException
        holder.tvTargetName.setText("Reported: " + report.getDisplayTargetName());
        
        String type = report.getDisplayTargetType();
        holder.tvTargetType.setText("Type: " + (type != null ? type.toUpperCase() : "UNKNOWN"));
        
        holder.tvReason.setText("Reason: " + (report.getReason() != null ? report.getReason() : "N/A"));
        holder.tvReporterName.setText("By: " + report.getDisplayReporterName());

        holder.btnReview.setOnClickListener(v -> {
            if (listener != null) listener.onReview(report);
        });
    }

    @Override
    public int getItemCount() {
        return reports.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvTargetName, tvTargetType, tvReason, tvReporterName;
        MaterialButton btnReview;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTargetName = itemView.findViewById(R.id.tvTargetName);
            tvTargetType = itemView.findViewById(R.id.tvTargetType);
            tvReason = itemView.findViewById(R.id.tvReason);
            tvReporterName = itemView.findViewById(R.id.tvReporterName);
            btnReview = itemView.findViewById(R.id.btnReview);
        }
    }
}
