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
    private final DatabaseReference rootRef = FirebaseDatabase.getInstance("https://i-must-finish-this-in-2026-default-rtdb.asia-southeast1.firebasedatabase.app/").getReference();

    public FlightWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        // Query shared couple flights node `/flights`
        rootRef.child("flights").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot pairSnap : snapshot.getChildren()) {
                    String pairKey = pairSnap.getKey();
                    if (pairKey == null) continue;

                    for (DataSnapshot flightSnap : pairSnap.getChildren()) {
                        String flightId = flightSnap.getKey();
                        if (flightId == null) continue;

                        FlightInfo flight = flightSnap.getValue(FlightInfo.class);
                        Long lastCheck = flightSnap.child("lastApiCheckTs").getValue(Long.class);
                        long lastCheckTs = (lastCheck != null) ? lastCheck : 0L;

                        if (flight != null && !"LANDED".equalsIgnoreCase(flight.status)) {
                            updateFlightStatus(rootRef.child("flights").child(pairKey).child(flightId), flight, lastCheckTs);
                        }
                    }
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });

        return Result.success();
    }

    private void updateFlightStatus(DatabaseReference flightRef, FlightInfo flight, long lastCheckTs) {
        long now = System.currentTimeMillis();
        long diff = flight.depTimeMillis - now;

        // 12-Hour Cache Cooldown Protection: Only check if last check was >= 12 hours ago AND departing within 24 hours
        if (now - lastCheckTs < 12 * 60 * 60 * 1000L) {
            return; // Skip API query, cache is still fresh!
        }

        if (diff > -12 * 60 * 60 * 1000L && diff <= 24 * 60 * 60 * 1000L) {
            String dateYmd = android.text.format.DateFormat.format("yyyy-MM-dd", flight.depTimeMillis).toString();
            apiHelper.getFlightStatus(flight.flightNumber, dateYmd, new AviationStackHelper.FlightCallback() {
                @Override
                public void onSuccess(JsonObject data) {
                    try {
                        String status = data.has("flight_status") && !data.get("flight_status").isJsonNull() ? data.get("flight_status").getAsString().toUpperCase() : flight.status;

                        JsonObject depObj = data.has("departure") && !data.get("departure").isJsonNull() ? data.getAsJsonObject("departure") : null;
                        JsonObject arrObj = data.has("arrival") && !data.get("arrival").isJsonNull() ? data.getAsJsonObject("arrival") : null;

                        flightRef.child("status").setValue(status);
                        flightRef.child("lastApiCheckTs").setValue(now);

                        if (depObj != null) {
                            if (depObj.has("terminal") && !depObj.get("terminal").isJsonNull()) {
                                flightRef.child("depTerminal").setValue(depObj.get("terminal").getAsString());
                            }
                            if (depObj.has("gate") && !depObj.get("gate").isJsonNull()) {
                                flightRef.child("depGate").setValue(depObj.get("gate").getAsString());
                            }
                        }

                        if (arrObj != null) {
                            if (arrObj.has("terminal") && !arrObj.get("terminal").isJsonNull()) {
                                flightRef.child("arrTerminal").setValue(arrObj.get("terminal").getAsString());
                            }
                            if (arrObj.has("gate") && !arrObj.get("gate").isJsonNull()) {
                                flightRef.child("arrGate").setValue(arrObj.get("gate").getAsString());
                            }
                        }
                    } catch (Exception ignored) {}
                }

                @Override
                public void onError(String message) {}
            });
        }
    }
}
