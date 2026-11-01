package com.app.projectcecily;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.Shader;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.text.format.DateFormat;
import android.util.Base64;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.res.ResourcesCompat;

import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.BoundingBox;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.Polyline;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

public class FlightMapActivity extends AppCompatActivity {

    private MapView map;
    private String flightId;
    private DatabaseReference dbRef;
    private Marker planeMarker;

    private TextView tvFlightNum, tvDepTime, tvProgress, tvArrTime;
    private TextView tvDepIata, tvDepCity, tvArrIata, tvArrCity, tvDuration;
    private TextView tvDepDate;
    private TextView tvArrDate, tvStatusText;
    private ProgressBar pbProgress;
    private ImageView ivPlaneTrackIcon;
    private View vRouteLine;

    private String partnerPhotoBase64 = "";
    private String partnerAccentColor = "#FF4081";

    private final Handler handler = new Handler();
    private Runnable updateRunnable;

    private static final Map<String, GeoPoint> AIRPORTS = new HashMap<String, GeoPoint>() {{
        put("KUL", new GeoPoint(2.7456, 101.7072));
        put("KCH", new GeoPoint(1.4847, 110.3469));
        put("BKI", new GeoPoint(5.9372, 116.0513));
        put("SIN", new GeoPoint(1.3644, 103.9915));
        put("PEN", new GeoPoint(5.2971, 100.2769));
    }};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Configuration.getInstance().load(this, PreferenceManager.getDefaultSharedPreferences(this));
        setContentView(R.layout.activity_flight_map);

        flightId = getIntent().getStringExtra("flight_id");
        if (flightId == null || flightId.isEmpty()) flightId = "demo_flight";

        dbRef = FirebaseDatabase.getInstance("https://i-must-finish-this-in-2026-default-rtdb.asia-southeast1.firebasedatabase.app/").getReference();

        View appBarView = findViewById(R.id.appBarLayout);
        UIUtils.applyTopInsetToAppBar(appBarView);

        com.google.android.material.appbar.MaterialToolbar toolbar = findViewById(R.id.toolbar);
        if (toolbar != null) {
            setSupportActionBar(toolbar);
            if (getSupportActionBar() != null) {
                getSupportActionBar().setDisplayHomeAsUpEnabled(true);
                getSupportActionBar().setTitle("Live Flight Tracking");
            }
        }

        map = findViewById(R.id.flight_map);
        map.setTileSource(TileSourceFactory.MAPNIK);
        map.setMultiTouchControls(true);

        tvFlightNum = findViewById(R.id.tvMapFlightNum);
        tvDepTime = findViewById(R.id.tvMapDepTime);
        tvProgress = findViewById(R.id.tvMapFlightProgress);
        tvArrTime = findViewById(R.id.tvMapArrTime);

        tvDepIata = findViewById(R.id.tvMapDepIata);
        tvDepCity = findViewById(R.id.tvMapDepCity);
        tvArrIata = findViewById(R.id.tvMapArrIata);
        tvArrCity = findViewById(R.id.tvMapArrCity);
        tvDuration = findViewById(R.id.tvMapDuration);

        tvDepDate = findViewById(R.id.tvMapDepDate);
        tvArrDate = findViewById(R.id.tvMapArrDate);
        tvStatusText = findViewById(R.id.tvMapStatusText);

        pbProgress = findViewById(R.id.pbFlightProgress);
        ivPlaneTrackIcon = findViewById(R.id.ivPlaneTrackIcon);
        vRouteLine = findViewById(R.id.vRouteLine);

        UIUtils.applyBottomMargin(findViewById(R.id.cardFlightBottomDetails));

