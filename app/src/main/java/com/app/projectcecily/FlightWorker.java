package com.app.projectcecily;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.gson.JsonObject;

public class FlightWorker extends Worker {

    private final AviationStackHelper apiHelper = new AviationStackHelper();
    private final DatabaseReference dbRef = FirebaseDatabase.getInstance("https://i-must-finish-this-in-2026-default-rtdb.asia-southeast1.firebasedatabase.app/").getReference("flights");

    public FlightWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        dbRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot ds : snapshot.getChildren()) {
                    FlightInfo flight = ds.getValue(FlightInfo.class);
                    if (flight != null && !"LANDED".equals(flight.status)) {
                        updateFlightStatus(flight);
                    }
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });

        return Result.success();
    }

    private void updateFlightStatus(FlightInfo flight) {
        apiHelper.getFlightStatus(flight.flightNumber, new AviationStackHelper.FlightCallback() {
            @Override
            public void onSuccess(JsonObject data) {
                String status = data.get("flight_status").getAsString().toUpperCase();
                // Update Both Active and History nodes
                dbRef.child(flight.id).child("status").setValue(status);
                FirebaseDatabase.getInstance("https://i-must-finish-this-in-2026-default-rtdb.asia-southeast1.firebasedatabase.app/")
                        .getReference("flight_history").child(flight.ownerId).child(flight.id).child("status").setValue(status);
                
                // If landed, we could remove from active, but for now we keep it for visibility
            }

            @Override
            public void onError(String message) {}
        });
    }
}