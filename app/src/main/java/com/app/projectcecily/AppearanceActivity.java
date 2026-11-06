package com.app.projectcecily;

import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;

public class AppearanceActivity extends AppCompatActivity {

    private MaterialButtonToggleGroup themeToggleGroup, toggleColorStyle, toggleDistanceUnit, toggleTemperatureUnit;
    private LinearLayout layoutAccentGrid;

    private MaterialCardView cardNone, cardRed, cardOrange, cardYellow, cardLime, cardGreen;
    private MaterialCardView cardDarkGreen, cardMint, cardTeal, cardCyan, cardBlue, cardIndigo;
    private MaterialCardView cardDeepPurple, cardPurple, cardMagenta, cardBrown, cardSlate, cardCustomPicker;

    private ImageView checkRed, checkOrange, checkYellow, checkLime, checkGreen;
    private ImageView checkDarkGreen, checkMint, checkTeal, checkCyan, checkBlue, checkIndigo;
    private ImageView checkDeepPurple, checkPurple, checkMagenta, checkBrown, checkSlate;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeUtils.applySavedThemeMode(this);
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
        toggleColorStyle = findViewById(R.id.toggleColorStyle);
        toggleDistanceUnit = findViewById(R.id.toggleDistanceUnit);
        toggleTemperatureUnit = findViewById(R.id.toggleTemperatureUnit);
        layoutAccentGrid = findViewById(R.id.layoutAccentGrid);

        cardNone = findViewById(R.id.colorNone);
        cardRed = findViewById(R.id.colorRed);
        cardOrange = findViewById(R.id.colorOrange);
        cardYellow = findViewById(R.id.colorYellow);
        cardLime = findViewById(R.id.colorLime);
        cardGreen = findViewById(R.id.colorGreen);

        cardDarkGreen = findViewById(R.id.colorDarkGreen);
        cardMint = findViewById(R.id.colorMint);
        cardTeal = findViewById(R.id.colorTeal);
        cardCyan = findViewById(R.id.colorCyan);
        cardBlue = findViewById(R.id.colorBlue);
        cardIndigo = findViewById(R.id.colorIndigo);

        cardDeepPurple = findViewById(R.id.colorDeepPurple);
        cardPurple = findViewById(R.id.colorPurple);
        cardMagenta = findViewById(R.id.colorMagenta);
        cardBrown = findViewById(R.id.colorBrown);
        cardSlate = findViewById(R.id.colorSlate);
        cardCustomPicker = findViewById(R.id.colorCustomPicker);

        checkRed = findViewById(R.id.checkRed);
        checkOrange = findViewById(R.id.checkOrange);
        checkYellow = findViewById(R.id.checkYellow);
        checkLime = findViewById(R.id.checkLime);
        checkGreen = findViewById(R.id.checkGreen);

        checkDarkGreen = findViewById(R.id.checkDarkGreen);
        checkMint = findViewById(R.id.checkMint);
        checkTeal = findViewById(R.id.checkTeal);
        checkCyan = findViewById(R.id.checkCyan);
        checkBlue = findViewById(R.id.checkBlue);
        checkIndigo = findViewById(R.id.checkIndigo);

        checkDeepPurple = findViewById(R.id.checkDeepPurple);
        checkPurple = findViewById(R.id.checkPurple);
        checkMagenta = findViewById(R.id.checkMagenta);
        checkBrown = findViewById(R.id.checkBrown);
        checkSlate = findViewById(R.id.checkSlate);

        setupThemeToggle();
        setupColorStyleToggle();
        setupColorSwatches();
        setupUnitToggles();
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

