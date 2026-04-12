package com.appdev.bilijuan.utils;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;

import java.util.HashMap;
import java.util.Map;

public class LocationHelper {

    public static final int LOCATION_PERMISSION_REQUEST = 2001;

    public interface OnLocationSaved {
        void onSaved(double lat, double lng);
    }

    public static void autoSaveLocation(Activity activity) {
        autoSaveLocation(activity, null);
    }

    public static void autoSaveLocation(Activity activity, OnLocationSaved callback) {
        if (ContextCompat.checkSelfPermission(activity,
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fetchAndSave(activity, callback);
        } else {
            ActivityCompat.requestPermissions(activity,
                    new String[]{
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                    },
                    LOCATION_PERMISSION_REQUEST);
            if (callback != null) callback.onSaved(0, 0);
        }
    }

    public static void onPermissionGranted(Activity activity, int requestCode,
                                           int[] grantResults) {
        onPermissionGranted(activity, requestCode, grantResults, null);
    }

    public static void onPermissionGranted(Activity activity, int requestCode,
                                           int[] grantResults,
                                           OnLocationSaved callback) {
        if (requestCode == LOCATION_PERMISSION_REQUEST
                && grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            fetchAndSave(activity, callback);
        }
    }

    public static void fetchAndSave(Activity activity, OnLocationSaved callback) {
        String uid = FirebaseHelper.getCurrentUid();
        if (uid == null) {
            if (callback != null) callback.onSaved(0, 0);
            return;
        }

        FusedLocationProviderClient client =
                LocationServices.getFusedLocationProviderClient(activity);

        try {
            client.getLastLocation().addOnSuccessListener(activity, location -> {
                if (location != null) {
                    saveToFirestore(uid, location.getLatitude(), location.getLongitude(), callback);
                } else {
                    client.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                            .addOnSuccessListener(activity, fresh -> {
                                if (fresh != null) {
                                    saveToFirestore(uid, fresh.getLatitude(), fresh.getLongitude(), callback);
                                } else {
                                    if (callback != null) callback.onSaved(0, 0);
                                }
                            });
                }
            });
        } catch (SecurityException e) {
            if (callback != null) callback.onSaved(0, 0);
        }
    }

    private static void saveToFirestore(String uid, double lat, double lng, OnLocationSaved callback) {
        Map<String, Object> update = new HashMap<>();
        update.put("latitude",  lat);
        update.put("longitude", lng);
        FirebaseHelper.getDb().collection("users").document(uid)
                .update(update)
                .addOnSuccessListener(v -> { if (callback != null) callback.onSaved(lat, lng); })
                .addOnFailureListener(e -> { if (callback != null) callback.onSaved(lat, lng); });
    }
}