package com.app.projectcecily;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class AccountActivity extends AppCompatActivity {

    private TextView tvUserName, tvSyncedPartner;
    private String userId, partnerId;
    private DatabaseReference dbRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_account);

        userId = getSharedPreferences("prefs", MODE_PRIVATE).getString("user_id", "user_1");
        partnerId = userId.equals("user_1") ? "user_2" : "user_1";
        dbRef = FirebaseDatabase.getInstance("https://i-must-finish-this-in-2026-default-rtdb.asia-southeast1.firebasedatabase.app/").getReference();

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Account Settings 👤");
        }

        tvUserName = findViewById(R.id.tvUserName);
        tvSyncedPartner = findViewById(R.id.tvSyncedPartner);
        
        UIUtils.applyBottomPadding(findViewById(R.id.account_scroll));
        
        setupIdentityToggle();
        setupThemeToggle();
        listenForProfileUpdates();

        findViewById(R.id.btnEditProfile).setOnClickListener(v -> showEditProfileDialog());
        
        findViewById(R.id.btnPartnerSettings).setOnClickListener(v -> 
            Toast.makeText(this, "Partner Settings Coming Soon", Toast.LENGTH_SHORT).show());
    }

    private void listenForProfileUpdates() {
        // Push initial names to cloud if they don't exist yet
        dbRef.child("profiles").child(userId).child("name").get().addOnSuccessListener(snapshot -> {
            if (!snapshot.exists()) {
                String localName = getSharedPreferences("prefs", MODE_PRIVATE).getString("user_name", "Dingus");
                dbRef.child("profiles").child(userId).child("name").setValue(localName);
            }
        });

        // Listen for YOUR name
        dbRef.child("profiles").child(userId).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String name = snapshot.child("name").getValue(String.class);
                if (name != null) tvUserName.setText(name);
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });

        // Listen for PARTNER's name
        dbRef.child("profiles").child(partnerId).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String name = snapshot.child("name").getValue(String.class);
                if (name != null) tvSyncedPartner.setText("Synced with " + name + " ❤️");
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
                    String newName = etYourName.getText().toString();
                    dbRef.child("profiles").child(userId).child("name").setValue(newName);
                    Toast.makeText(this, "Name updated in cloud!", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void setupIdentityToggle() {
        MaterialButtonToggleGroup toggleGroup = findViewById(R.id.toggleGroup);
        if (userId.equals("user_1")) toggleGroup.check(R.id.btnUserA);
        else toggleGroup.check(R.id.btnUserB);

        toggleGroup.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (isChecked) {
                String newId = (checkedId == R.id.btnUserA) ? "user_1" : "user_2";
                if (!newId.equals(userId)) {
                    getSharedPreferences("prefs", MODE_PRIVATE).edit().putString("user_id", newId).commit();
                    restartApp();
                }
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

    private void restartApp() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        Runtime.getRuntime().exit(0);
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