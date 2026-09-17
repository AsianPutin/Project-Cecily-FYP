package com.app.projectcecily;

import android.content.Intent;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.database.FirebaseDatabase;
import com.google.gson.JsonObject;
import java.util.List;

public class FlightAdapter extends RecyclerView.Adapter<FlightAdapter.ViewHolder> {

    private final List<FlightInfo> flights;
    private final String currentUserId;
    private final AviationStackHelper apiHelper = new AviationStackHelper();

    public FlightAdapter(List<FlightInfo> flights, String currentUserId) {
        this.flights = flights;
        this.currentUserId = currentUserId;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_flight, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        FlightInfo flight = flights.get(position);
        holder.tvFlightNum.setText(flight.flightNumber);
        holder.tvDep.setText(flight.depAirport);
        holder.tvArr.setText(flight.arrAirport);
        
        String dateStr = DateFormat.format("MMM dd, HH:mm", flight.depTimeMillis).toString();
        holder.tvETA.setText("Departure: " + dateStr);

        // --- HARD LOGIC OVERRIDE ---
        // We ignore the string in Firebase and calculate status LIVE based on time
        String displayStatus;
        int bgColor;
        int textColor;
        float cardAlpha = 1.0f;

        long now = System.currentTimeMillis();
        long depTime = flight.depTimeMillis;
        long arrTime = depTime + (110 * 60 * 1000); // 1h 50m duration

        if (now > arrTime + (30 * 60 * 1000)) { 
            // Truly Landed (30 mins after expected arrival)
            displayStatus = "LANDED";
            bgColor = 0xFFE8F5E9; // Green
            textColor = 0xFF2E7D32;
            cardAlpha = 0.6f;
        } else if (now > depTime) {
            // In the air
            long timeUntilArrival = arrTime - now;
            if (timeUntilArrival < (45 * 60 * 1000)) {
                displayStatus = "ARRIVING";
                bgColor = 0xFFFFF9C4; // Yellow
                textColor = 0xFF856404;
            } else {
                displayStatus = "FLYING";
                bgColor = 0xFFEEEEEE; // Grey
                textColor = 0xFF666666;
            }
        } else {
            // Future flight
            long timeUntilDep = depTime - now;
            if (timeUntilDep < (60 * 60 * 1000)) {
                displayStatus = "DEPARTING";
                bgColor = 0xFFFFF9C4; // Yellow
                textColor = 0xFF856404;
            } else {
                displayStatus = "AWAITING";
                bgColor = 0xFFEEEEEE; // Grey
                textColor = 0xFF666666;
            }
        }

        // Emergency Override for API specific errors
        if ("CANCELLED".equalsIgnoreCase(flight.status)) {
            displayStatus = "CANCELLED";
            bgColor = 0xFFFFEBEE;
            textColor = 0xFFD32F2F;
        }

        holder.tvStatus.setText(displayStatus);
        holder.tvStatus.setBackgroundColor(bgColor);
        holder.tvStatus.setTextColor(textColor);
        holder.card.setAlpha(cardAlpha);

        holder.btnTrack.setVisibility("LANDED".equals(displayStatus) ? View.GONE : View.VISIBLE);

        holder.btnTrack.setOnClickListener(v -> {
            Intent intent = new Intent(v.getContext(), FlightMapActivity.class);
            intent.putExtra("flight_id", flight.id);
            v.getContext().startActivity(intent);
        });

        holder.btnRefresh.setOnClickListener(v -> {
            Toast.makeText(v.getContext(), "Refreshing live data...", Toast.LENGTH_SHORT).show();
            apiHelper.getFlightStatus(flight.flightNumber, new AviationStackHelper.FlightCallback() {
                @Override
                public void onSuccess(JsonObject data) {
                    String apiStatus = data.get("flight_status").getAsString().toUpperCase();
                    FirebaseDatabase.getInstance("https://i-must-finish-this-in-2026-default-rtdb.asia-southeast1.firebasedatabase.app/")
                            .getReference("flights").child(flight.id).child("status").setValue(apiStatus);
                }
                @Override
                public void onError(String message) {
                    Toast.makeText(v.getContext(), "API: " + message, Toast.LENGTH_SHORT).show();
                }
            });
        });
    }

    @Override
    public int getItemCount() { return flights.size(); }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvFlightNum, tvStatus, tvDep, tvArr, tvETA;
        Button btnTrack, btnRefresh;
        MaterialCardView card;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvFlightNum = itemView.findViewById(R.id.tvFlightNumber);
            tvStatus = itemView.findViewById(R.id.tvFlightStatus);
            tvDep = itemView.findViewById(R.id.tvDepAirport);
            tvArr = itemView.findViewById(R.id.tvArrAirport);
            tvETA = itemView.findViewById(R.id.tvFlightETA);
            btnTrack = itemView.findViewById(R.id.btnTrackLive);
            btnRefresh = itemView.findViewById(R.id.btnRefreshStatus);
            card = itemView.findViewById(R.id.flightCard);
        }
    }
}