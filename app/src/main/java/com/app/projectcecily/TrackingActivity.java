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
import java.util.Locale;

public class TrackingActivity extends AppCompatActivity {

    private LocationTracker tracker;
    private TextView tvLiveDistanceText, tvPartnerStatus, tvTimeAtLocation;
    private TextView tvMyLat, tvMyLon, tvMyLastUpdated;
    private TextView tvPartnerLat, tvPartnerLon;
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

        tvMyLat = findViewById(R.id.tvMyLat);
        tvMyLon = findViewById(R.id.tvMyLon);
        tvMyLastUpdated = findViewById(R.id.tvMyLastUpdated);

        tvPartnerLat = findViewById(R.id.tvPartnerLat);
        tvPartnerLon = findViewById(R.id.tvPartnerLon);

        btnPoke = findViewById(R.id.btnPoke);
        btnOpenExternalMap = findViewById(R.id.btnOpenExternalMap);

        UIUtils.applyBottomPadding(findViewById(R.id.tracking_scroll));

        userId = UserManager.getUserId(this);
        partnerId = UserManager.getPartnerId(this);

        btnPoke.setOnClickListener(v -> sendPokeToFirebase());
        btnOpenExternalMap.setOnClickListener(v -> openPartnerInNativeMap());

        tracker = new LocationTracker(this, userId, partnerId);
        tracker.startTracking();

        tracker.listenToSelf((lat, lon) -> {
            tvMyLat.setText(String.format(Locale.ENGLISH, "%.6f", lat));
            tvMyLon.setText(String.format(Locale.ENGLISH, "%.6f", lon));
            tvMyLastUpdated.setText("Updated: Just now");
        });

        tracker.listenToPartner((lat, lon, distance) -> {
            if (lat == 0 && lon == 0) return;

            lastPartnerLat = lat;
            lastPartnerLon = lon;

            tvPartnerLat.setText(String.format(Locale.ENGLISH, "%.6f", lat));
            tvPartnerLon.setText(String.format(Locale.ENGLISH, "%.6f", lon));
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
            // Fallback to web browser OpenStreetMap
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