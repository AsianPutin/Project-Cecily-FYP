package com.app.projectcecily;

import android.annotation.SuppressLint;
import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import androidx.annotation.NonNull;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import java.util.HashMap;
import java.util.Map;

public class LocationTracker {

    private LocationManager locationManager;
    private DatabaseReference databaseReference;
    private String userId;
    private String partnerId;
    
    public interface SelfLocationListener {
        void onSelfLocationUpdate(double lat, double lon);
    }

    public interface PartnerLocationListener {
        void onPartnerLocationUpdate(double lat, double lon, float distance);
    }

    public void listenToSelf(SelfLocationListener listener) {
        databaseReference.child(userId).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    Double lat = snapshot.child("latitude").getValue(Double.class);
                    Double lon = snapshot.child("longitude").getValue(Double.class);
                    if (lat != null && lon != null && Math.abs(lat) > 0.1) {
                        listener.onSelfLocationUpdate(lat, lon);
                    }
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    public LocationTracker(Context context, String userId, String partnerId) {
        this.userId = userId;
        this.partnerId = partnerId;
        this.locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        this.databaseReference = FirebaseDatabase.getInstance("https://i-must-finish-this-in-2026-default-rtdb.asia-southeast1.firebasedatabase.app/").getReference("locations");
    }

    @SuppressLint("MissingPermission")
    public void startTracking() {
        Location last = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
        if (last != null && Math.abs(last.getLatitude()) > 0.1) {
            updateLocationInFirebase(last.getLatitude(), last.getLongitude());
        }

        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 5000, 10, new LocationListener() {
            @Override
            public void onLocationChanged(@NonNull Location location) {
                if (Math.abs(location.getLatitude()) > 0.1) {
                    updateLocationInFirebase(location.getLatitude(), location.getLongitude());
                }
            }
        });
    }

    public void listenToPartner(PartnerLocationListener listener) {
        databaseReference.child(partnerId).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    Double lat = snapshot.child("latitude").getValue(Double.class);
                    Double lon = snapshot.child("longitude").getValue(Double.class);
                    // FILTER invalid data (0,0)
                    if (lat != null && lon != null && Math.abs(lat) > 0.1) {
                        calculateDistanceAndNotify(lat, lon, listener);
                    }
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void updateLocationInFirebase(double lat, double lon) {
        Map<String, Object> locationMap = new HashMap<>();
        locationMap.put("latitude", lat);
        locationMap.put("longitude", lon);
        locationMap.put("timestamp", System.currentTimeMillis());
        databaseReference.child(userId).setValue(locationMap);
    }

    @SuppressLint("MissingPermission")
    private void calculateDistanceAndNotify(double pLat, double pLon, PartnerLocationListener listener) {
        Location myLoc = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        if (myLoc == null) myLoc = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);

        if (myLoc != null && Math.abs(myLoc.getLatitude()) > 0.1) {
            float[] results = new float[1];
            Location.distanceBetween(myLoc.getLatitude(), myLoc.getLongitude(), pLat, pLon, results);
            listener.onPartnerLocationUpdate(pLat, pLon, results[0] / 1000);
        } else {
            listener.onPartnerLocationUpdate(pLat, pLon, -1);
        }
    }
}