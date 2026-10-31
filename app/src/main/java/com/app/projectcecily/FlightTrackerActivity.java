package com.app.projectcecily;

import android.app.AlarmManager;
import android.app.DatePickerDialog;
import android.app.PendingIntent;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.gson.JsonObject;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FlightTrackerActivity extends AppCompatActivity {

    private FlightAdapter adapter;
    private List<FlightInfo> flightList = new ArrayList<>();
    private DatabaseReference activeRef, historyRef;
    private String userId;
    private Calendar selectedDateTime = Calendar.getInstance();
    private AviationStackHelper apiHelper = new AviationStackHelper();
    
    private TextInputEditText currentEtFlight;

    private final ActivityResultLauncher<Intent> ocrLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Uri imageUri = result.getData().getData();
                    performOcr(imageUri);
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_flight);

        userId = UserManager.getUserId(this);
        activeRef = FirebaseDatabase.getInstance("https://i-must-finish-this-in-2026-default-rtdb.asia-southeast1.firebasedatabase.app/").getReference("flight_history").child(userId);
        historyRef = activeRef;

        checkExactAlarmPermission();

        View appBarView = findViewById(R.id.appBarLayout);
        UIUtils.applyTopInsetToAppBar(appBarView);

        View toolbarView = findViewById(R.id.toolbar);
        if (toolbarView instanceof com.google.android.material.appbar.MaterialToolbar) {
            setSupportActionBar((com.google.android.material.appbar.MaterialToolbar) toolbarView);
            if (getSupportActionBar() != null) {
                getSupportActionBar().setDisplayHomeAsUpEnabled(true);
                getSupportActionBar().setTitle("Flight Tracker");
            }
        }

        RecyclerView rv = findViewById(R.id.rvFlights);
        rv.setLayoutManager(new LinearLayoutManager(this));
        adapter = new FlightAdapter(flightList, userId);
        rv.setAdapter(adapter);

        View fab = findViewById(R.id.fabAddFlight);
        fab.setOnClickListener(v -> showAddFlightDialog());
        
        UIUtils.applyBottomMargin(fab);
        UIUtils.applyBottomPadding(rv);
        UIUtils.applyTopInsetPadding(rv);

        listenForFlights();
    }

    private void checkExactAlarmPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            AlarmManager am = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
            if (am != null && !am.canScheduleExactAlarms()) {
                startActivity(new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM));
            }
        }
    }

    private void listenForFlights() {
        // Combined view: Active flights first, then history
        activeRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                flightList.clear();
                for (DataSnapshot ds : snapshot.getChildren()) {
                    FlightInfo info = ds.getValue(FlightInfo.class);
                    if (info != null) flightList.add(info);
                }
                loadHistory();
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void loadHistory() {
        historyRef.limitToLast(5).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot ds : snapshot.getChildren()) {
                    FlightInfo info = ds.getValue(FlightInfo.class);
                    if (info != null) {
                        boolean exists = false;
                        for (FlightInfo f : flightList) if (f.id.equals(info.id)) exists = true;
                        if (!exists) flightList.add(info);
                    }
                }
                adapter.notifyDataSetChanged();
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void showAddFlightDialog() {
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_add_flight, null);
        currentEtFlight = view.findViewById(R.id.etFlightNum);
        TextInputEditText etDate = view.findViewById(R.id.etFlightDate);

        view.findViewById(R.id.btnScanTicket).setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            ocrLauncher.launch(intent);
        });

        etDate.setOnClickListener(v -> {
            Calendar now = Calendar.getInstance();
            new DatePickerDialog(this, (view1, year, month, dayOfMonth) -> {
                selectedDateTime.set(Calendar.YEAR, year);
                selectedDateTime.set(Calendar.MONTH, month);
                selectedDateTime.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                new TimePickerDialog(this, (view2, h, m) -> {
                    selectedDateTime.set(Calendar.HOUR_OF_DAY, h);
                    selectedDateTime.set(Calendar.MINUTE, m);
                    etDate.setText(android.text.format.DateFormat.format("MMM dd, HH:mm", selectedDateTime));
                }, 12, 0, true).show();
            }, now.get(Calendar.YEAR), now.get(Calendar.MONTH), now.get(Calendar.DAY_OF_MONTH)).show();
        });

        new MaterialAlertDialogBuilder(this)
                .setTitle("Add Flight")
                .setView(view)
                .setPositiveButton("Auto-Fetch & Add", (dialog, which) -> {
                    String fn = currentEtFlight.getText().toString().toUpperCase().replace(" ", "");
                    if (!fn.isEmpty()) {
                        fetchAndSaveFlight(fn);
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void fetchAndSaveFlight(String flightNum) {
        Toast.makeText(this, "Fetching route details...", Toast.LENGTH_SHORT).show();
        apiHelper.getFlightStatus(flightNum, new AviationStackHelper.FlightCallback() {
            @Override
            public void onSuccess(JsonObject data) {
                try {
                    JsonObject depObj = data.has("departure") && !data.get("departure").isJsonNull() ? data.getAsJsonObject("departure") : new JsonObject();
                    JsonObject arrObj = data.has("arrival") && !data.get("arrival").isJsonNull() ? data.getAsJsonObject("arrival") : new JsonObject();
                    JsonObject airlineObj = data.has("airline") && !data.get("airline").isJsonNull() ? data.getAsJsonObject("airline") : new JsonObject();

                    String depIata = depObj.has("iata") && !depObj.get("iata").isJsonNull() ? depObj.get("iata").getAsString() : "KUL";
                    String arrIata = arrObj.has("iata") && !arrObj.get("iata").isJsonNull() ? arrObj.get("iata").getAsString() : "KCH";

                    String depAirportName = depObj.has("airport") && !depObj.get("airport").isJsonNull() ? depObj.get("airport").getAsString() : "Kuala Lumpur";
                    String arrAirportName = arrObj.has("airport") && !arrObj.get("airport").isJsonNull() ? arrObj.get("airport").getAsString() : "Kuching";

                    String status = data.has("flight_status") && !data.get("flight_status").isJsonNull() ? data.get("flight_status").getAsString().toUpperCase() : "IN FLIGHT";
                    String airline = airlineObj.has("name") && !airlineObj.get("name").isJsonNull() ? airlineObj.get("name").getAsString() : "AirAsia";

                    String depTerm = depObj.has("terminal") && !depObj.get("terminal").isJsonNull() ? depObj.get("terminal").getAsString() : "2";
                    String depGate = depObj.has("gate") && !depObj.get("gate").isJsonNull() ? depObj.get("gate").getAsString() : "J10";
                    String arrTerm = arrObj.has("terminal") && !arrObj.get("terminal").isJsonNull() ? arrObj.get("terminal").getAsString() : "-";
                    String arrGate = arrObj.has("gate") && !arrObj.get("gate").isJsonNull() ? arrObj.get("gate").getAsString() : "-";

                    String id = activeRef.push().getKey();
                    FlightInfo flight = new FlightInfo(id, flightNum, depIata, arrIata, selectedDateTime.getTimeInMillis(), userId);
                    flight.airlineName = airline;
                    flight.depCity = depAirportName;
                    flight.arrCity = arrAirportName;
                    flight.status = status;
                    flight.depTerminal = depTerm;
                    flight.depGate = depGate;
                    flight.arrTerminal = arrTerm;
                    flight.arrGate = arrGate;

                    activeRef.child(id).setValue(flight);
                    historyRef.child(id).setValue(flight);
                    scheduleReminders(flight);

                    Toast.makeText(FlightTrackerActivity.this, "Flight Added: " + depIata + " -> " + arrIata, Toast.LENGTH_LONG).show();
                } catch (Exception e) {
                    saveFallback(flightNum);
                }
            }

            @Override
            public void onError(String message) {
                saveFallback(flightNum);
            }
        });
    }

    private void saveFallback(String flightNum) {
        // If API fails or data is missing, save with placeholders so user can edit or just see the number
        String id = activeRef.push().getKey();
        FlightInfo flight = new FlightInfo(id, flightNum, "???", "???", selectedDateTime.getTimeInMillis(), userId);
        activeRef.child(id).setValue(flight);
        historyRef.child(id).setValue(flight);
        Toast.makeText(this, "Route info unavailable. Added flight number only.", Toast.LENGTH_SHORT).show();
    }

    private void performOcr(Uri uri) {
        try {
            InputImage image = InputImage.fromFilePath(this, uri);
            TextRecognizer recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);
            recognizer.process(image).addOnSuccessListener(text -> {
                Pattern p = Pattern.compile("([A-Z]{2,3})\\s?(\\d{3,4})");
                Matcher m = p.matcher(text.getText());
                if (m.find()) currentEtFlight.setText(m.group(0));
            });
        } catch (Exception ignored) {}
    }

    private void scheduleReminders(FlightInfo flight) {
        long dep = flight.depTimeMillis;
        scheduleReminder(flight.flightNumber, dep - (7L * 24 * 60 * 60 * 1000), "1 week until takeoff!", 1);
        scheduleReminder(flight.flightNumber, dep - (3L * 24 * 60 * 60 * 1000), "3 days until takeoff!", 2);
        scheduleReminder(flight.flightNumber, dep - (24 * 60 * 60 * 1000), "Tomorrow is the big day!", 3);
    }

    private void scheduleReminder(String fn, long time, String msg, int offset) {
        if (time < System.currentTimeMillis()) return;
        AlarmManager am = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(this, FlightReminderReceiver.class);
        intent.putExtra("flight_num", fn);
        intent.putExtra("message", msg);
        PendingIntent pi = PendingIntent.getBroadcast(this, (int)System.currentTimeMillis() + offset, intent, PendingIntent.FLAG_IMMUTABLE);
        if (am != null) am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, time, pi);
    }

    @Override
    protected void onResume() {
        super.onResume();
        ThemeUtils.applyAccentToActivity(this);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) { finish(); return true; }
        return super.onOptionsItemSelected(item);
    }
}