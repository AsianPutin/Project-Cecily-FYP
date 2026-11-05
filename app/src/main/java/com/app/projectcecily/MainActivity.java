package com.app.projectcecily;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
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

    private TextView tvDistance, tvWishlist, tvFlightSummary, tvTogetherDays, tvBirthdaysSummary, tvPartnerTimeSummary;

    private String partnerName = "Partner";
    private String partnerAccentColor = "#FF4081";
    private String partnerTimeZoneId = "";
    private RelationshipManager.Details cachedDetails;

    private android.os.Handler adHandler = new android.os.Handler(android.os.Looper.getMainLooper());
    private Runnable adRunnable;
    private int currentAdIndex = 0;
    private static final String[] MOCK_ADS = new String[]{
        "Sponsored: Explore Couple Travel Deals & Flights ✈️",
        "Sponsored: Send Secret Gifts & Flowers Overseas 💐",
        "Sponsored: Book Long-Distance Date Night Packages 🎟️",
        "Sponsored: Unlock Unlimited High-Speed eSIM Roaming 🌐"
    };

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

        syncLocalTimezone();

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
                    listenForPartnerProfile();
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
        tvPartnerTimeSummary = findViewById(R.id.tvPartnerTimeSummary);

        setupBannerAd();

        RelationshipManager.listenFromFirebase(this, details -> {
            cachedDetails = details;
            updatePartnerSummaryCards();
        });

        listenForPartnerProfile();

        setupDashboardClicks();
        setupNavigation();

        BottomNavigationView navView = findViewById(R.id.bottom_navigation);
        if (navView != null) {
            androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(navView, (v, insets) -> {
                int bottomInset = insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.systemBars()).bottom;
                v.setPadding(0, 0, 0, bottomInset);
                return insets;
            });
        }

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
        
        UIUtils.applyBottomPadding(findViewById(R.id.main_scroll));

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            startTracking();
        } else {
            checkLocationPermissions();
        }

        listenForPokes();
        listenForWishlistCount();
        listenForFlights();
        scheduleFlightTracking();
    }

    private void syncLocalTimezone() {
        String tzId = java.util.TimeZone.getDefault().getID();
        FirebaseDatabase.getInstance("https://i-must-finish-this-in-2026-default-rtdb.asia-southeast1.firebasedatabase.app/")
                .getReference("profiles").child(userId).child("timeZone").setValue(tzId);
    }

    private void listenForPartnerProfile() {
        if (partnerId == null || partnerId.isEmpty()) return;

        FirebaseDatabase.getInstance("https://i-must-finish-this-in-2026-default-rtdb.asia-southeast1.firebasedatabase.app/")
                .getReference("profiles").child(partnerId)
                .addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String pName = snapshot.child("name").getValue(String.class);
                    if (pName != null && !pName.trim().isEmpty()) partnerName = pName.trim();

                    String pColor = snapshot.child("accentColor").getValue(String.class);
                    if (pColor != null && !pColor.trim().isEmpty()) partnerAccentColor = pColor.trim();

                    String pTz = snapshot.child("timeZone").getValue(String.class);
                    if (pTz != null && !pTz.trim().isEmpty()) partnerTimeZoneId = pTz.trim();

                    updatePartnerSummaryCards();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void updatePartnerSummaryCards() {
        if (partnerId == null || partnerId.isEmpty()) {
            if (tvTogetherDays != null) tvTogetherDays.setText("Not paired yet");
            if (tvBirthdaysSummary != null) tvBirthdaysSummary.setText("Pair in Account Settings");
            if (tvPartnerTimeSummary != null) tvPartnerTimeSummary.setVisibility(View.GONE);
            return;
        }

        if (cachedDetails != null) {
            if (tvTogetherDays != null) tvTogetherDays.setText(RelationshipManager.formatDaysTogether(cachedDetails.anniversaryTimestamp));
            if (tvBirthdaysSummary != null) tvBirthdaysSummary.setText(RelationshipManager.formatPartnerBirthdayCountdown(partnerName, cachedDetails.partnerBirthday, partnerAccentColor));
        }

        CharSequence timeText = RelationshipManager.formatPartnerCurrentTime(this, partnerName, partnerTimeZoneId, partnerAccentColor);
        if (tvPartnerTimeSummary != null) {
            if (timeText != null && timeText.length() > 0) {
                tvPartnerTimeSummary.setText(timeText);
                tvPartnerTimeSummary.setVisibility(View.VISIBLE);
            } else {
                tvPartnerTimeSummary.setVisibility(View.GONE);
            }
        }
    }

    private void scheduleFlightTracking() {
        androidx.work.PeriodicWorkRequest request = new androidx.work.PeriodicWorkRequest.Builder(FlightWorker.class, 1, java.util.concurrent.TimeUnit.HOURS).build();
        androidx.work.WorkManager.getInstance(this).enqueueUniquePeriodicWork("flight_sync", androidx.work.ExistingPeriodicWorkPolicy.KEEP, request);
    }

    private float lastDistanceInKm = -1f;

    @Override
    protected void onResume() {
        super.onResume();
        updateDynamicAccent();
        ThemeUtils.applyAccentToActivity(this);
        if (lastDistanceInKm > 0 && tvDistance != null) {
            tvDistance.setText(UnitUtils.formatDistance(this, lastDistanceInKm));
        }
        updatePartnerSummaryCards();
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
        String pairKey = FlightTrackerActivity.getPairKey(this);
        FirebaseDatabase.getInstance("https://i-must-finish-this-in-2026-default-rtdb.asia-southeast1.firebasedatabase.app/")
                .getReference("flights").child(pairKey)
                .addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    FlightInfo nextFlight = null;
                    long now = System.currentTimeMillis();
                    long earliestTime = Long.MAX_VALUE;

                    for (DataSnapshot ds : snapshot.getChildren()) {
                        FlightInfo info = ds.getValue(FlightInfo.class);
                        if (info != null) {
                            long arrTime = info.depTimeMillis + (110 * 60 * 1000L);
                            if (now <= arrTime + (30 * 60 * 1000L)) {
                                if (info.depTimeMillis < earliestTime) {
                                    earliestTime = info.depTimeMillis;
                                    nextFlight = info;
                                }
                            }
                        }
                    }

                    if (nextFlight != null) {
                        String destCity = nextFlight.arrCity;
                        if (destCity == null || destCity.trim().isEmpty() || "Arrival".equalsIgnoreCase(destCity.trim())) {
                            destCity = resolveIataCityName(nextFlight.arrAirport);
                        }

                        String cleanFn = nextFlight.flightNumber != null ? nextFlight.flightNumber.replaceAll("\\s+", "").toUpperCase() : "";
                        if ("MH2547".equals(cleanFn)) {
                            destCity = "Kuala Lumpur";
                        } else if ("AK5234".equals(cleanFn)) {
                            destCity = "Kuching";
                        }

                        tvFlightSummary.setText(nextFlight.flightNumber + " to " + destCity);
                    } else {
                        tvFlightSummary.setText("No upcoming flights");
                    }
                } else {
                    tvFlightSummary.setText("No upcoming flights");
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private String resolveIataCityName(String iata) {
        if (iata == null || iata.trim().isEmpty()) return "Kuala Lumpur";
        String clean = iata.trim().toUpperCase();
        if ("KCH".equals(clean)) return "Kuching";
        if ("KUL".equals(clean)) return "Kuala Lumpur";
        if ("BKI".equals(clean)) return "Kota Kinabalu";
        if ("SIN".equals(clean)) return "Singapore";
        if ("PEN".equals(clean)) return "Penang";
        return clean;
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
                        if (timestamp != null && timestamp > startTime) {
                            startTime = timestamp; 
                            showPokeNotification();
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
                lastDistanceInKm = distance;
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

    private void setupBannerAd() {
        View cardBannerAd = findViewById(R.id.cardBannerAd);
        View btnDismissAd = findViewById(R.id.btnDismissAd);
        TextView tvAdText = findViewById(R.id.tvAdBannerText);

        android.content.SharedPreferences adPrefs = getSharedPreferences("ad_prefs", MODE_PRIVATE);
        boolean adDismissed = adPrefs.getBoolean("ad_dismissed", false);
        if (adDismissed && cardBannerAd != null) {
            cardBannerAd.setVisibility(View.GONE);
            return;
        }

        if (btnDismissAd != null && cardBannerAd != null) {
            btnDismissAd.setOnClickListener(v -> {
                if (adHandler != null && adRunnable != null) {
                    adHandler.removeCallbacks(adRunnable);
                }
                cardBannerAd.setVisibility(View.GONE);
                adPrefs.edit().putBoolean("ad_dismissed", true).apply();
                Toast.makeText(this, "Ad dismissed! Unlock permanent Ad-Free Freedom Pass in Settings.", Toast.LENGTH_SHORT).show();
            });
        }

        if (tvAdText != null && cardBannerAd != null && cardBannerAd.getVisibility() == View.VISIBLE) {
            adRunnable = new Runnable() {
                @Override
                public void run() {
                    currentAdIndex = (currentAdIndex + 1) % MOCK_ADS.length;
                    String nextAdText = MOCK_ADS[currentAdIndex];

                    // Conveyor belt slide animation out to left
                    tvAdText.animate()
                            .translationX(-tvAdText.getWidth())
                            .alpha(0f)
                            .setDuration(400)
                            .withEndAction(() -> {
                                tvAdText.setText(nextAdText);
                                tvAdText.setTranslationX(tvAdText.getWidth());
                                // Conveyor belt slide animation in from right
                                tvAdText.animate()
                                        .translationX(0f)
                                        .alpha(1f)
                                        .setDuration(400)
                                        .start();
                            })
                            .start();

                    adHandler.postDelayed(this, 3500);
                }
            };
            adHandler.postDelayed(adRunnable, 3500);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (adHandler != null && adRunnable != null) {
            adHandler.removeCallbacks(adRunnable);
        }
    }
}
