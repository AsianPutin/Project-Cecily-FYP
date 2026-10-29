package com.app.projectcecily;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.database.FirebaseDatabase;
import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;

public class TrackingActivity extends AppCompatActivity {

    private MapView mapView;
    private LocationTracker tracker;
    private TextView tvLiveDistanceText, tvPartnerStatus, tvTimeAtLocation;
    private Button btnPoke, btnOpenExternalMap;
    private String userId, partnerId;

    private double lastPartnerLat = 0;
    private double lastPartnerLon = 0;
    private Marker partnerMarker;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Important: Load OSMDroid configuration before layout inflation
        Configuration.getInstance().load(this, getSharedPreferences("osmdroid", MODE_PRIVATE));

        setContentView(R.layout.activity_tracking);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("LDR Tracker 📍");
        }

        mapView = findViewById(R.id.mapView);
        mapView.setTileSource(TileSourceFactory.MAPNIK);
        mapView.setMultiTouchControls(true);

        GeoPoint defaultPos = new GeoPoint(1.5533, 110.3592);
        mapView.getController().setZoom(14.0);
        mapView.getController().setCenter(defaultPos);

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

        tracker = new LocationTracker(this, userId, partnerId);
        tracker.startTracking();

        tracker.listenToPartner((lat, lon, distance) -> {
            if (lat == 0 && lon == 0) return;

            lastPartnerLat = lat;
            lastPartnerLon = lon;

            GeoPoint partnerPos = new GeoPoint(lat, lon);

            if (partnerMarker == null) {
                partnerMarker = new Marker(mapView);
                partnerMarker.setTitle("Partner 💖");
                mapView.getOverlays().add(partnerMarker);
            }
            partnerMarker.setPosition(partnerPos);
            mapView.getController().animateTo(partnerPos);
            mapView.invalidate();

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

    @Override
    protected void onResume() {
        super.onResume();
        if (mapView != null) mapView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mapView != null) mapView.onPause();
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