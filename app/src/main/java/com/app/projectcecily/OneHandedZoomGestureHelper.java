package com.app.projectcecily;

import android.content.Context;
import android.os.SystemClock;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import org.osmdroid.views.MapView;

public class OneHandedZoomGestureHelper implements View.OnTouchListener {

    private final MapView mapView;
    private final int doubleTapTimeout;

    private long lastTapUpTime = 0;
    private float lastTapUpX = 0f;
    private float lastTapUpY = 0f;

    private boolean isQuickScaling = false;
    private float startTouchY = 0f;
    private double startZoomLevel = 0.0;
    private boolean hasDraggedSignificantly = false;

    public static void attach(Context context, MapView mapView) {
        if (context == null || mapView == null) return;
        OneHandedZoomGestureHelper helper = new OneHandedZoomGestureHelper(context, mapView);
        mapView.setOnTouchListener(helper);
    }

    public OneHandedZoomGestureHelper(Context context, MapView mapView) {
        this.mapView = mapView;
        this.doubleTapTimeout = ViewConfiguration.getDoubleTapTimeout();
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        int action = event.getActionMasked();
        long now = SystemClock.uptimeMillis();

        switch (action) {
            case MotionEvent.ACTION_DOWN:
                long timeDiff = now - lastTapUpTime;
                float dx = event.getX() - lastTapUpX;
                float dy = event.getY() - lastTapUpY;
                float distSq = dx * dx + dy * dy;

                if (timeDiff <= doubleTapTimeout && distSq < (100 * 100)) {
                    // Second tap down within timeout -> Start continuous quick scale mode!
                    isQuickScaling = true;
                    hasDraggedSignificantly = false;
                    startTouchY = event.getY();
                    startZoomLevel = mapView.getZoomLevelDouble();
                    return true; // Consume touch
                }
                break;

            case MotionEvent.ACTION_MOVE:
                if (isQuickScaling) {
                    float dragDeltaY = event.getY() - startTouchY;
                    if (Math.abs(dragDeltaY) > 8) {
                        hasDraggedSignificantly = true;
                    }

                    // Continuous smooth zoom calculation
                    // Dragging DOWN (dragDeltaY > 0) -> Zoom IN
                    // Dragging UP (dragDeltaY < 0) -> Zoom OUT
                    double zoomDelta = dragDeltaY / 150.0;
                    double newZoom = startZoomLevel + zoomDelta;

                    double minZoom = mapView.getMinZoomLevel();
                    double maxZoom = mapView.getMaxZoomLevel();
                    double clampedZoom = Math.max(minZoom, Math.min(maxZoom, newZoom));

                    mapView.getController().setZoom(clampedZoom);
                    return true; // Keep consuming move events continuously while thumb is held down!
                }
                break;

            case MotionEvent.ACTION_UP:
                if (isQuickScaling) {
                    isQuickScaling = false;
                    lastTapUpTime = 0; // Reset tap state

                    if (!hasDraggedSignificantly) {
                        // Quick double tap without drag -> perform standard step zoom in
                        mapView.getController().zoomIn();
                    }
                    v.performClick();
                    return true;
                }

                // Record first tap up
                lastTapUpTime = now;
                lastTapUpX = event.getX();
                lastTapUpY = event.getY();
                break;

            case MotionEvent.ACTION_CANCEL:
                if (isQuickScaling) {
                    isQuickScaling = false;
                    lastTapUpTime = 0;
                    return true;
                }
                break;
        }

        return false; // Pass through normal single pan / multi-touch pinch to MapView
    }
}
