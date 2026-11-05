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

    @Override
    public void onReceive(Context context, Intent intent) {
        String flightNum = intent.getStringExtra("flight_num");
        String message = intent.getStringExtra("message");
        int notificationId = intent.getIntExtra("id", (int) System.currentTimeMillis());

        createNotificationChannel(context);

        String userId = UserManager.getUserId(context);
        String prefsName = "notification_settings_" + (userId.isEmpty() ? "local" : userId);
        android.content.SharedPreferences prefs = context.getSharedPreferences(prefsName, Context.MODE_PRIVATE);
        String soundUriStr = prefs.getString(NotificationSettingsActivity.KEY_CUSTOM_SOUND_URI, "");

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher_round)
                .setContentTitle("Flight Reminder: " + flightNum)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true);

        if (soundUriStr != null && !soundUriStr.isEmpty()) {
            builder.setSound(android.net.Uri.parse(soundUriStr));
        } else {
            builder.setSound(android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_NOTIFICATION));
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