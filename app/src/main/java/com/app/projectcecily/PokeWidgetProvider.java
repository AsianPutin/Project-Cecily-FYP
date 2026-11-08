package com.app.projectcecily;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.widget.RemoteViews;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatDelegate;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class PokeWidgetProvider extends AppWidgetProvider {

    public static final String ACTION_SEND_POKE = "com.app.projectcecily.ACTION_SEND_POKE";

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        for (int appWidgetId : appWidgetIds) {
            updatePokeWidget(context, appWidgetManager, appWidgetId);
        }
    }

    static void updatePokeWidget(Context context, AppWidgetManager appWidgetManager, int appWidgetId) {
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_poke);

        boolean isNight = isNightMode(context);
        views.setInt(R.id.widget_poke_root, "setBackgroundResource",
                isNight ? R.drawable.widget_background_dark : R.drawable.widget_background_light);
        views.setTextColor(R.id.tvPokeText, isNight ? Color.WHITE : Color.parseColor("#212121"));

        Intent intent = new Intent(context, PokeWidgetProvider.class);
        intent.setAction(ACTION_SEND_POKE);

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        views.setOnClickPendingIntent(R.id.widget_poke_root, pendingIntent);

        String userId = UserManager.getUserId(context);
        String partnerId = UserManager.getPartnerId(context);

        // When NOT logged in -> Display Monochrome App Icon!
        if (!UserManager.isLoggedIn(context) || userId.isEmpty()) {
            views.setImageViewResource(R.id.ivPokeHand, isNight ? R.drawable.ic_app_logo_mono_white : R.drawable.ic_app_logo_mono_black);
            views.setInt(R.id.ivPokeHand, "setColorFilter", 0);
            views.setTextViewText(R.id.tvPokeText, "Log In");
            appWidgetManager.updateAppWidget(appWidgetId, views);
            return;
        }

        // When Logged In -> Use Poke Hand & Partner Accent
        views.setImageViewResource(R.id.ivPokeHand, R.drawable.ic_poke_hand);
        views.setTextViewText(R.id.tvPokeText, "Poke");

        if (partnerId.isEmpty()) {
            int defaultPartnerColor = Color.parseColor("#FF4081");
            views.setInt(R.id.ivPokeHand, "setColorFilter", defaultPartnerColor);
            appWidgetManager.updateAppWidget(appWidgetId, views);
            return;
        }

        DatabaseReference dbRef = FirebaseDatabase.getInstance("https://i-must-finish-this-in-2026-default-rtdb.asia-southeast1.firebasedatabase.app/").getReference();
        dbRef.child("profiles").child(partnerId).child("accentColor").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String partnerAccentHex = snapshot.getValue(String.class);
                int partnerAccentColor = Color.parseColor("#FF4081");
                if (partnerAccentHex != null && !partnerAccentHex.trim().isEmpty()) {
                    try {
                        partnerAccentColor = Color.parseColor(partnerAccentHex.trim());
                    } catch (Exception ignored) {}
                }
                views.setInt(R.id.ivPokeHand, "setColorFilter", partnerAccentColor);
                appWidgetManager.updateAppWidget(appWidgetId, views);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                int defaultPartnerColor = Color.parseColor("#FF4081");
                views.setInt(R.id.ivPokeHand, "setColorFilter", defaultPartnerColor);
                appWidgetManager.updateAppWidget(appWidgetId, views);
            }
        });
    }

    private static boolean isNightMode(Context context) {
        int savedMode = ThemeUtils.getSavedThemeMode(context);
        if (savedMode == AppCompatDelegate.MODE_NIGHT_YES) return true;
        if (savedMode == AppCompatDelegate.MODE_NIGHT_NO) return false;
        int uiMode = context.getResources().getConfiguration().uiMode & android.content.res.Configuration.UI_MODE_NIGHT_MASK;
        return uiMode == android.content.res.Configuration.UI_MODE_NIGHT_YES;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
        if (ACTION_SEND_POKE.equals(intent.getAction())) {
            String partnerId = UserManager.getPartnerId(context);
            String userId = UserManager.getUserId(context);

            if (!partnerId.isEmpty() && !userId.isEmpty()) {
                DatabaseReference dbRef = FirebaseDatabase.getInstance("https://i-must-finish-this-in-2026-default-rtdb.asia-southeast1.firebasedatabase.app/").getReference();
                dbRef.child("pokes").child(partnerId).setValue(System.currentTimeMillis());
                Toast.makeText(context, "👉 Poked partner!", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(context, "Pair with partner first to send pokes!", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
