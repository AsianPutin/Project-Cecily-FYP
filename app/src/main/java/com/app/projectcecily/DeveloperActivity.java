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
import java.util.List;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;

import com.google.android.material.materialswitch.MaterialSwitch;

public class DeveloperActivity extends AppCompatActivity {

    private static final String CHANNEL_ID = "developer_debug_channel";
    private static final long[][] VIBRATION_PATTERNS = new long[][]{
        {0, 250, 150, 250},                      // Default Pulse
        {0, 100, 100, 100, 300, 100, 100, 100},  // Heartbeat
        {0, 100, 80, 100},                       // Double Tap
        {0, 600},                                // Long Buzz
        {0, 50, 200, 50}                         // Tick-Tock
    };

    private MaterialSwitch switchMonthlyDemo;
    private MaterialSwitch switchLifetimeDemo;
    private boolean isProgrammaticSwitching = false;

    // Photo Stream Controls State
    private com.google.android.material.button.MaterialButtonToggleGroup toggleDevPhotoStream;
    private android.widget.ImageView ivDevPhotoPreview;
    private com.google.android.material.card.MaterialCardView[] devThumbCards = new com.google.android.material.card.MaterialCardView[4];
    private android.widget.ImageView[] devThumbViews = new android.widget.ImageView[4];
    private Button btnDevDeletePhoto;

    private boolean isViewingMyPhotos = true;
    private List<PhotoItem> currentDevPhotos = new java.util.ArrayList<>();
    private int selectedPhotoIndex = 0;

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

        setupDevPhotoControls();

        switchMonthlyDemo = findViewById(R.id.switchMonthlyDemo);
        switchLifetimeDemo = findViewById(R.id.switchLifetimeDemo);
        Button btnEndSub = findViewById(R.id.btnEndSubscription);

        setupDevPhotoControls();
        setupSubscriptionSwitches();
        setupMonthlyDatePicker();
        applySwitchAccentTints();

