package com.app.projectcecily;

import android.graphics.Color;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.card.MaterialCardView;

public class AppearanceActivity extends AppCompatActivity {

    private MaterialButtonToggleGroup themeToggleGroup;

    private MaterialCardView cardBlue, cardPink, cardGreen, cardOrange, cardPurple, cardRed, cardTeal, cardGold;
    private ImageView checkBlue, checkPink, checkGreen, checkOrange, checkPurple, checkRed, checkTeal, checkGold;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_appearance);

        View appBarView = findViewById(R.id.appBarLayout);
        UIUtils.applyTopInsetToAppBar(appBarView);

        View toolbarView = findViewById(R.id.toolbar);
        if (toolbarView instanceof com.google.android.material.appbar.MaterialToolbar) {
            setSupportActionBar((com.google.android.material.appbar.MaterialToolbar) toolbarView);
            if (getSupportActionBar() != null) {
                getSupportActionBar().setDisplayHomeAsUpEnabled(true);
                getSupportActionBar().setTitle("Appearance & Theme");
            }
        }

        UIUtils.applyBottomPadding(findViewById(R.id.appearance_scroll));

        themeToggleGroup = findViewById(R.id.themeToggleGroup);

        cardBlue = findViewById(R.id.colorBlue);
        cardPink = findViewById(R.id.colorPink);
        cardGreen = findViewById(R.id.colorGreen);
        cardOrange = findViewById(R.id.colorOrange);
        cardPurple = findViewById(R.id.colorPurple);
        cardRed = findViewById(R.id.colorRed);
        cardTeal = findViewById(R.id.colorTeal);
        cardGold = findViewById(R.id.colorGold);

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
        if (cardBlue != null) cardBlue.setOnClickListener(v -> selectAccent("#007AFF"));
        if (cardPink != null) cardPink.setOnClickListener(v -> selectAccent("#FF4081"));
        if (cardGreen != null) cardGreen.setOnClickListener(v -> selectAccent("#25D366"));
        if (cardOrange != null) cardOrange.setOnClickListener(v -> selectAccent("#FF9500"));
        if (cardPurple != null) cardPurple.setOnClickListener(v -> selectAccent("#9C27B0"));
        if (cardRed != null) cardRed.setOnClickListener(v -> selectAccent("#E91E63"));
        if (cardTeal != null) cardTeal.setOnClickListener(v -> selectAccent("#009688"));
        if (cardGold != null) cardGold.setOnClickListener(v -> selectAccent("#FFC107"));
    }

    @Override
    protected void onResume() {
        super.onResume();
        ThemeUtils.applyAccentToActivity(this);
        updateSelectedAccentUI(ThemeUtils.getAccentColorHex(this));
    }

    private void selectAccent(String hexColor) {
        ThemeUtils.saveAccentColor(this, hexColor);
        updateSelectedAccentUI(hexColor);
        ThemeUtils.applyAccentToActivity(this);
        Toast.makeText(this, "Accent color updated!", Toast.LENGTH_SHORT).show();
    }

    private void updateSelectedAccentUI(String currentHex) {
        updateSwatch(cardBlue, checkBlue, "#007AFF".equalsIgnoreCase(currentHex), Color.WHITE);
        updateSwatch(cardPink, checkPink, "#FF4081".equalsIgnoreCase(currentHex), Color.WHITE);
        updateSwatch(cardGreen, checkGreen, "#25D366".equalsIgnoreCase(currentHex), Color.WHITE);
        updateSwatch(cardOrange, checkOrange, "#FF9500".equalsIgnoreCase(currentHex), Color.WHITE);
        updateSwatch(cardPurple, checkPurple, "#9C27B0".equalsIgnoreCase(currentHex), Color.WHITE);
        updateSwatch(cardRed, checkRed, "#E91E63".equalsIgnoreCase(currentHex), Color.WHITE);
        updateSwatch(cardTeal, checkTeal, "#009688".equalsIgnoreCase(currentHex), Color.WHITE);
        updateSwatch(cardGold, checkGold, "#FFC107".equalsIgnoreCase(currentHex), Color.BLACK);
    }

    private void updateSwatch(MaterialCardView card, ImageView check, boolean isSelected, int checkTint) {
        if (card != null) {
            if (isSelected) {
                boolean isNightMode = (getResources().getConfiguration().uiMode & android.content.res.Configuration.UI_MODE_NIGHT_MASK) == android.content.res.Configuration.UI_MODE_NIGHT_YES;
                card.setStrokeColor(isNightMode ? Color.WHITE : Color.BLACK);
                card.setStrokeWidth((int) (3 * getResources().getDisplayMetrics().density));
            } else {
                card.setStrokeWidth(0);
            }
        }
        if (check != null) {
            check.setImageTintList(android.content.res.ColorStateList.valueOf(checkTint));
            check.setVisibility(isSelected ? View.VISIBLE : View.GONE);
        }
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
