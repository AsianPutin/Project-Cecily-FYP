package com.app.projectcecily;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.drawable.BitmapDrawable;
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

import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FlightMapActivity extends AppCompatActivity {

    private MapView map;
    private String flightId;
    private DatabaseReference dbRef;
    private Marker planeMarker, partnerMarker;

    private TextView tvFlightNum, tvDepTime, tvProgress, tvArrTime;
    private TextView tvDepIata, tvDepCity, tvArrIata, tvArrCity, tvDuration;
    private TextView tvDepDate, tvDepTerminal, tvDepGate;
    private TextView tvArrDate, tvArrTerminal, tvArrGate, tvStatusText;
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
        tvDepTerminal = findViewById(R.id.tvMapDepTerminal);
        tvDepGate = findViewById(R.id.tvMapDepGate);

        tvArrDate = findViewById(R.id.tvMapArrDate);
        tvArrTerminal = findViewById(R.id.tvMapArrTerminal);
        tvArrGate = findViewById(R.id.tvMapArrGate);
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
        dbRef.child("flight_history").child(UserManager.getUserId(this)).child(flightId).addValueEventListener(new ValueEventListener() {
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
        if (DateFormat.is24HourFormat(this)) {
            return DateFormat.format("HH:mm", millis).toString();
        } else {
            return DateFormat.format("h:mm a", millis).toString().toLowerCase();
        }
    }

    private String formatDateSystem(long millis) {
        if (millis <= 0) return "Date pending";
        return DateFormat.format("EEE, dd MMM", millis).toString();
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
        tvDepCity.setText(depCityName != null ? depCityName : "Kuching");
        tvArrIata.setText(arrIataCode);
        tvArrCity.setText(arrCityName != null ? arrCityName : "Kuala Lumpur");

        long depMillis = info.depTimeMillis > 0 ? info.depTimeMillis : System.currentTimeMillis();
        long durationMs = (110 * 60 * 1000L); // 1h 50m duration
        long arrMillis = depMillis + durationMs;

        tvDepTime.setText(formatTimeSystem(depMillis));
        tvArrTime.setText(formatTimeSystem(arrMillis));
        tvDuration.setText("1h 50m");

        tvDepDate.setText(formatDateSystem(depMillis));
        tvArrDate.setText(formatDateSystem(arrMillis));

        String depTerm = info.depTerminal;
        if (depTerm == null || depTerm.trim().isEmpty() || "2".equals(depTerm.trim())) depTerm = "-";

        String depGate = info.depGate;
        if (depGate == null || depGate.trim().isEmpty() || "J10".equals(depGate.trim())) depGate = "-";

        String arrTerm = info.arrTerminal;
        if (arrTerm == null || arrTerm.trim().isEmpty() || "2".equals(arrTerm.trim())) arrTerm = "-";

        String arrGate = info.arrGate;
        if (arrGate == null || arrGate.trim().isEmpty() || "J10".equals(arrGate.trim())) arrGate = "-";

        tvDepTerminal.setText(depTerm);
        tvDepGate.setText(depGate);
        tvArrTerminal.setText(arrTerm);
        tvArrGate.setText(arrGate);

        GeoPoint depPos = AIRPORTS.get(depIataCode.toUpperCase());
        GeoPoint arrPos = AIRPORTS.get(arrIataCode.toUpperCase());

        if (depPos == null) depPos = new GeoPoint(1.4847, 110.3469); // KCH default
        if (arrPos == null) arrPos = new GeoPoint(2.7456, 101.7072); // KUL default

        map.getOverlays().clear();

        // Draw dotted flight trajectory polyline!
        List<GeoPoint> polyPoints = new ArrayList<>();
        polyPoints.add(depPos);
        polyPoints.add(arrPos);

        Polyline polyline = new Polyline(map);
        polyline.setPoints(polyPoints);

        int routeColor = Color.parseColor("#FF4081");
        try {
            routeColor = Color.parseColor(partnerAccentColor);
        } catch (Exception ignored) {}

        polyline.getOutlinePaint().setColor(routeColor);
        polyline.getOutlinePaint().setStrokeWidth(8.0f);
        polyline.getOutlinePaint().setPathEffect(new DashPathEffect(new float[]{20, 15}, 0));
        map.getOverlays().add(polyline);

        // Add Departure Airport Marker
        Marker depMarker = new Marker(map);
        depMarker.setPosition(depPos);
        depMarker.setTitle(depIataCode + " (" + depCityName + ")");
        depMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        map.getOverlays().add(depMarker);

        // Add Arrival Airport Marker
        Marker arrMarker = new Marker(map);
        arrMarker.setPosition(arrPos);
        arrMarker.setTitle(arrIataCode + " (" + arrCityName + ")");
        arrMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        map.getOverlays().add(arrMarker);

        GeoPoint centerPoint = new GeoPoint(
                (depPos.getLatitude() + arrPos.getLatitude()) / 2.0,
                (depPos.getLongitude() + arrPos.getLongitude()) / 2.0
        );
        map.getController().setZoom(6.0);
        map.getController().setCenter(centerPoint);

        startPseudoTracking(info, depPos, arrPos);
    }

    private void startPseudoTracking(FlightInfo info, GeoPoint start, GeoPoint end) {
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

                double lat = start.getLatitude() + (end.getLatitude() - start.getLatitude()) * progress;
                double lon = start.getLongitude() + (end.getLongitude() - start.getLongitude()) * progress;
                GeoPoint planePos = new GeoPoint(lat, lon);
                GeoPoint avatarPos = new GeoPoint(lat + 0.35, lon);

                if (planeMarker == null) {
                    planeMarker = new Marker(map);
                    planeMarker.setTitle(info.flightNumber);
                    map.getOverlays().add(planeMarker);
                }
                planeMarker.setPosition(planePos);

                Bitmap partnerPinBitmap = createPartnerAvatarPinBitmap();
                if (partnerMarker == null) {
                    partnerMarker = new Marker(map);
                    partnerMarker.setTitle("Partner on Board");
                    map.getOverlays().add(partnerMarker);
                }
                partnerMarker.setPosition(avatarPos);
                partnerMarker.setIcon(new BitmapDrawable(getResources(), partnerPinBitmap));

                map.invalidate();

                if (now >= depTime && now <= arrTime && progress < 1) {
                    handler.postDelayed(this, 5000);
                }
            }
        };
        handler.post(updateRunnable);
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

        int innerSize = size - 16;
        Paint innerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        innerPaint.setColor(Color.WHITE);
        canvas.drawCircle(size / 2f, size / 2f, innerSize / 2f, innerPaint);

        if (avatar != null) {
            Bitmap scaled = Bitmap.createScaledBitmap(avatar, innerSize, innerSize, true);
            canvas.drawBitmap(scaled, 8, 8, null);
        } else {
            Bitmap fallback = BitmapFactory.decodeResource(getResources(), R.drawable.ic_blank_user);
            if (fallback != null) {
                Bitmap scaled = Bitmap.createScaledBitmap(fallback, innerSize, innerSize, true);
                canvas.drawBitmap(scaled, 8, 8, null);
            }
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
