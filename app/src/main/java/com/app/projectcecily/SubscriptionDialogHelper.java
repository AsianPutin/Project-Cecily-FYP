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

    public static final int MODE_FREE = 0;
    public static final int MODE_MONTHLY = 1;
    public static final int MODE_LIFETIME = 2;

    public interface OnSubscriptionStatusChangedListener {
        void onStatusChanged(boolean isUnlocked);
    }

    public static int getSubscriptionMode(Context context) {
        if (context == null) return MODE_FREE;
        SharedPreferences prefs = context.getSharedPreferences("prefs", Context.MODE_PRIVATE);
        return prefs.getInt("premium_subscription_mode", MODE_FREE);
    }

    public static void setSubscriptionMode(Context context, int mode) {
        if (context == null) return;
        SharedPreferences prefs = context.getSharedPreferences("prefs", Context.MODE_PRIVATE);
        prefs.edit()
                .putInt("premium_subscription_mode", mode)
                .putBoolean("is_premium_unlocked", mode != MODE_FREE)
                .apply();

        String userId = UserManager.getUserId(context);
        if (userId != null && !userId.isEmpty()) {
            FirebaseDatabase.getInstance("https://i-must-finish-this-in-2026-default-rtdb.asia-southeast1.firebasedatabase.app/")
                    .getReference("profiles").child(userId).child("isPremium").setValue(mode != MODE_FREE);
        }
    }

    public static boolean isPremiumUnlocked(Context context) {
        return getSubscriptionMode(context) != MODE_FREE;
    }

    public static void setPremiumUnlocked(Context context, boolean unlocked) {
        setSubscriptionMode(context, unlocked ? MODE_LIFETIME : MODE_FREE);
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

        int mode = getSubscriptionMode(activity);
        if (mode == MODE_MONTHLY) {
            if (cardMonthly != null) {
                cardMonthly.setAlpha(0.4f);
                cardMonthly.setEnabled(false);
            }
            if (btnMonthly != null) {
                btnMonthly.setEnabled(false);
            }
        } else if (mode == MODE_LIFETIME) {
            if (cardMonthly != null) {
                cardMonthly.setAlpha(0.4f);
                cardMonthly.setEnabled(false);
            }
            if (btnMonthly != null) btnMonthly.setEnabled(false);

            if (cardLifetime != null) {
                cardLifetime.setAlpha(0.4f);
                cardLifetime.setEnabled(false);
            }
            if (btnLifetime != null) btnLifetime.setEnabled(false);
        }

        View.OnClickListener monthlyListener = v -> {
            sheet.dismiss();
            StripePaymentHelper.processMockPayment(activity, 99, "Monthly Ad-Free Pass ($0.99/mo)", new StripePaymentHelper.StripePaymentCallback() {
                @Override
                public void onSuccess(String paymentIntentId, long amountCents) {
                    setSubscriptionMode(activity, MODE_MONTHLY);
                    Toast.makeText(activity, "🎉 Monthly Ad-Free Pass Unlocked! Ads removed across all screens.", Toast.LENGTH_LONG).show();
                    if (listener != null) listener.onStatusChanged(true);
                }

                @Override
                public void onFailure(String errorMessage) {}
            });
        };

        View.OnClickListener lifetimeListener = v -> {
            sheet.dismiss();
            StripePaymentHelper.processMockPayment(activity, 999, "Lifetime Ad-Free Access ($9.99)", new StripePaymentHelper.StripePaymentCallback() {
                @Override
                public void onSuccess(String paymentIntentId, long amountCents) {
                    setSubscriptionMode(activity, MODE_LIFETIME);
                    Toast.makeText(activity, "🎉 Lifetime Ad-Free Access Unlocked! Ads removed across all screens.", Toast.LENGTH_LONG).show();
                    if (listener != null) listener.onStatusChanged(true);
                }

                @Override
                public void onFailure(String errorMessage) {}
            });
        };

        if (mode != MODE_MONTHLY && mode != MODE_LIFETIME) {
            if (btnMonthly != null) btnMonthly.setOnClickListener(monthlyListener);
            if (cardMonthly != null) cardMonthly.setOnClickListener(monthlyListener);
        }
        if (mode != MODE_LIFETIME) {
            if (btnLifetime != null) btnLifetime.setOnClickListener(lifetimeListener);
            if (cardLifetime != null) cardLifetime.setOnClickListener(lifetimeListener);
        }

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
