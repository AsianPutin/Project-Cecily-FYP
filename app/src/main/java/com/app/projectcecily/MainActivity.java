package com.app.projectcecily;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.provider.MediaStore;
import android.util.Base64;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
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

    private TextView tvDistance, tvWishlist, tvFlightSummary, tvTogetherDays, tvBirthdaysSummary, tvPartnerTimeSummary, tvAnniversaryDate;

    private String partnerName = "Partner";
    private String partnerAccentColor = "#FF4081";
    private String partnerTimeZoneId = "";
    private RelationshipManager.Details cachedDetails;

    // Photo Cycle Dashboard State
    private androidx.viewpager2.widget.ViewPager2 viewPagerDashPhotos;
    private List<PhotoItem> dashPartnerPhotos = new ArrayList<>();
    private List<PhotoItem> dashMyPhotos = new ArrayList<>();
    private android.os.Handler photoCycleHandler = new android.os.Handler(android.os.Looper.getMainLooper());
    private Runnable photoCycleRunnable;

    private ActivityResultLauncher<Intent> dashCameraLauncher;
    private ActivityResultLauncher<Intent> dashGalleryLauncher;

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
        ThemeUtils.applySavedThemeMode(this);

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

        SharedPreferences restartPrefs = getSharedPreferences("prefs_" + userId, MODE_PRIVATE);
        String targetMenu = restartPrefs.getString("restart_target_menu", "");
        if ("AppearanceActivity".equals(targetMenu)) {
            restartPrefs.edit().remove("restart_target_menu").apply();
            Intent intent = new Intent(this, AppearanceActivity.class);
            startActivity(intent);
        }

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
        tvAnniversaryDate = findViewById(R.id.tvAnniversaryDate);
        tvPartnerTimeSummary = findViewById(R.id.tvPartnerTimeSummary);

        setupBannerAd();

        setupDashCameraLaunchers();
        setupPhotoCycleDashboardControls();

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
            if (tvAnniversaryDate != null) tvAnniversaryDate.setText("Pair in Account Settings");
            if (tvBirthdaysSummary != null) tvBirthdaysSummary.setText("Pair in Account Settings");
            if (tvPartnerTimeSummary != null) tvPartnerTimeSummary.setVisibility(View.GONE);
            return;
        }

        if (cachedDetails != null) {
            if (tvTogetherDays != null) tvTogetherDays.setText(RelationshipManager.formatDaysTogether(cachedDetails.anniversaryTimestamp));
            
            CharSequence bdayCountdown = RelationshipManager.formatPartnerBirthdayCountdown(partnerName, cachedDetails.partnerBirthday, partnerAccentColor);
            if (tvAnniversaryDate != null) {
                if (cachedDetails.anniversaryTimestamp > 0) {
                    tvAnniversaryDate.setText(bdayCountdown);
                } else {
                    tvAnniversaryDate.setText("Tap to set relationship start date");
                }
            }
            if (tvBirthdaysSummary != null) tvBirthdaysSummary.setText(bdayCountdown);

            if (cachedDetails.anniversaryTimestamp <= 0) {
                RelationshipManager.promptForRelationshipStartDateIfMissing(this);
            }
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

        ImageView ivDashPokeIcon = findViewById(R.id.ivDashPokeIcon);
        if (ivDashPokeIcon != null && partnerAccentColor != null && !partnerAccentColor.isEmpty()) {
            try {
                int color = android.graphics.Color.parseColor(partnerAccentColor);
                ivDashPokeIcon.setImageTintList(android.content.res.ColorStateList.valueOf(color));
            } catch (Exception ignored) {}
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
        listenForDashPhotoStreams();

        if (photoCycleHandler != null && photoCycleRunnable != null) {
            photoCycleHandler.removeCallbacks(photoCycleRunnable);
            photoCycleHandler.postDelayed(photoCycleRunnable, 5000L);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (photoCycleHandler != null && photoCycleRunnable != null) {
            photoCycleHandler.removeCallbacks(photoCycleRunnable);
        }
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

    private Uri dashTempCameraUri;

    private void setupDashCameraLaunchers() {
        dashCameraLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getResultCode() == RESULT_OK && dashTempCameraUri != null) {
                Bitmap photo = PhotoCycleManager.decodeHighResBitmap(this, dashTempCameraUri);
                if (photo != null) {
                    PhotoCycleManager.saveUneditedPhotoToGallery(this, photo);
                    showDashCropEditorDialog(photo);
                }
            }
        });

        dashGalleryLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                Uri selectedImage = result.getData().getData();
                if (selectedImage != null) {
                    Bitmap bitmap = PhotoCycleManager.decodeHighResBitmap(this, selectedImage);
                    if (bitmap != null) {
                        showDashCropEditorDialog(bitmap);
                    }
                }
            }
        });
    }

    private void launchCaptureIntent(boolean isCamera) {
        if (isCamera) {
            dashTempCameraUri = PhotoCycleManager.createTempCameraUri(this);
            Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            if (dashTempCameraUri != null) {
                intent.putExtra(MediaStore.EXTRA_OUTPUT, dashTempCameraUri);
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            }
            dashCameraLauncher.launch(intent);
        } else {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            dashGalleryLauncher.launch(intent);
        }
    }

    private void setupPhotoCycleDashboardControls() {
        viewPagerDashPhotos = findViewById(R.id.viewPagerDashPhotos);
        if (viewPagerDashPhotos != null) {
            DashPhotoPagerAdapter adapter = new DashPhotoPagerAdapter();
            viewPagerDashPhotos.setAdapter(adapter);
        }

        View cardDashPhoto = findViewById(R.id.cardDashPhotoCycle);
        if (cardDashPhoto != null) {
            cardDashPhoto.setOnClickListener(v -> showDashPhotoOptionsSheet());
        }

        photoCycleRunnable = new Runnable() {
            @Override
            public void run() {
                if (!dashPartnerPhotos.isEmpty() && viewPagerDashPhotos != null) {
                    int nextPos = viewPagerDashPhotos.getCurrentItem() + 1;
                    viewPagerDashPhotos.setCurrentItem(nextPos, true);
                }
                long delay = 5000L + (long) (Math.random() * 5000L);
                photoCycleHandler.postDelayed(this, delay);
            }
        };
    }

    private void listenForDashPhotoStreams() {
        String myUserId = UserManager.getUserId(this);
        if (!myUserId.isEmpty()) {
            PhotoCycleManager.listenActivePhotos(this, myUserId, photos -> {
                dashMyPhotos = (photos != null) ? photos : new ArrayList<>();
            });
        }

        PhotoCycleManager.listenCombinedActivePhotos(this, photos -> {
            dashPartnerPhotos = (photos != null) ? photos : new ArrayList<>();
            updateDashPhotoUI();
        });
    }

    private void updateDashPhotoUI() {
        View layoutFallback = findViewById(R.id.layoutDashPhotoFallback);
        TextView tvFallback = findViewById(R.id.tvDashPhotoFallbackText);

        boolean isLoggedIn = UserManager.isLoggedIn(this);
        boolean isPaired = !UserManager.getPartnerId(this).isEmpty();

        if (!isLoggedIn || !isPaired || dashPartnerPhotos.isEmpty()) {
            if (viewPagerDashPhotos != null) viewPagerDashPhotos.setVisibility(View.GONE);
            if (layoutFallback != null) layoutFallback.setVisibility(View.VISIBLE);
            if (tvFallback != null) {
                if (!isLoggedIn) tvFallback.setText("Log In to view\npartner photos");
                else if (!isPaired) tvFallback.setText("Pair up to view\npartner photos!");
                else tvFallback.setText("No active photos\nTap to send or view!");
            }
        } else {
            if (layoutFallback != null) layoutFallback.setVisibility(View.GONE);
            if (viewPagerDashPhotos != null) {
                viewPagerDashPhotos.setVisibility(View.VISIBLE);
                if (viewPagerDashPhotos.getAdapter() != null) {
                    viewPagerDashPhotos.getAdapter().notifyDataSetChanged();
                    if (!dashPartnerPhotos.isEmpty() && viewPagerDashPhotos.getCurrentItem() == 0) {
                        int count = dashPartnerPhotos.size();
                        int startPos = (10000 / 2) - ((10000 / 2) % count);
                        viewPagerDashPhotos.setCurrentItem(startPos, false);
                    }
                }
            }
        }
    }

    private class DashPhotoPagerAdapter extends androidx.recyclerview.widget.RecyclerView.Adapter<DashPhotoPagerAdapter.DashPhotoViewHolder> {

        @NonNull
        @Override
        public DashPhotoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_dash_photo_page, parent, false);
            return new DashPhotoViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull DashPhotoViewHolder holder, int position) {
            if (dashPartnerPhotos != null && !dashPartnerPhotos.isEmpty()) {
                int realIndex = position % dashPartnerPhotos.size();
                PhotoItem item = dashPartnerPhotos.get(realIndex);
                try {
                    String clean = item.photoBase64.contains(",") ? item.photoBase64.split(",")[1] : item.photoBase64;
                    byte[] bytes = Base64.decode(clean, Base64.DEFAULT);
                    Bitmap bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                    holder.ivPhoto.setImageBitmap(bmp);
                    holder.ivPhoto.setOnClickListener(v -> showDashPhotoOptionsSheet());
                } catch (Exception e) {
                    holder.ivPhoto.setImageBitmap(null);
                }
            } else {
                holder.ivPhoto.setImageBitmap(null);
            }
        }

        @Override
        public int getItemCount() {
            return (dashPartnerPhotos == null || dashPartnerPhotos.isEmpty()) ? 0 : Integer.MAX_VALUE;
        }

        class DashPhotoViewHolder extends androidx.recyclerview.widget.RecyclerView.ViewHolder {
            ImageView ivPhoto;
            DashPhotoViewHolder(View v) {
                super(v);
                ivPhoto = v.findViewById(R.id.ivDashPagePhoto);
            }
        }
    }

    private void showDashPhotoOptionsSheet() {
        BottomSheetDialog sheet = new BottomSheetDialog(this);
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_photo_cycle_options, null);
        sheet.setContentView(view);

        View btnViewPhotos = view.findViewById(R.id.optionViewPhotos);
        if (btnViewPhotos != null) {
            btnViewPhotos.setVisibility(View.VISIBLE);
            btnViewPhotos.setOnClickListener(v -> {
                sheet.dismiss();
                startActivity(new Intent(this, PhotoCycleActivity.class));
            });
        }

        view.findViewById(R.id.optionCamera).setOnClickListener(v -> {
            sheet.dismiss();
            checkCapacityBeforeCapture(true);
        });

        view.findViewById(R.id.optionGallery).setOnClickListener(v -> {
            sheet.dismiss();
            checkCapacityBeforeCapture(false);
        });

        sheet.show();
    }

    private void checkCapacityBeforeCapture(boolean isCamera) {
        if (dashMyPhotos.size() >= PhotoCycleManager.MAX_PHOTOS_PER_USER) {
            // Option B Replace Prompt Dialog
            new MaterialAlertDialogBuilder(this)
                    .setTitle("Photo Stream Full (4/4)")
                    .setMessage("Your 24-hour photo stream is at maximum capacity (4/4 photos). Sending a new photo will replace your oldest active photo. Continue?")
                    .setPositiveButton("Replace & Send", (dialog, which) -> {
                        if (!dashMyPhotos.isEmpty()) {
                            PhotoItem oldest = dashMyPhotos.get(0);
                            String myUserId = UserManager.getUserId(this);
                            PhotoCycleManager.deletePhoto(this, myUserId, oldest.photoId, () -> {
                                launchCaptureIntent(isCamera);
                            });
                        } else {
                            launchCaptureIntent(isCamera);
                        }
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        } else {
            launchCaptureIntent(isCamera);
        }
    }

    private void showDashCropEditorDialog(Bitmap rawBitmap) {
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_crop_profile_pic, null);
        CropImageView ivCropTarget = view.findViewById(R.id.ivCropTarget);
        CircularOverlayView overlayView = view.findViewById(R.id.circularOverlayView);
        Button btnRotate = view.findViewById(R.id.btnRotateCrop);
        Button btnSave = view.findViewById(R.id.btnSaveCroppedPic);

        ivCropTarget.setImageAndReset(rawBitmap);

        AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                .setView(view)
                .setNegativeButton("Cancel", null)
                .create();

        btnRotate.setOnClickListener(v -> ivCropTarget.rotate90());

        btnSave.setOnClickListener(v -> {
            Bitmap cropped = ivCropTarget.getCroppedCircleBitmap(overlayView.getCircleRect());
            Bitmap finalUpload = (cropped != null) ? cropped : rawBitmap;
            PhotoCycleManager.uploadPhoto(this, finalUpload, new PhotoCycleManager.UploadCallback() {
                @Override
                public void onSuccess() {
                    Toast.makeText(MainActivity.this, "Photo sent to partner's stream! 📸✨", Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onError(String message) {
                    Toast.makeText(MainActivity.this, message, Toast.LENGTH_SHORT).show();
                }
            });
            dialog.dismiss();
        });

        dialog.show();
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
        View cardTogetherDays = findViewById(R.id.cardTogetherDays);
        if (cardTogetherDays != null) {
            cardTogetherDays.setOnClickListener(v -> {
                if (cachedDetails == null || cachedDetails.anniversaryTimestamp <= 0) {
                    RelationshipManager.promptForRelationshipStartDateIfMissing(this);
                } else {
                    startActivity(new Intent(this, ProfileSettingsActivity.class));
                }
            });
        }
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
                        FlightInfo info = null;
                        try {
                            info = ds.getValue(FlightInfo.class);
                        } catch (Exception ignored) {}
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
                .setSmallIcon(IconManager.getNotificationSmallIcon())
                .setContentTitle("Partner Poke 🔔")
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

        if (cardBannerAd instanceof com.google.android.material.card.MaterialCardView) {
            com.google.android.material.card.MaterialCardView adCard = (com.google.android.material.card.MaterialCardView) cardBannerAd;
            adCard.setStrokeColor(ThemeUtils.getAccentColor(this));
            adCard.setStrokeWidth((int) (1.5f * getResources().getDisplayMetrics().density));
        }

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
