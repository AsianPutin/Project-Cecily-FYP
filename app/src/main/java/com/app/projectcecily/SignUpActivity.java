package com.app.projectcecily;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.Calendar;
import java.util.Locale;

public class SignUpActivity extends AppCompatActivity {

    private ImageView ivProfilePic;
    private EditText etName, etUsername, etEmail, etPhoneNumber, etPassword, etBirthday;
    private android.widget.Spinner spinnerCountryCode;
    private MaterialButtonToggleGroup genderToggleGroup;
    private Button btnCreateAccount, btnBackToLogin;

    private String selectedGender = "Prefer not to say";
    private String photoBase64 = null;
    private Bitmap selectedPhotoBitmap = null;

    private ActivityResultLauncher<Intent> cameraLauncher;
    private ActivityResultLauncher<Intent> galleryLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);

        UIUtils.applyTopInsetToAppBar(findViewById(R.id.appBarLayout));
        UIUtils.applyBottomPadding(findViewById(R.id.signup_scroll));

        com.google.android.material.appbar.MaterialToolbar toolbar = findViewById(R.id.toolbar);
        if (toolbar != null) {
            setSupportActionBar(toolbar);
            if (getSupportActionBar() != null) {
                getSupportActionBar().setDisplayHomeAsUpEnabled(true);
                getSupportActionBar().setTitle("Create Account");
            }
        }

        ivProfilePic = findViewById(R.id.ivSignUpProfilePic);
        etName = findViewById(R.id.etSignUpName);
        etUsername = findViewById(R.id.etSignUpUsername);
        etEmail = findViewById(R.id.etSignUpEmail);
        spinnerCountryCode = findViewById(R.id.spinnerSignUpCountryCode);
        etPhoneNumber = findViewById(R.id.etSignUpPhoneNumber);
        etPassword = findViewById(R.id.etSignUpPassword);
        etBirthday = findViewById(R.id.etSignUpBirthday);
        genderToggleGroup = findViewById(R.id.genderToggleGroup);
        btnCreateAccount = findViewById(R.id.btnCreateAccount);
        btnBackToLogin = findViewById(R.id.btnBackToLogin);

        if (spinnerCountryCode != null && etPhoneNumber != null) {
            spinnerCountryCode.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                    if (position > 0) {
                        etPhoneNumber.setEnabled(true);
                        etPhoneNumber.setAlpha(1.0f);
                    } else {
                        etPhoneNumber.setEnabled(false);
                        etPhoneNumber.setAlpha(0.5f);
                        etPhoneNumber.setText("");
                    }
                }

                @Override
                public void onNothingSelected(android.widget.AdapterView<?> parent) {
                    etPhoneNumber.setEnabled(false);
                    etPhoneNumber.setAlpha(0.5f);
                }
            });
        }

        setupPhotoPickerLaunchers();

        findViewById(R.id.btnSignUpChangeProfilePic).setOnClickListener(v -> showProfilePictureOptions());
        ivProfilePic.setOnClickListener(v -> showProfilePictureOptions());

        genderToggleGroup.check(R.id.btnGenderOther);
        genderToggleGroup.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (isChecked) {
                if (checkedId == R.id.btnGenderMale) selectedGender = "Male";
                else if (checkedId == R.id.btnGenderFemale) selectedGender = "Female";
                else selectedGender = "Prefer not to say";
            }
        });

        etBirthday.setOnClickListener(v -> showDatePicker());

        btnCreateAccount.setOnClickListener(v -> handleSignUp());
        btnBackToLogin.setOnClickListener(v -> finish());
    }


    @Override
    protected void onResume() {
        super.onResume();
        ThemeUtils.applyAccentToActivity(this);
    }

    private void showDatePicker() {
        Calendar cal = Calendar.getInstance();
        DatePickerDialog dialog = new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
            String formatted = String.format(Locale.US, "%04d-%02d-%02d", year, month + 1, dayOfMonth);
            etBirthday.setText(formatted);
        }, cal.get(Calendar.YEAR) - 20, cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH));
        dialog.show();
    }

    private void setupPhotoPickerLaunchers() {
        cameraLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Bitmap photo = (Bitmap) result.getData().getExtras().get("data");
                    if (photo != null) showCropEditorDialog(photo);
                }
            }
        );

        galleryLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Uri imageUri = result.getData().getData();
                    if (imageUri != null) {
                        try {
                            InputStream is = getContentResolver().openInputStream(imageUri);
                            Bitmap bitmap = BitmapFactory.decodeStream(is);
                            if (bitmap != null) showCropEditorDialog(bitmap);
                        } catch (Exception e) {
                            Toast.makeText(this, "Failed to load image", Toast.LENGTH_SHORT).show();
                        }
                    }
                }
            }
        );
    }

    private void showProfilePictureOptions() {
        BottomSheetDialog sheet = new BottomSheetDialog(this);
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_profile_picture_options, null);
        sheet.setContentView(view);

        View optionViewPic = view.findViewById(R.id.optionViewPicture);
        if (optionViewPic != null) optionViewPic.setVisibility(View.GONE);

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
                processProfileImage(croppedCircleBitmap);
            } else {
                processProfileImage(rawBitmap);
            }
            dialog.dismiss();
        });

        dialog.show();
    }

    private void processProfileImage(Bitmap originalBitmap) {
        Bitmap scaled = Bitmap.createScaledBitmap(originalBitmap, 300, 300, true);
        selectedPhotoBitmap = scaled;
        ivProfilePic.setImageBitmap(scaled);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        scaled.compress(Bitmap.CompressFormat.JPEG, 75, baos);
        byte[] bytes = baos.toByteArray();
        photoBase64 = "data:image/jpeg;base64," + Base64.encodeToString(bytes, Base64.DEFAULT);
    }

    private void handleSignUp() {
        String name = etName.getText().toString().trim();
        String username = etUsername.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        String birthday = etBirthday.getText().toString().trim();

        String selectedCountryCode = (spinnerCountryCode != null && spinnerCountryCode.getSelectedItemPosition() > 0) ?
                spinnerCountryCode.getSelectedItem().toString().split(" ")[0] : "";
        String phoneNumber = (etPhoneNumber != null && etPhoneNumber.isEnabled()) ?
                etPhoneNumber.getText().toString().trim() : "";

        if (name.isEmpty() || username.isEmpty() || email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please fill in Name, Username, Email, and Password.", Toast.LENGTH_SHORT).show();
            return;
        }

        btnCreateAccount.setEnabled(false);
        btnCreateAccount.setText("Creating Account...");

        UserManager.signUp(this, name, username, email, selectedCountryCode, phoneNumber, password, selectedGender, birthday, photoBase64, new UserManager.AuthCallback() {
            @Override
            public void onSuccess(String message) {
                Toast.makeText(SignUpActivity.this, message, Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(SignUpActivity.this, MainActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            }

            @Override
            public void onError(String message) {
                btnCreateAccount.setEnabled(true);
                btnCreateAccount.setText("Create Account");
                Toast.makeText(SignUpActivity.this, message, Toast.LENGTH_LONG).show();
            }
        });
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
