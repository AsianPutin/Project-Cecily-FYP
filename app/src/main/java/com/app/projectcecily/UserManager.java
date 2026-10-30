package com.app.projectcecily;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.annotation.NonNull;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class UserManager {

    private static final String PREFS_NAME = "prefs";
    private static final String KEY_USER_ID = "user_id";
    private static final String KEY_PARTNER_ID = "partner_id";
    private static final String KEY_USER_NAME = "user_name";
    private static final String KEY_IS_LOGGED_IN = "is_logged_in";
    private static final String KEY_USER_EMAIL = "user_email";
    private static final String KEY_USER_USERNAME = "user_username";
    private static final String FIREBASE_URL = "https://i-must-finish-this-in-2026-default-rtdb.asia-southeast1.firebasedatabase.app/";

    public interface PairingCallback {
        void onSuccess(String partnerName);
        void onError(String message);
    }

    public interface AuthCallback {
        void onSuccess(String message);
        void onError(String message);
    }

    public static boolean isLoggedIn(Context context) {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getBoolean(KEY_IS_LOGGED_IN, false);
    }

    public static void setLoggedIn(Context context, boolean loggedIn) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit().putBoolean(KEY_IS_LOGGED_IN, loggedIn).apply();
    }

    public static String getUserId(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String id = prefs.getString(KEY_USER_ID, null);
        if (id == null || id.equals("user_1") || id.equals("user_2")) {
            id = "usr_" + (100000 + new Random().nextInt(900000));
            prefs.edit().putString(KEY_USER_ID, id).apply();
        }
        return id;
    }

    public static String getPartnerId(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getString(KEY_PARTNER_ID, "");
    }

    public static void setPartnerId(Context context, String partnerId) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit().putString(KEY_PARTNER_ID, partnerId != null ? partnerId : "").apply();
    }

    public static String getUserName(Context context) {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getString(KEY_USER_NAME, "User");
    }

    public static void setUserName(Context context, String name) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit().putString(KEY_USER_NAME, name).apply();
    }

    public static void login(Context context, String usernameOrEmail, String password, AuthCallback callback) {
        if (usernameOrEmail == null || usernameOrEmail.trim().isEmpty() || password == null || password.trim().isEmpty()) {
            callback.onError("Please enter both username/email and password.");
            return;
        }

        String input = usernameOrEmail.trim().toLowerCase();
        DatabaseReference db = FirebaseDatabase.getInstance(FIREBASE_URL).getReference("profiles");

        db.get().addOnSuccessListener(snapshot -> {
            boolean found = false;
            if (snapshot.exists()) {
                for (DataSnapshot ds : snapshot.getChildren()) {
                    String dbEmail = ds.child("email").getValue(String.class);
                    String dbUsername = ds.child("username").getValue(String.class);
                    String dbPassword = ds.child("password").getValue(String.class);

                    boolean emailMatch = dbEmail != null && dbEmail.trim().toLowerCase().equals(input);
                    boolean usernameMatch = dbUsername != null && dbUsername.trim().toLowerCase().equals(input);

                    if ((emailMatch || usernameMatch) && password.equals(dbPassword)) {
                        found = true;
                        String uId = ds.getKey();
                        String name = ds.child("name").getValue(String.class);
                        String partnerId = ds.child("partnerId").getValue(String.class);
                        String photoBase64 = ds.child("photoBase64").getValue(String.class);

                        SharedPreferences.Editor editor = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit();
                        editor.putString(KEY_USER_ID, uId);
                        editor.putString(KEY_USER_NAME, name != null ? name : "User");
                        editor.putString(KEY_USER_EMAIL, dbEmail);
                        editor.putString(KEY_USER_USERNAME, dbUsername);
                        editor.putString(KEY_PARTNER_ID, partnerId != null ? partnerId : "");
                        editor.putBoolean(KEY_IS_LOGGED_IN, true);
                        if (photoBase64 != null) {
                            editor.putString("profile_pic_base64", photoBase64);
                        }
                        editor.apply();

                        callback.onSuccess("Welcome back, " + (name != null ? name : dbUsername) + "!");
                        break;
                    }
                }
            }
            if (!found) {
                callback.onError("Invalid username/email or password.");
            }
        }).addOnFailureListener(e -> callback.onError("Login failed: " + e.getMessage()));
    }

    public static void signUp(Context context, String name, String username, String email, String password, String gender, String birthday, String photoBase64, AuthCallback callback) {
        if (name == null || name.trim().isEmpty() ||
            username == null || username.trim().isEmpty() ||
            email == null || email.trim().isEmpty() ||
            password == null || password.trim().isEmpty()) {
            callback.onError("Please fill in all required fields.");
            return;
        }

        String cleanUsername = username.trim().toLowerCase();
        String cleanEmail = email.trim().toLowerCase();
        DatabaseReference db = FirebaseDatabase.getInstance(FIREBASE_URL).getReference("profiles");

        db.get().addOnSuccessListener(snapshot -> {
            if (snapshot.exists()) {
                for (DataSnapshot ds : snapshot.getChildren()) {
                    String dbEmail = ds.child("email").getValue(String.class);
                    String dbUsername = ds.child("username").getValue(String.class);

                    if (dbUsername != null && dbUsername.trim().toLowerCase().equals(cleanUsername)) {
                        callback.onError("Username is already taken. Choose another.");
                        return;
                    }
                    if (dbEmail != null && dbEmail.trim().toLowerCase().equals(cleanEmail)) {
                        callback.onError("An account with this email already exists.");
                        return;
                    }
                }
            }

            String newUserId = "usr_" + (100000 + new Random().nextInt(900000));
            Map<String, Object> userMap = new HashMap<>();
            userMap.put("name", name.trim());
            userMap.put("username", cleanUsername);
            userMap.put("email", cleanEmail);
            userMap.put("password", password);
            userMap.put("gender", gender != null ? gender : "Prefer not to say");
            userMap.put("birthday", birthday != null ? birthday : "");
            userMap.put("partnerId", "");
            if (photoBase64 != null && !photoBase64.isEmpty()) {
                userMap.put("photoBase64", photoBase64);
            }

            db.child(newUserId).setValue(userMap).addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    SharedPreferences.Editor editor = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit();
                    editor.putString(KEY_USER_ID, newUserId);
                    editor.putString(KEY_USER_NAME, name.trim());
                    editor.putString(KEY_USER_EMAIL, cleanEmail);
                    editor.putString(KEY_USER_USERNAME, cleanUsername);
                    editor.putString(KEY_PARTNER_ID, "");
                    editor.putBoolean(KEY_IS_LOGGED_IN, true);
                    if (photoBase64 != null && !photoBase64.isEmpty()) {
                        editor.putString("profile_pic_base64", photoBase64);
                    }
                    editor.apply();

                    callback.onSuccess("Account created successfully!");
                } else {
                    callback.onError("Failed to create account. Try again.");
                }
            });

        }).addOnFailureListener(e -> callback.onError("Sign up failed: " + e.getMessage()));
    }

    public static void logout(Context context) {
        SharedPreferences.Editor editor = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit();
        editor.putBoolean(KEY_IS_LOGGED_IN, false);
        editor.remove(KEY_USER_ID);
        editor.remove(KEY_PARTNER_ID);
        editor.remove(KEY_USER_NAME);
        editor.remove(KEY_USER_EMAIL);
        editor.remove(KEY_USER_USERNAME);
        editor.remove("profile_pic_base64");
        editor.apply();
    }

    public static void generatePairingCode(Context context, PairingCallback callback) {
        String userId = getUserId(context);
        String userName = getUserName(context);
        DatabaseReference db = FirebaseDatabase.getInstance(FIREBASE_URL).getReference();

        String code = String.valueOf(100000 + new Random().nextInt(900000));

        Map<String, Object> codeData = new HashMap<>();
        codeData.put("creatorId", userId);
        codeData.put("creatorName", userName);
        codeData.put("createdAt", System.currentTimeMillis());

        db.child("pairing_codes").child(code).setValue(codeData).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                callback.onSuccess(code);
                listenForPartnerConnection(context, db, userId, callback);
            } else {
                callback.onError("Failed to generate code. Try again.");
            }
        });
    }

    private static void listenForPartnerConnection(Context context, DatabaseReference db, String userId, PairingCallback callback) {
        db.child("profiles").child(userId).child("partnerId").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String partnerId = snapshot.getValue(String.class);
                if (partnerId != null && !partnerId.isEmpty()) {
                    setPartnerId(context, partnerId);
                    db.child("profiles").child(partnerId).child("name").get().addOnSuccessListener(nameSnap -> {
                        String partnerName = nameSnap.getValue(String.class);
                        callback.onSuccess("PAIRED:" + (partnerName != null ? partnerName : "Partner"));
                    });
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    public static void connectWithCode(Context context, String code, PairingCallback callback) {
        if (code == null || code.trim().length() != 6) {
            callback.onError("Please enter a valid 6-digit code.");
            return;
        }

        String userId = getUserId(context);
        String userName = getUserName(context);
        DatabaseReference db = FirebaseDatabase.getInstance(FIREBASE_URL).getReference();

        db.child("pairing_codes").child(code.trim()).get().addOnSuccessListener(snapshot -> {
            if (snapshot.exists()) {
                String creatorId = snapshot.child("creatorId").getValue(String.class);
                String creatorName = snapshot.child("creatorName").getValue(String.class);

                if (creatorId != null && !creatorId.equals(userId)) {
                    db.child("profiles").child(userId).child("partnerId").setValue(creatorId);
                    db.child("profiles").child(creatorId).child("partnerId").setValue(userId);
                    db.child("profiles").child(userId).child("name").setValue(userName);
                    db.child("pairing_codes").child(code.trim()).removeValue();
                    setPartnerId(context, creatorId);

                    callback.onSuccess(creatorName != null ? creatorName : "Partner");
                } else {
                    callback.onError("You cannot pair with your own code!");
                }
            } else {
                callback.onError("Invalid or expired 6-digit code.");
            }
        }).addOnFailureListener(e -> callback.onError("Network error: " + e.getMessage()));
    }

    public static void unpairPartner(Context context, Runnable onComplete) {
        String userId = getUserId(context);
        String partnerId = getPartnerId(context);
        DatabaseReference db = FirebaseDatabase.getInstance(FIREBASE_URL).getReference();

        if (!partnerId.isEmpty()) {
            db.child("profiles").child(partnerId).child("partnerId").removeValue();
        }
        db.child("profiles").child(userId).child("partnerId").removeValue();
        setPartnerId(context, "");

        if (onComplete != null) onComplete.run();
    }
}
