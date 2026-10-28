package com.app.projectcecily;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
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
    private TextView tvStatus, tvTime;
    private Button btnPoke;
    private String userId, partnerId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tracking);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("LDR Tracker 📍");
        }

        tvStatus = findViewById(R.id.tvPartnerStatus);
        tvTime = findViewById(R.id.tvTimeAtLocation);
        btnPoke = findViewById(R.id.btnPoke);

        // Force the layout to sit above the navigation bar tray
        UIUtils.applyBottomMargin(findViewById(R.id.infoCard));

        userId = UserManager.getUserId(this);
        partnerId = UserManager.getPartnerId(this);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        btnPoke.setOnClickListener(v -> sendPokeToFirebase());

        tracker = new LocationTracker(this, userId, partnerId);
        tracker.startTracking();
    }

    private void sendPokeToFirebase() {
        FirebaseDatabase.getInstance("https://i-must-finish-this-in-2026-default-rtdb.asia-southeast1.firebasedatabase.app/").getReference("pokes")
                .child(partnerId).setValue(System.currentTimeMillis());
        Toast.makeText(this, "Poke sent!", Toast.LENGTH_SHORT).show();
    }

    @SuppressLint("MissingPermission")
    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;
        
        // Push Map UI buttons down from the top bar and up from the bottom card
        // (left, top, right, bottom)
        mMap.setPadding(0, 150, 0, 450);

        mMap.setMyLocationEnabled(true);
        mMap.getUiSettings().setMyLocationButtonEnabled(true);

        // Zoom to default city center initially so it's not a beige blank screen
        LatLng kuching = new LatLng(1.5533, 110.3592);
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(kuching, 12));

        tracker.listenToPartner((lat, lon, distance) -> {
            if (lat == 0 && lon == 0) return; // Ignore invalid data

            LatLng partnerPos = new LatLng(lat, lon);
            mMap.clear(); 
            mMap.addMarker(new MarkerOptions().position(partnerPos).title("Partner"));
            
            // Only zoom to partner if distance is realistic (within Malaysia area roughly)
            if (lat > 0.1 && lon > 100) {
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(partnerPos, 15));
                tvStatus.setText(String.format(java.util.Locale.ENGLISH, "Partner is %.2f km away", distance));
            } else {
                tvStatus.setText("Partner location pending...");
            }
            tvTime.setText("Updated just now");
        });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) { finish(); return true; }
        return super.onOptionsItemSelected(item);
    }
}