package com.app.projectcecily;

import android.app.Activity;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatDelegate;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

public class ThemeUtils {

    private static final String PREFS = "prefs";
    private static final String KEY_THEME_MODE = "theme_mode";
    private static final String KEY_ACCENT_COLOR = "accent_color";

    public static final String DEFAULT_ACCENT = "#007AFF"; // Classic Blue

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
    }

    public static int getSavedThemeMode(Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .getInt(KEY_THEME_MODE, AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
    }

    public static int getAccentColor(Context context) {
        String hex = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .getString(KEY_ACCENT_COLOR, DEFAULT_ACCENT);
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
    }

    public static int getContrastingTextColor(int color) {
        double luminance = (0.299 * Color.red(color) + 0.587 * Color.green(color) + 0.114 * Color.blue(color)) / 255.0;
        return luminance > 0.5 ? Color.BLACK : Color.WHITE;
    }

    public static void applyAccentToActivity(Activity activity) {
        if (activity == null) return;
        int accentColor = getAccentColor(activity);
        int textColorOnAccent = getContrastingTextColor(accentColor);

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
                MaterialButton mb = (MaterialButton) v;
                // Exclude toggle group child buttons from full background override
                if (mb.getParent() instanceof com.google.android.material.button.MaterialButtonToggleGroup) {
                    continue;
                }
                mb.setBackgroundTintList(ColorStateList.valueOf(accentColor));
                mb.setTextColor(textColorOnAccent);
            } else if (v instanceof MaterialCardView) {
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
