package com.appdev.bilijuan.utils;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

public class FirebaseHelper {
    private static FirebaseAuth auth;
    private static FirebaseFirestore db;

    public static FirebaseAuth getAuth() {
        if (auth == null) auth = FirebaseAuth.getInstance();
        return auth;
    }

    public static FirebaseFirestore getDb() {
        if (db == null) db = FirebaseFirestore.getInstance();
        return db;
    }

    public static String getCurrentUserId() {
        if (getAuth().getCurrentUser() != null)
            return getAuth().getCurrentUser().getUid();
        return null;
    }
}