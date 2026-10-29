package com.app.projectcecily;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;
import android.widget.Toast;
import androidx.annotation.NonNull;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class PartnerWidgetProvider extends AppWidgetProvider {

    private static final String ACTION_POKE = "com.app.projectcecily.ACTION_POKE";

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        for (int appWidgetId : appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId);
        }
    }

    static void updateAppWidget(Context context, AppWidgetManager appWidgetManager, int appWidgetId) {
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_location);

        Intent intent = new Intent(context, PartnerWidgetProvider.class);
        intent.setAction(ACTION_POKE);
        
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, intent, 
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        views.setOnClickPendingIntent(R.id.widget_root, pendingIntent);

        // Fetch actual distance from Firebase for the widget
        FirebaseDatabase.getInstance("https://i-must-finish-this-in-2026-default-rtdb.asia-southeast1.firebasedatabase.app/").getReference("locations/user_2") // Mocking user_2 as partner
            .addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (snapshot.exists()) {
                        // In a full implementation, you'd calculate distance here too.
                        // For the widget, we'll just show it's updated.
                        views.setTextViewText(R.id.widget_distance_text, "Partner: Online");
                        appWidgetManager.updateAppWidget(appWidgetId, views);
                    }
                }
                @Override
                public void onCancelled(@NonNull DatabaseError error) {}
            });

        appWidgetManager.updateAppWidget(appWidgetId, views);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
        if (ACTION_POKE.equals(intent.getAction())) {
            Toast.makeText(context, "You poked your Partner!", Toast.LENGTH_SHORT).show();
            
            // Trigger a widget refresh on poke
            AppWidgetManager mgr = AppWidgetManager.getInstance(context);
            int[] ids = mgr.getAppWidgetIds(new ComponentName(context, PartnerWidgetProvider.class));
            onUpdate(context, mgr, ids);
        }
    }
}