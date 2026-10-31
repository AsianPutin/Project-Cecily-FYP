package com.app.projectcecily;

import android.app.AlarmManager;
import android.app.DatePickerDialog;
import android.app.PendingIntent;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.pdf.PdfRenderer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
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
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FlightTrackerActivity extends AppCompatActivity {

    private static class ParsedFlightData {
        String flightNumber;
        long timeMillis;

        ParsedFlightData(String flightNumber, long timeMillis) {
            this.flightNumber = flightNumber;
            this.timeMillis = timeMillis;
        }
    }

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

    private final ActivityResultLauncher<Intent> cameraLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK) {
                    if (result.getData() != null && result.getData().getData() != null) {
                        performOcr(result.getData().getData());
                    } else if (result.getData() != null && result.getData().getExtras() != null) {
                        Bitmap photo = (Bitmap) result.getData().getExtras().get("data");
                        if (photo != null) performOcrBitmap(photo);
                    }
                }
            }
    );

    private final ActivityResultLauncher<String> pdfPickerLauncher = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                if (uri != null) {
                    processPdfItinerary(uri);
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

        view.findViewById(R.id.btnScanTicket).setOnClickListener(v -> showScanSourceOptions());

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
                        fetchAndSaveFlight(fn, selectedDateTime.getTimeInMillis());
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showScanSourceOptions() {
        String[] options = {"Take Photo (Camera)", "Choose Image (Gallery)", "Upload PDF Itinerary (.pdf)"};
        new MaterialAlertDialogBuilder(this)
                .setTitle("Scan Ticket / Itinerary")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                        cameraLauncher.launch(intent);
                    } else if (which == 1) {
                        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                        ocrLauncher.launch(intent);
                    } else {
                        pdfPickerLauncher.launch("application/pdf");
                    }
                })
                .show();
    }

    private void processPdfItinerary(Uri pdfUri) {
        Toast.makeText(this, "Processing PDF Itinerary...", Toast.LENGTH_SHORT).show();
        new Thread(() -> {
            StringBuilder fullText = new StringBuilder();
            try {
                ParcelFileDescriptor pfd = getContentResolver().openFileDescriptor(pdfUri, "r");
                if (pfd != null) {
                    PdfRenderer renderer = new PdfRenderer(pfd);
                    int pageCount = renderer.getPageCount();

                    for (int i = 0; i < pageCount; i++) {
                        PdfRenderer.Page page = renderer.openPage(i);
                        Bitmap bitmap = Bitmap.createBitmap(page.getWidth() * 2, page.getHeight() * 2, Bitmap.Config.ARGB_8888);
                        page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);
                        page.close();

                        InputImage image = InputImage.fromBitmap(bitmap, 0);
                        TextRecognizer recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);

                        com.google.android.gms.tasks.Task<Text> task = recognizer.process(image);
                        Text textResult = com.google.android.gms.tasks.Tasks.await(task);
                        if (textResult != null && textResult.getText() != null) {
                            fullText.append("\n").append(textResult.getText());
                        }
                    }
                    renderer.close();
                    pfd.close();
                }
            } catch (Exception e) {
                runOnUiThread(() -> Toast.makeText(this, "Failed to read PDF file.", Toast.LENGTH_SHORT).show());
                return;
            }

            final String extractedText = fullText.toString();
            runOnUiThread(() -> performOcrMultiFlight(extractedText));
        }).start();
    }

    private boolean isDuplicateFlight(String flightNum, long depTimeMillis) {
        if (flightList == null || flightList.isEmpty()) return false;
        String cleanNewFn = flightNum.replaceAll("\\s+", "").toUpperCase();

        for (FlightInfo existing : flightList) {
            if (existing.flightNumber != null) {
                String cleanExistFn = existing.flightNumber.replaceAll("\\s+", "").toUpperCase();
                if (cleanExistFn.equals(cleanNewFn)) {
                    if (depTimeMillis > 0 && existing.depTimeMillis > 0) {
                        long diff = Math.abs(existing.depTimeMillis - depTimeMillis);
                        if (diff < 24 * 60 * 60 * 1000L) { // Same flight on same day
                            return true;
                        }
                    } else {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private List<ParsedFlightData> parseFlightEntriesFromPdf(String text) {
        List<ParsedFlightData> results = new ArrayList<>();
        if (text == null || text.trim().isEmpty()) return results;

        Pattern flightPattern = Pattern.compile("\\b([A-Z0-9]{2,3})\\s?(\\d{3,4})\\b");
        Pattern datePattern = Pattern.compile("(\\d{1,2}:\\d{2})\\s*,?\\s*([A-Za-z]+)\\s+(\\d{1,2})\\s*,?\\s*(\\d{4})");

        String[] lines = text.split("\n");
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            Matcher fm = flightPattern.matcher(line);
            if (fm.find()) {
                String matchStr = fm.group(0);
                if (matchStr == null) continue;
                String flightNum = matchStr.replaceAll("\\s+", "").toUpperCase();
                if (flightNum.startsWith("NO") || flightNum.startsWith("PNR") || flightNum.startsWith("REF") || flightNum.startsWith("ID")) {
                    continue;
                }

                long extractedMillis = 0;
                int startLine = Math.max(0, i - 6);
                int endLine = Math.min(lines.length - 1, i + 6);

                for (int j = startLine; j <= endLine; j++) {
                    Matcher dm = datePattern.matcher(lines[j]);
                    if (dm.find()) {
                        String timePart = dm.group(1);
                        String monthPart = dm.group(2);
                        String dayPart = dm.group(3);
                        String yearPart = dm.group(4);

                        extractedMillis = parseDateAndTimeToMillis(timePart, monthPart, dayPart, yearPart);
                        if (extractedMillis > 0) break;
                    }
                }

                boolean exists = false;
                for (ParsedFlightData p : results) {
                    if (p.flightNumber.equals(flightNum)) {
                        exists = true;
                        break;
                    }
                }

                if (!exists) {
                    results.add(new ParsedFlightData(flightNum, extractedMillis));
                }
            }
        }
        return results;
    }

    private long parseDateAndTimeToMillis(String timeStr, String monthStr, String dayStr, String yearStr) {
        try {
            String fullStr = monthStr + " " + dayStr + " " + yearStr + " " + timeStr;
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("MMMM d yyyy HH:mm", java.util.Locale.US);
            java.util.Date date = sdf.parse(fullStr);
            if (date != null) return date.getTime();
        } catch (Exception e1) {
            try {
                String fullStr = monthStr + " " + dayStr + " " + yearStr + " " + timeStr;
                java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("MMM d yyyy HH:mm", java.util.Locale.US);
                java.util.Date date = sdf.parse(fullStr);
                if (date != null) return date.getTime();
            } catch (Exception ignored) {}
        }
        return 0;
    }

    private long parseIsoTimeToMillis(String isoStr) {
        if (isoStr == null || isoStr.trim().isEmpty()) return 0;
        try {
            String cleanIso = isoStr.contains("+") ? isoStr.split("\\+")[0] : isoStr;
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.US);
            java.util.Date date = sdf.parse(cleanIso);
            if (date != null) return date.getTime();
        } catch (Exception ignored) {}
        return 0;
    }

    private void performOcrMultiFlight(String text) {
        List<ParsedFlightData> parsedFlights = parseFlightEntriesFromPdf(text);

        if (parsedFlights.isEmpty()) {
            Toast.makeText(this, "No valid flight numbers detected in PDF.", Toast.LENGTH_SHORT).show();
            return;
        }

        List<ParsedFlightData> newFlights = new ArrayList<>();
        for (ParsedFlightData p : parsedFlights) {
            if (isDuplicateFlight(p.flightNumber, p.timeMillis)) {
                Toast.makeText(this, "Duplicate flight " + p.flightNumber + " skipped.", Toast.LENGTH_SHORT).show();
            } else {
                newFlights.add(p);
            }
        }

        if (newFlights.isEmpty()) {
            Toast.makeText(this, "All detected flights are already in your list!", Toast.LENGTH_SHORT).show();
            return;
        }

        if (newFlights.size() == 1) {
            ParsedFlightData target = newFlights.get(0);
            if (currentEtFlight != null) currentEtFlight.setText(target.flightNumber);
            fetchAndSaveFlight(target.flightNumber, target.timeMillis);
        } else {
            showMultipleFlightsDetectedDialog(newFlights);
        }
    }

    private void showMultipleFlightsDetectedDialog(List<ParsedFlightData> flightsData) {
        String[] items = new String[flightsData.size() + 1];
        for (int i = 0; i < flightsData.size(); i++) {
            ParsedFlightData p = flightsData.get(i);
            String dateLabel = p.timeMillis > 0 ? android.text.format.DateFormat.format("MMM dd, HH:mm", p.timeMillis).toString() : "Date pending";
            items[i] = "Flight " + (i + 1) + ": " + p.flightNumber + " (" + dateLabel + ")";
        }
        items[flightsData.size()] = "★ Add All " + flightsData.size() + " Flights Automatically";

        new MaterialAlertDialogBuilder(this)
                .setTitle("Multiple Flights Detected in PDF!")
                .setItems(items, (dialog, which) -> {
                    if (which == flightsData.size()) {
                        for (ParsedFlightData p : flightsData) {
                            fetchAndSaveFlight(p.flightNumber, p.timeMillis);
                        }
                        Toast.makeText(this, "Adding all " + flightsData.size() + " flights...", Toast.LENGTH_SHORT).show();
                    } else {
                        ParsedFlightData target = flightsData.get(which);
                        if (currentEtFlight != null) currentEtFlight.setText(target.flightNumber);
                        fetchAndSaveFlight(target.flightNumber, target.timeMillis);
                    }
                })
                .show();
    }

    private void fetchAndSaveFlight(String flightNum, long targetTimeMillis) {
        final long initialTime = targetTimeMillis > 0 ? targetTimeMillis : selectedDateTime.getTimeInMillis();
        Toast.makeText(this, "Fetching route details...", Toast.LENGTH_SHORT).show();

        String dateYmd = null;
        if (initialTime > 0) {
            dateYmd = android.text.format.DateFormat.format("yyyy-MM-dd", initialTime).toString();
        }

        apiHelper.getFlightStatus(flightNum, dateYmd, new AviationStackHelper.FlightCallback() {
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

                    // Keep user's explicit selection or PDF extracted departure timestamp permanently!
                    long finalDepTime = initialTime;
                    if (finalDepTime <= 0 && depObj.has("scheduled") && !depObj.get("scheduled").isJsonNull()) {
                        long apiSchedTime = parseIsoTimeToMillis(depObj.get("scheduled").getAsString());
                        if (apiSchedTime > 0) finalDepTime = apiSchedTime;
                    }

                    String id = activeRef.push().getKey();
                    FlightInfo flight = new FlightInfo(id, flightNum, depIata, arrIata, finalDepTime, userId);
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
                    saveFallback(flightNum, initialTime);
                }
            }

            @Override
            public void onError(String message) {
                saveFallback(flightNum, initialTime);
            }
        });
    }

    private void saveFallback(String flightNum, long targetTimeMillis) {
        String id = activeRef.push().getKey();
        long depTime = targetTimeMillis > 0 ? targetTimeMillis : selectedDateTime.getTimeInMillis();
        FlightInfo flight = new FlightInfo(id, flightNum, "KUL", "KCH", depTime, userId);
        activeRef.child(id).setValue(flight);
        historyRef.child(id).setValue(flight);
        Toast.makeText(this, "Route info saved", Toast.LENGTH_SHORT).show();
    }

    private void performOcr(Uri uri) {
        try {
            InputImage image = InputImage.fromFilePath(this, uri);
            TextRecognizer recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);
            recognizer.process(image).addOnSuccessListener(text -> {
                Pattern p = Pattern.compile("([A-Z]{2,3})\\s?(\\d{3,4})");
                Matcher m = p.matcher(text.getText());
                if (m.find()) {
                    if (currentEtFlight != null) currentEtFlight.setText(m.group(0));
                    Toast.makeText(this, "Found Flight: " + m.group(0), Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "No flight number detected.", Toast.LENGTH_SHORT).show();
                }
            });
        } catch (Exception ignored) {}
    }

    private void performOcrBitmap(Bitmap bitmap) {
        try {
            InputImage image = InputImage.fromBitmap(bitmap, 0);
            TextRecognizer recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);
            recognizer.process(image).addOnSuccessListener(text -> {
                Pattern p = Pattern.compile("([A-Z]{2,3})\\s?(\\d{3,4})");
                Matcher m = p.matcher(text.getText());
                if (m.find()) {
                    if (currentEtFlight != null) currentEtFlight.setText(m.group(0));
                    Toast.makeText(this, "Found Flight: " + m.group(0), Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "No flight number detected.", Toast.LENGTH_SHORT).show();
                }
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
