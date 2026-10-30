package com.app.projectcecily;

import android.content.Context;
import android.location.Address;
import android.location.Geocoder;
import java.util.List;
import java.util.Locale;

public class CityUtils {

    public interface CityCallback {
        void onCityResolved(String cityName);
    }

    public static void resolveCity(Context context, double lat, double lon, CityCallback callback) {
        if (lat == 0 && lon == 0) {
            callback.onCityResolved("Unknown City");
            return;
        }

        new Thread(() -> {
            String city = "Unknown City";
            try {
                if (Geocoder.isPresent()) {
                    Geocoder geocoder = new Geocoder(context, Locale.getDefault());
                    List<Address> addresses = geocoder.getFromLocation(lat, lon, 1);
                    if (addresses != null && !addresses.isEmpty()) {
                        Address addr = addresses.get(0);
                        if (addr.getLocality() != null && !addr.getLocality().trim().isEmpty()) {
                            city = addr.getLocality().trim();
                        } else if (addr.getSubAdminArea() != null && !addr.getSubAdminArea().trim().isEmpty()) {
                            city = addr.getSubAdminArea().trim();
                        } else if (addr.getAdminArea() != null && !addr.getAdminArea().trim().isEmpty()) {
                            city = addr.getAdminArea().trim();
                        }
                    }
                }
            } catch (Exception ignored) {}

            final String finalCity = city;
            if (context instanceof android.app.Activity) {
                ((android.app.Activity) context).runOnUiThread(() -> callback.onCityResolved(finalCity));
            } else {
                callback.onCityResolved(finalCity);
            }
        }).start();
    }

    public static boolean isSameCity(String city1, String city2) {
        if (city1 == null || city2 == null) return false;
        String c1 = city1.trim().toLowerCase();
        String c2 = city2.trim().toLowerCase();
        if (c1.isEmpty() || c2.isEmpty() || c1.equals("unknown city") || c2.equals("unknown city")) {
            return false;
        }
        return c1.equals(c2);
    }
}