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
        holder.tvStatus.setText(flight.status);
        holder.tvDep.setText(flight.depAirport);
        holder.tvArr.setText(flight.arrAirport);
        
        String dateStr = DateFormat.format("MMM dd, HH:mm", flight.depTimeMillis).toString();
        holder.tvETA.setText("Departure: " + dateStr);

        // Styling based on status
        if ("LANDED".equals(flight.status)) {
            holder.card.setAlpha(0.6f);
            holder.tvStatus.setBackgroundColor(0xFFEEEEEE);
            holder.tvStatus.setTextColor(0xFF666666);
            holder.btnTrack.setVisibility(View.GONE);
        } else if ("DELAYED".equals(flight.status)) {
            holder.card.setAlpha(1.0f);
            holder.tvStatus.setBackgroundColor(0xFFFFEBEE);
            holder.tvStatus.setTextColor(0xFFD32F2F);
            holder.btnTrack.setVisibility(View.VISIBLE);
        } else {
            holder.card.setAlpha(1.0f);
            holder.tvStatus.setBackgroundColor(0xFFE8F5E9);
            holder.tvStatus.setTextColor(0xFF2E7D32);
            holder.btnTrack.setVisibility(View.VISIBLE);
        }

        holder.btnTrack.setOnClickListener(v -> {
            Intent intent = new Intent(v.getContext(), FlightMapActivity.class);
            intent.putExtra("flight_id", flight.id);
            v.getContext().startActivity(intent);
        });

        holder.btnRefresh.setOnClickListener(v -> {
            Toast.makeText(v.getContext(), "Refreshing status...", Toast.LENGTH_SHORT).show();
            apiHelper.getFlightStatus(flight.flightNumber, new AviationStackHelper.FlightCallback() {
                @Override
                public void onSuccess(JsonObject data) {
                    String status = data.get("flight_status").getAsString().toUpperCase();
                    FirebaseDatabase.getInstance("https://i-must-finish-this-in-2026-default-rtdb.asia-southeast1.firebasedatabase.app/")
                            .getReference("flights").child(flight.id).child("status").setValue(status);
                    Toast.makeText(v.getContext(), "Status Updated: " + status, Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onError(String message) {
                    Toast.makeText(v.getContext(), "Error: " + message, Toast.LENGTH_SHORT).show();
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
            btnRefresh = itemView.findViewById(R.id.btnRefreshStatus); // New ID
            card = itemView.findViewById(R.id.flightCard);
        }
    }
}