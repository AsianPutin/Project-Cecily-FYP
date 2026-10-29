package com.app.projectcecily;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.util.Base64;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import org.osmdroid.config.Configuration;
import org.osmdroid.events.MapListener;
import org.osmdroid.events.ScrollEvent;
import org.osmdroid.events.ZoomEvent;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;

public class TrackingActivity extends AppCompatActivity {

    private MapView mapView;
    private LocationTracker tracker;
    private TextView tvLiveDistanceText, tvPartnerStatus, tvTimeAtLocation;
    private Button btnPoke, btnOpenExternalMap;

    private MaterialCardView cardPartnerEdgeWidget;
    private ImageView ivPartnerEdgeAvatar;

    private MaterialCardView cardMyEdgeWidget;
    private ImageView ivMyEdgeAvatar;

    private String userId, partnerId;

    private double lastPartnerLat = 0;
    private double lastPartnerLon = 0;
    private double lastSelfLat = 0;
    private double lastSelfLon = 0;

    private Marker myMarker;
    private Marker partnerMarker;

    private Bitmap myAvatarBitmap;
    private Bitmap partnerAvatarBitmap;

    private boolean hasCenteredOnPartner = false;

    private int myColor = Color.parseColor("#007AFF");
    private int partnerColor = Color.parseColor("#FF4081");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

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
        mapView.getController().setZoom(17.5);
        mapView.getController().setCenter(defaultPos);

        tvLiveDistanceText = findViewById(R.id.tvLiveDistanceText);
        tvPartnerStatus = findViewById(R.id.tvPartnerStatus);
        tvTimeAtLocation = findViewById(R.id.tvTimeAtLocation);

        btnPoke = findViewById(R.id.btnPoke);
        btnOpenExternalMap = findViewById(R.id.btnOpenExternalMap);

        cardPartnerEdgeWidget = findViewById(R.id.cardPartnerEdgeWidget);
        ivPartnerEdgeAvatar = findViewById(R.id.ivPartnerEdgeAvatar);

        cardMyEdgeWidget = findViewById(R.id.cardMyEdgeWidget);
        ivMyEdgeAvatar = findViewById(R.id.ivMyEdgeAvatar);

        UIUtils.applyBottomMargin(findViewById(R.id.infoCard));

        userId = UserManager.getUserId(this);
        partnerId = UserManager.getPartnerId(this);

        btnPoke.setOnClickListener(v -> sendPokeToFirebase());
        btnOpenExternalMap.setOnClickListener(v -> openPartnerInNativeMap());

        // Snap actions (Silent - no toasts)
        cardPartnerEdgeWidget.setOnClickListener(v -> snapMapToPartner());
        cardMyEdgeWidget.setOnClickListener(v -> snapMapToSelf());

        setupGenderColors();
        loadAvatarsAndStartTracking();

