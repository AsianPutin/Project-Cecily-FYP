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

    private static final String[] API_KEYS = {
        "f6f4ad8ea653ae0dff64825ed7c5d9ee", // Primary Key 1
        "d9a720842e1260de3eee3c259c5f4b52"  // Secondary Key 2 (Alt Account)
    };

    private static int activeKeyIndex = 0;
    private static final String BASE_URL = "https://api.aviationstack.com/v1/flights";
    private final OkHttpClient client = new OkHttpClient();
    private final Gson gson = new Gson();

    public interface FlightCallback {
        void onSuccess(JsonObject flightData);
        void onError(String message);
    }

    public void getFlightStatus(String flightIata, FlightCallback callback) {
        getFlightStatus(flightIata, null, callback);
    }

    public void getFlightStatus(String flightIata, String flightDateYmd, FlightCallback callback) {
        executeRequestWithFailover(flightIata, flightDateYmd, 0, callback);
    }

    private void executeRequestWithFailover(String flightIata, String flightDateYmd, int attemptCount, FlightCallback callback) {
        if (attemptCount >= API_KEYS.length) {
            new Handler(Looper.getMainLooper()).post(() -> callback.onError("All API keys exhausted or rate-limited."));
            return;
        }

        String apiKey = API_KEYS[activeKeyIndex];
        String url = BASE_URL + "?access_key=" + apiKey + "&flight_iata=" + flightIata;
        if (flightDateYmd != null && !flightDateYmd.trim().isEmpty()) {
            url += "&flight_date=" + flightDateYmd.trim();
        }

        Request request = new Request.Builder().url(url).build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                // Try next API key on failure
                activeKeyIndex = (activeKeyIndex + 1) % API_KEYS.length;
                executeRequestWithFailover(flightIata, flightDateYmd, attemptCount + 1, callback);
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response.isSuccessful() && response.body() != null) {
                    String jsonData = response.body().string();
                    try {
                        JsonObject root = gson.fromJson(jsonData, JsonObject.class);

                        // Check if AviationStack returned an error payload inside HTTP 200 (e.g. usage_limit_reached)
                        if (root != null && root.has("error") && !root.get("error").isJsonNull()) {
                            JsonObject errObj = root.getAsJsonObject("error");
                            String code = errObj.has("code") ? errObj.get("code").getAsString() : "";
                            if ("usage_limit_reached".equalsIgnoreCase(code) || "rate_limit_reached".equalsIgnoreCase(code) || errObj.has("info")) {
                                // Quota limit reached! Rotate to next API key and retry automatically!
                                activeKeyIndex = (activeKeyIndex + 1) % API_KEYS.length;
                                executeRequestWithFailover(flightIata, flightDateYmd, attemptCount + 1, callback);
                                return;
                            }
                        }

                        JsonArray data = (root != null && root.has("data") && !root.get("data").isJsonNull()) ? root.getAsJsonArray("data") : null;
                        if (data != null && data.size() > 0) {
                            JsonObject flight = data.get(0).getAsJsonObject();
                            new Handler(Looper.getMainLooper()).post(() -> callback.onSuccess(flight));
                        } else {
                            new Handler(Looper.getMainLooper()).post(() -> callback.onError("No flight data found"));
                        }
                    } catch (Exception e) {
                        new Handler(Looper.getMainLooper()).post(() -> callback.onError("JSON parse error: " + e.getMessage()));
                    }
                } else if (response.code() == 429 || response.code() == 403) {
                    // HTTP 429 / 403 Rate limit -> Rotate key and retry!
                    activeKeyIndex = (activeKeyIndex + 1) % API_KEYS.length;
                    executeRequestWithFailover(flightIata, flightDateYmd, attemptCount + 1, callback);
                } else {
                    new Handler(Looper.getMainLooper()).post(() -> callback.onError("API Error: " + response.code()));
                }
            }
        });
    }
}
