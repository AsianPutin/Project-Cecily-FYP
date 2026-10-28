package com.app.projectcecily;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.database.FirebaseDatabase;

public class TrackingActivity extends AppCompatActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private LocationTracker tracker;
    private TextView tvLiveDistanceText, tvPartnerStatus, tvTimeAtLocation;
    private Button btnPoke, btnOpenExternalMap;
    private String userId, partnerId;

    private double lastPartnerLat = 0;
    private double lastPartnerLon = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tracking);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("LDR Tracker 📍");
        }

        tvLiveDistanceText = findViewById(R.id.tvLiveDistanceText);
        tvPartnerStatus = findViewById(R.id.tvPartnerStatus);
        tvTimeAtLocation = findViewById(R.id.tvTimeAtLocation);

        btnPoke = findViewById(R.id.btnPoke);
        btnOpenExternalMap = findViewById(R.id.btnOpenExternalMap);

        UIUtils.applyBottomMargin(findViewById(R.id.infoCard));

        userId = UserManager.getUserId(this);
        partnerId = UserManager.getPartnerId(this);

        btnPoke.setOnClickListener(v -> sendPokeToFirebase());
        btnOpenExternalMap.setOnClickListener(v -> openPartnerInNativeMap());

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        tracker = new LocationTracker(this, userId, partnerId);
        tracker.startTracking();
    }

    @SuppressLint("MissingPermission")
    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;

        // Push map controls clear of top and bottom floating cards
        mMap.setPadding(0, 300, 0, 350);

        try {
            mMap.setMyLocationEnabled(true);
            mMap.getUiSettings().setMyLocationButtonEnabled(true);
        } catch (Exception ignored) {}

        // Default camera position
        LatLng defaultPos = new LatLng(1.5533, 110.3592);
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(defaultPos, 12));

        tracker.listenToPartner((lat, lon, distance) -> {
            if (lat == 0 && lon == 0) return;

            lastPartnerLat = lat;
            lastPartnerLon = lon;

            LatLng partnerPos = new LatLng(lat, lon);
            mMap.clear();
            mMap.addMarker(new MarkerOptions().position(partnerPos).title("Partner 💖"));

            if (lat > 0.1) {
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(partnerPos, 14));
            }

            tvTimeAtLocation.setText("Updated: Just now");

            if (distance >= 0) {
                String formattedDistance = UnitUtils.formatDistance(this, distance);
                tvLiveDistanceText.setText(formattedDistance);
                tvPartnerStatus.setText("Partner is " + formattedDistance + " away");
            } else {
                tvLiveDistanceText.setText("Calculating...");
                tvPartnerStatus.setText("Partner location received");
            }
        });
    }

    private void openPartnerInNativeMap() {
        if (lastPartnerLat == 0 && lastPartnerLon == 0) {
            Toast.makeText(this, "Partner coordinates not received yet!", Toast.LENGTH_SHORT).show();
            return;
        }

        Uri gmmIntentUri = Uri.parse("geo:" + lastPartnerLat + "," + lastPartnerLon + "?q=" + lastPartnerLat + "," + lastPartnerLon + "(Partner)");
        Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
        if (mapIntent.resolveActivity(getPackageManager()) != null) {
            startActivity(mapIntent);
        } else {
            String webUrl = "https://www.openstreetmap.org/?mlat=" + lastPartnerLat + "&mlon=" + lastPartnerLon + "#map=15/" + lastPartnerLat + "/" + lastPartnerLon;
            Intent webIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(webUrl));
            startActivity(webIntent);
        }
    }

    private void sendPokeToFirebase() {
        if (partnerId.isEmpty()) {
            Toast.makeText(this, "Please pair with a partner in Account Settings first!", Toast.LENGTH_SHORT).show();
            return;
        }
        FirebaseDatabase.getInstance("https://i-must-finish-this-in-2026-default-rtdb.asia-southeast1.firebasedatabase.app/")
                .getReference("pokes")
                .child(partnerId)
                .setValue(System.currentTimeMillis());
        Toast.makeText(this, "Poke sent! 👉", Toast.LENGTH_SHORT).show();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}