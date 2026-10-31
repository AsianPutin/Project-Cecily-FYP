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
    private final DatabaseReference dbRef = FirebaseDatabase.getInstance("https://i-must-finish-this-in-2026-default-rtdb.asia-southeast1.firebasedatabase.app/").getReference("flight_history");

    public FlightWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        dbRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot userSnap : snapshot.getChildren()) {
                    for (DataSnapshot flightSnap : userSnap.getChildren()) {
                        FlightInfo flight = flightSnap.getValue(FlightInfo.class);
                        if (flight != null && !"LANDED".equals(flight.status)) {
                            updateFlightStatus(flight);
                        }
                    }
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });

        return Result.success();
    }

    private void updateFlightStatus(FlightInfo flight) {
        long now = System.currentTimeMillis();
        long diff = flight.depTimeMillis - now;

        if (diff > -12 * 60 * 60 * 1000L && diff <= 3 * 24 * 60 * 60 * 1000L) {
            String dateYmd = android.text.format.DateFormat.format("yyyy-MM-dd", flight.depTimeMillis).toString();
            apiHelper.getFlightStatus(flight.flightNumber, dateYmd, new AviationStackHelper.FlightCallback() {
                @Override
                public void onSuccess(JsonObject data) {
                    try {
                        String status = data.has("flight_status") && !data.get("flight_status").isJsonNull() ? data.get("flight_status").getAsString().toUpperCase() : flight.status;

                        JsonObject depObj = data.has("departure") && !data.get("departure").isJsonNull() ? data.getAsJsonObject("departure") : null;
                        JsonObject arrObj = data.has("arrival") && !data.get("arrival").isJsonNull() ? data.getAsJsonObject("arrival") : null;

                        DatabaseReference userFlightRef = dbRef.child(flight.ownerId).child(flight.id);
                        userFlightRef.child("status").setValue(status);

                        if (depObj != null) {
                            if (depObj.has("terminal") && !depObj.get("terminal").isJsonNull()) {
                                userFlightRef.child("depTerminal").setValue(depObj.get("terminal").getAsString());
                            }
                            if (depObj.has("gate") && !depObj.get("gate").isJsonNull()) {
                                userFlightRef.child("depGate").setValue(depObj.get("gate").getAsString());
                            }
                        }

                        if (arrObj != null) {
                            if (arrObj.has("terminal") && !arrObj.get("terminal").isJsonNull()) {
                                userFlightRef.child("arrTerminal").setValue(arrObj.get("terminal").getAsString());
                            }
                            if (arrObj.has("gate") && !arrObj.get("gate").isJsonNull()) {
                                userFlightRef.child("arrGate").setValue(arrObj.get("gate").getAsString());
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
