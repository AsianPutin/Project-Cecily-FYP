package com.app.projectcecily;

import android.content.Intent;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.card.MaterialCardView;
import java.util.List;

public class FlightAdapter extends RecyclerView.Adapter<FlightAdapter.ViewHolder> {

    private final List<FlightInfo> flights;
    private final String currentUserId;
    private OnFlightActionListener listener;

    public interface OnFlightActionListener {
        void onEdit(FlightInfo flight);
        void onDelete(FlightInfo flight);
    }

    public FlightAdapter(List<FlightInfo> flights, String currentUserId, OnFlightActionListener listener) {
        this.flights = flights;
        this.currentUserId = currentUserId;
        this.listener = listener;
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
        holder.tvFlightNum.setText((flight.airlineName != null && !flight.airlineName.trim().isEmpty() ? flight.airlineName + " " : "") + flight.flightNumber);
        holder.tvDep.setText(flight.depAirport);
        holder.tvArr.setText(flight.arrAirport);
        
        String dateStr = DateFormat.format("MMM dd, HH:mm", flight.depTimeMillis).toString();
        holder.tvETA.setText("Departure: " + dateStr);

        String displayStatus;
        int bgColor;
        int textColor;
        float cardAlpha = 1.0f;

        long now = System.currentTimeMillis();
        long depTime = flight.depTimeMillis;
        long arrTime = depTime + (110 * 60 * 1000); // 1h 50m duration

        if (now > arrTime + (30 * 60 * 1000)) { 
            displayStatus = "LANDED";
            bgColor = 0xFFE8F5E9; // Green
            textColor = 0xFF2E7D32;
            cardAlpha = 0.5f; // Greyed out for landed flights
        } else if (now > depTime) {
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

        if ("CANCELLED".equalsIgnoreCase(flight.status)) {
            displayStatus = "CANCELLED";
            bgColor = 0xFFFFEBEE;
            textColor = 0xFFD32F2F;
        }

        holder.tvStatus.setText(displayStatus);
        holder.tvStatus.setBackgroundColor(bgColor);
        holder.tvStatus.setTextColor(textColor);
        holder.card.setAlpha(cardAlpha);

        int accentColor = ThemeUtils.getAccentColor(holder.itemView.getContext());
        if ("FLYING".equalsIgnoreCase(displayStatus) || "ARRIVING".equalsIgnoreCase(displayStatus)) {
            holder.card.setStrokeColor(accentColor);
            int strokePx = (int) (2 * holder.itemView.getContext().getResources().getDisplayMetrics().density);
            holder.card.setStrokeWidth(strokePx);
        } else {
            holder.card.setStrokeWidth(0);
        }

        holder.btnTrack.setVisibility("LANDED".equals(displayStatus) ? View.GONE : View.VISIBLE);

        holder.btnTrack.setOnClickListener(v -> {
            Intent intent = new Intent(v.getContext(), FlightMapActivity.class);
            intent.putExtra("flight_id", flight.id);
            v.getContext().startActivity(intent);
        });

        holder.btnEdit.setOnClickListener(v -> {
            if (listener != null) listener.onEdit(flight);
        });

        holder.btnDelete.setOnClickListener(v -> {
            if (listener != null) listener.onDelete(flight);
        });
    }

    @Override
    public int getItemCount() { return flights.size(); }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvFlightNum, tvStatus, tvDep, tvArr, tvETA;
        Button btnTrack, btnEdit, btnDelete;
        MaterialCardView card;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvFlightNum = itemView.findViewById(R.id.tvFlightNumber);
            tvStatus = itemView.findViewById(R.id.tvFlightStatus);
            tvDep = itemView.findViewById(R.id.tvDepAirport);
            tvArr = itemView.findViewById(R.id.tvArrAirport);
            tvETA = itemView.findViewById(R.id.tvFlightETA);
            btnTrack = itemView.findViewById(R.id.btnTrackLive);
            btnEdit = itemView.findViewById(R.id.btnEditFlight);
            btnDelete = itemView.findViewById(R.id.btnDeleteFlight);
            card = itemView.findViewById(R.id.flightCard);
        }
    }
}
