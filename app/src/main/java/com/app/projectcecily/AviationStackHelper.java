package com.app.projectcecily;

import android.os.Handler;
import android.os.Looper;
import androidx.annotation.NonNull;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.io.IOException;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class AviationStackHelper {

    private static final String API_KEY = "f6f4ad8ea653ae0dff64825ed7c5d9ee"; 
    private static final String BASE_URL = "https://api.aviationstack.com/v1/flights";
    private final OkHttpClient client = new OkHttpClient();
    private final Gson gson = new Gson();

    public interface FlightCallback {
        void onSuccess(JsonObject flightData);
        void onError(String message);
    }

    public void getFlightStatus(String flightIata, FlightCallback callback) {
        String url = BASE_URL + "?access_key=" + API_KEY + "&flight_iata=" + flightIata;
        
        Request request = new Request.Builder().url(url).build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                new Handler(Looper.getMainLooper()).post(() -> callback.onError(e.getMessage()));
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response.isSuccessful() && response.body() != null) {
                    String jsonData = response.body().string();
                    JsonObject root = gson.fromJson(jsonData, JsonObject.class);
                    JsonArray data = root.getAsJsonArray("data");
                    
                    if (data != null && data.size() > 0) {
                        JsonObject flight = data.get(0).getAsJsonObject();
                        new Handler(Looper.getMainLooper()).post(() -> callback.onSuccess(flight));
                    } else {
                        new Handler(Looper.getMainLooper()).post(() -> callback.onError("No flight data found"));
                    }
                } else {
                    new Handler(Looper.getMainLooper()).post(() -> callback.onError("API Error: " + response.code()));
                }
            }
        });
    }
}