package com.app.projectcecily;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.widget.RemoteViews;
import androidx.annotation.NonNull;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

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
            views.setTextViewText(R.id.tvWidgetPartnerTimeText, "Pair with partner to sync");
            views.setTextViewText(R.id.tvWidgetPartnerLocationText, "");
            appWidgetManager.updateAppWidget(appWidgetId, views);
            return;
        }

        DatabaseReference dbRef = FirebaseDatabase.getInstance("https://i-must-finish-this-in-2026-default-rtdb.asia-southeast1.firebasedatabase.app/").getReference();

        // Fetch partner name
        dbRef.child("profiles").child(partnerId).child("name").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot partnerSnapshot) {
                String partnerName = partnerSnapshot.getValue(String.class);
                String pName = (partnerName != null && !partnerName.isEmpty()) ? partnerName : "Partner";

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
                        String pCity = pLocSnap.child("cityName").getValue(String.class);
                        String pTimezone = pLocSnap.child("timezone").getValue(String.class);

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
                        }

                        // Partner Time
                        if (pTimezone != null && !pTimezone.isEmpty()) {
                            try {
                                TimeZone tz = TimeZone.getTimeZone(pTimezone);
                                SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.ENGLISH);
                                sdf.setTimeZone(tz);
                                String timeStr = sdf.format(new Date());

                                String shortTz = tz.getDisplayName(tz.inDaylightTime(new Date()), TimeZone.SHORT, Locale.ENGLISH);
                                views.setTextViewText(R.id.tvWidgetPartnerTimeText, pName + "'s Time: " + timeStr + " (" + shortTz + ")");
                            } catch (Exception e) {
                                views.setTextViewText(R.id.tvWidgetPartnerTimeText, pName + "'s Time: --:--");
                            }
                        } else {
                            views.setTextViewText(R.id.tvWidgetPartnerTimeText, pName + "'s Time: --:--");
                        }

                        // Partner Location
                        if (pCity != null && !pCity.isEmpty()) {
                            views.setTextViewText(R.id.tvWidgetPartnerLocationText, pCity);
                        } else {
                            views.setTextViewText(R.id.tvWidgetPartnerLocationText, "Location active");
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
