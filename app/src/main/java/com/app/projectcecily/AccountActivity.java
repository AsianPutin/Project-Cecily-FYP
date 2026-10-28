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

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Account Settings 👤");
        }

        ivProfilePic = findViewById(R.id.ivProfilePic);
        tvUserName = findViewById(R.id.tvUserName);
        tvSyncedPartner = findViewById(R.id.tvSyncedPartner);
        tvPairingStatus = findViewById(R.id.tvPairingStatus);
        btnPairPartner = findViewById(R.id.btnPairPartner);
        btnUnpairPartner = findViewById(R.id.btnUnpairPartner);

        UIUtils.applyBottomPadding(findViewById(R.id.account_scroll));

        setupThemeToggle();
        setupUnitToggle();
        loadSavedProfilePicture();
        listenForProfileUpdates();
        updatePairingUI();

        View.OnClickListener profilePicClickListener = v -> showProfilePictureOptionsSheet();
        ivProfilePic.setOnClickListener(profilePicClickListener);
        findViewById(R.id.btnChangeProfilePic).setOnClickListener(profilePicClickListener);

        findViewById(R.id.btnEditProfile).setOnClickListener(v -> showEditProfileDialog());
        btnPairPartner.setOnClickListener(v -> showPairingDialog());
        btnUnpairPartner.setOnClickListener(v -> unpairPartner());
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
        } else {
            btnPairPartner.setVisibility(View.VISIBLE);
            btnPairPartner.setText("Pair with 6-Digit Code");
            btnUnpairPartner.setVisibility(View.GONE);
            tvPairingStatus.setText("Not paired with anyone");
            tvSyncedPartner.setText("Not Synced");
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
                tvSyncedPartner.setText("Synced with " + nameStr + " ❤️");
                tvPairingStatus.setText("Paired with " + nameStr);
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void showEditProfileDialog() {
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
                .setTitle("Pairing Settings 🤝")
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
                        Toast.makeText(AccountActivity.this, "Paired with " + partnerName + "! ❤️", Toast.LENGTH_LONG).show();
                        if (dialog.isShowing()) dialog.dismiss();
                        updatePairingUI();
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
                    Toast.makeText(AccountActivity.this, "Successfully paired with " + partnerName + "! ❤️", Toast.LENGTH_LONG).show();
                    if (dialog.isShowing()) dialog.dismiss();
                    updatePairingUI();
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

    private void setupUnitToggle() {
        MaterialButtonToggleGroup unitGroup = findViewById(R.id.unitToggleGroup);
        String currentUnit = UnitUtils.getUnitSystem(this);
        if (UnitUtils.UNIT_IMPERIAL.equals(currentUnit)) {
            unitGroup.check(R.id.btnUnitImperial);
        } else {
            unitGroup.check(R.id.btnUnitMetric);
        }

        unitGroup.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (isChecked) {
                String unit = (checkedId == R.id.btnUnitImperial) ? UnitUtils.UNIT_IMPERIAL : UnitUtils.UNIT_METRIC;
                UnitUtils.setUnitSystem(this, unit);
                Toast.makeText(this, "Units changed to " + (unit.equals(UnitUtils.UNIT_IMPERIAL) ? "Imperial (mi/ft)" : "Metric (km/m)"), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupThemeToggle() {
        MaterialButtonToggleGroup themeGroup = findViewById(R.id.themeToggleGroup);
        int currentMode = getSharedPreferences("prefs", MODE_PRIVATE).getInt("theme_mode", AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
        if (currentMode == AppCompatDelegate.MODE_NIGHT_NO) themeGroup.check(R.id.btnThemeLight);
        else if (currentMode == AppCompatDelegate.MODE_NIGHT_YES) themeGroup.check(R.id.btnThemeDark);
        else themeGroup.check(R.id.btnThemeAuto);

        themeGroup.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (isChecked) {
                int mode = (checkedId == R.id.btnThemeLight) ? AppCompatDelegate.MODE_NIGHT_NO : 
                          (checkedId == R.id.btnThemeDark) ? AppCompatDelegate.MODE_NIGHT_YES : 
                          AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM;
                getSharedPreferences("prefs", MODE_PRIVATE).edit().putInt("theme_mode", mode).apply();
                AppCompatDelegate.setDefaultNightMode(mode);
            }
        });
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