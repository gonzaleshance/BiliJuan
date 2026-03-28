package com.appdev.bilijuan.utils;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

public class FirebaseHelper {

    private static FirebaseAuth       auth;
    private static FirebaseFirestore  db;
    private static FirebaseStorage    storage;

    // ── Auth ─────────────────────────────────────────────────────────────────
    public static FirebaseAuth getAuth() {
        if (auth == null) auth = FirebaseAuth.getInstance();
        return auth;
    }

    public static FirebaseUser getCurrentUser() {
        return getAuth().getCurrentUser();
    }

    public static String getCurrentUid() {
        FirebaseUser user = getCurrentUser();
        return user != null ? user.getUid() : null;
    }

    public static boolean isLoggedIn() {
        return getCurrentUser() != null;
    }

    public static void signOut() {
        getAuth().signOut();
    }

    // ── Firestore ────────────────────────────────────────────────────────────
    public static FirebaseFirestore getDb() {
        if (db == null) db = FirebaseFirestore.getInstance();
        return db;
    }

    // ── Storage ──────────────────────────────────────────────────────────────
    public static FirebaseStorage getStorage() {
        if (storage == null) storage = FirebaseStorage.getInstance();
        return storage;
    }

    /**
     * Returns a StorageReference for a user profile image.
     * Path: profile_images/{uid}.jpg
     */
    public static StorageReference getProfileImageRef(String uid) {
        return getStorage().getReference("profile_images/" + uid + ".jpg");
    }

    /**
     * Returns a StorageReference for a product image.
     * Path: product_images/{productId}.jpg
     */
    public static StorageReference getProductImageRef(String productId) {
        return getStorage().getReference("product_images/" + productId + ".jpg");
    }
}