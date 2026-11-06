package com.app.projectcecily;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class SettingsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        View appBarView = findViewById(R.id.appBarLayout);
        UIUtils.applyTopInsetToAppBar(appBarView);

        com.google.android.material.appbar.MaterialToolbar toolbar = findViewById(R.id.toolbar);
        if (toolbar != null) {
            setSupportActionBar(toolbar);
            if (getSupportActionBar() != null) {
                getSupportActionBar().setDisplayHomeAsUpEnabled(true);
                getSupportActionBar().setTitle("Settings");
            }
        }

        View cardFreedom = findViewById(R.id.cardFreedomPassSettings);
        View btnUpgrade = findViewById(R.id.btnUpgradeFreedomPass);
        View btnReset = findViewById(R.id.btnResetFreedomPass);

        View.OnClickListener showSubListener = v -> SubscriptionDialogHelper.showSubscriptionDialog(this, unlocked -> updateFreedomPassCard());

        if (cardFreedom != null) cardFreedom.setOnClickListener(showSubListener);
        if (btnUpgrade != null) btnUpgrade.setOnClickListener(showSubListener);

        if (btnReset != null) {
            btnReset.setOnClickListener(v -> {
                SubscriptionDialogHelper.setPremiumUnlocked(this, false);
                updateFreedomPassCard();
                Toast.makeText(this, "Demo Reset: Ads re-enabled for presentation testing.", Toast.LENGTH_SHORT).show();
            });
        }

        updateFreedomPassCard();

        findViewById(R.id.cardProfileSettings).setOnClickListener(v -> {
            startActivity(new Intent(this, ProfileSettingsActivity.class));
        });

        findViewById(R.id.cardAppearanceSettings).setOnClickListener(v -> {
            startActivity(new Intent(this, AppearanceActivity.class));
        });

        findViewById(R.id.cardNotificationSettings).setOnClickListener(v -> {
            startActivity(new Intent(this, NotificationSettingsActivity.class));
        });
    }

    private void updateFreedomPassCard() {
        TextView tvBadge = findViewById(R.id.tvFreedomStatusBadge);
        TextView tvSubtext = findViewById(R.id.tvFreedomSubtext);
        View btnUpgrade = findViewById(R.id.btnUpgradeFreedomPass);

        boolean unlocked = SubscriptionDialogHelper.isPremiumUnlocked(this);
        if (tvBadge != null) {
            tvBadge.setText(unlocked ? "🌟 Ad-Free Premium Active" : "Free Tier");
        }
        if (tvSubtext != null) {
            tvSubtext.setText(unlocked ? "Your Ad-Free Premium status is active! Banner ads are permanently hidden app-wide." : "Unlock a clean, 100% ad-free experience across all screens.");
        }
        if (btnUpgrade != null) {
            btnUpgrade.setVisibility(unlocked ? View.GONE : View.VISIBLE);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        ThemeUtils.applyAccentToActivity(this);
        updateFreedomPassCard();
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
