package com.app.projectcecily;

import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import com.google.android.material.button.MaterialButtonToggleGroup;

public class AppearanceActivity extends AppCompatActivity {

    private MaterialButtonToggleGroup themeToggleGroup;

    private ImageView checkBlue, checkPink, checkGreen, checkOrange, checkPurple, checkRed, checkTeal, checkGold;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_appearance);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Appearance & Theme");
        }

        UIUtils.applyBottomPadding(findViewById(R.id.appearance_scroll));
        UIUtils.applyTopInsetPadding(findViewById(R.id.appearance_scroll));

        themeToggleGroup = findViewById(R.id.themeToggleGroup);

        checkBlue = findViewById(R.id.checkBlue);
        checkPink = findViewById(R.id.checkPink);
        checkGreen = findViewById(R.id.checkGreen);
        checkOrange = findViewById(R.id.checkOrange);
        checkPurple = findViewById(R.id.checkPurple);
        checkRed = findViewById(R.id.checkRed);
        checkTeal = findViewById(R.id.checkTeal);
        checkGold = findViewById(R.id.checkGold);

        setupThemeToggle();
        setupColorSwatches();
        updateSelectedAccentUI(ThemeUtils.getAccentColorHex(this));
    }

    private void setupThemeToggle() {
        int currentMode = ThemeUtils.getSavedThemeMode(this);
        if (currentMode == AppCompatDelegate.MODE_NIGHT_NO) {
            themeToggleGroup.check(R.id.btnThemeLight);
        } else if (currentMode == AppCompatDelegate.MODE_NIGHT_YES) {
            themeToggleGroup.check(R.id.btnThemeDark);
        } else {
            themeToggleGroup.check(R.id.btnThemeAuto);
        }

        themeToggleGroup.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (!isChecked) return;
            int newMode = AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM;
            if (checkedId == R.id.btnThemeLight) {
                newMode = AppCompatDelegate.MODE_NIGHT_NO;
            } else if (checkedId == R.id.btnThemeDark) {
                newMode = AppCompatDelegate.MODE_NIGHT_YES;
            }
            ThemeUtils.saveThemeMode(this, newMode);
        });
    }

    private void setupColorSwatches() {
        findViewById(R.id.colorBlue).setOnClickListener(v -> selectAccent("#007AFF"));
        findViewById(R.id.colorPink).setOnClickListener(v -> selectAccent("#FF4081"));
        findViewById(R.id.colorGreen).setOnClickListener(v -> selectAccent("#25D366"));
        findViewById(R.id.colorOrange).setOnClickListener(v -> selectAccent("#FF9500"));
        findViewById(R.id.colorPurple).setOnClickListener(v -> selectAccent("#9C27B0"));
        findViewById(R.id.colorRed).setOnClickListener(v -> selectAccent("#E91E63"));
        findViewById(R.id.colorTeal).setOnClickListener(v -> selectAccent("#009688"));
        findViewById(R.id.colorGold).setOnClickListener(v -> selectAccent("#FFC107"));
    }

    @Override
    protected void onResume() {
        super.onResume();
        ThemeUtils.applyAccentToActivity(this);
    }

    private void selectAccent(String hexColor) {
        ThemeUtils.saveAccentColor(this, hexColor);
        updateSelectedAccentUI(hexColor);
        ThemeUtils.applyAccentToActivity(this);
        Toast.makeText(this, "Accent color updated!", Toast.LENGTH_SHORT).show();
    }

    private void updateSelectedAccentUI(String currentHex) {
        checkBlue.setVisibility("#007AFF".equalsIgnoreCase(currentHex) ? View.VISIBLE : View.GONE);
        checkPink.setVisibility("#FF4081".equalsIgnoreCase(currentHex) ? View.VISIBLE : View.GONE);
        checkGreen.setVisibility("#25D366".equalsIgnoreCase(currentHex) ? View.VISIBLE : View.GONE);
        checkOrange.setVisibility("#FF9500".equalsIgnoreCase(currentHex) ? View.VISIBLE : View.GONE);
        checkPurple.setVisibility("#9C27B0".equalsIgnoreCase(currentHex) ? View.VISIBLE : View.GONE);
        checkRed.setVisibility("#E91E63".equalsIgnoreCase(currentHex) ? View.VISIBLE : View.GONE);
        checkTeal.setVisibility("#009688".equalsIgnoreCase(currentHex) ? View.VISIBLE : View.GONE);
        checkGold.setVisibility("#FFC107".equalsIgnoreCase(currentHex) ? View.VISIBLE : View.GONE);
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
