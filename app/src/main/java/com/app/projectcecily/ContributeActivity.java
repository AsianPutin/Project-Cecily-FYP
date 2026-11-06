package com.app.projectcecily;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class ContributeActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_contribute);

        View appBarView = findViewById(R.id.appBarLayout);
        UIUtils.applyTopInsetToAppBar(appBarView);

        com.google.android.material.appbar.MaterialToolbar toolbar = findViewById(R.id.toolbar);
        if (toolbar != null) {
            setSupportActionBar(toolbar);
            if (getSupportActionBar() != null) {
                getSupportActionBar().setDisplayHomeAsUpEnabled(true);
                getSupportActionBar().setTitle("Contribute & Support");
            }
        }

        View btnKofi = findViewById(R.id.btnKofiSupport);
        View btnMonthly = findViewById(R.id.btnSubMonthly);
        View btnLifetime = findViewById(R.id.btnSubLifetime);

        if (btnKofi != null) {
            btnKofi.setOnClickListener(v -> {
                androidx.appcompat.app.AlertDialog dialog = new com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
                        .setTitle("Support on Ko-fi ☕")
                        .setMessage("This will take you outside the app to open the official Ko-fi support page in your web browser.")
                        .setPositiveButton("Continue", (d, which) -> {
                            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://ko-fi.com/O8O02A5TXU"));
                            startActivity(browserIntent);
                        })
                        .setNegativeButton("Cancel", null)
                        .show();

                android.widget.Button btnCancel = dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_NEGATIVE);
                if (btnCancel != null) {
                    btnCancel.setTextColor(android.graphics.Color.parseColor("#FF3B30"));
                }
            });
        }

        if (btnMonthly != null) {
            btnMonthly.setOnClickListener(v -> StripePaymentHelper.processMockPayment(
                this, 99, "Monthly Ad-Free Pass ($0.99/mo)", new StripePaymentHelper.StripePaymentCallback() {
                    @Override
                    public void onSuccess(String paymentIntentId, long amountCents) {
                        SubscriptionDialogHelper.setSubscriptionMode(ContributeActivity.this, SubscriptionDialogHelper.MODE_MONTHLY);
                        Toast.makeText(ContributeActivity.this, "🎉 Monthly Ad-Free Pass Unlocked! Ads removed across all screens.", Toast.LENGTH_LONG).show();
                        updateSubscriptionUi();
                    }

                    @Override
                    public void onFailure(String errorMessage) {}
                }
            ));
        }

        if (btnLifetime != null) {
            btnLifetime.setOnClickListener(v -> StripePaymentHelper.processMockPayment(
                this, 999, "Lifetime Ad-Free Access ($9.99)", new StripePaymentHelper.StripePaymentCallback() {
                    @Override
                    public void onSuccess(String paymentIntentId, long amountCents) {
                        SubscriptionDialogHelper.setSubscriptionMode(ContributeActivity.this, SubscriptionDialogHelper.MODE_LIFETIME);
                        Toast.makeText(ContributeActivity.this, "🎉 Lifetime Ad-Free Access Unlocked! Ads removed across all screens.", Toast.LENGTH_LONG).show();
                        updateSubscriptionUi();
                    }

                    @Override
                    public void onFailure(String errorMessage) {}
                }
            ));
        }
    }

    private void updateSubscriptionUi() {
        int mode = SubscriptionDialogHelper.getSubscriptionMode(this);
        View btnMonthly = findViewById(R.id.btnSubMonthly);
        View btnLifetime = findViewById(R.id.btnSubLifetime);
        View cardSub = findViewById(R.id.cardContributeSubscriptions);
        View tvThankYou = findViewById(R.id.tvThankYouMessage);

        if (mode == SubscriptionDialogHelper.MODE_MONTHLY) {
            if (btnMonthly != null) {
                btnMonthly.setEnabled(false);
                btnMonthly.setAlpha(0.4f);
            }
            if (btnLifetime != null) {
                btnLifetime.setEnabled(true);
                btnLifetime.setAlpha(1.0f);
            }
            if (cardSub != null) {
                cardSub.setAlpha(1.0f);
            }
            if (tvThankYou != null) {
                tvThankYou.setVisibility(View.GONE);
            }
        } else if (mode == SubscriptionDialogHelper.MODE_LIFETIME) {
            if (btnMonthly != null) {
                btnMonthly.setEnabled(false);
                btnMonthly.setAlpha(0.4f);
            }
            if (btnLifetime != null) {
                btnLifetime.setEnabled(false);
                btnLifetime.setAlpha(0.4f);
            }
            if (cardSub != null) {
                cardSub.setAlpha(0.4f);
            }
            if (tvThankYou != null) {
                tvThankYou.setVisibility(View.VISIBLE);
            }
        } else {
            if (btnMonthly != null) {
                btnMonthly.setEnabled(true);
                btnMonthly.setAlpha(1.0f);
            }
            if (btnLifetime != null) {
                btnLifetime.setEnabled(true);
                btnLifetime.setAlpha(1.0f);
            }
            if (cardSub != null) {
                cardSub.setAlpha(1.0f);
            }
            if (tvThankYou != null) {
                tvThankYou.setVisibility(View.GONE);
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        ThemeUtils.applyAccentToActivity(this);
        updateSubscriptionUi();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
