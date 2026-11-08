package com.app.projectcecily;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class BackgroundService extends Service {

    private static final String CHANNEL_ID = "background_sync";
    private long startTime;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        createNotificationChannel();
        
        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("LDR Sync Active")
                .setContentText("Project Cecily is waiting for partner pokes...")
                .setSmallIcon(IconManager.getNotificationSmallIcon())
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build();

        startForeground(101, notification);
        
        startTime = System.currentTimeMillis();
        String userId = UserManager.getUserId(this);
        listenForPokes(userId);
        listenForFlights();

        return START_STICKY;
    }

    private void listenForFlights() {
        FirebaseDatabase.getInstance("https://i-must-finish-this-in-2026-default-rtdb.asia-southeast1.firebasedatabase.app/")
                .getReference("flights")
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        for (DataSnapshot ds : snapshot.getChildren()) {
                            FlightInfo flight = ds.getValue(FlightInfo.class);
                            if (flight != null) {
                                checkFlightChanges(flight);
                            }
                        }
                    }
                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {}
                });
    }

    private void checkFlightChanges(FlightInfo flight) {
        // Detect Status Change
        if (flight.status != null && !flight.status.equals(flight.lastNotifiedStatus)) {
            String message = "";
            if ("ACTIVE".equals(flight.status)) message = "Flight " + flight.flightNumber + " has taken off! 🛫";
            else if ("LANDED".equals(flight.status)) message = "Flight " + flight.flightNumber + " has landed safely! 🛬";
            else if ("DELAYED".equals(flight.status)) message = "Flight " + flight.flightNumber + " is delayed. ⏳";

            if (!message.isEmpty()) {
                sendFlightNotification(flight.flightNumber, message);
                // Update Firebase to prevent duplicate notifications
                FirebaseDatabase.getInstance("https://i-must-finish-this-in-2026-default-rtdb.asia-southeast1.firebasedatabase.app/")
                        .getReference("flights").child(flight.id).child("lastNotifiedStatus").setValue(flight.status);
            }
        }
    }

    private void sendFlightNotification(String title, String message) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, "poke_channel")
                .setSmallIcon(IconManager.getNotificationSmallIcon())
                .setLargeIcon(IconManager.getNotificationLargeIcon(this))
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true);

        NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (nm != null) nm.notify((int) System.currentTimeMillis(), builder.build());
    }

    private void listenForPokes(String userId) {
        FirebaseDatabase.getInstance("https://i-must-finish-this-in-2026-default-rtdb.asia-southeast1.firebasedatabase.app/")
                .getReference("pokes").child(userId)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        Long timestamp = snapshot.getValue(Long.class);
                        if (timestamp != null && timestamp > startTime) {
                            startTime = timestamp;
                            sendPokeNotification();
                        }
                    }
                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {}
                });
    }

    private void sendPokeNotification() {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, "poke_channel")
                .setSmallIcon(IconManager.getNotificationSmallIcon())
                .setLargeIcon(IconManager.getNotificationLargeIcon(this))
                .setContentTitle("Partner Poke 🔔")
                .setContentText("Your partner poked you!")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true);

        NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (nm != null) nm.notify((int)System.currentTimeMillis(), builder.build());
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, "LDR Sync", NotificationManager.IMPORTANCE_LOW);
            NotificationManager nm = getSystemService(NotificationManager.class);
            if (nm != null) nm.createNotificationChannel(channel);
        }
    }

    @Override
    public IBinder onBind(Intent intent) { return null; }
}