        if (btnEndSub != null) {
            btnEndSub.setOnClickListener(v -> {
                isProgrammaticSwitching = true;
                if (switchMonthlyDemo != null) switchMonthlyDemo.setChecked(false);
                if (switchLifetimeDemo != null) switchLifetimeDemo.setChecked(false);
                isProgrammaticSwitching = false;

                SubscriptionDialogHelper.setSubscriptionMode(this, SubscriptionDialogHelper.MODE_FREE);
                updateMonthlyExpiryUi();
            });
        }
    }

    private void applySwitchAccentTints() {
        int accent = ThemeUtils.getAccentColor(this);
        int uncheckedTrack = android.graphics.Color.parseColor("#444444");

        int[][] states = new int[][]{
            new int[]{android.R.attr.state_checked},
            new int[]{-android.R.attr.state_checked}
        };

        int[] thumbColors = new int[]{
            android.graphics.Color.WHITE,
            android.graphics.Color.parseColor("#AAAAAA")
        };

        int[] trackColors = new int[]{
            accent,
            uncheckedTrack
        };

        android.content.res.ColorStateList thumbTintList = new android.content.res.ColorStateList(states, thumbColors);
        android.content.res.ColorStateList trackTintList = new android.content.res.ColorStateList(states, trackColors);

        if (switchMonthlyDemo != null) {
            switchMonthlyDemo.setThumbTintList(thumbTintList);
            switchMonthlyDemo.setTrackTintList(trackTintList);
        }
        if (switchLifetimeDemo != null) {
            switchLifetimeDemo.setThumbTintList(thumbTintList);
            switchLifetimeDemo.setTrackTintList(trackTintList);
        }
    }

    private void setupDevPhotoControls() {
        toggleDevPhotoStream = findViewById(R.id.toggleDevPhotoStream);
        ivDevPhotoPreview = findViewById(R.id.ivDevPhotoPreview);
        btnDevDeletePhoto = findViewById(R.id.btnDevDeletePhoto);

        devThumbCards[0] = findViewById(R.id.cardThumb1);
        devThumbCards[1] = findViewById(R.id.cardThumb2);
        devThumbCards[2] = findViewById(R.id.cardThumb3);
        devThumbCards[3] = findViewById(R.id.cardThumb4);

        devThumbViews[0] = findViewById(R.id.ivThumb1);
        devThumbViews[1] = findViewById(R.id.ivThumb2);
        devThumbViews[2] = findViewById(R.id.ivThumb3);
        devThumbViews[3] = findViewById(R.id.ivThumb4);

        if (toggleDevPhotoStream != null) {
            toggleDevPhotoStream.check(R.id.btnDevStreamMe);
            toggleDevPhotoStream.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
                if (!isChecked) return;
                isViewingMyPhotos = (checkedId == R.id.btnDevStreamMe);
                selectedPhotoIndex = 0;
                loadDevPhotoStream();
            });
        }

        for (int i = 0; i < 4; i++) {
            final int index = i;
            if (devThumbCards[i] != null) {
                devThumbCards[i].setOnClickListener(v -> {
                    selectedPhotoIndex = index;
                    updateDevPhotoUI();
                });
            }
        }

        if (btnDevDeletePhoto != null) {
            btnDevDeletePhoto.setOnClickListener(v -> {
                if (selectedPhotoIndex >= 0 && selectedPhotoIndex < currentDevPhotos.size()) {
                    PhotoItem target = currentDevPhotos.get(selectedPhotoIndex);
                    String targetUserId = isViewingMyPhotos ? UserManager.getUserId(this) : UserManager.getPartnerId(this);
                    PhotoCycleManager.deletePhoto(this, targetUserId, target.photoId, () -> {
                        android.widget.Toast.makeText(this, "Photo slot deleted!", android.widget.Toast.LENGTH_SHORT).show();
                        selectedPhotoIndex = 0;
                    });
                } else {
                    android.widget.Toast.makeText(this, "No photo in selected slot to delete.", android.widget.Toast.LENGTH_SHORT).show();
                }
            });
        }

        loadDevPhotoStream();
    }

    private void loadDevPhotoStream() {
        String targetUserId = isViewingMyPhotos ? UserManager.getUserId(this) : UserManager.getPartnerId(this);
        PhotoCycleManager.listenActivePhotos(this, targetUserId, photos -> {
            currentDevPhotos = (photos != null) ? photos : new java.util.ArrayList<>();
            if (selectedPhotoIndex >= currentDevPhotos.size()) {
                selectedPhotoIndex = Math.max(0, currentDevPhotos.size() - 1);
            }
            updateDevPhotoUI();
        });
    }

    private void updateDevPhotoUI() {
        int accentColor = ThemeUtils.getAccentColor(this);

        for (int i = 0; i < 4; i++) {
            com.google.android.material.card.MaterialCardView card = devThumbCards[i];
            android.widget.ImageView iv = devThumbViews[i];

            if (i < currentDevPhotos.size()) {
                PhotoItem item = currentDevPhotos.get(i);
                try {
                    String clean = item.photoBase64.contains(",") ? item.photoBase64.split(",")[1] : item.photoBase64;
                    byte[] bytes = android.util.Base64.decode(clean, android.util.Base64.DEFAULT);
                    android.graphics.Bitmap bmp = android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                    if (iv != null) iv.setImageBitmap(bmp);
                } catch (Exception e) {
                    if (iv != null) iv.setImageBitmap(null);
                }
            } else {
                if (iv != null) iv.setImageBitmap(null);
            }

            if (card != null) {
                if (i == selectedPhotoIndex && i < currentDevPhotos.size()) {
                    // Theme-colored border ring highlight for active selection!
                    card.setStrokeColor(accentColor);
                    card.setStrokeWidth((int) (3 * getResources().getDisplayMetrics().density));
                } else {
                    card.setStrokeWidth(0);
                }
            }
        }

        // Update Large Preview Window
        if (ivDevPhotoPreview != null) {
            if (selectedPhotoIndex >= 0 && selectedPhotoIndex < currentDevPhotos.size()) {
                PhotoItem selected = currentDevPhotos.get(selectedPhotoIndex);
                try {
                    String clean = selected.photoBase64.contains(",") ? selected.photoBase64.split(",")[1] : selected.photoBase64;
                    byte[] bytes = android.util.Base64.decode(clean, android.util.Base64.DEFAULT);
                    android.graphics.Bitmap bmp = android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                    ivDevPhotoPreview.setImageBitmap(bmp);
                } catch (Exception e) {
                    ivDevPhotoPreview.setImageBitmap(null);
                }
            } else {
                ivDevPhotoPreview.setImageBitmap(null);
            }
        }
    }

    private void setupSubscriptionSwitches() {
        int mode = SubscriptionDialogHelper.getSubscriptionMode(this);

        isProgrammaticSwitching = true;
        if (switchMonthlyDemo != null) switchMonthlyDemo.setChecked(mode == SubscriptionDialogHelper.MODE_MONTHLY);
        if (switchLifetimeDemo != null) switchLifetimeDemo.setChecked(mode == SubscriptionDialogHelper.MODE_LIFETIME);
        isProgrammaticSwitching = false;

        updateMonthlyExpiryUi();

        if (switchMonthlyDemo != null) {
            switchMonthlyDemo.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isProgrammaticSwitching) return;
                if (isChecked) {
                    isProgrammaticSwitching = true;
                    if (switchLifetimeDemo != null) switchLifetimeDemo.setChecked(false);
                    isProgrammaticSwitching = false;
                    SubscriptionDialogHelper.setSubscriptionMode(this, SubscriptionDialogHelper.MODE_MONTHLY);
                } else {
                    if (switchLifetimeDemo != null && !switchLifetimeDemo.isChecked()) {
                        SubscriptionDialogHelper.setSubscriptionMode(this, SubscriptionDialogHelper.MODE_FREE);
                    }
                }
                updateMonthlyExpiryUi();
            });
        }

        if (switchLifetimeDemo != null) {
            switchLifetimeDemo.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isProgrammaticSwitching) return;
                if (isChecked) {
                    isProgrammaticSwitching = true;
                    if (switchMonthlyDemo != null) switchMonthlyDemo.setChecked(false);
                    isProgrammaticSwitching = false;
                    SubscriptionDialogHelper.setSubscriptionMode(this, SubscriptionDialogHelper.MODE_LIFETIME);
                } else {
                    if (switchMonthlyDemo != null && !switchMonthlyDemo.isChecked()) {
                        SubscriptionDialogHelper.setSubscriptionMode(this, SubscriptionDialogHelper.MODE_FREE);
                    }
                }
                updateMonthlyExpiryUi();
            });
        }
    }

    private void setupMonthlyDatePicker() {
        View btnSetExpiry = findViewById(R.id.btnSetMonthlyExpiry);
        View layoutExpiry = findViewById(R.id.layoutMonthlyExpiryDate);

        View.OnClickListener pickerListener = v -> {
            java.util.Calendar cal = java.util.Calendar.getInstance();
            long currentExpiry = SubscriptionDialogHelper.getMonthlyExpiryMillis(this);
            if (currentExpiry > 0) cal.setTimeInMillis(currentExpiry);

            android.app.DatePickerDialog datePicker = new android.app.DatePickerDialog(
                this,
                (view, year, month, dayOfMonth) -> {
                    java.util.Calendar selectedCal = java.util.Calendar.getInstance();
                    selectedCal.set(year, month, dayOfMonth, 23, 59, 59);
                    long selectedMs = selectedCal.getTimeInMillis();

                    isProgrammaticSwitching = true;
                    if (switchLifetimeDemo != null) switchLifetimeDemo.setChecked(false);
                    if (switchMonthlyDemo != null) switchMonthlyDemo.setChecked(true);
                    isProgrammaticSwitching = false;

                    SharedPreferences prefs = getSharedPreferences("prefs", MODE_PRIVATE);
                    prefs.edit()
                            .putInt("premium_subscription_mode", SubscriptionDialogHelper.MODE_MONTHLY)
                            .putBoolean("is_premium_unlocked", true)
                            .putLong("monthly_expiry_millis", selectedMs)
                            .apply();

                    String userId = UserManager.getUserId(this);
                    if (!userId.isEmpty()) {
                        com.google.firebase.database.DatabaseReference userRef = com.google.firebase.database.FirebaseDatabase.getInstance("https://i-must-finish-this-in-2026-default-rtdb.asia-southeast1.firebasedatabase.app/")
                                .getReference("profiles").child(userId);
                        userRef.child("isPremium").setValue(true);
                        userRef.child("subscriptionMode").setValue(SubscriptionDialogHelper.MODE_MONTHLY);
                        userRef.child("monthlyExpiryMillis").setValue(selectedMs);
                    }

                    updateMonthlyExpiryUi();
                },
                cal.get(java.util.Calendar.YEAR),
                cal.get(java.util.Calendar.MONTH),
                cal.get(java.util.Calendar.DAY_OF_MONTH)
            );
            datePicker.show();
        };

        if (btnSetExpiry != null) btnSetExpiry.setOnClickListener(pickerListener);
        if (layoutExpiry != null) layoutExpiry.setOnClickListener(pickerListener);
    }

    private void updateMonthlyExpiryUi() {
        android.widget.TextView tvExpiry = findViewById(R.id.tvMonthlyExpiryDate);
        View layoutExpiry = findViewById(R.id.layoutMonthlyExpiryDate);
        View btnSetExpiry = findViewById(R.id.btnSetMonthlyExpiry);

        boolean isMonthly = switchMonthlyDemo != null && switchMonthlyDemo.isChecked();
        if (layoutExpiry != null) {
            layoutExpiry.setEnabled(isMonthly);
            layoutExpiry.setAlpha(isMonthly ? 1.0f : 0.4f);
        }
        if (btnSetExpiry != null) {
            btnSetExpiry.setEnabled(isMonthly);
            btnSetExpiry.setAlpha(isMonthly ? 1.0f : 0.4f);
        }

        if (tvExpiry != null) {
            if (isMonthly) {
                String expiryStr = SubscriptionDialogHelper.getFormattedMonthlyExpiryDate(this);
                tvExpiry.setText(expiryStr.isEmpty() ? "Not set (30 days default)" : "Expires " + expiryStr);
            } else {
                tvExpiry.setText("Disabled (Monthly inactive)");
            }
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
                .setSmallIcon(IconManager.getNotificationSmallIcon())
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
            }
        } catch (Exception ignored) {}
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
        } catch (Exception ignored) {}
    }

    @Override
    protected void onResume() {
        super.onResume();
        ThemeUtils.applyAccentToActivity(this);
        applySwitchAccentTints();
        updateMonthlyExpiryUi();
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
