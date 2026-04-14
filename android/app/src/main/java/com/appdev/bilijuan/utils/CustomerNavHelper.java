package com.appdev.bilijuan.utils;

import android.app.Activity;
import android.content.Intent;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import com.appdev.bilijuan.R;
import com.appdev.bilijuan.activities.customer.AccountActivity;
import com.appdev.bilijuan.activities.customer.CategoryActivity;
import com.appdev.bilijuan.activities.customer.HomeActivity;
import com.appdev.bilijuan.activities.customer.MyOrdersActivity;
import com.appdev.bilijuan.activities.customer.SearchActivity;

public class CustomerNavHelper {

    public enum Tab { HOME, ORDERS, SEARCH, PROFILE, CATEGORY }

    public static void setup(Activity activity, View navRoot, Tab activeTab) {
        View navHome     = navRoot.findViewById(R.id.navHome);
        View navOrders   = navRoot.findViewById(R.id.navOrders);
        View navSearch   = navRoot.findViewById(R.id.navSearch);
        View navProfile  = navRoot.findViewById(R.id.navProfile);
        View fabCategory = navRoot.findViewById(R.id.fabCategory);

        // Set active tab style
        setActive(activity, navRoot, R.id.iconHome,    R.id.labelHome,    activeTab == Tab.HOME);
        setActive(activity, navRoot, R.id.iconOrders,  R.id.labelOrders,  activeTab == Tab.ORDERS);
        setActive(activity, navRoot, R.id.iconSearch,  R.id.labelSearch,  activeTab == Tab.SEARCH);
        setActive(activity, navRoot, R.id.iconProfile, R.id.labelProfile, activeTab == Tab.PROFILE);

        // Click listeners
        navHome.setOnClickListener(v -> {
            if (activeTab != Tab.HOME) {
                activity.startActivity(new Intent(activity, HomeActivity.class));
                activity.overridePendingTransition(0, 0);
                activity.finish();
            }
        });

        navOrders.setOnClickListener(v -> {
            if (activeTab != Tab.ORDERS) {
                activity.startActivity(new Intent(activity, MyOrdersActivity.class));
                activity.overridePendingTransition(0, 0);
                activity.finish();
            }
        });

        navSearch.setOnClickListener(v -> {
            if (activeTab != Tab.SEARCH) {
                activity.startActivity(new Intent(activity, SearchActivity.class));
                activity.overridePendingTransition(0, 0);
                activity.finish();
            }
        });

        navProfile.setOnClickListener(v -> {
            if (activeTab != Tab.PROFILE) {
                activity.startActivity(new Intent(activity, AccountActivity.class));
                activity.overridePendingTransition(0, 0);
                activity.finish();
            }
        });

        fabCategory.setOnClickListener(v -> {
            if (activeTab != Tab.CATEGORY) {
                activity.startActivity(new Intent(activity, CategoryActivity.class));
                activity.overridePendingTransition(0, 0);
                activity.finish();
            }
        });
    }

    private static void setActive(Activity activity, View root,
                                  int iconId, int labelId, boolean active) {
        ImageView icon  = root.findViewById(iconId);
        TextView  label = root.findViewById(labelId);
        if (icon == null || label == null) return;
        
        int color = active ? R.color.primary : R.color.text_hint;
        icon.setColorFilter(ContextCompat.getColor(activity, color));
        label.setTextColor(ContextCompat.getColor(activity, color));
    }
}