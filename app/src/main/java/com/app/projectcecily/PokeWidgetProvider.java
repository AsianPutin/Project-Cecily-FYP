package com.app.projectcecily;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;
import android.widget.Toast;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

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

        Intent intent = new Intent(context, PokeWidgetProvider.class);
        intent.setAction(ACTION_SEND_POKE);

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        views.setOnClickPendingIntent(R.id.widget_poke_root, pendingIntent);
        appWidgetManager.updateAppWidget(appWidgetId, views);
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
