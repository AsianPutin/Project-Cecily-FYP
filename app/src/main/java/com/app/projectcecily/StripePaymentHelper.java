package com.app.projectcecily;

import android.app.ProgressDialog;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.json.JSONObject;

public class StripePaymentHelper {

    private static final String STRIPE_PUBLISHABLE_KEY = "pk_test_51UXwug9S7cbyBifayZTY0exOJdqCgklHMfbMBmk2flTLBd9WMA5T2SEhOBzCGHLkxZDO1belUfIRCDrL1FY17pI700XEMDz094";
    private static final String STRIPE_SECRET_KEY = "sk_test_51UXwug9S7cbyBifa9KKsToYrQIvCv5Sg60n9b8P4e7CWVJVsMaCU5SqkEj1uIj6vhnlTyFOGRW9dhuOmRzVTTuB9004pk8UFoN";
    private static final String PAYMENT_INTENTS_URL = "https://api.stripe.com/v1/payment_intents";
    private static final OkHttpClient client = new OkHttpClient();

    public interface StripePaymentCallback {
        void onSuccess(String paymentIntentId, long amountCents);
        void onFailure(String errorMessage);
    }

    public static void processMockPayment(AppCompatActivity activity, long amountCents, String description, StripePaymentCallback callback) {
        if (activity == null || activity.isFinishing()) return;

        ProgressDialog progressDialog = new ProgressDialog(activity);
        progressDialog.setMessage("Authorizing with Stripe API (Test Mode)...");
        progressDialog.setCancelable(false);
        progressDialog.show();

        new Thread(() -> {
            try {
                RequestBody formBody = new FormBody.Builder()
                        .add("amount", String.valueOf(amountCents))
                        .add("currency", "usd")
                        .add("payment_method", "pm_card_visa")
                        .add("confirm", "true")
                        .add("description", description)
                        .build();

                Request request = new Request.Builder()
                        .url(PAYMENT_INTENTS_URL)
                        .addHeader("Authorization", "Bearer " + STRIPE_SECRET_KEY)
                        .post(formBody)
                        .build();

                try (Response response = client.newCall(request).execute()) {
                    String jsonString = (response.body() != null) ? response.body().string() : "";
                    JSONObject json = new JSONObject(jsonString);

                    new Handler(Looper.getMainLooper()).post(() -> {
                        if (progressDialog.isShowing()) {
                            progressDialog.dismiss();
                        }

                        if (response.isSuccessful() && json.has("id")) {
                            String intentId = json.optString("id", "pi_mock_12345");
                            String status = json.optString("status", "succeeded");
                            showStripeReceiptDialog(activity, intentId, status, amountCents, description, callback);
                        } else {
                            String errorMsg = "Stripe Authorization Failed";
                            if (json.has("error")) {
                                errorMsg = json.optJSONObject("error").optString("message", errorMsg);
                            }
                            Toast.makeText(activity, "⚠️ Stripe Error: " + errorMsg, Toast.LENGTH_LONG).show();
                            if (callback != null) {
                                callback.onFailure(errorMsg);
                            }
                        }
                    });
                }
            } catch (Exception e) {
                new Handler(Looper.getMainLooper()).post(() -> {
                    if (progressDialog.isShowing()) {
                        progressDialog.dismiss();
                    }
                    String errorMsg = (e.getMessage() != null) ? e.getMessage() : "Network error";
                    Toast.makeText(activity, "⚠️ Stripe Error: " + errorMsg, Toast.LENGTH_LONG).show();
                    if (callback != null) {
                        callback.onFailure(errorMsg);
                    }
                });
            }
        }).start();
    }

    private static void showStripeReceiptDialog(AppCompatActivity activity, String intentId, String status, long amountCents, String description, StripePaymentCallback callback) {
        String formattedAmount = String.format(java.util.Locale.US, "$%.2f USD", amountCents / 100.0);

        String receiptMessage = "💳 Stripe Test Mode Payment Authorized\n\n" +
                "• Item: " + description + "\n" +
                "• Amount: " + formattedAmount + "\n" +
                "• Status: " + status.toUpperCase() + " ✅\n" +
                "• Payment ID: " + intentId + "\n" +
                "• Test Card: Visa ending in 4242";

        new MaterialAlertDialogBuilder(activity)
                .setTitle("Stripe Checkout Receipt")
                .setMessage(receiptMessage)
                .setPositiveButton("Done", (dialog, which) -> {
                    if (callback != null) {
                        callback.onSuccess(intentId, amountCents);
                    }
                })
                .setCancelable(false)
                .show();
    }
}