        listenForPartnerProfile();
        listenForFlightUpdates();
    }

    private void listenForPartnerProfile() {
        String partnerId = UserManager.getPartnerId(this);
        if (partnerId.isEmpty()) return;

        dbRef.child("profiles").child(partnerId).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String photo = snapshot.child("photoBase64").getValue(String.class);
                    if (photo != null) partnerPhotoBase64 = photo;

                    String color = snapshot.child("accentColor").getValue(String.class);
                    if (color != null && !color.trim().isEmpty()) partnerAccentColor = color.trim();

                    applyPartnerThemeAccents();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void applyPartnerThemeAccents() {
        try {
            int accent = Color.parseColor(partnerAccentColor);
            tvProgress.setTextColor(accent);
            tvStatusText.setTextColor(accent);
            ivPlaneTrackIcon.setColorFilter(accent);
            pbProgress.setProgressTintList(android.content.res.ColorStateList.valueOf(accent));
            if (vRouteLine != null) vRouteLine.setBackgroundColor(accent);
        } catch (Exception ignored) {}
    }

    private void listenForFlightUpdates() {
        String pairKey = FlightTrackerActivity.getPairKey(this);
        dbRef.child("flights").child(pairKey).child(flightId).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                FlightInfo info = snapshot.getValue(FlightInfo.class);
                if (info != null) {
                    updateMapAndDetails(info);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private String formatTimeSystem(long millis) {
        if (millis <= 0) return "--:--";
        TimeZone tz = TimeZone.getDefault();
        String tzCode = tz.getDisplayName(tz.inDaylightTime(new Date(millis)), TimeZone.SHORT);

        String timeStr;
        if (DateFormat.is24HourFormat(this)) {
            timeStr = DateFormat.format("HH:mm", millis).toString();
        } else {
            timeStr = DateFormat.format("h:mm a", millis).toString().toLowerCase();
        }

        return timeStr + " (" + tzCode + ")";
    }

    private String formatDateSystem(long millis) {
        if (millis <= 0) return "Date pending";
        return DateFormat.format("EEE, dd MMM", millis).toString();
    }

    private String resolveCityAndCountry(String iata, String defaultCity) {
        if (iata != null && !iata.trim().isEmpty()) {
            String clean = iata.trim().toUpperCase();
            if ("KCH".equals(clean)) return "Kuching, Malaysia";
            if ("KUL".equals(clean)) return "Kuala Lumpur, Malaysia";
            if ("BKI".equals(clean)) return "Kota Kinabalu, Malaysia";
            if ("SIN".equals(clean)) return "Singapore, Singapore";
            if ("PEN".equals(clean)) return "Penang, Malaysia";
        }
        if (defaultCity != null && !defaultCity.trim().isEmpty() && !"Arrival".equalsIgnoreCase(defaultCity.trim())) {
            String clean = defaultCity.trim();
            if (clean.toLowerCase().contains("kuching")) return "Kuching, Malaysia";
            if (clean.toLowerCase().contains("kuala lumpur")) return "Kuala Lumpur, Malaysia";
            if (clean.toLowerCase().contains("kota kinabalu")) return "Kota Kinabalu, Malaysia";
            if (clean.toLowerCase().contains("singapore")) return "Singapore, Singapore";
            if (clean.toLowerCase().contains("penang")) return "Penang, Malaysia";
            if (clean.contains(",")) return clean;
            return clean + ", Malaysia";
        }
        return "Kuching, Malaysia";
    }

    private float calculateBearing(GeoPoint p1, GeoPoint p2) {
        if (p1 == null || p2 == null || p1.equals(p2)) return 90f;
        double dLon = Math.toRadians(p2.getLongitude() - p1.getLongitude());
        double lat1 = Math.toRadians(p1.getLatitude());
        double lat2 = Math.toRadians(p2.getLatitude());
        double y = Math.sin(dLon) * Math.cos(lat2);
        double x = Math.cos(lat1) * Math.sin(lat2) - Math.sin(lat1) * Math.cos(lat2) * Math.cos(dLon);
        float bearing = (float) Math.toDegrees(Math.atan2(y, x));
        return (bearing + 360) % 360;
    }

    private List<GeoPoint> createCurvedRoute(GeoPoint start, GeoPoint end) {
        List<GeoPoint> points = new ArrayList<>();
        double midLat = (start.getLatitude() + end.getLatitude()) / 2.0;
        double midLon = (start.getLongitude() + end.getLongitude()) / 2.0;

        double dLat = end.getLatitude() - start.getLatitude();
        double dLon = end.getLongitude() - start.getLongitude();

        double controlLat = midLat - (dLon * 0.25);
        double controlLon = midLon + (dLat * 0.25);

        int numPoints = 40;
        for (int i = 0; i <= numPoints; i++) {
            double t = (double) i / numPoints;
            double lat = (1 - t) * (1 - t) * start.getLatitude() + 2 * (1 - t) * t * controlLat + t * t * end.getLatitude();
            double lon = (1 - t) * (1 - t) * start.getLongitude() + 2 * (1 - t) * t * controlLon + t * t * end.getLongitude();
            points.add(new GeoPoint(lat, lon));
        }
        return points;
    }

    private Bitmap createAirportDotBitmap() {
        int size = 56;
        Bitmap bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);

        int color = Color.parseColor("#FF4081");
        try {
            color = Color.parseColor(partnerAccentColor);
        } catch (Exception ignored) {}

        Paint ringPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        ringPaint.setColor(color);
        canvas.drawCircle(size / 2f, size / 2f, size / 2f, ringPaint);

        Paint whitePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        whitePaint.setColor(Color.WHITE);
        canvas.drawCircle(size / 2f, size / 2f, size / 2f - 6, whitePaint);

        Paint innerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        innerPaint.setColor(color);
        canvas.drawCircle(size / 2f, size / 2f, size / 2f - 14, innerPaint);

        return bitmap;
    }

    private Bitmap getBitmapFromVectorDrawable(int drawableId, int width, int height) {
        try {
            Drawable drawable = ResourcesCompat.getDrawable(getResources(), drawableId, getTheme());
            if (drawable == null) return null;

            Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmap);
            drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
            drawable.draw(canvas);
            return bitmap;
        } catch (Exception e) {
            return null;
        }
    }

    private void updateMapAndDetails(FlightInfo info) {
        tvFlightNum.setText((info.airlineName != null ? info.airlineName + " " : "") + info.flightNumber);

        String depIataCode = info.depAirport;
        String arrIataCode = info.arrAirport;
        String depCityName = info.depCity;
        String arrCityName = info.arrCity;

        if ("MH2547".equalsIgnoreCase(info.flightNumber != null ? info.flightNumber.replaceAll("\\s+", "") : "")) {
            depIataCode = "KCH";
            arrIataCode = "KUL";
            depCityName = "Kuching";
            arrCityName = "Kuala Lumpur";
        }

        tvDepIata.setText(depIataCode);
        tvDepCity.setText(resolveCityAndCountry(depIataCode, depCityName));
        tvArrIata.setText(arrIataCode);
        tvArrCity.setText(resolveCityAndCountry(arrIataCode, arrCityName));

        long depMillis = info.depTimeMillis > 0 ? info.depTimeMillis : System.currentTimeMillis();
        long durationMs = (110 * 60 * 1000L); // 1h 50m duration
        long arrMillis = depMillis + durationMs;

        tvDepTime.setText(formatTimeSystem(depMillis));
        tvArrTime.setText(formatTimeSystem(arrMillis));
        tvDuration.setText("1h 50m");

        tvDepDate.setText(formatDateSystem(depMillis));
        tvArrDate.setText(formatDateSystem(arrMillis));

        GeoPoint depPos = AIRPORTS.get(depIataCode.toUpperCase());
        GeoPoint arrPos = AIRPORTS.get(arrIataCode.toUpperCase());

        if (depPos == null) depPos = new GeoPoint(1.4847, 110.3469); // KCH default
        if (arrPos == null) arrPos = new GeoPoint(2.7456, 101.7072); // KUL default

        map.getOverlays().clear();

        List<GeoPoint> curvePoints = createCurvedRoute(depPos, arrPos);

        Polyline polyline = new Polyline(map);
        polyline.setPoints(curvePoints);

        int routeColor = Color.parseColor("#FF4081");
        try {
            routeColor = Color.parseColor(partnerAccentColor);
        } catch (Exception ignored) {}

        polyline.getOutlinePaint().setColor(routeColor);
        polyline.getOutlinePaint().setStrokeWidth(8.0f);
        polyline.getOutlinePaint().setPathEffect(new DashPathEffect(new float[]{20, 15}, 0));
        map.getOverlays().add(polyline);

        Bitmap dotBitmap = createAirportDotBitmap();

        // Departure Airport Circular Dot Marker
        Marker depMarker = new Marker(map);
        depMarker.setPosition(depPos);
        depMarker.setTitle(depIataCode + " (" + depCityName + ")");
        depMarker.setIcon(new BitmapDrawable(getResources(), dotBitmap));
        depMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER);
        map.getOverlays().add(depMarker);

        // Arrival Airport Circular Dot Marker
        Marker arrMarker = new Marker(map);
        arrMarker.setPosition(arrPos);
        arrMarker.setTitle(arrIataCode + " (" + arrCityName + ")");
        arrMarker.setIcon(new BitmapDrawable(getResources(), dotBitmap));
        arrMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER);
        map.getOverlays().add(arrMarker);

        BoundingBox box = BoundingBox.fromGeoPoints(curvePoints);
        map.zoomToBoundingBox(box, true, 180);

        startPseudoTracking(info, curvePoints);
    }

    private void startPseudoTracking(FlightInfo info, List<GeoPoint> curvePoints) {
        if (updateRunnable != null) handler.removeCallbacks(updateRunnable);

        updateRunnable = new Runnable() {
            @Override
            public void run() {
                long now = System.currentTimeMillis();
                long totalDuration = 2 * 60 * 60 * 1000;
                long depTime = info.depTimeMillis;
                long arrTime = depTime + totalDuration;

                double progress;
                if (now < depTime) {
                    progress = 0.0;
                    pbProgress.setProgress(0);
                    tvProgress.setText("Awaiting Flight");
                    tvStatusText.setText("AWAITING");
                } else if (now > arrTime) {
                    progress = 1.0;
                    pbProgress.setProgress(100);
                    tvProgress.setText("Flight Landed");
                    tvStatusText.setText("LANDED");
                } else {
                    long elapsed = now - depTime;
                    progress = (double) elapsed / totalDuration;
                    if (progress < 0) progress = 0;
                    if (progress > 1) progress = 1;

                    int pct = (int) (progress * 100);
                    pbProgress.setProgress(pct);
                    tvProgress.setText("In Flight - " + pct + "% Completed");
                    tvStatusText.setText("IN FLIGHT");
                }

                final double finalProgress = progress;
                pbProgress.post(() -> {
                    int trackWidth = pbProgress.getWidth();
                    int iconWidth = ivPlaneTrackIcon.getWidth();
                    if (trackWidth > iconWidth) {
                        float availableTrack = trackWidth - iconWidth;
                        float targetX = (float) (finalProgress * availableTrack);
                        ivPlaneTrackIcon.setTranslationX(targetX);
                        ivPlaneTrackIcon.setTranslationY(0f);
                    }
                });

                int pointIdx = (int) (progress * (curvePoints.size() - 1));
                if (pointIdx < 0) pointIdx = 0;
                if (pointIdx >= curvePoints.size()) pointIdx = curvePoints.size() - 1;

                GeoPoint planePos = curvePoints.get(pointIdx);
                GeoPoint nextPos = (pointIdx < curvePoints.size() - 1) ? curvePoints.get(pointIdx + 1) : planePos;

                float bearing = calculateBearing(planePos, nextPos);
                Bitmap compositeBitmap = createPlaneWithAvatarBitmap(bearing);

                int avatarHeight = 130;
                int planeSize = 80;
                int gap = 2;
                int totalHeight = avatarHeight + gap + planeSize;
                float planeCenterY = avatarHeight + gap + (planeSize / 2f);
                float anchorY = planeCenterY / totalHeight;

                if (planeMarker == null) {
                    planeMarker = new Marker(map);
                    planeMarker.setTitle(info.flightNumber);
                    map.getOverlays().add(planeMarker);
                }
                planeMarker.setPosition(planePos);
                planeMarker.setIcon(new BitmapDrawable(getResources(), compositeBitmap));
                planeMarker.setAnchor(Marker.ANCHOR_CENTER, anchorY);

                map.invalidate();

                if (now >= depTime && now <= arrTime && progress < 1) {
                    handler.postDelayed(this, 5000);
                }
            }
        };
        handler.post(updateRunnable);
    }

    private Bitmap createPlaneWithAvatarBitmap(float bearingAngle) {
        Bitmap avatarPin = createPartnerAvatarPinBitmap();
        int avatarWidth = avatarPin.getWidth();
        int avatarHeight = avatarPin.getHeight();

        int planeSize = 80;
        int gap = 2;

        int totalWidth = Math.max(avatarWidth, planeSize + 20);
        int totalHeight = avatarHeight + gap + planeSize;

        Bitmap composite = Bitmap.createBitmap(totalWidth, totalHeight, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(composite);

        // 1. Draw Partner Avatar Pin at top (x centered, y = 0)
        float avatarX = (totalWidth - avatarWidth) / 2f;
        canvas.drawBitmap(avatarPin, avatarX, 0, null);

        // 2. Render Plane Vector Drawable into Bitmap
        Bitmap planeBm = getBitmapFromVectorDrawable(R.drawable.ic_plane, planeSize, planeSize);
        if (planeBm != null) {
            int accent = Color.parseColor("#FF4081");
            try { accent = Color.parseColor(partnerAccentColor); } catch (Exception ignored) {}

            Matrix matrix = new Matrix();
            // CALIBRATED ROTATION: bearing - 90f because ic_plane vector natively points RIGHT (90°)!
            float rotationAngle = (bearingAngle - 90f + 360f) % 360f;
            matrix.postRotate(rotationAngle, planeSize / 2f, planeSize / 2f);

            float planeX = (totalWidth - planeSize) / 2f;
            float planeY = avatarHeight + gap;
            matrix.postTranslate(planeX, planeY);

            Paint planePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            planePaint.setColorFilter(new PorterDuffColorFilter(accent, PorterDuff.Mode.SRC_IN));

            canvas.drawBitmap(planeBm, matrix, planePaint);
        }

        return composite;
    }

    private Bitmap createPartnerAvatarPinBitmap() {
        int size = 110;
        Bitmap bitmap = Bitmap.createBitmap(size, size + 20, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);

        Paint ringPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        try {
            ringPaint.setColor(Color.parseColor(partnerAccentColor));
        } catch (Exception e) {
            ringPaint.setColor(Color.parseColor("#FF4081"));
        }

        canvas.drawCircle(size / 2f, size / 2f, size / 2f, ringPaint);

        Bitmap avatar = null;
        if (partnerPhotoBase64 != null && !partnerPhotoBase64.isEmpty()) {
            try {
                String clean = partnerPhotoBase64.contains(",") ? partnerPhotoBase64.split(",")[1] : partnerPhotoBase64;
                byte[] bytes = Base64.decode(clean, Base64.DEFAULT);
                avatar = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
            } catch (Exception ignored) {}
        }

        if (avatar == null) {
            avatar = BitmapFactory.decodeResource(getResources(), R.drawable.ic_blank_user);
        }

        int innerSize = size - 16;
        if (avatar != null) {
            Bitmap scaled = Bitmap.createScaledBitmap(avatar, innerSize, innerSize, true);

            // 100% CIRCULAR CROPPING USING BITMAP SHADER (Zero square overflow!)
            Bitmap circularAvatar = Bitmap.createBitmap(innerSize, innerSize, Bitmap.Config.ARGB_8888);
            Canvas avatarCanvas = new Canvas(circularAvatar);
            Paint avatarPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

            BitmapShader shader = new BitmapShader(scaled, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP);
            avatarPaint.setShader(shader);

            avatarCanvas.drawCircle(innerSize / 2f, innerSize / 2f, innerSize / 2f, avatarPaint);

            canvas.drawBitmap(circularAvatar, 8, 8, null);
        }

        Paint tipPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        tipPaint.setColor(ringPaint.getColor());
        android.graphics.Path path = new android.graphics.Path();
        path.moveTo(size / 2f - 14, size - 4);
        path.lineTo(size / 2f + 14, size - 4);
        path.lineTo(size / 2f, size + 16);
        path.close();
        canvas.drawPath(path, tipPaint);

        return bitmap;
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (map != null) map.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (map != null) map.onPause();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) { finish(); return true; }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (updateRunnable != null) handler.removeCallbacks(updateRunnable);
    }
}
