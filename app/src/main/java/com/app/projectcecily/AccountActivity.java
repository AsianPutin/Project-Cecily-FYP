package com.app.projectcecily;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;

public class AccountActivity extends AppCompatActivity {

    private TextView tvUserName, tvSyncedPartner, tvPairingStatus;
    private ShapeableImageView ivProfilePic;
    private Button btnPairPartner, btnUnpairPartner;
    private String userId, partnerId;
    private DatabaseReference dbRef;
    private Bitmap currentProfileBitmap;

    private final ActivityResultLauncher<Intent> galleryLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Uri imageUri = result.getData().getData();
                    if (imageUri != null) {
                        try {
                            InputStream inputStream = getContentResolver().openInputStream(imageUri);
                            Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
                            if (bitmap != null) {
                                showCropEditorDialog(bitmap);
                            }
                        } catch (Exception e) {
                            Toast.makeText(this, "Failed to load image from gallery", Toast.LENGTH_SHORT).show();
                        }
                    }
                }
            }
    );

    private final ActivityResultLauncher<Intent> cameraLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Bundle extras = result.getData().getExtras();
                    if (extras != null && extras.get("data") instanceof Bitmap) {
                        Bitmap bitmap = (Bitmap) extras.get("data");
                        showCropEditorDialog(bitmap);
                    }
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_account);

        userId = UserManager.getUserId(this);
        partnerId = UserManager.getPartnerId(this);
        dbRef = FirebaseDatabase.getInstance("https://i-must-finish-this-in-2026-default-rtdb.asia-southeast1.firebasedatabase.app/").getReference();

        View appBarView = findViewById(R.id.appBarLayout);
        UIUtils.applyTopInsetToAppBar(appBarView);

        View toolbarView = findViewById(R.id.toolbar);
        if (toolbarView instanceof com.google.android.material.appbar.MaterialToolbar) {
            setSupportActionBar((com.google.android.material.appbar.MaterialToolbar) toolbarView);
            if (getSupportActionBar() != null) {
                getSupportActionBar().setDisplayHomeAsUpEnabled(true);
                getSupportActionBar().setTitle("Account Settings");
            }
        }

        ivProfilePic = findViewById(R.id.ivProfilePic);
        tvUserName = findViewById(R.id.tvUserName);
        tvSyncedPartner = findViewById(R.id.tvSyncedPartner);
        tvPairingStatus = findViewById(R.id.tvPairingStatus);
        btnPairPartner = findViewById(R.id.btnPairPartner);
        btnUnpairPartner = findViewById(R.id.btnUnpairPartner);

        UIUtils.applyBottomPadding(findViewById(R.id.account_scroll));
        UIUtils.applyTopInsetPadding(findViewById(R.id.account_scroll));

        loadSavedProfilePicture();
        listenForProfileUpdates();
        updatePairingUI();

        View.OnClickListener profilePicClickListener = v -> showProfilePictureOptionsSheet();
        ivProfilePic.setOnClickListener(profilePicClickListener);
        findViewById(R.id.btnChangeProfilePic).setOnClickListener(profilePicClickListener);

        findViewById(R.id.btnProfileInfo).setOnClickListener(v -> startActivity(new Intent(this, ProfileSettingsActivity.class)));
        findViewById(R.id.btnAppearance).setOnClickListener(v -> startActivity(new Intent(this, AppearanceActivity.class)));
        findViewById(R.id.btnNotificationSettings).setOnClickListener(v -> startActivity(new Intent(this, NotificationSettingsActivity.class)));
        btnPairPartner.setOnClickListener(v -> showPairingDialog());
        btnUnpairPartner.setOnClickListener(v -> unpairPartner());

        findViewById(R.id.btnLogout).setOnClickListener(v -> {
            new MaterialAlertDialogBuilder(this)
                    .setTitle("Log Out?")
                    .setMessage("Are you sure you want to log out?")
                    .setPositiveButton("Log Out", (dialog, which) -> {
                        UserManager.logout(this);
                        Intent intent = new Intent(this, LoginActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                        finish();
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        });
    }

    private void showProfilePictureOptionsSheet() {
        BottomSheetDialog sheet = new BottomSheetDialog(this);
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_profile_picture_options, null);
        sheet.setContentView(view);

        view.findViewById(R.id.optionViewPicture).setOnClickListener(v -> {
            sheet.dismiss();
            showFullProfilePictureDialog();
        });

        view.findViewById(R.id.optionCamera).setOnClickListener(v -> {
            sheet.dismiss();
            Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            cameraLauncher.launch(intent);
        });

        view.findViewById(R.id.optionGallery).setOnClickListener(v -> {
            sheet.dismiss();
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            galleryLauncher.launch(intent);
        });

        sheet.show();
    }

    private void showFullProfilePictureDialog() {
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_view_full_image, null);
        ImageView ivFull = view.findViewById(R.id.ivFullImage);
        
        if (currentProfileBitmap != null) {
            ivFull.setImageBitmap(currentProfileBitmap);
        } else {
            ivFull.setImageResource(R.mipmap.ic_launcher_round);
        }

        AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                .setView(view)
                .create();

        view.findViewById(R.id.btnCloseFullImage).setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    private void showCropEditorDialog(Bitmap rawBitmap) {
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
            Bitmap croppedCircleBitmap = ivCropTarget.getCroppedCircleBitmap(overlayView.getCircleRect());
            if (croppedCircleBitmap != null) {
                processAndSaveProfileImage(croppedCircleBitmap);
            } else {
                processAndSaveProfileImage(rawBitmap);
            }
            dialog.dismiss();
        });

        dialog.show();
    }

    private void processAndSaveProfileImage(Bitmap originalBitmap) {
        Bitmap scaled = Bitmap.createScaledBitmap(originalBitmap, 300, 300, true);
        currentProfileBitmap = scaled;
        ivProfilePic.setImageBitmap(scaled);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        scaled.compress(Bitmap.CompressFormat.JPEG, 75, baos);
        byte[] bytes = baos.toByteArray();
        String base64Image = "data:image/jpeg;base64," + Base64.encodeToString(bytes, Base64.DEFAULT);

        // Save locally
        getSharedPreferences("prefs", MODE_PRIVATE).edit().putString("profile_pic_base64", base64Image).apply();

        // Sync to cloud
        dbRef.child("profiles").child(userId).child("photoBase64").setValue(base64Image);
        Toast.makeText(this, "Profile picture updated!", Toast.LENGTH_SHORT).show();
    }

    private void loadSavedProfilePicture() {
        String base64Str = getSharedPreferences("prefs", MODE_PRIVATE).getString("profile_pic_base64", null);
        if (base64Str != null) {
            displayBase64Image(base64Str);
        }
    }

    private void displayBase64Image(String base64Str) {
        try {
            String clean = base64Str.contains(",") ? base64Str.split(",")[1] : base64Str;
            byte[] decodedBytes = Base64.decode(clean, Base64.DEFAULT);
            Bitmap bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
            if (bitmap != null) {
                currentProfileBitmap = bitmap;
                ivProfilePic.setImageBitmap(bitmap);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void updatePairingUI() {
        partnerId = UserManager.getPartnerId(this);
        if (!partnerId.isEmpty()) {
            btnPairPartner.setVisibility(View.GONE);
            btnUnpairPartner.setVisibility(View.VISIBLE);
            btnUnpairPartner.setBackgroundTintList(android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#FF3B30")));
            btnUnpairPartner.setTextColor(android.graphics.Color.WHITE);
        } else {
            btnPairPartner.setVisibility(View.VISIBLE);
            btnPairPartner.setText("Pair with 6-Digit Code");
            btnUnpairPartner.setVisibility(View.GONE);
            tvPairingStatus.setText("Not paired with anyone");
            tvSyncedPartner.setText("Not Synced");
        }

        View btnLogout = findViewById(R.id.btnLogout);
        if (btnLogout instanceof Button) {
            ((Button) btnLogout).setBackgroundTintList(android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#FF3B30")));
            ((Button) btnLogout).setTextColor(android.graphics.Color.WHITE);
        }
    }

    private void listenForProfileUpdates() {
        dbRef.child("profiles").child(userId).child("name").get().addOnSuccessListener(snapshot -> {
            if (!snapshot.exists()) {
                String localName = UserManager.getUserName(this);
                dbRef.child("profiles").child(userId).child("name").setValue(localName);
            }
        });

        // Listen for YOUR profile updates (Name & Photo)
        dbRef.child("profiles").child(userId).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String name = snapshot.child("name").getValue(String.class);
                if (name != null) {
                    tvUserName.setText(name);
                    UserManager.setUserName(AccountActivity.this, name);
                }

                String photoBase64 = snapshot.child("photoBase64").getValue(String.class);
                if (photoBase64 != null && currentProfileBitmap == null) {
                    displayBase64Image(photoBase64);
                }

                String currentPartner = snapshot.child("partnerId").getValue(String.class);
                if (currentPartner != null && !currentPartner.isEmpty()) {
                    UserManager.setPartnerId(AccountActivity.this, currentPartner);
                    partnerId = currentPartner;
                    updatePairingUI();
                    listenForPartnerName();
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });

        if (!partnerId.isEmpty()) {
            listenForPartnerName();
        }
    }

    private void listenForPartnerName() {
        if (partnerId.isEmpty()) return;
        dbRef.child("profiles").child(partnerId).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String name = snapshot.child("name").getValue(String.class);
                String nameStr = (name != null) ? name : "Partner";
                tvSyncedPartner.setText("Synced with " + nameStr);
                tvPairingStatus.setText("Paired with " + nameStr);
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    public void showEditProfileDialog() {
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_edit_profile, null);
        TextInputEditText etYourName = view.findViewById(R.id.etYourName);
        etYourName.setText(tvUserName.getText().toString());

        new MaterialAlertDialogBuilder(this)
                .setView(view)
                .setTitle("Update Your Display Name")
                .setPositiveButton("Save", (dialog, which) -> {
                    String newName = etYourName.getText() != null ? etYourName.getText().toString().trim() : "";
                    if (!newName.isEmpty()) {
                        UserManager.setUserName(this, newName);
                        dbRef.child("profiles").child(userId).child("name").setValue(newName);
                        Toast.makeText(this, "Name updated!", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showPairingDialog() {
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_partner_pairing, null);
        TextView tvGeneratedCode = view.findViewById(R.id.tvGeneratedCode);
        Button btnGenerate = view.findViewById(R.id.btnGenerateCode);
        TextInputEditText etInputCode = view.findViewById(R.id.etInputCode);
        Button btnConnect = view.findViewById(R.id.btnConnectCode);

        AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                .setView(view)
                .setTitle("Pairing Settings")
                .setNegativeButton("Close", null)
                .create();

        btnGenerate.setOnClickListener(v -> {
            btnGenerate.setEnabled(false);
            tvGeneratedCode.setText("Generating...");
            UserManager.generatePairingCode(this, new UserManager.PairingCallback() {
                @Override
                public void onSuccess(String codeOrResult) {
                    if (codeOrResult.startsWith("PAIRED:")) {
                        String partnerName = codeOrResult.replace("PAIRED:", "");
                        Toast.makeText(AccountActivity.this, "Paired with " + partnerName + "!", Toast.LENGTH_LONG).show();
                        if (dialog.isShowing()) dialog.dismiss();
                        updatePairingUI();
                        showRelationshipDetailsDialog();
                    } else {
                        tvGeneratedCode.setText(codeOrResult);
                        btnGenerate.setEnabled(true);
                        Toast.makeText(AccountActivity.this, "Share code " + codeOrResult + " with your partner!", Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onError(String message) {
                    tvGeneratedCode.setText("------");
                    btnGenerate.setEnabled(true);
                    Toast.makeText(AccountActivity.this, message, Toast.LENGTH_SHORT).show();
                }
            });
        });

        btnConnect.setOnClickListener(v -> {
            String code = etInputCode.getText() != null ? etInputCode.getText().toString() : "";
            btnConnect.setEnabled(false);
            UserManager.connectWithCode(this, code, new UserManager.PairingCallback() {
                @Override
                public void onSuccess(String partnerName) {
                    Toast.makeText(AccountActivity.this, "Successfully paired with " + partnerName + "!", Toast.LENGTH_LONG).show();
                    if (dialog.isShowing()) dialog.dismiss();
                    updatePairingUI();
                    showRelationshipDetailsDialog();
                }

                @Override
                public void onError(String message) {
                    btnConnect.setEnabled(true);
                    Toast.makeText(AccountActivity.this, message, Toast.LENGTH_SHORT).show();
                }
            });
        });

        dialog.show();
    }

    private void showRelationshipDetailsDialog() {
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_relationship_details, null);
        TextInputEditText etYourBirthday = view.findViewById(R.id.etYourBirthday);
        TextInputEditText etPartnerBirthday = view.findViewById(R.id.etPartnerBirthday);
        TextInputEditText etAnniversaryDate = view.findViewById(R.id.etAnniversaryDate);
        TextInputEditText etMilestones = view.findViewById(R.id.etMilestones);

        MaterialButtonToggleGroup toggleYourGender = view.findViewById(R.id.toggleYourGender);
        MaterialButtonToggleGroup togglePartnerGender = view.findViewById(R.id.togglePartnerGender);

        Button btnCancel = view.findViewById(R.id.btnCancelRelationship);
        Button btnSave = view.findViewById(R.id.btnSaveRelationship);

        RelationshipManager.Details currentDetails = RelationshipManager.getLocalDetails(this);

        // Store initial snapshot for unsaved changes comparison
        String initialYourBday = currentDetails.yourBirthday;
        String initialPartnerBday = currentDetails.partnerBirthday;
        String initialAnniversary = currentDetails.anniversaryDate;
        String initialMilestones = currentDetails.milestones;
        String initialYourGender = currentDetails.yourGender;
        String initialPartnerGender = currentDetails.partnerGender;

        etYourBirthday.setText(currentDetails.yourBirthday);
        etPartnerBirthday.setText(currentDetails.partnerBirthday);
        etAnniversaryDate.setText(currentDetails.anniversaryDate);
        etMilestones.setText(currentDetails.milestones);

        final long[] selectedAnniversaryTs = {currentDetails.anniversaryTimestamp};

        // Your Gender selection
        if ("Female (She/Her)".equals(currentDetails.yourGender)) toggleYourGender.check(R.id.btnYourFemale);
        else if ("Prefer not to say (They/Them)".equals(currentDetails.yourGender)) toggleYourGender.check(R.id.btnYourThey);
        else toggleYourGender.check(R.id.btnYourMale);

        // Partner Gender selection
        if ("Female (She/Her)".equals(currentDetails.partnerGender)) togglePartnerGender.check(R.id.btnPartnerFemale);
        else if ("Prefer not to say (They/Them)".equals(currentDetails.partnerGender)) togglePartnerGender.check(R.id.btnPartnerThey);
        else togglePartnerGender.check(R.id.btnPartnerMale);

        // Date Pickers
        etYourBirthday.setOnClickListener(v -> openDatePickerDialog(etYourBirthday, null));
        etPartnerBirthday.setOnClickListener(v -> openDatePickerDialog(etPartnerBirthday, null));
        etAnniversaryDate.setOnClickListener(v -> openDatePickerDialog(etAnniversaryDate, ts -> selectedAnniversaryTs[0] = ts));

        AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                .setView(view)
                .create();

        java.util.function.Supplier<Boolean> hasUnsavedChanges = () -> {
            String curYourBday = etYourBirthday.getText() != null ? etYourBirthday.getText().toString().trim() : "";
            String curPartnerBday = etPartnerBirthday.getText() != null ? etPartnerBirthday.getText().toString().trim() : "";
            String curAnniversary = etAnniversaryDate.getText() != null ? etAnniversaryDate.getText().toString().trim() : "";
            String curMilestones = etMilestones.getText() != null ? etMilestones.getText().toString().trim() : "";

            String curYourGender;
            int yourCheck = toggleYourGender.getCheckedButtonId();
            if (yourCheck == R.id.btnYourFemale) curYourGender = "Female (She/Her)";
            else if (yourCheck == R.id.btnYourThey) curYourGender = "Prefer not to say (They/Them)";
            else curYourGender = "Male (He/Him)";

            String curPartnerGender;
            int partnerCheck = togglePartnerGender.getCheckedButtonId();
            if (partnerCheck == R.id.btnPartnerFemale) curPartnerGender = "Female (She/Her)";
            else if (partnerCheck == R.id.btnPartnerThey) curPartnerGender = "Prefer not to say (They/Them)";
            else curPartnerGender = "Male (He/Him)";

            return !curYourBday.equals(initialYourBday) ||
                   !curPartnerBday.equals(initialPartnerBday) ||
                   !curAnniversary.equals(initialAnniversary) ||
                   !curMilestones.equals(initialMilestones) ||
                   !curYourGender.equals(initialYourGender) ||
                   !curPartnerGender.equals(initialPartnerGender);
        };

        Runnable handleCancelAction = () -> {
            if (hasUnsavedChanges.get()) {
                new MaterialAlertDialogBuilder(this)
                        .setTitle("Discard Changes?")
                        .setMessage("You have unsaved relationship changes. Are you sure you want to discard them?")
                        .setPositiveButton("Discard", (d, which) -> dialog.dismiss())
                        .setNegativeButton("Keep Editing", null)
                        .show();
            } else {
                dialog.dismiss();
            }
        };

        btnCancel.setOnClickListener(v -> handleCancelAction.run());

        btnSave.setOnClickListener(v -> {
            RelationshipManager.Details newDetails = new RelationshipManager.Details();
            newDetails.yourBirthday = etYourBirthday.getText() != null ? etYourBirthday.getText().toString().trim() : "";
            newDetails.partnerBirthday = etPartnerBirthday.getText() != null ? etPartnerBirthday.getText().toString().trim() : "";
            newDetails.anniversaryDate = etAnniversaryDate.getText() != null ? etAnniversaryDate.getText().toString().trim() : "";
            newDetails.anniversaryTimestamp = selectedAnniversaryTs[0];
            newDetails.milestones = etMilestones.getText() != null ? etMilestones.getText().toString().trim() : "";

            int yourCheck = toggleYourGender.getCheckedButtonId();
            if (yourCheck == R.id.btnYourFemale) newDetails.yourGender = "Female (She/Her)";
            else if (yourCheck == R.id.btnYourThey) newDetails.yourGender = "Prefer not to say (They/Them)";
            else newDetails.yourGender = "Male (He/Him)";

            int partnerCheck = togglePartnerGender.getCheckedButtonId();
            if (partnerCheck == R.id.btnPartnerFemale) newDetails.partnerGender = "Female (She/Her)";
            else if (partnerCheck == R.id.btnPartnerThey) newDetails.partnerGender = "Prefer not to say (They/Them)";
            else newDetails.partnerGender = "Male (He/Him)";

            RelationshipManager.syncWithFirebase(this, newDetails);
            Toast.makeText(this, "Relationship details saved!", Toast.LENGTH_SHORT).show();
            dialog.dismiss();
        });

        dialog.show();
    }

    private interface DateSelectedCallback {
        void onDateSelected(long timestamp);
    }

    private void openDatePickerDialog(TextInputEditText targetEditText, DateSelectedCallback callback) {
        java.util.Calendar cal = java.util.Calendar.getInstance();
        android.app.DatePickerDialog datePicker = new android.app.DatePickerDialog(
                this,
                (view, year, month, dayOfMonth) -> {
                    cal.set(year, month, dayOfMonth, 0, 0, 0);
                    String dateStr = String.format(java.util.Locale.ENGLISH, "%04d-%02d-%02d", year, month + 1, dayOfMonth);
                    targetEditText.setText(dateStr);
                    if (callback != null) callback.onDateSelected(cal.getTimeInMillis());
                },
                cal.get(java.util.Calendar.YEAR),
                cal.get(java.util.Calendar.MONTH),
                cal.get(java.util.Calendar.DAY_OF_MONTH)
        );
        datePicker.show();
    }

    private void unpairPartner() {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Unpair Partner?")
                .setMessage("Are you sure you want to unpair? You can pair again later using a new code.")
                .setPositiveButton("Unpair", (dialog, which) -> {
                    UserManager.unpairPartner(this, () -> {
                        updatePairingUI();
                        Toast.makeText(this, "Unpaired", Toast.LENGTH_SHORT).show();
                    });
                })
                .setNegativeButton("Cancel", null)
                .show();
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