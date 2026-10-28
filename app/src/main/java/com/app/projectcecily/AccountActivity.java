package com.app.projectcecily;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
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

    private TextView tvUserName, tvSyncedPartner, tvPairingStatus;
    private Button btnPairPartner, btnUnpairPartner;
    private String userId, partnerId;
    private DatabaseReference dbRef;

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

        tvUserName = findViewById(R.id.tvUserName);
        tvSyncedPartner = findViewById(R.id.tvSyncedPartner);
        tvPairingStatus = findViewById(R.id.tvPairingStatus);
        btnPairPartner = findViewById(R.id.btnPairPartner);
        btnUnpairPartner = findViewById(R.id.btnUnpairPartner);

        UIUtils.applyBottomPadding(findViewById(R.id.account_scroll));

        setupThemeToggle();
        listenForProfileUpdates();
        updatePairingUI();

        findViewById(R.id.btnEditProfile).setOnClickListener(v -> showEditProfileDialog());
        findViewById(R.id.btnPartnerSettings).setOnClickListener(v -> showPairingDialog());
        btnPairPartner.setOnClickListener(v -> showPairingDialog());
        btnUnpairPartner.setOnClickListener(v -> unpairPartner());
    }

    private void updatePairingUI() {
        partnerId = UserManager.getPartnerId(this);
        if (!partnerId.isEmpty()) {
            btnUnpairPartner.setVisibility(View.VISIBLE);
            btnPairPartner.setText("Manage Pairing");
        } else {
            btnUnpairPartner.setVisibility(View.GONE);
            btnPairPartner.setText("Pair with 6-Digit Code");
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

        // Listen for YOUR name
        dbRef.child("profiles").child(userId).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String name = snapshot.child("name").getValue(String.class);
                if (name != null) {
                    tvUserName.setText(name);
                    UserManager.setUserName(AccountActivity.this, name);
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