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
    private static final String FIREBASE_URL = "https://i-must-finish-this-in-2026-default-rtdb.asia-southeast1.firebasedatabase.app/";

    public interface PairingCallback {
        void onSuccess(String partnerName);
        void onError(String message);
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
                // Listen for when partner completes pairing using this code
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
                    // Set partner IDs for both
                    db.child("profiles").child(userId).child("partnerId").setValue(creatorId);
                    db.child("profiles").child(creatorId).child("partnerId").setValue(userId);

                    // Ensure profiles exist with names
                    db.child("profiles").child(userId).child("name").setValue(userName);

                    // Delete the used pairing code
                    db.child("pairing_codes").child(code.trim()).removeValue();

                    // Save partnerId locally
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