package com.app.projectcecily;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.widget.RemoteViews;
import androidx.annotation.NonNull;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import java.util.List;
import java.util.Locale;

public class LdrStatusWidgetProvider extends AppWidgetProvider {

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        for (int appWidgetId : appWidgetIds) {
            updateWidget(context, appWidgetManager, appWidgetId);
        }
    }

    static void updateWidget(Context context, AppWidgetManager appWidgetManager, int appWidgetId) {
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_ldr_status);

        // Click widget to launch MainActivity
        Intent intent = new Intent(context, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        views.setOnClickPendingIntent(R.id.widget_ldr_root, pendingIntent);

        String userId = UserManager.getUserId(context);
        String partnerId = UserManager.getPartnerId(context);

        if (userId.isEmpty() || partnerId.isEmpty()) {
            views.setTextViewText(R.id.tvWidgetHeader, "Status");
            views.setTextViewText(R.id.tvWidgetDistanceText, "Not Paired");
            views.setTextViewText(R.id.tvWidgetPartnerTimeText, "Pair in Account Settings");
            views.setTextViewText(R.id.tvWidgetPartnerLocationText, "");
            appWidgetManager.updateAppWidget(appWidgetId, views);
            return;
        }

        DatabaseReference dbRef = FirebaseDatabase.getInstance("https://i-must-finish-this-in-2026-default-rtdb.asia-southeast1.firebasedatabase.app/").getReference();

        // Fetch partner profile info (name, timeZone, accentColor)
        dbRef.child("profiles").child(partnerId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot partnerProfileSnap) {
                String partnerName = partnerProfileSnap.child("name").getValue(String.class);
                String pName = (partnerName != null && !partnerName.trim().isEmpty()) ? partnerName.trim() : "Partner";

                String partnerTz = partnerProfileSnap.child("timeZone").getValue(String.class);
                String partnerAccent = partnerProfileSnap.child("accentColor").getValue(String.class);

                // Format Partner Time
                CharSequence formattedTime = RelationshipManager.formatPartnerCurrentTime(context, pName, partnerTz, partnerAccent);
                views.setTextViewText(R.id.tvWidgetPartnerTimeText, formattedTime.toString());

                // Fetch User & Partner Locations
                dbRef.child("locations").addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot locationsSnapshot) {
                        DataSnapshot myLocSnap = locationsSnapshot.child(userId);
                        DataSnapshot pLocSnap = locationsSnapshot.child(partnerId);

                        Double myLat = myLocSnap.child("latitude").getValue(Double.class);
                        Double myLng = myLocSnap.child("longitude").getValue(Double.class);

                        Double pLat = pLocSnap.child("latitude").getValue(Double.class);
                        Double pLng = pLocSnap.child("longitude").getValue(Double.class);

                        if (myLat != null && myLng != null && pLat != null && pLng != null) {
                            float[] results = new float[1];
                            Location.distanceBetween(myLat, myLng, pLat, pLng, results);
                            double meters = results[0];
                            double km = meters / 1000.0;

                            if (meters < 100) {
                                // Within 100 meters: Display Together 💕
                                views.setTextViewText(R.id.tvWidgetHeader, "Status");
                                views.setTextViewText(R.id.tvWidgetDistanceText, "Together 💕");
                            } else {
                                views.setTextViewText(R.id.tvWidgetHeader, "Distance Apart");
                                String formattedDistance = UnitUtils.formatDistance(context, (float) km);
                                views.setTextViewText(R.id.tvWidgetDistanceText, formattedDistance);
                            }

                            // Geocode Partner Location City
                            try {
                                Geocoder geocoder = new Geocoder(context, Locale.ENGLISH);
                                List<Address> addresses = geocoder.getFromLocation(pLat, pLng, 1);
                                if (addresses != null && !addresses.isEmpty()) {
                                    Address addr = addresses.get(0);
                                    String city = addr.getLocality();
                                    if (city == null) city = addr.getSubAdminArea();
                                    if (city == null) city = addr.getAdminArea();
                                    String country = addr.getCountryName();

                                    String fullCity = (city != null ? city : "") + (country != null ? ", " + country : "");
                                    if (!fullCity.isEmpty()) {
                                        views.setTextViewText(R.id.tvWidgetPartnerLocationText, fullCity);
                                    } else {
                                        views.setTextViewText(R.id.tvWidgetPartnerLocationText, "Location active");
                                    }
                                } else {
                                    views.setTextViewText(R.id.tvWidgetPartnerLocationText, "Location active");
                                }
                            } catch (Exception e) {
                                views.setTextViewText(R.id.tvWidgetPartnerLocationText, "Location active");
                            }
                        }

                        appWidgetManager.updateAppWidget(appWidgetId, views);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {}
                });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });

        appWidgetManager.updateAppWidget(appWidgetId, views);
    }
}