    private void setupColorStyleToggle() {
        String currentHex = ThemeUtils.getAccentColorHex(this);
        boolean isDynamic = "DYNAMIC".equalsIgnoreCase(currentHex);
        boolean isProfilePic = "PROFILE_PIC".equalsIgnoreCase(currentHex) || "AVATAR".equalsIgnoreCase(currentHex);

        if (isDynamic) {
            toggleColorStyle.check(R.id.btnStyleMaterialYou);
            setGridEnabled(false);
        } else if (isProfilePic) {
            toggleColorStyle.check(R.id.btnStyleProfilePic);
            setGridEnabled(false);
        } else {
            toggleColorStyle.check(R.id.btnStyleDefault);
            setGridEnabled(true);
        }

        toggleColorStyle.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (!isChecked) return;
            if (checkedId == R.id.btnStyleMaterialYou) {
                ThemeUtils.saveAccentColor(this, "DYNAMIC");
                setGridEnabled(false);
                clearAllCheckmarks();
                ThemeUtils.applyAccentToActivity(this);
            } else if (checkedId == R.id.btnStyleProfilePic) {
                ThemeUtils.saveAccentColor(this, "PROFILE_PIC");
                setGridEnabled(false);
                clearAllCheckmarks();
                ThemeUtils.applyAccentToActivity(this);
            } else {
                ThemeUtils.saveAccentColor(this, "#25D366");
                setGridEnabled(true);
                updateSelectedAccentUI("#25D366");
                ThemeUtils.applyAccentToActivity(this);
            }
        });
    }

    private void setupUnitToggles() {
        View cardUnits = findViewById(R.id.cardUnitsMeasurement);
        boolean isLoggedIn = UserManager.isLoggedIn(this);
        if (cardUnits != null) {
            cardUnits.setVisibility(isLoggedIn ? View.VISIBLE : View.GONE);
        }

        if (toggleDistanceUnit != null) {
            String currentDistUnit = UnitUtils.getUnitSystem(this);
            if (UnitUtils.UNIT_IMPERIAL.equals(currentDistUnit)) {
                toggleDistanceUnit.check(R.id.btnUnitImperial);
            } else {
                toggleDistanceUnit.check(R.id.btnUnitMetric);
            }

            toggleDistanceUnit.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
                if (!isChecked) return;
                if (checkedId == R.id.btnUnitImperial) {
                    UnitUtils.setUnitSystem(this, UnitUtils.UNIT_IMPERIAL);
                } else {
                    UnitUtils.setUnitSystem(this, UnitUtils.UNIT_METRIC);
                }
                ThemeUtils.notifyWidgets(this);
            });
        }

        if (toggleTemperatureUnit != null) {
            String currentTempUnit = UnitUtils.getTemperatureUnit(this);
            if (UnitUtils.TEMP_FAHRENHEIT.equals(currentTempUnit)) {
                toggleTemperatureUnit.check(R.id.btnTempFahrenheit);
            } else {
                toggleTemperatureUnit.check(R.id.btnTempCelsius);
            }

            toggleTemperatureUnit.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
                if (!isChecked) return;
                if (checkedId == R.id.btnTempFahrenheit) {
                    UnitUtils.setTemperatureUnit(this, UnitUtils.TEMP_FAHRENHEIT);
                } else {
                    UnitUtils.setTemperatureUnit(this, UnitUtils.TEMP_CELSIUS);
                }
                ThemeUtils.notifyWidgets(this);
            });
        }
    }

    private void setGridEnabled(boolean enabled) {
        if (layoutAccentGrid != null) {
            layoutAccentGrid.setAlpha(enabled ? 1.0f : 0.4f);
            setViewGroupEnabled(layoutAccentGrid, enabled);
        }
    }

    private void setViewGroupEnabled(View view, boolean enabled) {
        view.setEnabled(enabled);
        if (view instanceof android.view.ViewGroup) {
            android.view.ViewGroup group = (android.view.ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                setViewGroupEnabled(group.getChildAt(i), enabled);
            }
        }
    }

    private void setupColorSwatches() {
        if (cardNone != null) cardNone.setOnClickListener(v -> selectAccent("#25D366"));
        if (cardRed != null) cardRed.setOnClickListener(v -> selectAccent("#E53935"));
        if (cardOrange != null) cardOrange.setOnClickListener(v -> selectAccent("#FB8C00"));
        if (cardYellow != null) cardYellow.setOnClickListener(v -> selectAccent("#FDD835"));
        if (cardLime != null) cardLime.setOnClickListener(v -> selectAccent("#C0CA33"));
        if (cardGreen != null) cardGreen.setOnClickListener(v -> selectAccent("#4CAF50"));

        if (cardDarkGreen != null) cardDarkGreen.setOnClickListener(v -> selectAccent("#2E7D32"));
        if (cardMint != null) cardMint.setOnClickListener(v -> selectAccent("#00E676"));
        if (cardTeal != null) cardTeal.setOnClickListener(v -> selectAccent("#00897B"));
        if (cardCyan != null) cardCyan.setOnClickListener(v -> selectAccent("#00ACC1"));
        if (cardBlue != null) cardBlue.setOnClickListener(v -> selectAccent("#1E88E5"));
        if (cardIndigo != null) cardIndigo.setOnClickListener(v -> selectAccent("#3F51B5"));

        if (cardDeepPurple != null) cardDeepPurple.setOnClickListener(v -> selectAccent("#673AB7"));
        if (cardPurple != null) cardPurple.setOnClickListener(v -> selectAccent("#9C27B0"));
        if (cardMagenta != null) cardMagenta.setOnClickListener(v -> selectAccent("#E91E63"));
        if (cardBrown != null) cardBrown.setOnClickListener(v -> selectAccent("#795548"));
        if (cardSlate != null) cardSlate.setOnClickListener(v -> selectAccent("#546E7A"));

        if (cardCustomPicker != null) cardCustomPicker.setOnClickListener(v -> showCustomColorPickerDialog());
    }

    private void showCustomColorPickerDialog() {
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_color_picker, null);
        View previewCircle = view.findViewById(R.id.previewColorCircle);
        TextInputEditText etHexCode = view.findViewById(R.id.etHexCode);
        ColorPickerView colorPickerView = view.findViewById(R.id.colorPickerView);
        SeekBar hueSeekBar = view.findViewById(R.id.hueSeekBar);
        Button btnCancel = view.findViewById(R.id.btnCancelColor);
        Button btnSave = view.findViewById(R.id.btnSaveColor);

        // Rainbow Gradient Track for Hue SeekBar
        int[] rainbowColors = new int[]{
            Color.RED, Color.YELLOW, Color.GREEN, Color.CYAN, Color.BLUE, Color.MAGENTA, Color.RED
        };
        GradientDrawable rainbowDrawable = new GradientDrawable(
            GradientDrawable.Orientation.LEFT_RIGHT, rainbowColors
        );
        rainbowDrawable.setCornerRadius(16f);
        hueSeekBar.setProgressDrawable(rainbowDrawable);
        hueSeekBar.setSplitTrack(false);

        int currentColor = ThemeUtils.getAccentColor(this);
        String currentHex = String.format("#%06X", (0xFFFFFF & currentColor));

        etHexCode.setText(currentHex);
        previewCircle.getBackground().setTint(currentColor);
        colorPickerView.setColor(currentColor);

        float[] initialHsv = new float[3];
        Color.colorToHSV(currentColor, initialHsv);
        hueSeekBar.setProgress((int) initialHsv[0]);

        colorPickerView.setOnColorChangeListener((color, hue, sat, val) -> {
            String hex = String.format("#%06X", (0xFFFFFF & color));
            etHexCode.setText(hex);
            previewCircle.getBackground().setTint(color);
        });

        hueSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    colorPickerView.setHue(progress);
                }
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                .setView(view)
                .create();

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnSave.setOnClickListener(v -> {
            String typedHex = etHexCode.getText() != null ? etHexCode.getText().toString().trim() : "";
            try {
                Color.parseColor(typedHex);
                selectAccent(typedHex);
                dialog.dismiss();
            } catch (Exception e) {
                Toast.makeText(this, "Invalid HEX color format!", Toast.LENGTH_SHORT).show();
            }
        });

        dialog.show();
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
    }

    private void updateSelectedAccentUI(String currentHex) {
        boolean isDynamic = "DYNAMIC".equalsIgnoreCase(currentHex);
        boolean isProfilePic = "PROFILE_PIC".equalsIgnoreCase(currentHex) || "AVATAR".equalsIgnoreCase(currentHex);
        if (isDynamic || isProfilePic) {
            clearAllCheckmarks();
            return;
        }

        updateSwatch(cardRed, checkRed, "#E53935".equalsIgnoreCase(currentHex), Color.WHITE);
        updateSwatch(cardOrange, checkOrange, "#FB8C00".equalsIgnoreCase(currentHex), Color.WHITE);
        updateSwatch(cardYellow, checkYellow, "#FDD835".equalsIgnoreCase(currentHex), Color.BLACK);
        updateSwatch(cardLime, checkLime, "#C0CA33".equalsIgnoreCase(currentHex), Color.BLACK);
        updateSwatch(cardGreen, checkGreen, "#4CAF50".equalsIgnoreCase(currentHex), Color.WHITE);

        updateSwatch(cardDarkGreen, checkDarkGreen, "#2E7D32".equalsIgnoreCase(currentHex), Color.WHITE);
        updateSwatch(cardMint, checkMint, "#00E676".equalsIgnoreCase(currentHex), Color.BLACK);
        updateSwatch(cardTeal, checkTeal, "#00897B".equalsIgnoreCase(currentHex), Color.WHITE);
        updateSwatch(cardCyan, checkCyan, "#00ACC1".equalsIgnoreCase(currentHex), Color.WHITE);
        updateSwatch(cardBlue, checkBlue, "#1E88E5".equalsIgnoreCase(currentHex), Color.WHITE);
        updateSwatch(cardIndigo, checkIndigo, "#3F51B5".equalsIgnoreCase(currentHex), Color.WHITE);

        updateSwatch(cardDeepPurple, checkDeepPurple, "#673AB7".equalsIgnoreCase(currentHex), Color.WHITE);
        updateSwatch(cardPurple, checkPurple, "#9C27B0".equalsIgnoreCase(currentHex), Color.WHITE);
        updateSwatch(cardMagenta, checkMagenta, "#E91E63".equalsIgnoreCase(currentHex), Color.WHITE);
        updateSwatch(cardBrown, checkBrown, "#795548".equalsIgnoreCase(currentHex), Color.WHITE);
        updateSwatch(cardSlate, checkSlate, "#546E7A".equalsIgnoreCase(currentHex), Color.WHITE);
    }

    private void clearAllCheckmarks() {
        ImageView[] checks = {checkRed, checkOrange, checkYellow, checkLime, checkGreen,
                              checkDarkGreen, checkMint, checkTeal, checkCyan, checkBlue, checkIndigo,
                              checkDeepPurple, checkPurple, checkMagenta, checkBrown, checkSlate};
        MaterialCardView[] cards = {cardNone, cardRed, cardOrange, cardYellow, cardLime, cardGreen,
                                   cardDarkGreen, cardMint, cardTeal, cardCyan, cardBlue, cardIndigo,
                                   cardDeepPurple, cardPurple, cardMagenta, cardBrown, cardSlate, cardCustomPicker};
        for (ImageView chk : checks) if (chk != null) chk.setVisibility(View.GONE);
        for (MaterialCardView cd : cards) if (cd != null) cd.setStrokeWidth(0);
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