        mapView.addMapListener(new MapListener() {
            @Override
            public boolean onScroll(ScrollEvent event) {
                updateDualEdgeWidgets();
                return false;
            }

            @Override
            public boolean onZoom(ZoomEvent event) {
                updateDualEdgeWidgets();
                return false;
            }
        });
    }

    private void setupGenderColors() {
        RelationshipManager.Details relDetails = RelationshipManager.getLocalDetails(this);
        myColor = getGenderColor(relDetails.yourGender);
        partnerColor = getGenderColor(relDetails.partnerGender);

        cardMyEdgeWidget.setCardBackgroundColor(myColor);
        cardPartnerEdgeWidget.setCardBackgroundColor(partnerColor);
    }

    private int getGenderColor(String gender) {
        if (gender == null) return Color.parseColor("#25D366");
        if (gender.contains("Male") || gender.contains("He/Him")) {
            return Color.parseColor("#007AFF"); // Blue
        } else if (gender.contains("Female") || gender.contains("She/Her")) {
            return Color.parseColor("#FF4081"); // Pink
        } else {
            return Color.parseColor("#25D366"); // Green for prefer not to say / they/them / none
        }
    }

    private void loadAvatarsAndStartTracking() {
        // Load self avatar
        String selfBase64 = getSharedPreferences("prefs", MODE_PRIVATE).getString("profile_pic_base64", null);
        if (selfBase64 != null) {
            myAvatarBitmap = decodeBase64Bitmap(selfBase64);
            if (myAvatarBitmap != null) {
                ivMyEdgeAvatar.setImageBitmap(myAvatarBitmap);
            }
        }

        // Listen for partner avatar
        if (!partnerId.isEmpty()) {
            DatabaseReference db = FirebaseDatabase.getInstance("https://i-must-finish-this-in-2026-default-rtdb.asia-southeast1.firebasedatabase.app/").getReference();
            db.child("profiles").child(partnerId).child("photoBase64").addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    String base64 = snapshot.getValue(String.class);
                    if (base64 != null) {
                        partnerAvatarBitmap = decodeBase64Bitmap(base64);
                        if (partnerAvatarBitmap != null) {
                            ivPartnerEdgeAvatar.setImageBitmap(partnerAvatarBitmap);
                            updatePartnerMarkerOnMap();
                        }
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {}
            });
        }

        tracker = new LocationTracker(this, userId, partnerId);
        tracker.startTracking();

        // 1. Listen to Self Location
        tracker.listenToSelf((lat, lon) -> {
            lastSelfLat = lat;
            lastSelfLon = lon;
            updateSelfMarkerOnMap();
            updateDualEdgeWidgets();
        });

        // 2. Listen to Partner Location
        tracker.listenToPartner((lat, lon, distance) -> {
            if (lat == 0 && lon == 0) return;

            lastPartnerLat = lat;
            lastPartnerLon = lon;

            // Center on Partner FIRST on initial load
            if (!hasCenteredOnPartner && mapView != null) {
                GeoPoint partnerPos = new GeoPoint(lat, lon);
                mapView.getController().setZoom(17.5);
                mapView.getController().setCenter(partnerPos);
                hasCenteredOnPartner = true;
            }

            updatePartnerMarkerOnMap();

            tvTimeAtLocation.setText("Updated: Just now");

            if (distance >= 0) {
                String formattedDistance = UnitUtils.formatDistance(this, distance);
                tvLiveDistanceText.setText(formattedDistance);
                tvPartnerStatus.setText("Partner is " + formattedDistance + " away");
            } else {
                tvLiveDistanceText.setText("Calculating...");
                tvPartnerStatus.setText("Partner location received");
            }

            updateDualEdgeWidgets();
        });
    }

    private void updateSelfMarkerOnMap() {
        if (lastSelfLat == 0 && lastSelfLon == 0) return;
        GeoPoint selfPos = new GeoPoint(lastSelfLat, lastSelfLon);

        if (myMarker == null) {
            myMarker = new Marker(mapView);
            myMarker.setTitle("You 📍");
            myMarker.setInfoWindow(null);
            mapView.getOverlays().add(myMarker);
        }
        myMarker.setInfoWindow(null);
        myMarker.setPosition(selfPos);
        myMarker.setIcon(createAvatarMarkerDrawable(this, myAvatarBitmap, "Y", myColor));
        mapView.invalidate();
    }

    private void updatePartnerMarkerOnMap() {
        if (lastPartnerLat == 0 && lastPartnerLon == 0) return;
        GeoPoint partnerPos = new GeoPoint(lastPartnerLat, lastPartnerLon);

        if (partnerMarker == null) {
            partnerMarker = new Marker(mapView);
            partnerMarker.setTitle("Partner 💖");
            partnerMarker.setInfoWindow(null);
            mapView.getOverlays().add(partnerMarker);
        }
        partnerMarker.setInfoWindow(null);
        partnerMarker.setPosition(partnerPos);
        partnerMarker.setIcon(createAvatarMarkerDrawable(this, partnerAvatarBitmap, "P", partnerColor));
        mapView.invalidate();
    }

    private void updateDualEdgeWidgets() {
        updateEdgeWidgetForPoint(lastPartnerLat, lastPartnerLon, cardPartnerEdgeWidget);
        updateEdgeWidgetForPoint(lastSelfLat, lastSelfLon, cardMyEdgeWidget);
    }

    private void updateEdgeWidgetForPoint(double lat, double lon, MaterialCardView widget) {
        if (lat == 0 && lon == 0 || mapView == null || widget == null) {
            if (widget != null) widget.setVisibility(View.GONE);
            return;
        }

        GeoPoint targetPos = new GeoPoint(lat, lon);

        try {
            boolean isVisibleOnScreen = mapView.getBoundingBox().contains(targetPos);
            if (isVisibleOnScreen) {
                widget.setVisibility(View.GONE);
                return;
            }

            widget.setVisibility(View.VISIBLE);

            Point targetPixel = new Point();
            mapView.getProjection().toPixels(targetPos, targetPixel);

            int viewWidth = mapView.getWidth();
            int viewHeight = mapView.getHeight();

            if (viewWidth <= 0 || viewHeight <= 0) return;

            View cardDistanceHeader = findViewById(R.id.cardDistanceHeader);
            View infoCard = findViewById(R.id.infoCard);

            float widgetW = widget.getWidth() > 0 ? widget.getWidth() : 140f;
            float widgetH = widget.getHeight() > 0 ? widget.getHeight() : 140f;

            float topLimit = cardDistanceHeader != null ? cardDistanceHeader.getBottom() + 16f : 150f;
            float bottomLimit = infoCard != null ? infoCard.getTop() - widgetH - 16f : viewHeight - widgetH - 150f;
            if (bottomLimit <= topLimit) bottomLimit = topLimit + 100f;

            float leftLimit = 24f;
            float rightLimit = viewWidth - widgetW - 24f;
            if (rightLimit <= leftLimit) rightLimit = leftLimit + 100f;

            float targetY = Math.max(topLimit, Math.min((float) targetPixel.y - widgetH / 2f, bottomLimit));
            float targetX = Math.max(leftLimit, Math.min((float) targetPixel.x - widgetW / 2f, rightLimit));

            widget.setTranslationX(targetX);
            widget.setTranslationY(targetY);

        } catch (Exception e) {
            widget.setVisibility(View.GONE);
        }
    }

    private void snapMapToPartner() {
        if (lastPartnerLat == 0 && lastPartnerLon == 0) return;
        GeoPoint partnerPos = new GeoPoint(lastPartnerLat, lastPartnerLon);
        mapView.getController().animateTo(partnerPos);
        mapView.getController().setZoom(17.5);
        cardPartnerEdgeWidget.setVisibility(View.GONE);
    }

    private void snapMapToSelf() {
        if (lastSelfLat == 0 && lastSelfLon == 0) return;
        GeoPoint selfPos = new GeoPoint(lastSelfLat, lastSelfLon);
        mapView.getController().animateTo(selfPos);
        mapView.getController().setZoom(17.5);
        cardMyEdgeWidget.setVisibility(View.GONE);
    }

    private Drawable createAvatarMarkerDrawable(Context context, Bitmap avatarBitmap, String defaultLetter, int borderColor) {
        int size = 160;
        Bitmap markerBitmap = Bitmap.createBitmap(size, size + 28, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(markerBitmap);

        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

        float cx = size / 2f;
        float cy = size / 2f;
        float radius = size / 2f - 8f;

        // Draw pin pointer triangle at bottom
        Path pointerPath = new Path();
        pointerPath.moveTo(cx - 20f, cy + radius - 5f);
        pointerPath.lineTo(cx + 20f, cy + radius - 5f);
        pointerPath.lineTo(cx, size + 24f);
        pointerPath.close();

        paint.setColor(borderColor);
        paint.setStyle(Paint.Style.FILL);
        canvas.drawPath(pointerPath, paint);

        // Draw circular border
        canvas.drawCircle(cx, cy, radius, paint);

        if (avatarBitmap != null) {
            Bitmap scaledAvatar = Bitmap.createScaledBitmap(avatarBitmap, (int) (radius * 2 - 12f), (int) (radius * 2 - 12f), true);
            Paint circlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            circlePaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));

            Bitmap circleBitmap = Bitmap.createBitmap(scaledAvatar.getWidth(), scaledAvatar.getHeight(), Bitmap.Config.ARGB_8888);
            Canvas cCanvas = new Canvas(circleBitmap);
            Paint maskPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            maskPaint.setColor(Color.BLACK);
            cCanvas.drawCircle(scaledAvatar.getWidth() / 2f, scaledAvatar.getHeight() / 2f, scaledAvatar.getWidth() / 2f, maskPaint);
            cCanvas.drawBitmap(scaledAvatar, 0, 0, circlePaint);

            canvas.drawBitmap(circleBitmap, cx - circleBitmap.getWidth() / 2f, cy - circleBitmap.getHeight() / 2f, null);
        } else {
            paint.setColor(Color.WHITE);
            paint.setStyle(Paint.Style.FILL);
            canvas.drawCircle(cx, cy, radius - 6f, paint);

            paint.setColor(borderColor);
            paint.setTextSize(52f);
            paint.setFakeBoldText(true);
            paint.setTextAlign(Paint.Align.CENTER);
            canvas.drawText(defaultLetter, cx, cy + 18f, paint);
        }

        return new BitmapDrawable(context.getResources(), markerBitmap);
    }

    private Bitmap decodeBase64Bitmap(String base64Str) {
        try {
            String clean = base64Str.contains(",") ? base64Str.split(",")[1] : base64Str;
            byte[] bytes = Base64.decode(clean, Base64.DEFAULT);
            return BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
        } catch (Exception e) {
            return null;
        }
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
    protected void onResume() {
        super.onResume();
        if (mapView != null) mapView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mapView != null) mapView.onPause();
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