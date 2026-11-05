package com.app.projectcecily;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.format.DateFormat;
import android.text.style.ForegroundColorSpan;
import android.util.Base64;
import android.widget.RemoteViews;
import androidx.annotation.NonNull;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
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
            views.setTextViewText(R.id.tvWidgetTopLine, "Not Paired");
            views.setTextViewText(R.id.tvWidgetBottomLine, "Pair in Account Settings");
            appWidgetManager.updateAppWidget(appWidgetId, views);
            return;
        }

        DatabaseReference dbRef = FirebaseDatabase.getInstance("https://i-must-finish-this-in-2026-default-rtdb.asia-southeast1.firebasedatabase.app/").getReference();

        // Load User Profile (Photo & Accent)
        dbRef.child("profiles").child(userId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot myProfileSnap) {
                String myPhotoBase64 = myProfileSnap.child("photoBase64").getValue(String.class);
                if (myPhotoBase64 != null) {
                    Bitmap myBitmap = base64ToCircularBitmap(myPhotoBase64);
                    if (myBitmap != null) {
                        views.setImageViewBitmap(R.id.ivWidgetMyAvatar, myBitmap);
                    }
                }

                int userAccentColor = ThemeUtils.getAccentColor(context);

                // Load Partner Profile (Photo, TimeZone, Accent)
                dbRef.child("profiles").child(partnerId).addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot partnerProfileSnap) {
                        String partnerPhotoBase64 = partnerProfileSnap.child("photoBase64").getValue(String.class);
                        if (partnerPhotoBase64 != null) {
                            Bitmap pBitmap = base64ToCircularBitmap(partnerPhotoBase64);
                            if (pBitmap != null) {
                                views.setImageViewBitmap(R.id.ivWidgetPartnerAvatar, pBitmap);
                            }
                        }

                        String partnerTz = partnerProfileSnap.child("timeZone").getValue(String.class);
                        String partnerAccentHex = partnerProfileSnap.child("accentColor").getValue(String.class);
                        int partnerAccentColor = Color.parseColor("#FF4081");
                        if (partnerAccentHex != null && !partnerAccentHex.trim().isEmpty()) {
                            try {
                                partnerAccentColor = Color.parseColor(partnerAccentHex.trim());
                            } catch (Exception ignored) {}
                        }

                        final int finalPartnerAccentColor = partnerAccentColor;

                        // Fetch Locations
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

                                    boolean isTogether = meters < 100;
                                    CharSequence topLineStr = formatTopLine(context, km, isTogether, userAccentColor);
                                    views.setTextViewText(R.id.tvWidgetTopLine, topLineStr);

                                    // Resolve partner city
                                    String partnerCity = "";
                                    try {
                                        Geocoder geocoder = new Geocoder(context, Locale.ENGLISH);
                                        List<Address> addresses = geocoder.getFromLocation(pLat, pLng, 1);
                                        if (addresses != null && !addresses.isEmpty()) {
                                            Address addr = addresses.get(0);
                                            String locality = addr.getLocality();
                                            if (locality == null) locality = addr.getSubAdminArea();
                                            if (locality == null) locality = addr.getAdminArea();
                                            String country = addr.getCountryName();

                                            partnerCity = (locality != null ? locality : "") + (country != null ? ", " + country : "");
                                        }
                                    } catch (Exception ignored) {}

                                    CharSequence bottomLineStr = formatBottomLine(context, partnerTz, partnerCity, finalPartnerAccentColor);
                                    views.setTextViewText(R.id.tvWidgetBottomLine, bottomLineStr);
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
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });

        appWidgetManager.updateAppWidget(appWidgetId, views);
    }

    private static CharSequence formatTopLine(Context context, double km, boolean isTogether, int accentColor) {
        if (isTogether) {
            SpannableString ss = new SpannableString("We're together 💕");
            ss.setSpan(new ForegroundColorSpan(accentColor), 0, ss.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            return ss;
        }

        boolean isImperial = UnitUtils.UNIT_IMPERIAL.equals(UnitUtils.getUnitSystem(context));
        String amountStr;
        if (isImperial) {
            int roundedMiles = (int) Math.round(km * 0.621371);
            amountStr = roundedMiles + " mi";
        } else {
            int roundedKm = (int) Math.round(km);
            amountStr = roundedKm + " km";
        }

        String fullText = amountStr + " apart";
        SpannableString ss = new SpannableString(fullText);
        ss.setSpan(new ForegroundColorSpan(accentColor), 0, amountStr.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        ss.setSpan(new ForegroundColorSpan(Color.WHITE), amountStr.length(), fullText.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        return ss;
    }

    private static CharSequence formatBottomLine(Context context, String partnerTz, String partnerCity, int partnerAccentColor) {
        String tzId = (partnerTz != null && !partnerTz.trim().isEmpty()) ? partnerTz.trim() : TimeZone.getDefault().getID();
        TimeZone tz = TimeZone.getTimeZone(tzId);

        boolean is24Hr = DateFormat.is24HourFormat(context);
        SimpleDateFormat sdf = new SimpleDateFormat(is24Hr ? "HH:mm" : "h:mm a", Locale.ENGLISH);
        sdf.setTimeZone(tz);

        String timeStr = sdf.format(new Date());
        String shortTz = tz.getDisplayName(tz.inDaylightTime(new Date()), TimeZone.SHORT, Locale.ENGLISH);
        String timePart = timeStr + " (" + shortTz + ")";

        String fullText;
        if (partnerCity != null && !partnerCity.trim().isEmpty()) {
            fullText = timePart + ", " + partnerCity.trim();
        } else {
            fullText = timePart;
        }

        SpannableString ss = new SpannableString(fullText);
        ss.setSpan(new ForegroundColorSpan(partnerAccentColor), 0, timePart.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        if (fullText.length() > timePart.length()) {
            ss.setSpan(new ForegroundColorSpan(Color.parseColor("#CCCCCC")), timePart.length(), fullText.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        return ss;
    }

    private static Bitmap base64ToCircularBitmap(String base64Str) {
        try {
            String clean = base64Str.contains(",") ? base64Str.split(",")[1] : base64Str;
            byte[] bytes = Base64.decode(clean, Base64.DEFAULT);
            Bitmap raw = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
            if (raw == null) return null;

            int size = Math.min(raw.getWidth(), raw.getHeight());
            Bitmap output = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(output);

            Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
            Rect rect = new Rect(0, 0, size, size);

            canvas.drawARGB(0, 0, 0, 0);
            canvas.drawCircle(size / 2f, size / 2f, size / 2f, paint);
            paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
            canvas.drawBitmap(raw, null, rect, paint);

            return output;
        } catch (Exception e) {
            return null;
        }
    }
}
