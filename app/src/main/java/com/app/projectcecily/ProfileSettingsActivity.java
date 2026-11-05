package com.app.projectcecily;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import java.util.Calendar;

public class ProfileSettingsActivity extends AppCompatActivity {

    private TextInputEditText etUsername, etEmail, etProfilePhoneNumber;
    private android.widget.Spinner spinnerProfileCountryCode;
    private TextInputEditText etOldPassword, etNewPassword, etConfirmNewPassword;

    private TextInputEditText etMyName, etMyBirthday;
    private MaterialButtonToggleGroup toggleMyGender;

    private TextInputEditText etPartnerName, etPartnerBirthday;
    private MaterialButtonToggleGroup togglePartnerGender;

    private String userId, partnerId = "";
    private DatabaseReference dbRef;

    private final Calendar myCal = Calendar.getInstance();
    private final Calendar partnerCal = Calendar.getInstance();
    private RelationshipManager.Details cachedDetails = new RelationshipManager.Details();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile_settings);

        userId = UserManager.getUserId(this);
        partnerId = UserManager.getPartnerId(this);
        dbRef = FirebaseDatabase.getInstance("https://i-must-finish-this-in-2026-default-rtdb.asia-southeast1.firebasedatabase.app/").getReference();

        View appBarView = findViewById(R.id.appBarLayout);
        UIUtils.applyTopInsetToAppBar(appBarView);

        com.google.android.material.appbar.MaterialToolbar toolbar = findViewById(R.id.toolbar);
        if (toolbar != null) {
            setSupportActionBar(toolbar);
            if (getSupportActionBar() != null) {
                getSupportActionBar().setDisplayHomeAsUpEnabled(true);
                getSupportActionBar().setTitle("Profile & Personal Info");
            }
        }

        etUsername = findViewById(R.id.etUsername);
        etEmail = findViewById(R.id.etEmail);
        spinnerProfileCountryCode = findViewById(R.id.spinnerProfileCountryCode);
        etProfilePhoneNumber = findViewById(R.id.etProfilePhoneNumber);
        etOldPassword = findViewById(R.id.etOldPassword);
        etNewPassword = findViewById(R.id.etNewPassword);
        etConfirmNewPassword = findViewById(R.id.etConfirmNewPassword);
        Button btnSaveSecurity = findViewById(R.id.btnSaveSecurity);

        if (spinnerProfileCountryCode != null && etProfilePhoneNumber != null) {
            spinnerProfileCountryCode.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                    if (position > 0) {
                        etProfilePhoneNumber.setEnabled(true);
                        etProfilePhoneNumber.setAlpha(1.0f);
                    } else {
                        etProfilePhoneNumber.setEnabled(false);
                        etProfilePhoneNumber.setAlpha(0.5f);
                    }
                }

                @Override
                public void onNothingSelected(android.widget.AdapterView<?> parent) {
                    etProfilePhoneNumber.setEnabled(false);
                    etProfilePhoneNumber.setAlpha(0.5f);
                }
            });
        }

        etMyName = findViewById(R.id.etMyName);
        toggleMyGender = findViewById(R.id.toggleMyGender);
        etMyBirthday = findViewById(R.id.etMyBirthday);

        etPartnerName = findViewById(R.id.etPartnerName);
        togglePartnerGender = findViewById(R.id.togglePartnerGender);
        etPartnerBirthday = findViewById(R.id.etPartnerBirthday);
        Button btnSaveProfileDetails = findViewById(R.id.btnSaveProfileDetails);

        setupDatePickers();
        loadProfileData();

        btnSaveSecurity.setOnClickListener(v -> updateSecurityCredentials());
        btnSaveProfileDetails.setOnClickListener(v -> saveProfileDetails());
    }


    private void setupDatePickers() {
        etMyBirthday.setOnClickListener(v -> new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
            myCal.set(year, month, dayOfMonth);
            etMyBirthday.setText(android.text.format.DateFormat.format("yyyy-MM-dd", myCal));
        }, myCal.get(Calendar.YEAR), myCal.get(Calendar.MONTH), myCal.get(Calendar.DAY_OF_MONTH)).show());

        etPartnerBirthday.setOnClickListener(v -> new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
            partnerCal.set(year, month, dayOfMonth);
            etPartnerBirthday.setText(android.text.format.DateFormat.format("yyyy-MM-dd", partnerCal));
        }, partnerCal.get(Calendar.YEAR), partnerCal.get(Calendar.MONTH), partnerCal.get(Calendar.DAY_OF_MONTH)).show());
    }

    private void loadProfileData() {
        dbRef.child("profiles").child(userId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String username = snapshot.child("username").getValue(String.class);
                    String email = snapshot.child("email").getValue(String.class);
                    String name = snapshot.child("name").getValue(String.class);
                    String countryCode = snapshot.child("countryCode").getValue(String.class);
                    String phoneNumber = snapshot.child("phoneNumber").getValue(String.class);

                    if (username != null) etUsername.setText(username);
                    if (email != null) etEmail.setText(email);
                    if (name != null) etMyName.setText(name);

                    if (countryCode != null && !countryCode.isEmpty() && spinnerProfileCountryCode != null) {
                        String[] array = getResources().getStringArray(R.array.country_codes_array);
                        for (int i = 0; i < array.length; i++) {
                            if (array[i].startsWith(countryCode)) {
                                spinnerProfileCountryCode.setSelection(i);
                                break;
                            }
                        }
                    }

                    if (phoneNumber != null && etProfilePhoneNumber != null) {
                        etProfilePhoneNumber.setText(phoneNumber);
                    }
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });

        RelationshipManager.listenFromFirebase(this, details -> {
            if (details != null) {
                cachedDetails = details;
                if (details.yourBirthday != null && !details.yourBirthday.isEmpty()) {
                    etMyBirthday.setText(details.yourBirthday);
                }
                if (details.partnerBirthday != null && !details.partnerBirthday.isEmpty()) {
                    etPartnerBirthday.setText(details.partnerBirthday);
                }
                setGenderToggle(toggleMyGender, details.yourGender, true);
                setGenderToggle(togglePartnerGender, details.partnerGender, false);
            }
        });

        if (partnerId != null && !partnerId.isEmpty()) {
            dbRef.child("profiles").child(partnerId).child("name").addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    String pName = snapshot.getValue(String.class);
                    if (pName != null) etPartnerName.setText(pName);
                }
                @Override
                public void onCancelled(@NonNull DatabaseError error) {}
            });
        }
    }

    private void setGenderToggle(MaterialButtonToggleGroup toggleGroup, String gender, boolean isMy) {
        if (gender == null) {
            toggleGroup.check(isMy ? R.id.btnMyMale : R.id.btnPartnerMale);
            return;
        }
        if (gender.contains("Female")) {
            toggleGroup.check(isMy ? R.id.btnMyFemale : R.id.btnPartnerFemale);
        } else if (gender.contains("Male") && !gender.contains("Fe")) {
            toggleGroup.check(isMy ? R.id.btnMyMale : R.id.btnPartnerMale);
        } else {
            toggleGroup.check(isMy ? R.id.btnMyPreferNot : R.id.btnPartnerPreferNot);
        }
    }

    private String getGenderFromToggle(MaterialButtonToggleGroup toggleGroup, boolean isMy) {
        int checkedId = toggleGroup.getCheckedButtonId();
        if (checkedId == (isMy ? R.id.btnMyFemale : R.id.btnPartnerFemale)) {
            return "Female";
        } else if (checkedId == (isMy ? R.id.btnMyPreferNot : R.id.btnPartnerPreferNot)) {
            return "Prefer not to say";
        }
        return "Male";
    }

    private void updateSecurityCredentials() {
        String newUsername = etUsername.getText() != null ? etUsername.getText().toString().trim() : "";
        String newEmail = etEmail.getText() != null ? etEmail.getText().toString().trim() : "";
        String oldPass = etOldPassword.getText() != null ? etOldPassword.getText().toString().trim() : "";
        String newPass = etNewPassword.getText() != null ? etNewPassword.getText().toString().trim() : "";
        String confirmPass = etConfirmNewPassword.getText() != null ? etConfirmNewPassword.getText().toString().trim() : "";

        dbRef.child("profiles").child(userId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists()) return;

                String dbPassword = snapshot.child("password").getValue(String.class);

                if (!newPass.isEmpty()) {
                    if (oldPass.isEmpty()) {
                        Toast.makeText(ProfileSettingsActivity.this, "Please enter your current (old) password.", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (dbPassword != null && !dbPassword.equals(oldPass)) {
                        Toast.makeText(ProfileSettingsActivity.this, "Incorrect current password!", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (!newPass.equals(confirmPass)) {
                        Toast.makeText(ProfileSettingsActivity.this, "New passwords do not match!", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (newPass.length() < 6) {
                        Toast.makeText(ProfileSettingsActivity.this, "New password must be at least 6 characters.", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    dbRef.child("profiles").child(userId).child("password").setValue(newPass);
                    etOldPassword.setText("");
                    etNewPassword.setText("");
                    etConfirmNewPassword.setText("");
                }

                if (!newUsername.isEmpty()) {
                    dbRef.child("profiles").child(userId).child("username").setValue(newUsername.toLowerCase());
                    dbRef.child("profiles").child(userId).child("name").setValue(newUsername);
                    UserManager.setUserName(ProfileSettingsActivity.this, newUsername);
                }

                if (!newEmail.isEmpty()) {
                    dbRef.child("profiles").child(userId).child("email").setValue(newEmail.toLowerCase());
                }

                if (spinnerProfileCountryCode != null && spinnerProfileCountryCode.getSelectedItemPosition() > 0) {
                    String item = spinnerProfileCountryCode.getSelectedItem().toString();
                    String code = "";
                    for (String token : item.split(" ")) {
                        if (token.startsWith("+")) {
                            code = token;
                            break;
                        }
                    }
                    String phone = etProfilePhoneNumber != null ? etProfilePhoneNumber.getText().toString().trim() : "";
                    dbRef.child("profiles").child(userId).child("countryCode").setValue(code);
                    dbRef.child("profiles").child(userId).child("phoneNumber").setValue(phone);
                }

                Toast.makeText(ProfileSettingsActivity.this, "Security credentials updated!", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void saveProfileDetails() {
        String myName = etMyName.getText() != null ? etMyName.getText().toString().trim() : "";
        String pName = etPartnerName.getText() != null ? etPartnerName.getText().toString().trim() : "";

        if (!myName.isEmpty()) {
            dbRef.child("profiles").child(userId).child("name").setValue(myName);
            UserManager.setUserName(this, myName);
        }

        if (!pName.isEmpty() && partnerId != null && !partnerId.isEmpty()) {
            dbRef.child("profiles").child(partnerId).child("name").setValue(pName);
        }

        cachedDetails.yourGender = getGenderFromToggle(toggleMyGender, true);
        cachedDetails.partnerGender = getGenderFromToggle(togglePartnerGender, false);
        cachedDetails.yourBirthday = etMyBirthday.getText() != null ? etMyBirthday.getText().toString().trim() : "";
        cachedDetails.partnerBirthday = etPartnerBirthday.getText() != null ? etPartnerBirthday.getText().toString().trim() : "";

        RelationshipManager.syncWithFirebase(this, cachedDetails);
        Toast.makeText(this, "Profile details saved successfully!", Toast.LENGTH_SHORT).show();
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
