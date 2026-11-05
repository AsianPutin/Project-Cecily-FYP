package com.app.projectcecily;

import android.content.Context;
import java.util.Locale;

public class UnitUtils {

    public static final String UNIT_METRIC = "metric";
    public static final String UNIT_IMPERIAL = "imperial";

    public static final String TEMP_CELSIUS = "celsius";
    public static final String TEMP_FAHRENHEIT = "fahrenheit";

    public static String getUnitSystem(Context context) {
        return context.getSharedPreferences("prefs", Context.MODE_PRIVATE)
                .getString("unit_system", UNIT_METRIC);
    }

    public static void setUnitSystem(Context context, String unitSystem) {
        context.getSharedPreferences("prefs", Context.MODE_PRIVATE)
                .edit().putString("unit_system", unitSystem).apply();
    }

    public static String getTemperatureUnit(Context context) {
        return context.getSharedPreferences("prefs", Context.MODE_PRIVATE)
                .getString("temp_unit", TEMP_CELSIUS);
    }

    public static void setTemperatureUnit(Context context, String tempUnit) {
        context.getSharedPreferences("prefs", Context.MODE_PRIVATE)
                .edit().putString("temp_unit", tempUnit).apply();
    }

    public static String formatTemperature(Context context, float tempInCelsius) {
        String unit = getTemperatureUnit(context);
        if (TEMP_FAHRENHEIT.equals(unit)) {
            double fahrenheit = (tempInCelsius * 9.0 / 5.0) + 32.0;
            return String.format(Locale.ENGLISH, "%.0f°F", fahrenheit);
        } else {
            return String.format(Locale.ENGLISH, "%.0f°C", tempInCelsius);
        }
    }

    public static String formatDistance(Context context, float distanceInKm) {
        if (distanceInKm < 0) return "Calculating...";

        String unit = getUnitSystem(context);

        if (UNIT_IMPERIAL.equals(unit)) {
            double miles = distanceInKm * 0.621371;
            if (miles >= 0.1) {
                return String.format(Locale.ENGLISH, "%.2f mi", miles);
            } else {
                double feet = distanceInKm * 3280.84;
                return String.format(Locale.ENGLISH, "%.0f ft", feet);
            }
        } else {
            if (distanceInKm >= 1.0) {
                return String.format(Locale.ENGLISH, "%.2f km", distanceInKm);
            } else {
                double meters = distanceInKm * 1000;
                return String.format(Locale.ENGLISH, "%.0f m", meters);
            }
        }
    }
}