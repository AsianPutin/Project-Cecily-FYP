package com.app.projectcecily;

import android.content.ComponentName;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;

public class IconManager {

    public static final String KEY_ICON_LOGO = "icon_logo_style";
    public static final String KEY_ICON_BG = "icon_bg_style";

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

    public static int getNotificationSmallIcon() {
        return R.mipmap.ic_launcher;
    }

    public static android.graphics.Bitmap getNotificationLargeIcon(Context context) {
        if (context == null) return null;
        try {
            String logoStyle = getIconLogoStyle(context);
            int bgColor = getIconBackgroundColor(context);

            int size = 192;
            android.graphics.Bitmap bitmap = android.graphics.Bitmap.createBitmap(size, size, android.graphics.Bitmap.Config.ARGB_8888);
            android.graphics.Canvas canvas = new android.graphics.Canvas(bitmap);

            android.graphics.Paint bgPaint = new android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG);
            bgPaint.setColor(bgColor);
            canvas.drawRoundRect(0, 0, size, size, size * 0.22f, size * 0.22f, bgPaint);

            int logoRes = R.drawable.ic_app_logo;
            if ("WHITE".equalsIgnoreCase(logoStyle)) {
                logoRes = R.drawable.ic_app_logo_mono_white;
            } else if ("BLACK".equalsIgnoreCase(logoStyle) || "MONOCHROME".equalsIgnoreCase(logoStyle)) {
                logoRes = R.drawable.ic_app_logo_mono_black;
            }

            android.graphics.drawable.Drawable drawable = context.getDrawable(logoRes);
            if (drawable != null) {
                int padding = (int) (size * 0.12f);
                drawable.setBounds(padding, padding, size - padding, size - padding);
                drawable.draw(canvas);
            }
            return bitmap;
        } catch (Exception e) {
            return null;
        }
    }

    public static void applyLauncherComponent(Context context, String logoStyle) {
        if (context == null) return;
        try {
            PackageManager pm = context.getPackageManager();
            ComponentName mainComp = new ComponentName(context, "com.app.projectcecily.MainActivity");
            ComponentName monoComp = new ComponentName(context, "com.app.projectcecily.MainActivityMonochrome");

            if ("BLACK".equalsIgnoreCase(logoStyle) || "WHITE".equalsIgnoreCase(logoStyle) || "MONOCHROME".equalsIgnoreCase(logoStyle)) {
                pm.setComponentEnabledSetting(monoComp, PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP);
                pm.setComponentEnabledSetting(mainComp, PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP);
            } else {
                pm.setComponentEnabledSetting(mainComp, PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP);
                pm.setComponentEnabledSetting(monoComp, PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP);
            }
        } catch (Exception ignored) {}
    }
}
