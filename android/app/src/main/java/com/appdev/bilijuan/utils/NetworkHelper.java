package com.appdev.bilijuan.utils;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;

public class NetworkHelper {

    public static boolean isOnline(Context context) {
        ConnectivityManager cm =
                (ConnectivityManager) context.getSystemService(
                        Context.CONNECTIVITY_SERVICE);
        if (cm == null) return false;
        android.net.Network network = cm.getActiveNetwork();
        if (network == null) return false;
        NetworkCapabilities caps = cm.getNetworkCapabilities(network);
        return caps != null && (
                caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                        caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                        caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET));
    }

    public static void showOfflineToast(Context context) {
        android.widget.Toast.makeText(context,
                "No internet connection. Please check your network.",
                android.widget.Toast.LENGTH_LONG).show();
    }
}