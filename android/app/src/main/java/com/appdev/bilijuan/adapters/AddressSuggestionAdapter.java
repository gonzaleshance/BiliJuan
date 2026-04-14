package com.appdev.bilijuan.adapters;

import android.location.Address;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.appdev.bilijuan.R;

import java.util.List;

public class AddressSuggestionAdapter extends RecyclerView.Adapter<AddressSuggestionAdapter.VH> {

    public interface OnSuggestionClickListener {
        void onSuggestionClick(Address address);
    }

    private final List<Address> suggestions;
    private final OnSuggestionClickListener listener;

    public AddressSuggestionAdapter(List<Address> suggestions, OnSuggestionClickListener listener) {
        this.suggestions = suggestions;
        this.listener = listener;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(android.R.layout.simple_list_item_2, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        Address addr = suggestions.get(position);
        holder.tvLine1.setText(addr.getFeatureName());
        holder.tvLine2.setText(addr.getAddressLine(0));
        holder.itemView.setOnClickListener(v -> listener.onSuggestionClick(addr));
    }

    @Override
    public int getItemCount() {
        return suggestions.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvLine1, tvLine2;

        VH(@NonNull View v) {
            super(v);
            tvLine1 = v.findViewById(android.R.id.text1);
            tvLine2 = v.findViewById(android.R.id.text2);
        }
    }
}
