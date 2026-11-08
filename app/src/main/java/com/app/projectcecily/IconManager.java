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
        if (context == null) return "LIGHT";
        SharedPreferences prefs = context.getSharedPreferences(getPrefsName(context), Context.MODE_PRIVATE);
        return prefs.getString(KEY_ICON_BG, "LIGHT");
    }

    public static void setIconLogoStyle(Context context, String style) {
        if (context == null) return;
        SharedPreferences prefs = context.getSharedPreferences(getPrefsName(context), Context.MODE_PRIVATE);
        prefs.edit().putString(KEY_ICON_LOGO, style).apply();
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
        if (context == null) return Color.WHITE;
        String bgStyle = getIconBgStyle(context);
        if ("DARK".equalsIgnoreCase(bgStyle)) {
            return Color.parseColor("#121212");
        }
        return Color.WHITE;
    }

    public static void applySavedIconComponent(Context context) {
        if (context == null) return;
        String logoStyle = getIconLogoStyle(context);
        Context appContext = context.getApplicationContext();

        new Thread(() -> {
            try {
                PackageManager pm = appContext.getPackageManager();
                ComponentName mainComp = new ComponentName(appContext, "com.app.projectcecily.MainActivity");
                ComponentName monoComp = new ComponentName(appContext, "com.app.projectcecily.MainActivityMonochrome");

                if ("BLACK".equalsIgnoreCase(logoStyle) || "WHITE".equalsIgnoreCase(logoStyle) || "MONOCHROME".equalsIgnoreCase(logoStyle)) {
                    if (pm.getComponentEnabledSetting(monoComp) != PackageManager.COMPONENT_ENABLED_STATE_ENABLED) {
                        pm.setComponentEnabledSetting(monoComp, PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP);
                        pm.setComponentEnabledSetting(mainComp, PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP);
                    }
                } else {
                    if (pm.getComponentEnabledSetting(mainComp) != PackageManager.COMPONENT_ENABLED_STATE_ENABLED) {
                        pm.setComponentEnabledSetting(mainComp, PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP);
                        pm.setComponentEnabledSetting(monoComp, PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP);
                    }
                }
            } catch (Exception ignored) {}
        }).start();
    }
}
