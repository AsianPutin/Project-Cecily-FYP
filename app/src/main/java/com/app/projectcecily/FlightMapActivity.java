package com.app.projectcecily;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Bundle;
import android.os.Handler;
import android.util.Base64;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import java.util.HashMap;
import java.util.Map;

public class FlightMapActivity extends AppCompatActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
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

    private static final Map<String, LatLng> AIRPORTS = new HashMap<String, LatLng>() {{
        put("KUL", new LatLng(2.7456, 101.7072));
        put("KCH", new LatLng(1.4847, 110.3469));
        put("BKI", new LatLng(5.9372, 116.0513));
        put("SIN", new LatLng(1.3644, 103.9915));
        put("PEN", new LatLng(5.2971, 100.2769));
    }};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_flight_map);

        flightId = getIntent().getStringExtra("flight_id");
        if (flightId == null || flightId.isEmpty()) flightId = "demo_flight";

        dbRef = FirebaseDatabase.getInstance("https://i-must-finish-this-in-2026-default-rtdb.asia-southeast1.firebasedatabase.app/").getReference();

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Live Flight Tracking");
        }

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

        UIUtils.applyTopInsetPadding(findViewById(R.id.cardFlightTopProgress));
        UIUtils.applyBottomMargin(findViewById(R.id.cardFlightBottomDetails));

        listenForPartnerProfile();

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.flight_map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }
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

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;
        listenForFlightUpdates();
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

    private void updateMapAndDetails(FlightInfo info) {
        tvFlightNum.setText((info.airlineName != null ? info.airlineName + " " : "") + info.flightNumber);
        tvDepIata.setText(info.depAirport);
        tvDepCity.setText(info.depCity != null ? info.depCity : "Departure");
        tvArrIata.setText(info.arrAirport);
        tvArrCity.setText(info.arrCity != null ? info.arrCity : "Arrival");

        tvDepTime.setText(info.depTimeStr != null ? info.depTimeStr : "7:17 am");
        tvArrTime.setText(info.arrTimeStr != null ? info.arrTimeStr : "9:04 am");
        tvDuration.setText(info.durationStr != null ? info.durationStr : "1h 47m");

        tvDepDate.setText(info.depDateStr != null ? info.depDateStr : "Sat, 31 Oct");
        tvArrDate.setText(info.arrDateStr != null ? info.arrDateStr : "Sat, 31 Oct");

        tvDepTerminal.setText(info.depTerminal != null ? info.depTerminal : "2");
        tvDepGate.setText(info.depGate != null ? info.depGate : "J10");

        tvArrTerminal.setText(info.arrTerminal != null ? info.arrTerminal : "-");
        tvArrGate.setText(info.arrGate != null ? info.arrGate : "-");

        tvStatusText.setText(info.status != null ? info.status : "IN FLIGHT");

        LatLng depPos = AIRPORTS.get(info.depAirport.toUpperCase());
        LatLng arrPos = AIRPORTS.get(info.arrAirport.toUpperCase());

        if (depPos == null) depPos = new LatLng(2.7456, 101.7072); // KUL default
        if (arrPos == null) arrPos = new LatLng(1.4847, 110.3469); // KCH default

        mMap.clear();
        mMap.addMarker(new MarkerOptions().position(depPos).title(info.depAirport));
        mMap.addMarker(new MarkerOptions().position(arrPos).title(info.arrAirport));

        try {
            int routeColor = Color.parseColor(partnerAccentColor);
            mMap.addPolyline(new PolylineOptions().add(depPos, arrPos).color(routeColor).width(8));
        } catch (Exception e) {
            mMap.addPolyline(new PolylineOptions().add(depPos, arrPos).color(Color.parseColor("#FF4081")).width(8));
        }

        startPseudoTracking(info, depPos, arrPos);
    }

    private void startPseudoTracking(FlightInfo info, LatLng start, LatLng end) {
        if (updateRunnable != null) handler.removeCallbacks(updateRunnable);

        updateRunnable = new Runnable() {
            @Override
            public void run() {
                long now = System.currentTimeMillis();
                long totalDuration = 2 * 60 * 60 * 1000;
                long elapsed = now - info.depTimeMillis;

                double progress = (double) elapsed / totalDuration;
                if (progress < 0) progress = 0.45; // Default 45% for demo if not in range
                if (progress > 1) progress = 1.0;

                int pct = (int) (progress * 100);
                pbProgress.setProgress(pct);
                tvProgress.setText("In Flight - " + pct + "% Completed");

                // Translate track plane icon on top card
                View trackContainer = findViewById(R.id.trackContainer);
                if (trackContainer != null && trackContainer.getWidth() > 0) {
                    float maxX = trackContainer.getWidth() - ivPlaneTrackIcon.getWidth();
                    ivPlaneTrackIcon.setTranslationX((float) (progress * maxX));
                }

                double lat = start.latitude + (end.latitude - start.latitude) * progress;
                double lon = start.longitude + (end.longitude - start.longitude) * progress;
                LatLng planePos = new LatLng(lat, lon);

                // LatLng slightly above plane for Partner Avatar Marker
                LatLng avatarPos = new LatLng(lat + 0.35, lon);

                if (planeMarker == null) {
                    planeMarker = mMap.addMarker(new MarkerOptions()
                            .position(planePos)
                            .icon(BitmapDescriptorFactory.fromResource(android.R.drawable.ic_menu_send))
                            .title(info.flightNumber));
                } else {
                    planeMarker.setPosition(planePos);
                }

                // Add Partner Avatar Pin above plane
                Bitmap partnerPinBitmap = createPartnerAvatarPinBitmap();
                if (partnerMarker == null) {
                    partnerMarker = mMap.addMarker(new MarkerOptions()
                            .position(avatarPos)
                            .icon(BitmapDescriptorFactory.fromBitmap(partnerPinBitmap))
                            .title("Partner on Board"));
                } else {
                    partnerMarker.setPosition(avatarPos);
                    partnerMarker.setIcon(BitmapDescriptorFactory.fromBitmap(partnerPinBitmap));
                }

                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(planePos, 5.5f));

                if (progress < 1) handler.postDelayed(this, 5000);
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

        // Draw pin ring
        canvas.drawCircle(size / 2f, size / 2f, size / 2f, ringPaint);

        // Draw avatar image or fallback
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

        // Draw pin tip triangle at bottom
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
