package com.app.projectcecily;

import android.content.ComponentName;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;

public class IconManager {

    public static final String KEY_ICON_LOGO = "icon_logo_style"; // "DEFAULT" vs "MONOCHROME"
    public static final String KEY_ICON_BG = "icon_bg_style";     // "FOLLOW_THEME", "WHITE", "DARK"

    public static String getIconLogoStyle(Context context) {
        if (context == null) return "DEFAULT";
        SharedPreferences prefs = context.getSharedPreferences(getPrefsName(context), Context.MODE_PRIVATE);
        return prefs.getString(KEY_ICON_LOGO, "DEFAULT");
    }

    public static String getIconBgStyle(Context context) {
        if (context == null) return "FOLLOW_THEME";
        SharedPreferences prefs = context.getSharedPreferences(getPrefsName(context), Context.MODE_PRIVATE);
        return prefs.getString(KEY_ICON_BG, "FOLLOW_THEME");
    }

    public static void setIconLogoStyle(Context context, String style) {
        if (context == null) return;
        SharedPreferences prefs = context.getSharedPreferences(getPrefsName(context), Context.MODE_PRIVATE);
        prefs.edit().putString(KEY_ICON_LOGO, style).apply();
        applyLauncherComponent(context, style);
    }

    public static void setIconBgStyle(Context context, String bgStyle) {
        if (context == null) return;
        SharedPreferences prefs = context.getSharedPreferences(getPrefsName(context), Context.MODE_PRIVATE);
        prefs.edit().putString(KEY_ICON_BG, bgStyle).apply();
    }

    private static String getPrefsName(Context context) {
        String userId = UserManager.getUserId(context);
        return "prefs_" + (userId.isEmpty() ? "guest" : userId);
    }

    public static int getIconBackgroundColor(Context context) {
        String bgStyle = getIconBgStyle(context);
        if ("WHITE".equalsIgnoreCase(bgStyle)) {
            return Color.WHITE;
        } else if ("DARK".equalsIgnoreCase(bgStyle)) {
            return Color.parseColor("#121212");
        } else {
            // "FOLLOW_THEME" -> Follow active app accent color!
            return ThemeUtils.getAccentColor(context);
        }
    }

    public static void applyLauncherComponent(Context context, String logoStyle) {
        try {
            PackageManager pm = context.getPackageManager();
            ComponentName defaultComp = new ComponentName(context, "com.app.projectcecily.MainActivityDefault");
            ComponentName monoComp = new ComponentName(context, "com.app.projectcecily.MainActivityMonochrome");
            ComponentName mainComp = new ComponentName(context, "com.app.projectcecily.MainActivity");

            if ("MONOCHROME".equalsIgnoreCase(logoStyle)) {
                pm.setComponentEnabledSetting(monoComp, PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP);
                pm.setComponentEnabledSetting(defaultComp, PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP);
                pm.setComponentEnabledSetting(mainComp, PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP);
            } else {
                pm.setComponentEnabledSetting(defaultComp, PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP);
                pm.setComponentEnabledSetting(monoComp, PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP);
                pm.setComponentEnabledSetting(mainComp, PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP);
            }
        } catch (Exception ignored) {}
    }
}
