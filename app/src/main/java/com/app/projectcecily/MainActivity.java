package com.app.projectcecily;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class MainActivity extends AppCompatActivity {

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;
    private static final String CHANNEL_ID = "poke_channel";
    private LocationTracker tracker;
    private String userId;
    private String partnerId;
    private long startTime;
    private long backPressedTime = 0;

    private TextView tvDistance, tvWishlist, tvFlightSummary, tvTogetherDays, tvBirthdaysSummary;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        int savedMode = getSharedPreferences("prefs", MODE_PRIVATE).getInt("theme_mode", AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
        AppCompatDelegate.setDefaultNightMode(savedMode);

        super.onCreate(savedInstanceState);

        if (!UserManager.isLoggedIn(this)) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        setContentView(R.layout.activity_main);
        
        createNotificationChannel();
        requestNotificationPermission();

        userId = UserManager.getUserId(this);
        partnerId = UserManager.getPartnerId(this);

        // Keep partnerId synced in real time
        FirebaseDatabase.getInstance("https://i-must-finish-this-in-2026-default-rtdb.asia-southeast1.firebasedatabase.app/")
                .getReference("profiles").child(userId).child("partnerId")
                .addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String pId = snapshot.getValue(String.class);
                if (pId != null && !pId.isEmpty()) {
                    boolean wasEmpty = partnerId == null || partnerId.isEmpty();
                    partnerId = pId;
                    UserManager.setPartnerId(MainActivity.this, pId);
                    if (wasEmpty) {
                        startTracking();
                        listenForWishlistCount();
                        listenForFlights();
                    }
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
        
        startTime = System.currentTimeMillis();

        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        tvDistance = findViewById(R.id.tvLiveDistance);
        if (partnerId == null || partnerId.isEmpty()) {
            tvDistance.setText("Not paired yet");
        }
        tvWishlist = findViewById(R.id.tvWishlistSummary);
        tvFlightSummary = findViewById(R.id.tvFlightSummary);
        tvTogetherDays = findViewById(R.id.tvTogetherDays);
        tvBirthdaysSummary = findViewById(R.id.tvBirthdaysSummary);

        RelationshipManager.listenFromFirebase(this, details -> {
            String pId = UserManager.getPartnerId(this);
            if (pId == null || pId.isEmpty()) {
                tvTogetherDays.setText("Not paired yet");
                tvBirthdaysSummary.setText("Pair in Account Settings");
            } else {
                tvTogetherDays.setText(RelationshipManager.formatDaysTogether(details.anniversaryTimestamp));
                tvBirthdaysSummary.setText(RelationshipManager.formatPartnerBirthdayCountdown(details.partnerBirthday));
            }
        });

        setupDashboardClicks();
        setupNavigation();

        // Double-tap back press to exit
        getOnBackPressedDispatcher().addCallback(this, new androidx.activity.OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (backPressedTime + 2000 > System.currentTimeMillis()) {
                    finish();
                } else {
                    Toast.makeText(MainActivity.this, "Press back again to exit", Toast.LENGTH_SHORT).show();
                    backPressedTime = System.currentTimeMillis();
                }
            }
        });
        
        UIUtils.applyBottomPadding(findViewById(R.id.bottom_navigation));
        UIUtils.applyBottomPadding(findViewById(R.id.dashboard_scroll));

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            startTracking();
        } else {
            checkLocationPermissions();
        }

        listenForPokes();
        listenForProfileNames();
        listenForWishlistCount();
        listenForFlights();
        scheduleFlightTracking();
    }

    private void scheduleFlightTracking() {
        androidx.work.PeriodicWorkRequest request = new androidx.work.PeriodicWorkRequest.Builder(FlightWorker.class, 1, java.util.concurrent.TimeUnit.HOURS).build();
        androidx.work.WorkManager.getInstance(this).enqueueUniquePeriodicWork("flight_sync", androidx.work.ExistingPeriodicWorkPolicy.KEEP, request);
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateDynamicAccent();
        ThemeUtils.applyAccentToActivity(this);
    }

    private void updateDynamicAccent() {
        BottomNavigationView navView = findViewById(R.id.bottom_navigation);
        if (navView != null) {
            int accentColor = ThemeUtils.getAccentColor(this);
            int unselectedColor = android.graphics.Color.parseColor("#888888");

            int[][] states = new int[][]{
                new int[]{android.R.attr.state_checked},
                new int[]{-android.R.attr.state_checked}
            };

            int[] colors = new int[]{
                accentColor,
                unselectedColor
            };

            android.content.res.ColorStateList colorList = new android.content.res.ColorStateList(states, colors);
            navView.setItemIconTintList(colorList);
            navView.setItemTextColor(colorList);
        }
    }

    private void setupNavigation() {
        BottomNavigationView navView = findViewById(R.id.bottom_navigation);
        navView.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_swipe) {
                startActivity(new Intent(this, SwipeActivity.class));
                return true;
            } else if (id == R.id.nav_location) {
                startActivity(new Intent(this, TrackingActivity.class));
                return true;
            } else if (id == R.id.nav_flight) {
                startActivity(new Intent(this, FlightTrackerActivity.class));
                return true;
            } else if (id == R.id.nav_wishlist) {
                startActivity(new Intent(this, WishlistActivity.class));
                return true;
            } else if (id == R.id.nav_account) {
                startActivity(new Intent(this, AccountActivity.class));
                return true;
            }
            return false;
        });
    }

    private void setupDashboardClicks() {
        findViewById(R.id.cardProximity).setOnClickListener(v -> startActivity(new Intent(this, TrackingActivity.class)));
        findViewById(R.id.cardWishlist).setOnClickListener(v -> startActivity(new Intent(this, WishlistActivity.class)));
        findViewById(R.id.cardFlight).setOnClickListener(v -> startActivity(new Intent(this, FlightTrackerActivity.class)));
    }

    private void listenForFlights() {
        FirebaseDatabase.getInstance("https://i-must-finish-this-in-2026-default-rtdb.asia-southeast1.firebasedatabase.app/")
                .getReference("flight_history").child(userId).orderByChild("depTimeMillis").limitToFirst(1)
                .addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    for (DataSnapshot ds : snapshot.getChildren()) {
                        FlightInfo info = ds.getValue(FlightInfo.class);
                        if (info != null) {
                            tvFlightSummary.setText(info.flightNumber + " to " + info.arrAirport);
                        }
                    }
                } else {
                    tvFlightSummary.setText("No upcoming flights");
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void listenForWishlistCount() {
        String pId = UserManager.getPartnerId(this);
        String pairKey = (pId == null || pId.isEmpty()) ? userId : (userId.compareTo(pId) < 0 ? userId + "_" + pId : pId + "_" + userId);
        FirebaseDatabase.getInstance("https://i-must-finish-this-in-2026-default-rtdb.asia-southeast1.firebasedatabase.app/")
                .getReference("wishlists").child(pairKey).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                long count = snapshot.getChildrenCount();
                tvWishlist.setText(count + " items on wishlist");
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void startForegroundService() {
        Intent serviceIntent = new Intent(this, BackgroundService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent);
        } else {
            startService(serviceIntent);
        }
    }

    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.POST_NOTIFICATIONS}, 1002);
            }
        }
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, "Partner Pokes", NotificationManager.IMPORTANCE_HIGH);
            NotificationManager nm = getSystemService(NotificationManager.class);
            if (nm != null) nm.createNotificationChannel(channel);
        }
    }

    private void listenForPokes() {
        FirebaseDatabase.getInstance("https://i-must-finish-this-in-2026-default-rtdb.asia-southeast1.firebasedatabase.app/").getReference("pokes").child(userId)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        Long timestamp = snapshot.getValue(Long.class);
                        // Trigger only for NEW pokes that happen after the app started
                        if (timestamp != null && timestamp > startTime) {
                            startTime = timestamp; 
                            showPokeNotification();
                        }
                    }
                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {}
                });
    }

    private void listenForProfileNames() {
        FirebaseDatabase.getInstance("https://i-must-finish-this-in-2026-default-rtdb.asia-southeast1.firebasedatabase.app/")
                .getReference("profiles").child(partnerId).child("name")
                .addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String name = snapshot.getValue(String.class);
                if (name != null) {
                    // Update partner setting in dashboard if needed
                    findViewById(R.id.tvDashboardTitle).post(() -> {
                        // Optional: update health card with partner name
                    });
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void showPokeNotification() {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher_round)
                .setContentTitle("Project Notification")
                .setContentText("Your partner poked you!")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
            NotificationManagerCompat.from(this).notify((int)System.currentTimeMillis(), builder.build());
        }
        Toast.makeText(this, "Your partner poked you!", Toast.LENGTH_SHORT).show();
    }

    private void checkLocationPermissions() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
        } else {
            startTracking();
        }
    }

    private void startTracking() {
        if (partnerId == null || partnerId.isEmpty()) {
            tvDistance.setText("Not paired yet");
            return;
        }
        tvDistance.setText("Calculating...");
        tracker = new LocationTracker(this, userId, partnerId);
        tracker.startTracking();
        tracker.listenToPartner((lat, lon, distance) -> {
            if (distance > 0) {
                tvDistance.setText(UnitUtils.formatDistance(this, distance));
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            startTracking();
        }
    }
}