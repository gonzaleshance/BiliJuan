package com.appdev.bilijuan.utils;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class FirebaseHelper {

    private static FirebaseAuth      auth;
    private static FirebaseFirestore db;

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
}