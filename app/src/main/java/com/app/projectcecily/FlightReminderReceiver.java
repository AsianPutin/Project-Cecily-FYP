package com.app.projectcecily;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import androidx.core.app.NotificationCompat;

public class FlightReminderReceiver extends BroadcastReceiver {

    private static final String CHANNEL_ID = "flight_reminders";

    private static final long[][] VIBRATION_PATTERNS = new long[][]{
        {0, 250, 150, 250},                      // Default Pulse
        {0, 100, 100, 100, 300, 100, 100, 100},  // Heartbeat
        {0, 100, 80, 100},                       // Double Tap
        {0, 600},                                // Long Buzz
        {0, 50, 200, 50}                         // Tick-Tock
    };

    @Override
    public void onReceive(Context context, Intent intent) {
        String flightNum = intent.getStringExtra("flight_num");
        String message = intent.getStringExtra("message");
        boolean isLandingAlert = intent.getBooleanExtra("is_landing", false);
        int notificationId = intent.getIntExtra("id", (int) System.currentTimeMillis());

        String userId = UserManager.getUserId(context);
        String prefsName = "notification_settings_" + (userId.isEmpty() ? "local" : userId);
        android.content.SharedPreferences prefs = context.getSharedPreferences(prefsName, Context.MODE_PRIVATE);

        boolean remindersEnabled = prefs.getBoolean("flight_reminders", true);
        boolean landingEnabled = prefs.getBoolean("landing_alerts", true);

        if (isLandingAlert && !landingEnabled) return;
        if (!isLandingAlert && !remindersEnabled) return;

        createNotificationChannel(context);

        boolean soundEnabled = prefs.getBoolean("sound_enabled", true);
        boolean vibrationEnabled = prefs.getBoolean("vibration_enabled", true);
        int vibrationIndex = prefs.getInt(NotificationSettingsActivity.KEY_VIBRATION_PATTERN_INDEX, 0);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher_round)
                .setContentTitle(isLandingAlert ? "Flight Landing Alert: " + flightNum : "Flight Reminder: " + flightNum)
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

        NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm != null) {
            nm.notify(notificationId, builder.build());
        }
    }

    private void createNotificationChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Flight Reminders",
                    NotificationManager.IMPORTANCE_HIGH
            );
            NotificationManager nm = context.getSystemService(NotificationManager.class);
            if (nm != null) {
                nm.createNotificationChannel(channel);
            }
        }
    }
}