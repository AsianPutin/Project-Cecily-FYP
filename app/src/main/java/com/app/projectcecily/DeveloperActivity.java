package com.app.projectcecily;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;

public class DeveloperActivity extends AppCompatActivity {

    private static final String CHANNEL_ID = "developer_debug_channel";
    private static final long[][] VIBRATION_PATTERNS = new long[][]{
        {0, 250, 150, 250},                      // Default Pulse
        {0, 100, 100, 100, 300, 100, 100, 100},  // Heartbeat
        {0, 100, 80, 100},                       // Double Tap
        {0, 600},                                // Long Buzz
        {0, 50, 200, 50}                         // Tick-Tock
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_developer);

        View appBarView = findViewById(R.id.appBarLayout);
        UIUtils.applyTopInsetToAppBar(appBarView);

        com.google.android.material.appbar.MaterialToolbar toolbar = findViewById(R.id.toolbar);
        if (toolbar != null) {
            setSupportActionBar(toolbar);
            if (getSupportActionBar() != null) {
                getSupportActionBar().setDisplayHomeAsUpEnabled(true);
                getSupportActionBar().setTitle("Developer Debug Menu");
            }
        }

        createNotificationChannel();

        findViewById(R.id.btnForcePokeNotif).setOnClickListener(v -> postSystemNotification("Partner Poke 🔔", "Your partner poked you! Touch back to reply."));
        findViewById(R.id.btnForceFlightReminder).setOnClickListener(v -> postSystemNotification("Flight Departure Reminder ✈️", "Flight MH2522 to Kuching departs in 1 hour! Get ready to board."));
        findViewById(R.id.btnForceFlightTouchdown).setOnClickListener(v -> postSystemNotification("Flight Touchdown Alert 🛬", "Touchdown! Flight MH2522 has safely landed in Kuching."));
        findViewById(R.id.btnForceWishlistNotif).setOnClickListener(v -> postSystemNotification("Wishlist Update 🎁", "Your partner added a new secret gift to your shared wishlist!"));
        findViewById(R.id.btnForceProximityNotif).setOnClickListener(v -> postSystemNotification("Proximity Update 📍", "Your partner is currently 5.2 km away in Kuching."));

        findViewById(R.id.btnTestVibration).setOnClickListener(v -> triggerCurrentVibration());

        Button btnTogglePremium = findViewById(R.id.btnTogglePremiumDemo);
        if (btnTogglePremium != null) {
            boolean isUnlocked = SubscriptionDialogHelper.isPremiumUnlocked(this);
            btnTogglePremium.setText(isUnlocked ? "Reset Freedom Pass (Show Ads)" : "Unlock Freedom Pass (Ad-Free)");

            btnTogglePremium.setOnClickListener(v -> {
                boolean current = SubscriptionDialogHelper.isPremiumUnlocked(this);
                SubscriptionDialogHelper.setPremiumUnlocked(this, !current);
                boolean updated = SubscriptionDialogHelper.isPremiumUnlocked(this);
                btnTogglePremium.setText(updated ? "Reset Freedom Pass (Show Ads)" : "Unlock Freedom Pass (Ad-Free)");
                Toast.makeText(this, updated ? "🎉 Premium Unlocked! Ads hidden." : "🔄 Premium Reset! Ads restored.", Toast.LENGTH_SHORT).show();
            });
        }
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Developer Debug Alerts",
                    NotificationManager.IMPORTANCE_HIGH
            );
            NotificationManager nm = getSystemService(NotificationManager.class);
            if (nm != null) {
                nm.createNotificationChannel(channel);
            }
        }
    }

    private void postSystemNotification(String title, String message) {
        String userId = UserManager.getUserId(this);
        String prefsName = "notification_settings_" + (userId.isEmpty() ? "local" : userId);
        SharedPreferences prefs = getSharedPreferences(prefsName, MODE_PRIVATE);

        boolean soundEnabled = prefs.getBoolean("sound_enabled", true);
        boolean vibrationEnabled = prefs.getBoolean("vibration_enabled", true);
        int vibrationIndex = prefs.getInt(NotificationSettingsActivity.KEY_VIBRATION_PATTERN_INDEX, 0);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher_round)
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true);

        if (soundEnabled) {
            String soundUriStr = prefs.getString(NotificationSettingsActivity.KEY_CUSTOM_SOUND_URI, "");
            if (soundUriStr != null && !soundUriStr.isEmpty()) {
                builder.setSound(android.net.Uri.parse(soundUriStr));
            } else {
                builder.setSound(android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_NOTIFICATION));
            }
        } else {
            builder.setSound(null);
        }

        if (vibrationEnabled) {
            if (vibrationIndex >= 0 && vibrationIndex < VIBRATION_PATTERNS.length) {
                builder.setVibrate(VIBRATION_PATTERNS[vibrationIndex]);
            }
        } else {
            builder.setVibrate(new long[]{0L});
        }

        try {
            NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            if (nm != null) {
                nm.notify((int) System.currentTimeMillis(), builder.build());
                Toast.makeText(this, "Notification posted: " + title, Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Toast.makeText(this, "Failed to post notification: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void triggerCurrentVibration() {
        String userId = UserManager.getUserId(this);
        String prefsName = "notification_settings_" + (userId.isEmpty() ? "local" : userId);
        SharedPreferences prefs = getSharedPreferences(prefsName, MODE_PRIVATE);
        int vibrationIndex = prefs.getInt(NotificationSettingsActivity.KEY_VIBRATION_PATTERN_INDEX, 0);

        if (vibrationIndex < 0 || vibrationIndex >= VIBRATION_PATTERNS.length) vibrationIndex = 0;
        long[] pattern = VIBRATION_PATTERNS[vibrationIndex];

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                android.os.VibratorManager vm = (android.os.VibratorManager) getSystemService(Context.VIBRATOR_MANAGER_SERVICE);
                if (vm != null) {
                    vm.getDefaultVibrator().vibrate(android.os.VibrationEffect.createWaveform(pattern, -1));
                }
            } else {
                android.os.Vibrator v = (android.os.Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
                if (v != null) {
                    v.vibrate(pattern, -1);
                }
            }
            Toast.makeText(this, "Vibrating pattern index: " + vibrationIndex, Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(this, "Vibration failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
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
