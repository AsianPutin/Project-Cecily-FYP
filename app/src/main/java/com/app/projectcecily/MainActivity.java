package com.app.projectcecily;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
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
    private final List<String> randomizedAdsList = new ArrayList<>();
    private GestureDetector adGestureDetector;
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
        ThemeUtils.syncUserThemeFromFirebase(this);
        updateDynamicAccent();
        ThemeUtils.applyAccentToActivity(this);
        SubscriptionDialogHelper.syncSubscriptionFromFirebase(this);
        setupBannerAd();
        if (lastDistanceInKm > 0 && tvDistance != null) {
            tvDistance.setText(UnitUtils.formatDistance(this, lastDistanceInKm));
        }
        updatePartnerSummaryCards();
    }

    private void updateDynamicAccent() {
        BottomNavigationView navView = findViewById(R.id.bottom_navigation);
        if (navView != null) {
            navView.setItemActiveIndicatorEnabled(false);
            boolean isNightMode = (getResources().getConfiguration().uiMode & android.content.res.Configuration.UI_MODE_NIGHT_MASK) == android.content.res.Configuration.UI_MODE_NIGHT_YES;
            int itemColor = isNightMode ? android.graphics.Color.parseColor("#FFFFFF") : android.graphics.Color.parseColor("#212121");
            android.content.res.ColorStateList colorList = android.content.res.ColorStateList.valueOf(itemColor);
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

        if (cardBannerAd == null) return;

        if (SubscriptionDialogHelper.isPremiumUnlocked(this)) {
            if (adHandler != null && adRunnable != null) {
                adHandler.removeCallbacks(adRunnable);
            }
            cardBannerAd.setVisibility(View.GONE);
            return;
        }

        cardBannerAd.setVisibility(View.VISIBLE);

        if (randomizedAdsList.isEmpty()) {
            randomizedAdsList.addAll(Arrays.asList(MOCK_ADS));
            Collections.shuffle(randomizedAdsList);
        }

        if (tvAdText != null && !randomizedAdsList.isEmpty()) {
            tvAdText.setText(randomizedAdsList.get(0));
        }

        if (btnDismissAd != null && cardBannerAd != null) {
            btnDismissAd.setOnClickListener(v -> {
                if (adHandler != null && adRunnable != null) {
                    adHandler.removeCallbacks(adRunnable);
                }
                cardBannerAd.setVisibility(View.GONE);
                Toast.makeText(this, "Ad dismissed! Unlock permanent Ad-Free Premium in Settings.", Toast.LENGTH_SHORT).show();
            });
        }

        if (tvAdText != null && cardBannerAd != null && cardBannerAd.getVisibility() == View.VISIBLE) {
            adGestureDetector = new GestureDetector(this, new GestureDetector.SimpleOnGestureListener() {
                private static final int SWIPE_THRESHOLD = 50;
                private static final int SWIPE_VELOCITY_THRESHOLD = 50;

                @Override
                public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
                    if (e1 == null || e2 == null) return false;
                    float diffX = e2.getX() - e1.getX();
                    float diffY = e2.getY() - e1.getY();
                    if (Math.abs(diffX) > Math.abs(diffY) && Math.abs(diffX) > SWIPE_THRESHOLD && Math.abs(velocityX) > SWIPE_VELOCITY_THRESHOLD) {
                        if (diffX < 0) {
                            showNextAd(tvAdText, true);
                        } else {
                            showPreviousAd(tvAdText);
                        }
                        resetAdTimer(tvAdText);
                        return true;
                    }
                    return false;
                }
            });

            cardBannerAd.setOnTouchListener((v, event) -> {
                if (event.getAction() == MotionEvent.ACTION_UP) {
                    v.performClick();
                }
                if (adGestureDetector != null) {
                    adGestureDetector.onTouchEvent(event);
                }
                return true;
            });

            resetAdTimer(tvAdText);
        }
    }

    private void resetAdTimer(TextView tvAdText) {
        if (adHandler != null) {
            if (adRunnable != null) {
                adHandler.removeCallbacks(adRunnable);
            }
            adRunnable = () -> {
                showNextAd(tvAdText, true);
                resetAdTimer(tvAdText);
            };
            adHandler.postDelayed(adRunnable, 5000);
        }
    }

    private void showNextAd(TextView tvAdText, boolean animateLeft) {
        if (randomizedAdsList.isEmpty()) return;
        currentAdIndex = (currentAdIndex + 1) % randomizedAdsList.size();
        String nextText = randomizedAdsList.get(currentAdIndex);
        animateAdTextChange(tvAdText, nextText, animateLeft);
    }

    private void showPreviousAd(TextView tvAdText) {
        if (randomizedAdsList.isEmpty()) return;
        currentAdIndex = (currentAdIndex - 1 + randomizedAdsList.size()) % randomizedAdsList.size();
        String prevText = randomizedAdsList.get(currentAdIndex);
        animateAdTextChange(tvAdText, prevText, false);
    }

    private void animateAdTextChange(TextView tvAdText, String newText, boolean animateLeft) {
        float exitX = animateLeft ? -tvAdText.getWidth() : tvAdText.getWidth();
        float enterX = animateLeft ? tvAdText.getWidth() : -tvAdText.getWidth();

        tvAdText.animate()
                .translationX(exitX)
                .alpha(0f)
                .setDuration(350)
                .withEndAction(() -> {
                    tvAdText.setText(newText);
                    tvAdText.setTranslationX(enterX);
                    tvAdText.animate()
                            .translationX(0f)
                            .alpha(1f)
                            .setDuration(350)
                            .start();
                })
                .start();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (adHandler != null && adRunnable != null) {
            adHandler.removeCallbacks(adRunnable);
        }
    }
}
