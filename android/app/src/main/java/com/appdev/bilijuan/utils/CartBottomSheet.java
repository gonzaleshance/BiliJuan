package com.appdev.bilijuan.utils;

import android.app.Activity;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;

import androidx.recyclerview.widget.LinearLayoutManager;

import com.appdev.bilijuan.R;
import com.appdev.bilijuan.activities.customer.PinLocationActivity;
import com.appdev.bilijuan.adapters.CartAdapter;
import com.appdev.bilijuan.databinding.ActivityCartBinding;
import com.appdev.bilijuan.models.CartItem;
import com.google.android.material.bottomsheet.BottomSheetDialog;

import java.util.List;

public class CartBottomSheet {

    public static void show(Activity activity, Runnable onCartChanged) {
        BottomSheetDialog dialog = new BottomSheetDialog(activity, R.style.BottomSheetDialogTheme);
        ActivityCartBinding binding = ActivityCartBinding.inflate(LayoutInflater.from(activity));
        dialog.setContentView(binding.getRoot());

        List<CartItem> cartItems = CartHelper.getCart(activity);
        CartAdapter adapter = new CartAdapter(cartItems, new CartAdapter.OnCartActionListener() {
            @Override
            public void onQuantityChanged(int position, int newQuantity) {
                cartItems.get(position).setQuantity(newQuantity);
                CartHelper.saveCart(activity, cartItems);
                updateUI(binding, cartItems, onCartChanged);
            }

            @Override
            public void onRemoveItem(int position) {
                cartItems.remove(position);
                CartHelper.saveCart(activity, cartItems);
                updateUI(binding, cartItems, onCartChanged);
            }
        });

        binding.rvCartItems.setLayoutManager(new LinearLayoutManager(activity));
        binding.rvCartItems.setAdapter(adapter);

        binding.btnCloseCart.setOnClickListener(v -> dialog.dismiss());
        binding.btnShopNow.setOnClickListener(v -> dialog.dismiss());

        binding.btnCheckout.setOnClickListener(v -> {
            if (cartItems.isEmpty()) return;
            dialog.dismiss();
            
            // Pass fromCart flag to PinLocationActivity
            Intent intent = new Intent(activity, PinLocationActivity.class);
            intent.putExtra("fromCart", true);
            // We still pass the first item's productId for some legacy logic if needed
            intent.putExtra("productId", cartItems.get(0).getProductId());
            activity.startActivity(intent);
        });

        updateUI(binding, cartItems, onCartChanged);
        dialog.show();
    }

    private static void updateUI(ActivityCartBinding binding, List<CartItem> cartItems, Runnable onCartChanged) {
        if (cartItems.isEmpty()) {
            binding.rvCartItems.setVisibility(View.GONE);
            binding.layoutEmptyCart.setVisibility(View.VISIBLE);
            binding.layoutCheckoutContainer.setVisibility(View.GONE);
            binding.cardStoreInfo.setVisibility(View.GONE);
        } else {
            binding.rvCartItems.setVisibility(View.VISIBLE);
            binding.layoutEmptyCart.setVisibility(View.GONE);
            binding.layoutCheckoutContainer.setVisibility(View.VISIBLE);
            binding.cardStoreInfo.setVisibility(View.VISIBLE);
            binding.tvCartStoreName.setText(cartItems.get(0).getSellerName());

            double subtotal = 0;
            for (CartItem item : cartItems) {
                subtotal += (item.getPrice() * item.getQuantity());
            }
            binding.tvCartSubtotal.setText(String.format("₱%.0f", subtotal));
        }
        if (binding.rvCartItems.getAdapter() != null) {
            binding.rvCartItems.getAdapter().notifyDataSetChanged();
        }
        if (onCartChanged != null) onCartChanged.run();
    }
}
