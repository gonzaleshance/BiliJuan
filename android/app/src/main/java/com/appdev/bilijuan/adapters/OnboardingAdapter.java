package com.appdev.bilijuan.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.appdev.bilijuan.R;

import java.util.List;

public class OnboardingAdapter extends RecyclerView.Adapter<OnboardingAdapter.SlideViewHolder> {

    // ── Slide data class ──────────────────────────────────────────────────────
    public static class Slide {
        public final int imageResId;
        public final String title;
        public final String description;

        public Slide(int imageResId, String title, String description) {
            this.imageResId  = imageResId;
            this.title       = title;
            this.description = description;
        }
    }

    // ── Adapter ───────────────────────────────────────────────────────────────
    private final List<Slide> slides;

    public OnboardingAdapter(List<Slide> slides) {
        this.slides = slides;
    }

    @NonNull
    @Override
    public SlideViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_onboarding_slide, parent, false);
        return new SlideViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SlideViewHolder holder, int position) {
        Slide slide = slides.get(position);
        holder.ivImage.setImageResource(slide.imageResId);
        holder.tvTitle.setText(slide.title);
        holder.tvDesc.setText(slide.description);
    }

    @Override
    public int getItemCount() {
        return slides.size();
    }

    // ── ViewHolder ────────────────────────────────────────────────────────────
    static class SlideViewHolder extends RecyclerView.ViewHolder {
        final ImageView ivImage;
        final TextView tvTitle;
        final TextView tvDesc;

        SlideViewHolder(@NonNull View itemView) {
            super(itemView);
            ivImage = itemView.findViewById(R.id.ivSlideImage);
            tvTitle = itemView.findViewById(R.id.tvSlideTitle);
            tvDesc  = itemView.findViewById(R.id.tvSlideDescription);
        }
    }
}
