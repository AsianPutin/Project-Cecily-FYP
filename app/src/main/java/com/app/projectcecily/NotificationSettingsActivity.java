package com.app.projectcecily;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.materialswitch.MaterialSwitch;

public class NotificationSettingsActivity extends AppCompatActivity {

    private static final String PREFS_NAME = "notification_settings";
    private SharedPreferences prefs;

    private MaterialSwitch switchFlightReminders, switchLandingAlerts;
    private MaterialSwitch switchPartnerPokes, switchProximityAlerts;
    private MaterialSwitch switchWishlistAlerts, switchSound, switchVibration;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notification_settings);

        prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

        View appBarView = findViewById(R.id.appBarLayout);
        UIUtils.applyTopInsetToAppBar(appBarView);

        com.google.android.material.appbar.MaterialToolbar toolbar = findViewById(R.id.toolbar);
        if (toolbar != null) {
            setSupportActionBar(toolbar);
            if (getSupportActionBar() != null) {
                getSupportActionBar().setDisplayHomeAsUpEnabled(true);
                getSupportActionBar().setTitle("Notification Settings");
            }
        }

        switchFlightReminders = findViewById(R.id.switchFlightReminders);
        switchLandingAlerts = findViewById(R.id.switchLandingAlerts);
        switchPartnerPokes = findViewById(R.id.switchPartnerPokes);
        switchProximityAlerts = findViewById(R.id.switchProximityAlerts);
        switchWishlistAlerts = findViewById(R.id.switchWishlistAlerts);
        switchSound = findViewById(R.id.switchSound);
        switchVibration = findViewById(R.id.switchVibration);

        loadPreferences();
        setupSwitchListeners();
    }

    private void loadPreferences() {
        switchFlightReminders.setChecked(prefs.getBoolean("flight_reminders", true));
        switchLandingAlerts.setChecked(prefs.getBoolean("landing_alerts", true));
        switchPartnerPokes.setChecked(prefs.getBoolean("partner_pokes", true));
        switchProximityAlerts.setChecked(prefs.getBoolean("proximity_alerts", true));
        switchWishlistAlerts.setChecked(prefs.getBoolean("wishlist_alerts", true));
        switchSound.setChecked(prefs.getBoolean("sound_enabled", true));
        switchVibration.setChecked(prefs.getBoolean("vibration_enabled", true));
    }

    private void setupSwitchListeners() {
        switchFlightReminders.setOnCheckedChangeListener((v, isChecked) -> prefs.edit().putBoolean("flight_reminders", isChecked).apply());
        switchLandingAlerts.setOnCheckedChangeListener((v, isChecked) -> prefs.edit().putBoolean("landing_alerts", isChecked).apply());
        switchPartnerPokes.setOnCheckedChangeListener((v, isChecked) -> prefs.edit().putBoolean("partner_pokes", isChecked).apply());
        switchProximityAlerts.setOnCheckedChangeListener((v, isChecked) -> prefs.edit().putBoolean("proximity_alerts", isChecked).apply());
        switchWishlistAlerts.setOnCheckedChangeListener((v, isChecked) -> prefs.edit().putBoolean("wishlist_alerts", isChecked).apply());
        switchSound.setOnCheckedChangeListener((v, isChecked) -> prefs.edit().putBoolean("sound_enabled", isChecked).apply());
        switchVibration.setOnCheckedChangeListener((v, isChecked) -> prefs.edit().putBoolean("vibration_enabled", isChecked).apply());
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
