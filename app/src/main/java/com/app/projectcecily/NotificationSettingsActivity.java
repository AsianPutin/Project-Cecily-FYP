package com.app.projectcecily;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.media.MediaPlayer;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.textfield.TextInputEditText;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class NotificationSettingsActivity extends AppCompatActivity {

    public static final String KEY_MY_SCHEDULES = "my_flight_schedules";
    public static final String KEY_PARTNER_SCHEDULES = "partner_flight_schedules";
    public static final String KEY_CUSTOM_SOUND_URI = "custom_sound_uri";
    public static final String KEY_CUSTOM_SOUND_NAME = "custom_sound_name";
    public static final String KEY_VIBRATION_PATTERN_NAME = "vibration_pattern_name";
    public static final String KEY_VIBRATION_PATTERN_INDEX = "vibration_pattern_index";

    private static final String[] VIBRATION_PATTERN_NAMES = new String[]{
        "Default Pulse 📳",
        "Heartbeat 💓",
        "Double Tap ⚡",
        "Long Buzz 📳",
        "Tick-Tock ⏰"
    };

    private static final long[][] VIBRATION_PATTERNS = new long[][]{
        {0, 250, 150, 250},                      // Default Pulse
        {0, 100, 100, 100, 300, 100, 100, 100},  // Heartbeat
        {0, 100, 80, 100},                       // Double Tap
        {0, 600},                                // Long Buzz
        {0, 50, 200, 50}                         // Tick-Tock
    };

    private SharedPreferences prefs;

    private MaterialSwitch switchFlightReminders, switchLandingAlerts;
    private MaterialSwitch switchPartnerPokes, switchProximityAlerts;
    private MaterialSwitch switchWishlistAlerts, switchSound, switchVibration;

    private TextView tvSelectedSoundName, tvSelectedVibrationName;
    private Button btnChangeSound, btnChangeVibration;
    private ImageView btnPreviewSound;
    private View layoutSelectNotificationSound, layoutVibrationPattern;

    private LinearLayout layoutFlightRemindersSection;
    private LinearLayout layoutMyFlightRemindersContainer;
    private LinearLayout layoutPartnerFlightRemindersContainer;

    private final List<String> myFlightSchedules = new ArrayList<>();
    private final List<String> partnerFlightSchedules = new ArrayList<>();

    private ActivityResultLauncher<Intent> systemRingtoneLauncher;
    private ActivityResultLauncher<Intent> audioFileLauncher;
    private MediaPlayer mediaPlayer;
    private boolean isPlayingPreview = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notification_settings);

        String userId = UserManager.getUserId(this);
        String prefsName = "notification_settings_" + (userId.isEmpty() ? "local" : userId);
        prefs = getSharedPreferences(prefsName, MODE_PRIVATE);

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

        tvSelectedSoundName = findViewById(R.id.tvSelectedSoundName);
        tvSelectedVibrationName = findViewById(R.id.tvSelectedVibrationName);

        btnChangeSound = findViewById(R.id.btnChangeSound);
        btnChangeVibration = findViewById(R.id.btnChangeVibration);
        btnPreviewSound = findViewById(R.id.btnPreviewSound);

        layoutSelectNotificationSound = findViewById(R.id.layoutSelectNotificationSound);
        layoutVibrationPattern = findViewById(R.id.layoutVibrationPattern);

        layoutFlightRemindersSection = findViewById(R.id.layoutFlightRemindersSection);
        layoutMyFlightRemindersContainer = findViewById(R.id.layoutMyFlightRemindersContainer);
        layoutPartnerFlightRemindersContainer = findViewById(R.id.layoutPartnerFlightRemindersContainer);

        TextView btnAddMyNotification = findViewById(R.id.btnAddMyNotification);
        TextView btnAddPartnerNotification = findViewById(R.id.btnAddPartnerNotification);
        TextView btnTestMyNotif = findViewById(R.id.btnTestMyFlightNotification);
        TextView btnTestPartnerNotif = findViewById(R.id.btnTestPartnerFlightNotification);

        if (btnAddMyNotification != null) {
            btnAddMyNotification.setOnClickListener(v -> showAddPresetDialog(true));
        }
        if (btnAddPartnerNotification != null) {
            btnAddPartnerNotification.setOnClickListener(v -> showAddPresetDialog(false));
        }

        if (btnTestMyNotif != null) {
            btnTestMyNotif.setOnClickListener(v -> sendTestFlightNotification(true));
        }
        if (btnTestPartnerNotif != null) {
            btnTestPartnerNotif.setOnClickListener(v -> sendTestFlightNotification(false));
        }

        if (btnChangeSound != null) {
            btnChangeSound.setOnClickListener(v -> showSoundChoiceDialog());
        }
        if (btnPreviewSound != null) {
            btnPreviewSound.setOnClickListener(v -> toggleSoundPreview());
        }
        if (btnChangeVibration != null) {
            btnChangeVibration.setOnClickListener(v -> showVibrationPatternDialog());
        }

        setupLaunchers();
        loadPreferences();
        setupSwitchListeners();
        applySwitchAccentTints();
        renderFlightSchedules();
        updateSoundUiSummary();
        updateVibrationUiSummary();
        fetchPartnerNameAndHeader();
    }

    private String cachedPartnerName = "Partner";

    private void fetchPartnerNameAndHeader() {
        String partnerId = UserManager.getPartnerId(this);
        if (!partnerId.isEmpty()) {
            com.google.firebase.database.FirebaseDatabase.getInstance("https://i-must-finish-this-in-2026-default-rtdb.asia-southeast1.firebasedatabase.app/")
                    .getReference("profiles").child(partnerId).child("name").get().addOnSuccessListener(snapshot -> {
                String pName = snapshot.getValue(String.class);
                if (pName != null && !pName.isEmpty()) {
                    cachedPartnerName = pName;
                    TextView tvHeader = findViewById(R.id.tvPartnerFlightRemindersHeader);
                    if (tvHeader != null) {
                        tvHeader.setText(pName + "'s Flight Reminders");
                    }
                }
            });
        }
    }

    private void sendTestFlightNotification(boolean isMy) {
        String userId = UserManager.getUserId(this);
        String partnerId = UserManager.getPartnerId(this);
        String pairKey = partnerId.isEmpty() ? userId : (userId.compareTo(partnerId) < 0 ? userId + "_" + partnerId : partnerId + "_" + userId);

        com.google.firebase.database.FirebaseDatabase.getInstance("https://i-must-finish-this-in-2026-default-rtdb.asia-southeast1.firebasedatabase.app/")
                .getReference("flights").child(pairKey).get().addOnSuccessListener(snapshot -> {
            FlightInfo selectedFlight = null;
            long now = System.currentTimeMillis();

            for (com.google.firebase.database.DataSnapshot ds : snapshot.getChildren()) {
                FlightInfo f = ds.getValue(FlightInfo.class);
                if (f != null && f.depTimeMillis > now) {
                    boolean isOwner = f.creatorId != null && f.creatorId.equals(userId);
                    if (isMy && isOwner) {
                        if (selectedFlight == null || f.depTimeMillis < selectedFlight.depTimeMillis) {
                            selectedFlight = f;
                        }
                    } else if (!isMy && !isOwner) {
                        if (selectedFlight == null || f.depTimeMillis < selectedFlight.depTimeMillis) {
                            selectedFlight = f;
                        }
                    }
                }
            }

            String title;
            String message;
            String subtext;

            if (selectedFlight != null) {
                String fn = selectedFlight.flightNumber != null ? selectedFlight.flightNumber : "MH2522";
                String arrCity = selectedFlight.arrCity != null ? selectedFlight.arrCity : "Kuching";
                long diffMs = selectedFlight.depTimeMillis - now;
                String timeFormatted = formatDurationHuman(diffMs);
                String timeOfDay = android.text.format.DateFormat.format("HH:mm", selectedFlight.depTimeMillis).toString();

                if (isMy) {
                    title = "Flight " + fn + " Reminder ✈️";
                    message = "Flight " + fn + " to " + arrCity + " departs in " + timeFormatted + " (" + timeOfDay + " MYT).";
                    subtext = "(This is a test notification)";
                } else {
                    title = cachedPartnerName + "'s Flight " + fn + " Reminder ✈️";
                    message = cachedPartnerName + "'s flight " + fn + " to " + arrCity + " departs in " + timeFormatted + " (" + timeOfDay + " MYT).";
                    subtext = "(" + cachedPartnerName + " will not receive this notification • This is a test notification)";
                }
            } else {
                if (isMy) {
                    title = "My Flight Reminder Test ✈️";
                    message = "No upcoming flights found. Test flight MH2522 departs in 2 hours.";
                    subtext = "(This is a test notification)";
                } else {
                    title = cachedPartnerName + "'s Flight Reminder Test ✈️";
                    message = cachedPartnerName + " has no upcoming flights. Test flight MH2522 departs in 2 hours.";
                    subtext = "(" + cachedPartnerName + " will not receive this notification • This is a test notification)";
                }
            }

            postTestNotification(title, message, subtext);
        }).addOnFailureListener(e -> {
            String title = isMy ? "My Flight Reminder Test ✈️" : cachedPartnerName + "'s Flight Reminder Test ✈️";
            String message = isMy ? "No upcoming flights found. Test flight MH2522 departs in 2 hours." : cachedPartnerName + " has no upcoming flights. Test flight MH2522 departs in 2 hours.";
            String subtext = isMy ? "(This is a test notification)" : "(" + cachedPartnerName + " will not receive this notification • This is a test notification)";
            postTestNotification(title, message, subtext);
        });
    }

    private String formatDurationHuman(long diffMs) {
        long minutes = diffMs / (60 * 1000L);
        long hours = diffMs / (60 * 60 * 1000L);
        long days = diffMs / (24 * 60 * 60 * 1000L);

        if (days >= 1) {
            return days + (days == 1 ? " day" : " days");
        } else if (hours >= 1) {
            return hours + (hours == 1 ? " hour" : " hours");
        } else {
            return Math.max(1, minutes) + (minutes == 1 ? " minute" : " minutes");
        }
    }

    private void postTestNotification(String title, String message, String subtext) {
        String channelId = "flight_alerts_test_channel";
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            android.app.NotificationChannel channel = new android.app.NotificationChannel(
                    channelId,
                    "Flight Alerts",
                    android.app.NotificationManager.IMPORTANCE_HIGH
            );
            android.app.NotificationManager nm = getSystemService(android.app.NotificationManager.class);
            if (nm != null) nm.createNotificationChannel(channel);
        }

        boolean soundEnabled = prefs.getBoolean("sound_enabled", true);
        boolean vibrationEnabled = prefs.getBoolean("vibration_enabled", true);
        int vibrationIndex = prefs.getInt(KEY_VIBRATION_PATTERN_INDEX, 0);

        androidx.core.app.NotificationCompat.Builder builder = new androidx.core.app.NotificationCompat.Builder(this, channelId)
                .setSmallIcon(R.mipmap.ic_launcher_round)
                .setContentTitle(title)
                .setContentText(message)
                .setSubText(subtext)
                .setStyle(new androidx.core.app.NotificationCompat.BigTextStyle().bigText(message + "\n" + subtext))
                .setPriority(androidx.core.app.NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true);

        if (soundEnabled) {
            String soundUriStr = prefs.getString(KEY_CUSTOM_SOUND_URI, "");
            if (soundUriStr != null && !soundUriStr.isEmpty()) {
                builder.setSound(Uri.parse(soundUriStr));
            } else {
                builder.setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION));
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
            android.app.NotificationManager nm = (android.app.NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            if (nm != null) {
                nm.notify((int) System.currentTimeMillis(), builder.build());
            }
        } catch (Exception ignored) {}
    }

    private void setupLaunchers() {
        // 1. System Ringtone / Sound Picker Launcher
        systemRingtoneLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Uri uri = result.getData().getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI);
                    if (uri != null) {
                        Ringtone ringtone = RingtoneManager.getRingtone(this, uri);
                        String name = (ringtone != null) ? ringtone.getTitle(this) : "System Sound";
                        saveCustomSound(uri.toString(), name);
                    }
                }
            }
        );

        // 2. Music / File Manager App Launcher (audio/*)
        audioFileLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Uri audioUri = result.getData().getData();
                    if (audioUri != null) {
                        try {
                            getContentResolver().takePersistableUriPermission(audioUri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
                        } catch (Exception ignored) {}

                        String fileName = resolveFileNameFromUri(audioUri);
                        saveCustomSound(audioUri.toString(), fileName);
                    }
                }
            }
        );
    }

    private void showSoundChoiceDialog() {
        com.google.android.material.bottomsheet.BottomSheetDialog sheet = new com.google.android.material.bottomsheet.BottomSheetDialog(this);
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_notification_sound_options, null);
        sheet.setContentView(view);

        view.findViewById(R.id.optionDefaultSound).setOnClickListener(v -> {
            sheet.dismiss();
            saveCustomSound("", "Default System Sound");
        });

        view.findViewById(R.id.optionSystemRingtone).setOnClickListener(v -> {
            sheet.dismiss();
            Intent intent = new Intent(RingtoneManager.ACTION_RINGTONE_PICKER);
            intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_NOTIFICATION);
            intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, "Select Notification Sound");
            String currentUriStr = prefs.getString(KEY_CUSTOM_SOUND_URI, "");
            if (!currentUriStr.isEmpty()) {
                intent.putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, Uri.parse(currentUriStr));
            }
            systemRingtoneLauncher.launch(intent);
        });

        view.findViewById(R.id.optionAudioFile).setOnClickListener(v -> {
            sheet.dismiss();
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("audio/*");
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            audioFileLauncher.launch(Intent.createChooser(intent, "Select Audio File / Music App"));
        });

        sheet.show();
    }

    private void saveCustomSound(String uriStr, String name) {
        prefs.edit()
                .putString(KEY_CUSTOM_SOUND_URI, uriStr)
                .putString(KEY_CUSTOM_SOUND_NAME, name)
                .apply();

        updateSoundUiSummary();
        stopPreviewPlayer();
        Toast.makeText(this, "Notification tone updated: " + name, Toast.LENGTH_SHORT).show();
    }

    private void updateSoundUiSummary() {
        String name = prefs.getString(KEY_CUSTOM_SOUND_NAME, "Default System Sound");
        if (tvSelectedSoundName != null) {
            tvSelectedSoundName.setText(name);
        }
    }

    private void updateVibrationUiSummary() {
        String name = prefs.getString(KEY_VIBRATION_PATTERN_NAME, "Default Pulse 📳");
        if (tvSelectedVibrationName != null) {
            tvSelectedVibrationName.setText(name);
        }
    }

    private void showVibrationPatternDialog() {
        int initialIndex = prefs.getInt(KEY_VIBRATION_PATTERN_INDEX, 0);
        final int[] selectedIndexHolder = {initialIndex};

        com.google.android.material.bottomsheet.BottomSheetDialog sheet = new com.google.android.material.bottomsheet.BottomSheetDialog(this);
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_vibration_pattern_options, null);
        sheet.setContentView(view);

        RadioGroup radioGroup = view.findViewById(R.id.radioGroupVibrations);
        Button btnCancel = view.findViewById(R.id.btnCancelVibration);
        Button btnConfirm = view.findViewById(R.id.btnConfirmVibration);

        int[] radioIds = {R.id.radioVib0, R.id.radioVib1, R.id.radioVib2, R.id.radioVib3, R.id.radioVib4};
        if (initialIndex >= 0 && initialIndex < radioIds.length) {
            radioGroup.check(radioIds[initialIndex]);
        }

        radioGroup.setOnCheckedChangeListener((group, checkedId) -> {
            int newIndex = 0;
            if (checkedId == R.id.radioVib1) newIndex = 1;
            else if (checkedId == R.id.radioVib2) newIndex = 2;
            else if (checkedId == R.id.radioVib3) newIndex = 3;
            else if (checkedId == R.id.radioVib4) newIndex = 4;

            selectedIndexHolder[0] = newIndex;
            vibrateDevicePattern(VIBRATION_PATTERNS[newIndex]);
        });

        if (btnCancel != null) btnCancel.setOnClickListener(v -> sheet.dismiss());

        if (btnConfirm != null) {
            btnConfirm.setOnClickListener(v -> {
                sheet.dismiss();
                saveVibrationPattern(selectedIndexHolder[0]);
            });
        }

        sheet.show();
    }

    private void saveVibrationPattern(int index) {
        if (index < 0 || index >= VIBRATION_PATTERN_NAMES.length) index = 0;
        String name = VIBRATION_PATTERN_NAMES[index];
        prefs.edit()
                .putInt(KEY_VIBRATION_PATTERN_INDEX, index)
                .putString(KEY_VIBRATION_PATTERN_NAME, name)
                .apply();

        updateVibrationUiSummary();
        vibrateDevicePattern(VIBRATION_PATTERNS[index]);
    }

    private void vibrateDevicePattern(long[] pattern) {
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

    private void toggleSoundPreview() {
        if (isPlayingPreview) {
            stopPreviewPlayer();
            return;
        }

        String uriStr = prefs.getString(KEY_CUSTOM_SOUND_URI, "");
        Uri soundUri;
        if (uriStr.isEmpty()) {
            soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        } else {
            soundUri = Uri.parse(uriStr);
        }

        try {
            stopPreviewPlayer();
            mediaPlayer = new MediaPlayer();
            mediaPlayer.setDataSource(this, soundUri);
            mediaPlayer.prepare();
            mediaPlayer.setOnCompletionListener(mp -> stopPreviewPlayer());
            mediaPlayer.start();

            isPlayingPreview = true;
            if (btnPreviewSound != null) {
                btnPreviewSound.setImageResource(android.R.drawable.ic_media_pause);
            }
        } catch (Exception e) {
            Toast.makeText(this, "Unable to play audio preview", Toast.LENGTH_SHORT).show();
            stopPreviewPlayer();
        }
    }

    private void stopPreviewPlayer() {
        if (mediaPlayer != null) {
            try {
                if (mediaPlayer.isPlaying()) {
                    mediaPlayer.stop();
                }
                mediaPlayer.release();
            } catch (Exception ignored) {}
            mediaPlayer = null;
        }
        isPlayingPreview = false;
        if (btnPreviewSound != null) {
            btnPreviewSound.setImageResource(android.R.drawable.ic_media_play);
        }
    }

    private String resolveFileNameFromUri(Uri uri) {
        String result = null;
        if ("content".equals(uri.getScheme())) {
            try (Cursor cursor = getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                    if (index >= 0) {
                        result = cursor.getString(index);
                    }
                }
            } catch (Exception ignored) {}
        }
        if (result == null) {
            result = uri.getPath();
            if (result != null) {
                int cut = result.lastIndexOf('/');
                if (cut != -1) {
                    result = result.substring(cut + 1);
                }
            }
        }
        return result != null ? result : "Custom Audio File";
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

        MaterialSwitch[] switches = {
            switchFlightReminders, switchLandingAlerts, switchPartnerPokes,
            switchProximityAlerts, switchWishlistAlerts, switchSound, switchVibration
        };

        for (MaterialSwitch sw : switches) {
            if (sw != null) {
                sw.setThumbTintList(thumbTintList);
                sw.setTrackTintList(trackTintList);
            }
        }
    }

    private void loadPreferences() {
        boolean remindersEnabled = prefs.getBoolean("flight_reminders", true);
        switchFlightReminders.setChecked(remindersEnabled);
        updateFlightRemindersState(remindersEnabled);

        switchLandingAlerts.setChecked(prefs.getBoolean("landing_alerts", true));
        switchPartnerPokes.setChecked(prefs.getBoolean("partner_pokes", true));
        switchProximityAlerts.setChecked(prefs.getBoolean("proximity_alerts", true));
        switchWishlistAlerts.setChecked(prefs.getBoolean("wishlist_alerts", true));

        boolean soundOn = prefs.getBoolean("sound_enabled", true);
        switchSound.setChecked(soundOn);
        updateSoundEnabledState(soundOn);

        boolean vibrationOn = prefs.getBoolean("vibration_enabled", true);
        switchVibration.setChecked(vibrationOn);
        updateVibrationState(vibrationOn);

        // Load My Flight Schedules
        Set<String> defaultMy = new HashSet<>(Arrays.asList("1 day before", "5 hours before", "1 hour before", "30 minutes before"));
        Set<String> savedMy = prefs.getStringSet(KEY_MY_SCHEDULES, defaultMy);
        myFlightSchedules.clear();
        myFlightSchedules.addAll(savedMy);

        // Load Partner Flight Schedules
        Set<String> defaultPartner = new HashSet<>(Arrays.asList("1 day before", "5 hours before", "1 hour before", "30 minutes before"));
        Set<String> savedPartner = prefs.getStringSet(KEY_PARTNER_SCHEDULES, defaultPartner);
        partnerFlightSchedules.clear();
        partnerFlightSchedules.addAll(savedPartner);
    }

    private void updateFlightRemindersState(boolean enabled) {
        if (layoutFlightRemindersSection != null) {
            layoutFlightRemindersSection.setVisibility(View.VISIBLE);
            layoutFlightRemindersSection.setEnabled(enabled);
            layoutFlightRemindersSection.setAlpha(enabled ? 1.0f : 0.4f);
        }
        View btnAddMy = findViewById(R.id.btnAddMyNotification);
        View btnAddPartner = findViewById(R.id.btnAddPartnerNotification);
        View btnTestMy = findViewById(R.id.btnTestMyFlightNotification);
        View btnTestPartner = findViewById(R.id.btnTestPartnerFlightNotification);

        if (btnAddMy != null) {
            btnAddMy.setEnabled(enabled);
            btnAddMy.setAlpha(enabled ? 1.0f : 0.4f);
        }
        if (btnAddPartner != null) {
            btnAddPartner.setEnabled(enabled);
            btnAddPartner.setAlpha(enabled ? 1.0f : 0.4f);
        }
        if (btnTestMy != null) {
            btnTestMy.setEnabled(enabled);
            btnTestMy.setAlpha(enabled ? 1.0f : 0.4f);
        }
        if (btnTestPartner != null) {
            btnTestPartner.setEnabled(enabled);
            btnTestPartner.setAlpha(enabled ? 1.0f : 0.4f);
        }
    }

    private void updateSoundEnabledState(boolean soundEnabled) {
        if (layoutSelectNotificationSound != null) {
            layoutSelectNotificationSound.setEnabled(soundEnabled);
            layoutSelectNotificationSound.setAlpha(soundEnabled ? 1.0f : 0.4f);
        }
        if (btnPreviewSound != null) {
            btnPreviewSound.setEnabled(soundEnabled);
            btnPreviewSound.setAlpha(soundEnabled ? 1.0f : 0.4f);
        }
        if (btnChangeSound != null) {
            btnChangeSound.setEnabled(soundEnabled);
            btnChangeSound.setAlpha(soundEnabled ? 1.0f : 0.4f);
        }
    }

    private void updateVibrationState(boolean vibrationEnabled) {
        if (layoutVibrationPattern != null) {
            layoutVibrationPattern.setEnabled(vibrationEnabled);
            layoutVibrationPattern.setAlpha(vibrationEnabled ? 1.0f : 0.4f);
        }
        if (btnChangeVibration != null) {
            btnChangeVibration.setEnabled(vibrationEnabled);
            btnChangeVibration.setAlpha(vibrationEnabled ? 1.0f : 0.4f);
        }
    }

    private void saveSchedules(boolean isMy) {
        if (isMy) {
            prefs.edit().putStringSet(KEY_MY_SCHEDULES, new HashSet<>(myFlightSchedules)).apply();
        } else {
            prefs.edit().putStringSet(KEY_PARTNER_SCHEDULES, new HashSet<>(partnerFlightSchedules)).apply();
        }
        renderFlightSchedules();
    }

    private void renderFlightSchedules() {
        renderScheduleContainer(layoutMyFlightRemindersContainer, myFlightSchedules, true);
        renderScheduleContainer(layoutPartnerFlightRemindersContainer, partnerFlightSchedules, false);
    }

    private void renderScheduleContainer(LinearLayout container, List<String> schedules, boolean isMy) {
        if (container == null) return;
        container.removeAllViews();

        int textColor = androidx.core.content.ContextCompat.getColor(this, R.color.text_primary);

        for (int i = 0; i < schedules.size(); i++) {
            String scheduleText = schedules.get(i);

            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(0, 10, 0, 10);

            // Bell icon
            ImageView ivBell = new ImageView(this);
            ivBell.setImageResource(android.R.drawable.ic_popup_reminder);
            LinearLayout.LayoutParams bellParams = new LinearLayout.LayoutParams(48, 48);
            bellParams.setMargins(0, 0, 16, 0);
            ivBell.setLayoutParams(bellParams);
            ivBell.setColorFilter(textColor);

            // Schedule text
            TextView tvSchedule = new TextView(this);
            tvSchedule.setText(scheduleText);
            tvSchedule.setTextColor(textColor);
            tvSchedule.setTextSize(14f);
            LinearLayout.LayoutParams textParams = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
            tvSchedule.setLayoutParams(textParams);

            // Delete X button (Red and Bigger for easy tapping)
            ImageView ivDelete = new ImageView(this);
            ivDelete.setImageResource(android.R.drawable.ic_menu_close_clear_cancel);
            int deleteSize = (int) (32 * getResources().getDisplayMetrics().density);
            int deletePadding = (int) (4 * getResources().getDisplayMetrics().density);
            LinearLayout.LayoutParams delParams = new LinearLayout.LayoutParams(deleteSize, deleteSize);
            ivDelete.setLayoutParams(delParams);
            ivDelete.setPadding(deletePadding, deletePadding, deletePadding, deletePadding);
            ivDelete.setColorFilter(android.graphics.Color.parseColor("#FF3B30"));
            ivDelete.setClickable(true);
            ivDelete.setFocusable(true);

            final int index = i;
            ivDelete.setOnClickListener(v -> {
                if (switchFlightReminders != null && !switchFlightReminders.isChecked()) return;
                schedules.remove(index);
                saveSchedules(isMy);
            });

            row.addView(ivBell);
            row.addView(tvSchedule);
            row.addView(ivDelete);

            container.addView(row);
        }
    }

    private void showAddPresetDialog(boolean isMy) {
        if (switchFlightReminders != null && !switchFlightReminders.isChecked()) return;
        String[] options = {
            "10 minutes before",
            "15 minutes before",
            "2 hours before",
            "5 hours before",
            "1 day before",
            "Custom..."
        };

        new MaterialAlertDialogBuilder(this)
                .setTitle(isMy ? "Add My Flight Notification" : "Add Partner's Flight Notification")
                .setItems(options, (dialog, which) -> {
                    if (which == options.length - 1) {
                        showCustomNotificationDialog(isMy);
                    } else {
                        String selectedPreset = options[which];
                        List<String> targetList = isMy ? myFlightSchedules : partnerFlightSchedules;
                        if (!targetList.contains(selectedPreset)) {
                            targetList.add(selectedPreset);
                            saveSchedules(isMy);
                        }
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showCustomNotificationDialog(boolean isMy) {
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_custom_notification, null);
        TextInputEditText etCustomAmount = view.findViewById(R.id.etCustomAmount);
        RadioGroup radioGroupUnits = view.findViewById(R.id.radioGroupUnits);

        Button btnCancel = view.findViewById(R.id.btnCancelCustomNotif);
        Button btnSave = view.findViewById(R.id.btnSaveCustomNotif);

        AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                .setView(view)
                .create();

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnSave.setOnClickListener(v -> {
            String amountStr = etCustomAmount.getText() != null ? etCustomAmount.getText().toString().trim() : "";
            if (amountStr.isEmpty()) {
                Toast.makeText(this, "Please enter an amount.", Toast.LENGTH_SHORT).show();
                return;
            }

            int checkedId = radioGroupUnits.getCheckedRadioButtonId();
            String unit;
            if (checkedId == R.id.radioHours) {
                unit = "hours before";
            } else if (checkedId == R.id.radioDays) {
                unit = "days before";
            } else if (checkedId == R.id.radioWeeks) {
                unit = "weeks before";
            } else {
                unit = "minutes before";
            }

            String customSchedule = amountStr + " " + unit;
            List<String> targetList = isMy ? myFlightSchedules : partnerFlightSchedules;
            if (!targetList.contains(customSchedule)) {
                targetList.add(customSchedule);
                saveSchedules(isMy);
            }
            dialog.dismiss();
        });

        dialog.show();
    }

    private void setupSwitchListeners() {
        switchFlightReminders.setOnCheckedChangeListener((v, isChecked) -> {
            prefs.edit().putBoolean("flight_reminders", isChecked).apply();
            updateFlightRemindersState(isChecked);
        });
        switchLandingAlerts.setOnCheckedChangeListener((v, isChecked) -> prefs.edit().putBoolean("landing_alerts", isChecked).apply());
        switchPartnerPokes.setOnCheckedChangeListener((v, isChecked) -> prefs.edit().putBoolean("partner_pokes", isChecked).apply());
        switchProximityAlerts.setOnCheckedChangeListener((v, isChecked) -> prefs.edit().putBoolean("proximity_alerts", isChecked).apply());
        switchWishlistAlerts.setOnCheckedChangeListener((v, isChecked) -> prefs.edit().putBoolean("wishlist_alerts", isChecked).apply());
        
        switchSound.setOnCheckedChangeListener((v, isChecked) -> {
            prefs.edit().putBoolean("sound_enabled", isChecked).apply();
            updateSoundEnabledState(isChecked);
        });

        switchVibration.setOnCheckedChangeListener((v, isChecked) -> {
            prefs.edit().putBoolean("vibration_enabled", isChecked).apply();
            updateVibrationState(isChecked);
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopPreviewPlayer();
    }

    @Override
    protected void onResume() {
        super.onResume();
        ThemeUtils.applyAccentToActivity(this);
        applySwitchAccentTints();
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
