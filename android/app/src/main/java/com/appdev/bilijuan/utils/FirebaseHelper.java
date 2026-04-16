package com.appdev.bilijuan.utils;

import android.content.Context;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.firestore.FirebaseFirestore;

public class FirebaseHelper {

    private static FirebaseAuth      auth;
    private static FirebaseFirestore db;
    private static GoogleSignInClient googleSignInClient;

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

    public static void signOut(Context context) {
        getAuth().signOut();
        getGoogleSignInClient(context).signOut();
    }

    // ── Firestore ────────────────────────────────────────────────────────────
    public static FirebaseFirestore getDb() {
        if (db == null) db = FirebaseFirestore.getInstance();
        return db;
    }

    // ── Google Sign-In ───────────────────────────────────────────────────────
    public static GoogleSignInClient getGoogleSignInClient(Context context) {
        if (googleSignInClient == null) {
            GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                    .requestIdToken("106335735791433606022")  // Add your web client ID from Firebase console
                    .requestEmail()
                    .build();
            googleSignInClient = GoogleSignIn.getClient(context, gso);
        }
        return googleSignInClient;
    }

    public static GoogleSignInAccount getLastSignedInAccount(Context context) {
        return GoogleSignIn.getLastSignedInAccount(context);
    }

    public static AuthCredential getGoogleAuthCredential(String idToken) {
        return GoogleAuthProvider.getCredential(idToken, null);
    }

    public static void revokeGoogleAccess(Context context) {
        getGoogleSignInClient(context).revokeAccess();
    }
}