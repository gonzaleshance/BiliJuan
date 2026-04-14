package com.appdev.bilijuan.activities.customer;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.appdev.bilijuan.adapters.CartAdapter;
import com.appdev.bilijuan.databinding.ActivityCartBinding;
import com.appdev.bilijuan.models.CartItem;
import com.appdev.bilijuan.utils.CartHelper;

import java.util.List;

public class CartActivity extends AppCompatActivity {

    private ActivityCartBinding binding;
    private CartAdapter adapter;
    private List<CartItem> cartItems;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityCartBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // FIXED: btnBack was renamed to btnCloseCart in the modal layout
        binding.btnCloseCart.setOnClickListener(v -> finish());
        
        setupRecyclerView();
        updateUI();

        binding.btnCheckout.setOnClickListener(v -> {
            if (cartItems.isEmpty()) return;
            
            CartItem first = cartItems.get(0);
            Intent intent = new Intent(this, PinLocationActivity.class);
            intent.putExtra("productId", first.getProductId());
            intent.putExtra("quantity", first.getQuantity());
            startActivity(intent);
        });

        binding.btnShopNow.setOnClickListener(v -> finish());
    }

    private void setupRecyclerView() {
        cartItems = CartHelper.getCart(this);
        adapter = new CartAdapter(cartItems, new CartAdapter.OnCartActionListener() {
            @Override
            public void onQuantityChanged(int position, int newQuantity) {
                cartItems.get(position).setQuantity(newQuantity);
                CartHelper.saveCart(CartActivity.this, cartItems);
                updateUI();
            }

            @Override
            public void onRemoveItem(int position) {
                cartItems.remove(position);
                CartHelper.saveCart(CartActivity.this, cartItems);
                updateUI();
            }
        });
        binding.rvCartItems.setLayoutManager(new LinearLayoutManager(this));
        binding.rvCartItems.setAdapter(adapter);
    }

    private void updateUI() {
        if (cartItems.isEmpty()) {
            binding.rvCartItems.setVisibility(View.GONE);
            binding.layoutEmptyCart.setVisibility(View.VISIBLE);
            // FIXED: cardCheckout was renamed to layoutCheckoutContainer
            binding.layoutCheckoutContainer.setVisibility(View.GONE);
            binding.cardStoreInfo.setVisibility(View.GONE);
        } else {
            binding.rvCartItems.setVisibility(View.VISIBLE);
            binding.layoutEmptyCart.setVisibility(View.GONE);
            // FIXED: cardCheckout was renamed to layoutCheckoutContainer
            binding.layoutCheckoutContainer.setVisibility(View.VISIBLE);
            binding.cardStoreInfo.setVisibility(View.VISIBLE);
            binding.tvCartStoreName.setText(cartItems.get(0).getSellerName());

            double subtotal = 0;
            for (CartItem item : cartItems) {
                subtotal += (item.getPrice() * item.getQuantity());
            }
            binding.tvCartSubtotal.setText(String.format("₱%.0f", subtotal));
        }
        adapter.notifyDataSetChanged();
    }
}
