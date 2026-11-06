package com.app.projectcecily;

import android.app.ProgressDialog;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
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

    public static boolean isValidLuhn(String cardNumber) {
        String digits = cardNumber.replaceAll("\\D+", "");
        if (digits.length() < 13 || digits.length() > 19) return false;

        int sum = 0;
        boolean alternate = false;
        for (int i = digits.length() - 1; i >= 0; i--) {
            int n = Integer.parseInt(digits.substring(i, i + 1));
            if (alternate) {
                n *= 2;
                if (n > 9) n = (n % 10) + 1;
            }
            sum += n;
            alternate = !alternate;
        }
        return (sum % 10 == 0);
    }

    public static boolean isValidExpiry(String exp) {
        if (exp == null || !exp.contains("/")) return false;
        String[] parts = exp.split("/");
        if (parts.length != 2) return false;
        try {
            int month = Integer.parseInt(parts[0].trim());
            int year = Integer.parseInt(parts[1].trim());
            if (year < 100) year += 2000;
            if (month < 1 || month > 12) return false;

            java.util.Calendar cal = java.util.Calendar.getInstance();
            int curYear = cal.get(java.util.Calendar.YEAR);
            int curMonth = cal.get(java.util.Calendar.MONTH) + 1;

            if (year < curYear) return false;
            if (year == curYear && month < curMonth) return false;
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public static String getCardBrand(String number) {
        String digits = number.replaceAll("\\D+", "");
        if (digits.startsWith("4")) return "Visa";
        if (digits.startsWith("51") || digits.startsWith("52") || digits.startsWith("53") || digits.startsWith("54") || digits.startsWith("55")) return "Mastercard";
        if (digits.startsWith("34") || digits.startsWith("37")) return "American Express";
        if (digits.startsWith("6011") || digits.startsWith("65")) return "Discover";
        return "Card";
    }

    public static void processMockPayment(AppCompatActivity activity, long amountCents, String description, StripePaymentCallback callback) {
        if (activity == null || activity.isFinishing()) return;

        BottomSheetDialog sheet = new BottomSheetDialog(activity);
        View view = LayoutInflater.from(activity).inflate(R.layout.dialog_stripe_card_checkout, null);
        sheet.setContentView(view);

        TextView tvDesc = view.findViewById(R.id.tvCheckoutDescription);
        TextInputEditText etCardNumber = view.findViewById(R.id.etCardNumber);
        TextInputEditText etCardExpiry = view.findViewById(R.id.etCardExpiry);
        TextInputEditText etCardCvc = view.findViewById(R.id.etCardCvc);

        Button btnPayNow = view.findViewById(R.id.btnStripePayNow);
        Button btnCancel = view.findViewById(R.id.btnStripeCancel);

        String formattedAmount = String.format(java.util.Locale.US, "$%.2f USD", amountCents / 100.0);
        if (tvDesc != null) {
            tvDesc.setText(description + " • " + formattedAmount);
        }

        if (btnPayNow != null) {
            btnPayNow.setText("Pay " + formattedAmount + " 🔒");
            btnPayNow.setOnClickListener(v -> {
                String rawCardNum = (etCardNumber != null && etCardNumber.getText() != null) ? etCardNumber.getText().toString().trim() : "";
                String rawExpiry = (etCardExpiry != null && etCardExpiry.getText() != null) ? etCardExpiry.getText().toString().trim() : "";
                String rawCvc = (etCardCvc != null && etCardCvc.getText() != null) ? etCardCvc.getText().toString().trim() : "";

                if (!isValidLuhn(rawCardNum)) {
                    Toast.makeText(activity, "⚠️ Invalid Credit Card Number! Checksum check failed.", Toast.LENGTH_LONG).show();
                    return;
                }

                if (!isValidExpiry(rawExpiry)) {
                    Toast.makeText(activity, "⚠️ Invalid Expiration Date! Format must be MM/YY.", Toast.LENGTH_LONG).show();
                    return;
                }

                if (rawCvc.length() < 3) {
                    Toast.makeText(activity, "⚠️ Invalid CVC Code! Please enter 3 or 4 digits.", Toast.LENGTH_LONG).show();
                    return;
                }

                String digits = rawCardNum.replaceAll("\\D+", "");
                if ("4000000200110044".equals(digits)) {
                    Toast.makeText(activity, "⚠️ Stripe Error: Your card was declined by the bank.", Toast.LENGTH_LONG).show();
                    return;
                }

                String last4 = digits.length() >= 4 ? digits.substring(digits.length() - 4) : "4242";
                String brand = getCardBrand(rawCardNum);

                sheet.dismiss();
                executeStripeApiCharge(activity, amountCents, description, brand, last4, callback);
            });
        }

        if (btnCancel != null) {
            btnCancel.setOnClickListener(v -> sheet.dismiss());
        }

        sheet.show();
    }

    private static void executeStripeApiCharge(AppCompatActivity activity, long amountCents, String description, String cardBrand, String last4, StripePaymentCallback callback) {
        ProgressDialog progressDialog = new ProgressDialog(activity);
        progressDialog.setMessage("Authorizing with Stripe API (Test Mode)...");
        progressDialog.setCancelable(false);
        progressDialog.show();

        String userName = UserManager.getUserName(activity);
        String userEmail = UserManager.getUserEmail(activity);
        if (userName == null || userName.trim().isEmpty() || userName.equalsIgnoreCase("User")) {
            userName = "Alister Goh";
        }
        if (userEmail == null || userEmail.trim().isEmpty()) {
            userEmail = "alister@example.com";
        }

        final String finalUserName = userName;
        final String finalUserEmail = userEmail;

        new Thread(() -> {
            try {
                String customerId = "";
                try {
                    RequestBody custBody = new FormBody.Builder()
                            .add("name", finalUserName)
                            .add("email", finalUserEmail)
                            .build();

                    Request custReq = new Request.Builder()
                            .url("https://api.stripe.com/v1/customers")
                            .addHeader("Authorization", "Bearer " + STRIPE_SECRET_KEY)
                            .post(custBody)
                            .build();

                    try (Response custRes = client.newCall(custReq).execute()) {
                        if (custRes.isSuccessful() && custRes.body() != null) {
                            JSONObject custJson = new JSONObject(custRes.body().string());
                            if (custJson.has("id")) {
                                customerId = custJson.getString("id");
                            }
                        }
                    }
                } catch (Exception ignored) {}

                FormBody.Builder formBuilder = new FormBody.Builder()
                        .add("amount", String.valueOf(amountCents))
                        .add("currency", "usd")
                        .add("payment_method", "pm_card_visa")
                        .add("confirm", "true")
                        .add("description", description)
                        .add("receipt_email", finalUserEmail)
                        .add("automatic_payment_methods[enabled]", "true")
                        .add("automatic_payment_methods[allow_redirects]", "never");

                if (!customerId.isEmpty()) {
                    formBuilder.add("customer", customerId);
                }

                Request request = new Request.Builder()
                        .url(PAYMENT_INTENTS_URL)
                        .addHeader("Authorization", "Bearer " + STRIPE_SECRET_KEY)
                        .post(formBuilder.build())
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
                            showStripeReceiptDialog(activity, intentId, status, amountCents, description, cardBrand, last4, callback);
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

    private static void showStripeReceiptDialog(AppCompatActivity activity, String intentId, String status, long amountCents, String description, String cardBrand, String last4, StripePaymentCallback callback) {
        String formattedAmount = String.format(java.util.Locale.US, "$%.2f USD", amountCents / 100.0);

        String receiptMessage = "💳 Stripe Test Mode Payment Authorized\n\n" +
                "• Item: " + description + "\n" +
                "• Amount: " + formattedAmount + "\n" +
                "• Status: " + status.toUpperCase() + " ✅\n" +
                "• Payment ID: " + intentId + "\n" +
                "• Test Card: " + cardBrand + " ending in " + last4;

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
