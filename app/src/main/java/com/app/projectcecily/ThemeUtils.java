package com.app.projectcecily;

import android.content.Context;
import android.graphics.Color;
import androidx.appcompat.app.AppCompatDelegate;

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
}
