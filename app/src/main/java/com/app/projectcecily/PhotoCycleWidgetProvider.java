package com.app.projectcecily;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;
import android.view.View;
import android.widget.RemoteViews;
import java.util.ArrayList;
import java.util.List;

public class PhotoCycleWidgetProvider extends AppWidgetProvider {

    public static final String ACTION_PREV_PHOTO = "com.app.projectcecily.ACTION_PREV_PHOTO";
    public static final String ACTION_NEXT_PHOTO = "com.app.projectcecily.ACTION_NEXT_PHOTO";
    public static final String ACTION_CYCLE_AUTO = "com.app.projectcecily.ACTION_CYCLE_AUTO";

    private static int currentPhotoIndex = 0;
    private static List<PhotoItem> cachedPartnerPhotos = new ArrayList<>();

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        super.onUpdate(context, appWidgetManager, appWidgetIds);
        fetchAndRenderWidgets(context, appWidgetManager, appWidgetIds);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
        String action = intent.getAction();

        if (ACTION_PREV_PHOTO.equals(action)) {
            if (!cachedPartnerPhotos.isEmpty()) {
                currentPhotoIndex = (currentPhotoIndex - 1 + cachedPartnerPhotos.size()) % cachedPartnerPhotos.size();
                updateAllWidgets(context);
            }
        } else if (ACTION_NEXT_PHOTO.equals(action) || ACTION_CYCLE_AUTO.equals(action)) {
            if (!cachedPartnerPhotos.isEmpty()) {
                currentPhotoIndex = (currentPhotoIndex + 1) % cachedPartnerPhotos.size();
                updateAllWidgets(context);
            }
        } else if (AppWidgetManager.ACTION_APPWIDGET_UPDATE.equals(action)) {
            AppWidgetManager manager = AppWidgetManager.getInstance(context);
            ComponentName thisWidget = new ComponentName(context, PhotoCycleWidgetProvider.class);
            int[] appWidgetIds = manager.getAppWidgetIds(thisWidget);
            fetchAndRenderWidgets(context, manager, appWidgetIds);
        }
    }

    private static void fetchAndRenderWidgets(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        boolean isLoggedIn = UserManager.isLoggedIn(context);
        if (!isLoggedIn) {
            cachedPartnerPhotos.clear();
            for (int appWidgetId : appWidgetIds) {
                renderWidget(context, appWidgetManager, appWidgetId, null);
            }
            return;
        }

        PhotoCycleManager.listenCombinedActivePhotos(context, photos -> {
            cachedPartnerPhotos = (photos != null) ? photos : new ArrayList<>();
            if (currentPhotoIndex >= cachedPartnerPhotos.size()) {
                currentPhotoIndex = 0;
            }
            for (int appWidgetId : appWidgetIds) {
                PhotoItem item = cachedPartnerPhotos.isEmpty() ? null : cachedPartnerPhotos.get(currentPhotoIndex);
                renderWidget(context, appWidgetManager, appWidgetId, item);
            }
        });
    }

    private static void updateAllWidgets(Context context) {
        AppWidgetManager manager = AppWidgetManager.getInstance(context);
        ComponentName thisWidget = new ComponentName(context, PhotoCycleWidgetProvider.class);
        int[] appWidgetIds = manager.getAppWidgetIds(thisWidget);

        PhotoItem item = (cachedPartnerPhotos != null && !cachedPartnerPhotos.isEmpty()) ? cachedPartnerPhotos.get(currentPhotoIndex % cachedPartnerPhotos.size()) : null;
        for (int appWidgetId : appWidgetIds) {
            renderWidget(context, manager, appWidgetId, item);
        }
    }

    private static void renderWidget(Context context, AppWidgetManager appWidgetManager, int appWidgetId, PhotoItem item) {
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_photo_cycle);

        boolean isLoggedIn = UserManager.isLoggedIn(context);
        boolean isPaired = !UserManager.getPartnerId(context).isEmpty();

        if (!isLoggedIn || !isPaired || item == null) {
            // Display clean Fallback container
            views.setViewVisibility(R.id.ivWidgetPhoto, View.GONE);
            views.setViewVisibility(R.id.layoutFallback, View.VISIBLE);

            if (!isLoggedIn) {
                views.setTextViewText(R.id.tvFallbackText, "Log In to view\npartner photos");
            } else if (!isPaired) {
                views.setTextViewText(R.id.tvFallbackText, "Pair up to view\npartner photos!");
            } else {
                views.setTextViewText(R.id.tvFallbackText, "No active photos\nTap to send one!");
            }

            // Center click launches MainActivity
            Intent launchIntent = new Intent(context, MainActivity.class);
            PendingIntent launchPending = PendingIntent.getActivity(
                context, 0, launchIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );
            views.setOnClickPendingIntent(R.id.widgetContainer, launchPending);
            views.setOnClickPendingIntent(R.id.btnNavCenter, launchPending);

        } else {
            // Render Clean Square Photo
            views.setViewVisibility(R.id.layoutFallback, View.GONE);
            views.setViewVisibility(R.id.ivWidgetPhoto, View.VISIBLE);

            try {
                String clean = item.photoBase64.contains(",") ? item.photoBase64.split(",")[1] : item.photoBase64;
                byte[] bytes = Base64.decode(clean, Base64.DEFAULT);
                Bitmap bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                views.setImageViewBitmap(R.id.ivWidgetPhoto, bmp);
            } catch (Exception e) {
                views.setViewVisibility(R.id.ivWidgetPhoto, View.GONE);
                views.setViewVisibility(R.id.layoutFallback, View.VISIBLE);
            }

            // Left Click Pending Intent -> Previous Photo
            Intent prevIntent = new Intent(context, PhotoCycleWidgetProvider.class);
            prevIntent.setAction(ACTION_PREV_PHOTO);
            PendingIntent prevPending = PendingIntent.getBroadcast(
                context, 1, prevIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );
            views.setOnClickPendingIntent(R.id.btnNavPrev, prevPending);

            // Right Click Pending Intent -> Next Photo
            Intent nextIntent = new Intent(context, PhotoCycleWidgetProvider.class);
            nextIntent.setAction(ACTION_NEXT_PHOTO);
            PendingIntent nextPending = PendingIntent.getBroadcast(
                context, 2, nextIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );
            views.setOnClickPendingIntent(R.id.btnNavNext, nextPending);

            // Center Click Pending Intent -> Open MainActivity / PhotoCycle
            Intent launchIntent = new Intent(context, MainActivity.class);
            PendingIntent launchPending = PendingIntent.getActivity(
                context, 3, launchIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );
            views.setOnClickPendingIntent(R.id.btnNavCenter, launchPending);
        }

        appWidgetManager.updateAppWidget(appWidgetId, views);
    }
}
