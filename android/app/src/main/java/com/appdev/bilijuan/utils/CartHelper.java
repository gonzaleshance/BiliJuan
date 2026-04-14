package com.appdev.bilijuan.utils;

import android.content.Context;
import android.content.SharedPreferences;

import com.appdev.bilijuan.models.CartItem;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class CartHelper {
    private static final String PREF_NAME = "bilijuan_cart";
    private static final String KEY_CART = "cart_items";
    private static final Gson gson = new Gson();

    public static List<CartItem> getCart(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        String json = prefs.getString(KEY_CART, null);
        if (json == null) return new ArrayList<>();
        Type type = new TypeToken<ArrayList<CartItem>>() {}.getType();
        return gson.fromJson(json, type);
    }

    public static void saveCart(Context context, List<CartItem> cart) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        String json = gson.toJson(cart);
        prefs.edit().putString(KEY_CART, json).apply();
    }

    public static void addToCart(Context context, CartItem newItem) {
        List<CartItem> cart = getCart(context);
        
        // Check if existing item is from the same seller
        if (!cart.isEmpty()) {
            if (!cart.get(0).getSellerId().equals(newItem.getSellerId())) {
                // This shouldn't happen if UI logic is correct, but safety first
                return;
            }
        }

        boolean found = false;
        for (CartItem item : cart) {
            if (item.getProductId().equals(newItem.getProductId())) {
                item.setQuantity(item.getQuantity() + newItem.getQuantity());
                found = true;
                break;
            }
        }

        if (!found) {
            cart.add(newItem);
        }

        saveCart(context, cart);
    }

    public static void clearCart(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        prefs.edit().remove(KEY_CART).apply();
    }

    public static String getCartSellerId(Context context) {
        List<CartItem> cart = getCart(context);
        if (cart.isEmpty()) return null;
        return cart.get(0).getSellerId();
    }
    
    public static int getCartCount(Context context) {
        List<CartItem> cart = getCart(context);
        int count = 0;
        for (CartItem item : cart) count += item.getQuantity();
        return count;
    }
}
