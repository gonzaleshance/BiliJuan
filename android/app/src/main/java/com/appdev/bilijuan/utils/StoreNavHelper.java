package com.appdev.bilijuan.utils;

import android.app.Activity;
import android.content.Intent;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import com.appdev.bilijuan.R;
import com.appdev.bilijuan.activities.seller.AddProductActivity;
import com.appdev.bilijuan.activities.seller.SellerAccountActivity;
import com.appdev.bilijuan.activities.seller.SellerDashboardActivity;
import com.appdev.bilijuan.activities.seller.SellerOrdersActivity;
import com.appdev.bilijuan.activities.seller.SellerReportsActivity;

public class StoreNavHelper {

    public enum Tab { HOME, ORDERS, POST, REPORTS, STORE }

    public static void setup(Activity activity, View navRoot, Tab activeTab) {
        View navHome    = navRoot.findViewById(R.id.navHome);
        View navOrders  = navRoot.findViewById(R.id.navOrders);
        View fabPost    = navRoot.findViewById(R.id.fabPost);
        View navReports = navRoot.findViewById(R.id.navReports);
        View navStore   = navRoot.findViewById(R.id.navStore);

        // Set active tab style
        setActive(activity, navRoot, R.id.iconHome,    R.id.labelHome,    activeTab == Tab.HOME);
        setActive(activity, navRoot, R.id.iconOrders,  R.id.labelOrders,  activeTab == Tab.ORDERS);
        setActive(activity, navRoot, R.id.iconReports, R.id.labelReports, activeTab == Tab.REPORTS);
        setActive(activity, navRoot, R.id.iconStore,   R.id.labelStore,   activeTab == Tab.STORE);

        // Click listeners
        navHome.setOnClickListener(v -> {
            if (activeTab != Tab.HOME) {
                activity.startActivity(new Intent(activity, SellerDashboardActivity.class));
                activity.overridePendingTransition(0, 0);
                activity.finish();
            }
        });

        navOrders.setOnClickListener(v -> {
            if (activeTab != Tab.ORDERS) {
                activity.startActivity(new Intent(activity, SellerOrdersActivity.class));
                activity.overridePendingTransition(0, 0);
                activity.finish();
            }
        });

        fabPost.setOnClickListener(v ->
                activity.startActivity(new Intent(activity, AddProductActivity.class)));

        navReports.setOnClickListener(v -> {
            if (activeTab != Tab.REPORTS) {
                activity.startActivity(new Intent(activity, SellerReportsActivity.class));
                activity.overridePendingTransition(0, 0);
                activity.finish();
            }
        });

        navStore.setOnClickListener(v -> {
            if (activeTab != Tab.STORE) {
                activity.startActivity(new Intent(activity, SellerAccountActivity.class));
                activity.overridePendingTransition(0, 0);
                activity.finish();
            }
        });
    }

    private static void setActive(Activity activity, View root,
                                  int iconId, int labelId, boolean active) {
        ImageView icon  = root.findViewById(iconId);
        TextView  label = root.findViewById(labelId);
        int color = active ? R.color.primary : R.color.text_hint;
        icon.setColorFilter(ContextCompat.getColor(activity, color));
        label.setTextColor(ContextCompat.getColor(activity, color));
    }
}