package com.app.projectcecily;

import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.database.FirebaseDatabase;

public class ThemeUtils {

    private static final String PREFS = "prefs";
    private static final String KEY_THEME_MODE = "theme_mode";
    private static final String KEY_ACCENT_COLOR = "accent_color";

    public static final String DEFAULT_ACCENT = "#25D366"; // Green

    public static void applySavedThemeMode(Context context) {
        int savedMode = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .getInt(KEY_THEME_MODE, AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
        AppCompatDelegate.setDefaultNightMode(savedMode);
    }

    public static void saveThemeMode(Context context, int mode) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit()
                .putInt(KEY_THEME_MODE, mode)
                .apply();
        AppCompatDelegate.setDefaultNightMode(mode);
        notifyWidgets(context);
    }

    public static int getSavedThemeMode(Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .getInt(KEY_THEME_MODE, AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
    }

    public static int getAccentColor(Context context) {
        String hex = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .getString(KEY_ACCENT_COLOR, DEFAULT_ACCENT);

        if ("DYNAMIC".equalsIgnoreCase(hex)) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                try {
                    return context.getColor(android.R.color.system_accent1_500);
                } catch (Exception ignored) {}
            }
            return Color.parseColor(DEFAULT_ACCENT);
        }

        try {
            return Color.parseColor(hex);
        } catch (Exception e) {
            return Color.parseColor(DEFAULT_ACCENT);
        }
    }

    public static String getAccentColorHex(Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .getString(KEY_ACCENT_COLOR, DEFAULT_ACCENT);
    }

    public static void saveAccentColor(Context context, String hexColor) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_ACCENT_COLOR, hexColor)
                .apply();

        // Broadcast accent update to widgets instantly!
        notifyWidgets(context);

        // Sync to Firebase for real-time partner map highlights
        String userId = UserManager.getUserId(context);
        if (userId != null && !userId.isEmpty()) {
            try {
                FirebaseDatabase.getInstance("https://i-must-finish-this-in-2026-default-rtdb.asia-southeast1.firebasedatabase.app/")
                        .getReference("profiles")
                        .child(userId)
                        .child("accentColor")
                        .setValue(hexColor);
            } catch (Exception ignored) {}
        }
    }

    public static void notifyWidgets(Context context) {
        if (context == null) return;
        try {
            AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);

            // 1. Notify Quick Poke Widget
            ComponentName pokeComponent = new ComponentName(context, PokeWidgetProvider.class);
            int[] pokeIds = appWidgetManager.getAppWidgetIds(pokeComponent);
            if (pokeIds != null && pokeIds.length > 0) {
                Intent pokeIntent = new Intent(context, PokeWidgetProvider.class);
                pokeIntent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
                pokeIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, pokeIds);
                context.sendBroadcast(pokeIntent);
            }

            // 2. Notify LDR Status Widget
            ComponentName ldrComponent = new ComponentName(context, LdrStatusWidgetProvider.class);
            int[] ldrIds = appWidgetManager.getAppWidgetIds(ldrComponent);
            if (ldrIds != null && ldrIds.length > 0) {
                Intent ldrIntent = new Intent(context, LdrStatusWidgetProvider.class);
                ldrIntent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
                ldrIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ldrIds);
                context.sendBroadcast(ldrIntent);
            }
        } catch (Exception ignored) {}
    }

    public static int getContrastingTextColor(int color) {
        double luminance = (0.299 * Color.red(color) + 0.587 * Color.green(color) + 0.114 * Color.blue(color)) / 255.0;
        return luminance > 0.5 ? Color.BLACK : Color.WHITE;
    }

    public static int getActionBarColor(int accentColor, boolean isNightMode) {
        float[] hsv = new float[3];
        Color.colorToHSV(accentColor, hsv);
        if (isNightMode) {
            // Dark Mode: Dark container shade
            hsv[1] = Math.min(hsv[1], 0.35f);
            hsv[2] = 0.18f;
        } else {
            // Light Mode: Soft pastel shade
            hsv[1] = Math.min(hsv[1], 0.18f);
            hsv[2] = 0.96f;
        }
        return Color.HSVToColor(hsv);
    }

    public static void applyAccentToActivity(Activity activity) {
        if (activity == null) return;
        int accentColor = getAccentColor(activity);
        int textColorOnAccent = getContrastingTextColor(accentColor);

        // Ensure user's accent color is synced to Firebase for partner's map view
        String userId = UserManager.getUserId(activity);
        String accentHex = getAccentColorHex(activity);
        if (userId != null && !userId.isEmpty()) {
            try {
                FirebaseDatabase.getInstance("https://i-must-finish-this-in-2026-default-rtdb.asia-southeast1.firebasedatabase.app/")
                        .getReference("profiles")
                        .child(userId)
                        .child("accentColor")
                        .setValue(accentHex);
            } catch (Exception ignored) {}
        }

        boolean isNightMode = (activity.getResources().getConfiguration().uiMode & android.content.res.Configuration.UI_MODE_NIGHT_MASK) == android.content.res.Configuration.UI_MODE_NIGHT_YES;
        int actionBarBgColor = getActionBarColor(accentColor, isNightMode);
        int actionBarTextColor = getContrastingTextColor(actionBarBgColor);

        View navView = activity.findViewById(R.id.bottom_navigation);
        if (navView instanceof com.google.android.material.bottomnavigation.BottomNavigationView) {
            com.google.android.material.bottomnavigation.BottomNavigationView bottomNav = (com.google.android.material.bottomnavigation.BottomNavigationView) navView;
            bottomNav.setItemActiveIndicatorEnabled(false);

            int unselectedColor = isNightMode ? Color.parseColor("#888888") : Color.parseColor("#666666");
            int[][] states = new int[][] {
                new int[] { android.R.attr.state_checked },
                new int[] { -android.R.attr.state_checked }
            };
            int[] colors = new int[] {
                accentColor,
                unselectedColor
            };
            ColorStateList csl = new ColorStateList(states, colors);
            bottomNav.setItemIconTintList(csl);
            bottomNav.setItemTextColor(csl);
        }

        View toolbarView = activity.findViewById(R.id.toolbar);
        if (toolbarView instanceof com.google.android.material.appbar.MaterialToolbar) {
            com.google.android.material.appbar.MaterialToolbar toolbar = (com.google.android.material.appbar.MaterialToolbar) toolbarView;
            toolbar.setBackgroundColor(actionBarBgColor);
            toolbar.setTitleTextColor(actionBarTextColor);
            if (toolbar.getNavigationIcon() != null) {
                toolbar.getNavigationIcon().setTint(actionBarTextColor);
            }
        }

        if (activity instanceof AppCompatActivity) {
            AppCompatActivity appCompatActivity = (AppCompatActivity) activity;
            if (appCompatActivity.getSupportActionBar() != null) {
                appCompatActivity.getSupportActionBar().setBackgroundDrawable(new ColorDrawable(actionBarBgColor));
                CharSequence title = appCompatActivity.getSupportActionBar().getTitle();
                if (title != null && title.length() > 0) {
                    SpannableString s = new SpannableString(title);
                    s.setSpan(new ForegroundColorSpan(actionBarTextColor), 0, s.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                    appCompatActivity.getSupportActionBar().setTitle(s);
                }
            }
        }

        View rootView = activity.findViewById(android.R.id.content);
        if (rootView instanceof ViewGroup) {
            applyAccentToViewGroup((ViewGroup) rootView, accentColor, textColorOnAccent);
        }
    }

    private static void applyAccentToViewGroup(ViewGroup group, int accentColor, int textColorOnAccent) {
        if (group == null) return;
        for (int i = 0; i < group.getChildCount(); i++) {
            View v = group.getChildAt(i);
            if (v instanceof FloatingActionButton) {
                FloatingActionButton fab = (FloatingActionButton) v;
                fab.setBackgroundTintList(ColorStateList.valueOf(accentColor));
                fab.setImageTintList(ColorStateList.valueOf(textColorOnAccent));
            } else if (v instanceof MaterialButton) {
                int id = v.getId();
                if (id == R.id.btnUnpairPartner || id == R.id.btnLogout ||
                    id == R.id.btnMarkBought || id == R.id.btnMarkReceived ||
                    id == R.id.btnDelete || id == R.id.btnDeleteFlight) {
                    continue; // Exclude Mark Bought, Received, and Delete from accent overrides!
                }
                MaterialButton mb = (MaterialButton) v;
                if (mb.getParent() instanceof com.google.android.material.button.MaterialButtonToggleGroup) {
                    continue;
                }
                mb.setBackgroundTintList(ColorStateList.valueOf(accentColor));
                mb.setTextColor(textColorOnAccent);
            } else if (v instanceof MaterialCardView) {
                int id = v.getId();
                if (id == R.id.cardAccentPalette || id == R.id.cardPartnerEdgeWidget) {
                    continue; // Exclude card Accent Palette container and partner's edge widget from stroke overrides!
                }
                MaterialCardView card = (MaterialCardView) v;
                if (card.getStrokeWidth() > 0) {
                    card.setStrokeColor(accentColor);
                }
                applyAccentToViewGroup(card, accentColor, textColorOnAccent);
            } else if (v instanceof TextView) {
                TextView tv = (TextView) v;
                int id = tv.getId();
                if (id == R.id.tvTogetherDays || 
                    id == R.id.tvLiveDistance || 
                    id == R.id.tvLiveDistanceText) {
                    tv.setTextColor(accentColor);
                }
            } else if (v instanceof ViewGroup) {
                applyAccentToViewGroup((ViewGroup) v, accentColor, textColorOnAccent);
            }
        }
    }
}
