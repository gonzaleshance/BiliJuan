package com.appdev.bilijuan.utils;

import android.app.Activity;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.appdev.bilijuan.R;
import com.appdev.bilijuan.activities.admin.AdminDashboardActivity;
import com.appdev.bilijuan.activities.admin.AdminFoodsActivity;
import com.appdev.bilijuan.activities.admin.AdminOrdersActivity;
import com.appdev.bilijuan.activities.admin.AdminSellersActivity;
import com.appdev.bilijuan.activities.admin.AdminUsersActivity;

public class AdminNavHelper {

    public enum Tab { OVERVIEW, USERS, ORDERS, SELLERS, FOODS }

    // Hardcoded Brand Colors
    private static final String COLOR_PRIMARY = "#27AE60";
    private static final String COLOR_INACTIVE = "#B2BEC3";

    /**
     * Standard setup for Activities that handle their own tabs via separate Activity classes.
     */
    public static void setup(Activity activity, View navRoot, Tab activeTab) {
        setup(activity, navRoot, activeTab, null);
    }

    /**
     * Setups the Admin Bottom Navigation Bar.
     * @param listener If provided, it will handle the tab switch internally. If null, it starts new activities.
     */
    public static void setup(Activity activity, View navRoot, Tab activeTab, OnTabSelectedListener listener) {
        View navOverview = navRoot.findViewById(R.id.navOverview);
        View navUsers    = navRoot.findViewById(R.id.navUsers);
        View navOrders   = navRoot.findViewById(R.id.navOrders);
        View navSellers  = navRoot.findViewById(R.id.navSellers);
        View fabFoods    = navRoot.findViewById(R.id.fabFoods);

        // Highlight active tab
        setActive(navRoot, R.id.iconOverview, R.id.labelOverview, activeTab == Tab.OVERVIEW);
        setActive(navRoot, R.id.iconUsers,    R.id.labelUsers,    activeTab == Tab.USERS);
        setActive(navRoot, R.id.iconOrders,   R.id.labelOrders,   activeTab == Tab.ORDERS);
        setActive(navRoot, R.id.iconSellers,  R.id.labelSellers,  activeTab == Tab.SELLERS);

        // FAB Color
        if (fabFoods != null) {
            fabFoods.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor(COLOR_PRIMARY)));
        }

        // Click listeners
        if (navOverview != null) navOverview.setOnClickListener(v -> handleTabClick(activity, activeTab, Tab.OVERVIEW, listener, AdminDashboardActivity.class));
        if (navUsers != null)    navUsers.setOnClickListener(v -> handleTabClick(activity, activeTab, Tab.USERS, listener, AdminUsersActivity.class));
        if (navOrders != null)   navOrders.setOnClickListener(v -> handleTabClick(activity, activeTab, Tab.ORDERS, listener, AdminOrdersActivity.class));
        if (navSellers != null)  navSellers.setOnClickListener(v -> handleTabClick(activity, activeTab, Tab.SELLERS, listener, AdminSellersActivity.class));
        if (fabFoods != null)    fabFoods.setOnClickListener(v -> handleTabClick(activity, activeTab, Tab.FOODS, listener, AdminFoodsActivity.class));
    }

    private static void handleTabClick(Activity activity, Tab currentTab, Tab targetTab, OnTabSelectedListener listener, Class<?> targetClass) {
        if (currentTab == targetTab) return;

        if (listener != null) {
            listener.onTabSelected(targetTab);
        } else {
            Intent intent = new Intent(activity, targetClass);
            activity.startActivity(intent);
            activity.finish();
            activity.overridePendingTransition(0, 0);
        }
    }

    private static void setActive(View root, int iconId, int labelId, boolean active) {
        ImageView icon = root.findViewById(iconId);
        TextView label = root.findViewById(labelId);
        if (icon == null || label == null) return;
        
        int color = Color.parseColor(active ? COLOR_PRIMARY : COLOR_INACTIVE);
        icon.setColorFilter(color);
        label.setTextColor(color);
    }

    public interface OnTabSelectedListener {
        void onTabSelected(Tab tab);
    }
}
