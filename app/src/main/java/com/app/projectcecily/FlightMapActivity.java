package com.app.projectcecily;

import android.os.Bundle;
import android.os.Handler;
import android.view.MenuItem;
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
    private Marker planeMarker;
    private TextView tvFlightNum, tvProgress;
    private ProgressBar pbProgress;
    
    private final Handler handler = new Handler();
    private Runnable updateRunnable;

    // Hardcoded demo coordinates for major airports
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
        dbRef = FirebaseDatabase.getInstance("https://i-must-finish-this-in-2026-default-rtdb.asia-southeast1.firebasedatabase.app/")
                .getReference("flights").child(flightId);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Live Flight Tracking");
        }

        tvFlightNum = findViewById(R.id.tvMapFlightNum);
        tvProgress = findViewById(R.id.tvMapFlightProgress);
        pbProgress = findViewById(R.id.pbFlightProgress);

        UIUtils.applyBottomMargin(findViewById(R.id.flight_map_card));

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.flight_map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;
        listenForFlightUpdates();
    }

    private void listenForFlightUpdates() {
        dbRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                FlightInfo info = snapshot.getValue(FlightInfo.class);
                if (info != null) {
                    updateMap(info);
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void updateMap(FlightInfo info) {
        tvFlightNum.setText(info.flightNumber);
        
        LatLng depPos = AIRPORTS.get(info.depAirport);
        LatLng arrPos = AIRPORTS.get(info.arrAirport);

        if (depPos != null && arrPos != null) {
            mMap.clear();
            mMap.addMarker(new MarkerOptions().position(depPos).title(info.depAirport));
            mMap.addMarker(new MarkerOptions().position(arrPos).title(info.arrAirport));
            mMap.addPolyline(new PolylineOptions().add(depPos, arrPos).color(0xFF999999).width(5));

            startPseudoTracking(info, depPos, arrPos);
        }
    }

    private void startPseudoTracking(FlightInfo info, LatLng start, LatLng end) {
        if (updateRunnable != null) handler.removeCallbacks(updateRunnable);

        updateRunnable = new Runnable() {
            @Override
            public void run() {
                long now = System.currentTimeMillis();
                long totalDuration = 2 * 60 * 60 * 1000; // Assume 2 hours for demo if not provided
                long elapsed = now - info.depTimeMillis;
                
                double progress = (double) elapsed / totalDuration;
                if (progress < 0) progress = 0;
                if (progress > 1) progress = 1;

                int pct = (int) (progress * 100);
                pbProgress.setProgress(pct);
                tvProgress.setText("Status: " + info.status + " - " + pct + "% Completed");

                double lat = start.latitude + (end.latitude - start.latitude) * progress;
                double lon = start.longitude + (end.longitude - start.longitude) * progress;
                LatLng planePos = new LatLng(lat, lon);

                if (planeMarker == null) {
                    planeMarker = mMap.addMarker(new MarkerOptions()
                            .position(planePos)
                            .icon(BitmapDescriptorFactory.fromResource(android.R.drawable.ic_menu_send)) // Needs a plane icon
                            .title(info.flightNumber));
                } else {
                    planeMarker.setPosition(planePos);
                }

                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(planePos, 6));

                if (progress < 1) handler.postDelayed(this, 5000);
            }
        };
        handler.post(updateRunnable);
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