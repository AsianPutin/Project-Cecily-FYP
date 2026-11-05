package com.app.projectcecily;

import android.content.Context;
import android.content.SharedPreferences;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.database.FirebaseDatabase;

public class SubscriptionDialogHelper {

    public interface OnSubscriptionStatusChangedListener {
        void onStatusChanged(boolean isUnlocked);
    }

    public static boolean isPremiumUnlocked(Context context) {
        if (context == null) return false;
        SharedPreferences prefs = context.getSharedPreferences("prefs", Context.MODE_PRIVATE);
        return prefs.getBoolean("is_premium_unlocked", false);
    }

    public static void setPremiumUnlocked(Context context, boolean unlocked) {
        if (context == null) return;
        SharedPreferences prefs = context.getSharedPreferences("prefs", Context.MODE_PRIVATE);
        prefs.edit().putBoolean("is_premium_unlocked", unlocked).apply();

        String userId = UserManager.getUserId(context);
        if (userId != null && !userId.isEmpty()) {
            FirebaseDatabase.getInstance("https://i-must-finish-this-in-2026-default-rtdb.asia-southeast1.firebasedatabase.app/")
                    .getReference("profiles").child(userId).child("isPremium").setValue(unlocked);
        }
    }

    public static void showSubscriptionDialog(AppCompatActivity activity, OnSubscriptionStatusChangedListener listener) {
        if (activity == null) return;

        BottomSheetDialog sheet = new BottomSheetDialog(activity);
        View view = LayoutInflater.from(activity).inflate(R.layout.dialog_subscription, null);
        sheet.setContentView(view);

        View btnMonthly = view.findViewById(R.id.btnSelectMonthly);
        View btnLifetime = view.findViewById(R.id.btnSelectLifetime);
        View cardMonthly = view.findViewById(R.id.cardMonthlySub);
        View cardLifetime = view.findViewById(R.id.cardLifetimeSub);

        View.OnClickListener purchaseListener = v -> {
            sheet.dismiss();
            confirmMockPayment(activity, listener);
        };

        if (btnMonthly != null) btnMonthly.setOnClickListener(purchaseListener);
        if (btnLifetime != null) btnLifetime.setOnClickListener(purchaseListener);
        if (cardMonthly != null) cardMonthly.setOnClickListener(purchaseListener);
        if (cardLifetime != null) cardLifetime.setOnClickListener(purchaseListener);

        sheet.show();
    }

    private static void confirmMockPayment(AppCompatActivity activity, OnSubscriptionStatusChangedListener listener) {
        new MaterialAlertDialogBuilder(activity)
                .setTitle("Confirm Test Payment")
                .setMessage("Simulate payment authorization for Freedom Pass?")
                .setPositiveButton("Authorize ($0.99 / $9.99)", (dialog, which) -> {
                    setPremiumUnlocked(activity, true);
                    Toast.makeText(activity, "🎉 Freedom Pass Unlocked! Ads removed across all screens.", Toast.LENGTH_LONG).show();
                    if (listener != null) {
                        listener.onStatusChanged(true);
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}
