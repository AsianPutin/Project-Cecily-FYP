package com.app.projectcecily;

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
            btnKofi.setOnClickListener(v -> StripePaymentHelper.processMockPayment(
                this, 300, "Developer Ko-fi Support ($3.00 USD)", new StripePaymentHelper.StripePaymentCallback() {
                    @Override
                    public void onSuccess(String paymentIntentId, long amountCents) {
                        Toast.makeText(ContributeActivity.this, "☕ Thank you for supporting the developer!", Toast.LENGTH_LONG).show();
                    }

                    @Override
                    public void onFailure(String errorMessage) {}
                }
            ));
        }

        if (btnMonthly != null) {
            btnMonthly.setOnClickListener(v -> StripePaymentHelper.processMockPayment(
                this, 99, "Freedom Pass - Monthly Impulse ($0.99/mo)", new StripePaymentHelper.StripePaymentCallback() {
                    @Override
                    public void onSuccess(String paymentIntentId, long amountCents) {
                        SubscriptionDialogHelper.setPremiumUnlocked(ContributeActivity.this, true);
                        Toast.makeText(ContributeActivity.this, "🎉 Freedom Pass Monthly Unlocked! Ads removed across all screens.", Toast.LENGTH_LONG).show();
                    }

                    @Override
                    public void onFailure(String errorMessage) {}
                }
            ));
        }

        if (btnLifetime != null) {
            btnLifetime.setOnClickListener(v -> StripePaymentHelper.processMockPayment(
                this, 999, "Freedom Pass - Lifetime Access ($9.99)", new StripePaymentHelper.StripePaymentCallback() {
                    @Override
                    public void onSuccess(String paymentIntentId, long amountCents) {
                        SubscriptionDialogHelper.setPremiumUnlocked(ContributeActivity.this, true);
                        Toast.makeText(ContributeActivity.this, "🎉 Freedom Pass Lifetime Unlocked! Ads removed across all screens.", Toast.LENGTH_LONG).show();
                    }

                    @Override
                    public void onFailure(String errorMessage) {}
                }
            ));
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        ThemeUtils.applyAccentToActivity(this);
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
