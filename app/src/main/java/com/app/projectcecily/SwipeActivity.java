package com.app.projectcecily;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.bumptech.glide.Glide;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.checkbox.MaterialCheckBox;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import java.util.ArrayList;
import java.util.List;

public class SwipeActivity extends AppCompatActivity {

    private String userId, partnerId, pairKey;
    private DatabaseReference dbRef;

    private TextView tvSwipeModeHeader, tvCardCategory, tvCardTitle, tvCardDescription;
    private ImageView ivCardPhoto;
    private MaterialCardView cardSwipeContainer;
    private View layoutDeckFinished;
    private FloatingActionButton btnPass, btnLike;
    private Button btnViewMatches, btnEditDatePreferences, btnResetDeck;

    private List<DateCard> currentDeck = new ArrayList<>();
    private int currentCardIndex = 0;
    private DatePreferences localPrefs = new DatePreferences();

    private boolean isSameCityMode = false;
    private String myCityStr = "Unknown City";
    private String partnerCityStr = "Unknown City";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_swipe);

        userId = UserManager.getUserId(this);
        partnerId = UserManager.getPartnerId(this);
        pairKey = partnerId.isEmpty() ? userId : (userId.compareTo(partnerId) < 0 ? userId + "_" + partnerId : partnerId + "_" + userId);

        dbRef = FirebaseDatabase.getInstance("https://i-must-finish-this-in-2026-default-rtdb.asia-southeast1.firebasedatabase.app/").getReference();

        View appBarView = findViewById(R.id.appBarLayout);
        UIUtils.applyTopInsetToAppBar(appBarView);

        com.google.android.material.appbar.MaterialToolbar toolbar = findViewById(R.id.toolbar);
        if (toolbar != null) {
            setSupportActionBar(toolbar);
            if (getSupportActionBar() != null) {
                getSupportActionBar().setDisplayHomeAsUpEnabled(true);
                getSupportActionBar().setTitle("Food & Date Ideas");
            }
        }

        UIUtils.applyBottomPadding(findViewById(R.id.swipe_root));

        tvSwipeModeHeader = findViewById(R.id.tvSwipeModeHeader);
        tvCardCategory = findViewById(R.id.tvCardCategory);
        tvCardTitle = findViewById(R.id.tvCardTitle);
        tvCardDescription = findViewById(R.id.tvCardDescription);
        ivCardPhoto = findViewById(R.id.ivCardPhoto);
        cardSwipeContainer = findViewById(R.id.cardSwipeContainer);
        layoutDeckFinished = findViewById(R.id.layoutDeckFinished);

        btnPass = findViewById(R.id.btnSwipePass);
        btnLike = findViewById(R.id.btnSwipeLike);
        btnViewMatches = findViewById(R.id.btnViewMatches);
        btnEditDatePreferences = findViewById(R.id.btnEditDatePreferences);
        btnResetDeck = findViewById(R.id.btnResetDeck);

        btnPass.setOnClickListener(v -> handleSwipe(false));
        btnLike.setOnClickListener(v -> handleSwipe(true));
        btnResetDeck.setOnClickListener(v -> resetDeck());
        btnViewMatches.setOnClickListener(v -> showMatchesDialog());
        btnEditDatePreferences.setOnClickListener(v -> showPreferencesDialog());

        resolveCityAndStart();
        listenForRealTimeMatches();
    }

    private void resolveCityAndStart() {
        tvSwipeModeHeader.setText("Detecting partner proximity...");

        // Fetch location of self and partner to resolve city
        dbRef.child("locations").child(userId).get().addOnSuccessListener(selfSnap -> {
            Double selfLat = selfSnap.child("latitude").getValue(Double.class);
            Double selfLon = selfSnap.child("longitude").getValue(Double.class);

            CityUtils.resolveCity(this, selfLat != null ? selfLat : 0, selfLon != null ? selfLon : 0, myCity -> {
                myCityStr = myCity;

                if (!partnerId.isEmpty()) {
                    dbRef.child("locations").child(partnerId).get().addOnSuccessListener(partnerSnap -> {
                        Double pLat = partnerSnap.child("latitude").getValue(Double.class);
                        Double pLon = partnerSnap.child("longitude").getValue(Double.class);

                        CityUtils.resolveCity(this, pLat != null ? pLat : 0, pLon != null ? pLon : 0, partnerCity -> {
                            partnerCityStr = partnerCity;
                            isSameCityMode = CityUtils.isSameCity(myCityStr, partnerCityStr);
                            updateModeBanner();
                            loadPreferencesAndBuildDeck();
                        });
                    }).addOnFailureListener(e -> {
                        isSameCityMode = false;
                        updateModeBanner();
                        loadPreferencesAndBuildDeck();
                    });
                } else {
                    isSameCityMode = false;
                    updateModeBanner();
                    loadPreferencesAndBuildDeck();
                }
            });
        }).addOnFailureListener(e -> {
            isSameCityMode = false;
            updateModeBanner();
            loadPreferencesAndBuildDeck();
        });
    }

    private void updateModeBanner() {
        if (isSameCityMode) {
            tvSwipeModeHeader.setText("📍 Same City Mode (" + myCityStr + ")");
        } else {
            if (!partnerId.isEmpty() && !partnerCityStr.equalsIgnoreCase("Unknown City")) {
                tvSwipeModeHeader.setText("✈️ LDR Mode (" + myCityStr + " ↔ " + partnerCityStr + ")");
            } else {
                tvSwipeModeHeader.setText("✈️ Long Distance Relationship Mode");
            }
        }
    }

    private void loadPreferencesAndBuildDeck() {
        dbRef.child("profiles").child(userId).child("datePreferences").get().addOnSuccessListener(snapshot -> {
            if (snapshot.exists()) {
                DatePreferences prefs = snapshot.getValue(DatePreferences.class);
                if (prefs != null) localPrefs = prefs;
            }
            currentDeck = DateCardGenerator.generateDeck(isSameCityMode, myCityStr, localPrefs);
            currentCardIndex = 0;
            displayCurrentCard();
        }).addOnFailureListener(e -> {
            currentDeck = DateCardGenerator.generateDeck(isSameCityMode, myCityStr, localPrefs);
            currentCardIndex = 0;
            displayCurrentCard();
        });
    }

    private void displayCurrentCard() {
        if (currentCardIndex < currentDeck.size()) {
            cardSwipeContainer.setVisibility(View.VISIBLE);
            layoutDeckFinished.setVisibility(View.GONE);

            DateCard card = currentDeck.get(currentCardIndex);
            tvCardTitle.setText(card.title);
            tvCardCategory.setText(card.category);
            tvCardDescription.setText(card.description);

            Glide.with(this)
                    .load(card.imageUrl)
                    .placeholder(R.drawable.ic_blank_user)
                    .error(R.drawable.ic_blank_user)
                    .into(ivCardPhoto);
        } else {
            cardSwipeContainer.setVisibility(View.GONE);
            layoutDeckFinished.setVisibility(View.VISIBLE);
        }
    }

    private void handleSwipe(boolean isLike) {
        if (currentCardIndex >= currentDeck.size()) return;

        DateCard card = currentDeck.get(currentCardIndex);

        if (isLike) {
            // Save like to Firebase
            dbRef.child("swipes").child(pairKey).child(card.id).child(userId).setValue(true);
            checkIfMutualMatch(card);
        }

        currentCardIndex++;
        displayCurrentCard();
    }

    private void checkIfMutualMatch(DateCard card) {
        if (partnerId.isEmpty()) return;

        dbRef.child("swipes").child(pairKey).child(card.id).child(partnerId).get().addOnSuccessListener(snapshot -> {
            Boolean partnerLiked = snapshot.getValue(Boolean.class);
            if (Boolean.TRUE.equals(partnerLiked)) {
                // Save match to mutual matches node
                dbRef.child("matches").child(pairKey).child(card.id).setValue(card);
                showMatchCelebrationDialog(card);
            }
        });
    }

    private void listenForRealTimeMatches() {
        if (partnerId.isEmpty()) return;

        dbRef.child("matches").child(pairKey).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                // Keep matches updated in background
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void showMatchCelebrationDialog(DateCard card) {
        new MaterialAlertDialogBuilder(this)
                .setTitle("🎉 IT'S A MATCH!")
                .setMessage("Both you and your partner swiped YES on:\n\n★ " + card.title + " (" + card.category + ")\n\nDate Night Planned!")
                .setPositiveButton("Awesome!", null)
                .show();
    }

    private void showMatchesDialog() {
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_view_full_image, null);
        // Clean dialog view for matches list
        ListView lvMatches = new ListView(this);

        AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                .setTitle("Your Mutual Date Matches")
                .setView(lvMatches)
                .setPositiveButton("Close", null)
                .create();

        dbRef.child("matches").child(pairKey).get().addOnSuccessListener(snapshot -> {
            List<String> matchNames = new ArrayList<>();
            if (snapshot.exists()) {
                for (DataSnapshot ds : snapshot.getChildren()) {
                    DateCard card = ds.getValue(DateCard.class);
                    if (card != null) {
                        matchNames.add("★ " + card.title + " (" + card.category + ")");
                    }
                }
            }
            if (matchNames.isEmpty()) {
                matchNames.add("No mutual matches yet! Keep swiping.");
            }

            ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, matchNames);
            lvMatches.setAdapter(adapter);
        });

        dialog.show();
    }

    private void showPreferencesDialog() {
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_date_preferences, null);
        MaterialCheckBox chkCoOp = view.findViewById(R.id.chkCoOpGames);
        MaterialCheckBox chkCasual = view.findViewById(R.id.chkCasualGames);
        MaterialCheckBox chkAnime = view.findViewById(R.id.chkAnimeMovies);
        MaterialCheckBox chkHorror = view.findViewById(R.id.chkHorrorMovies);
        MaterialCheckBox chkRomance = view.findViewById(R.id.chkRomanceMovies);
        MaterialCheckBox chkCafes = view.findViewById(R.id.chkCafes);
        MaterialCheckBox chkDining = view.findViewById(R.id.chkDining);
        MaterialCheckBox chkParks = view.findViewById(R.id.chkOutdoorParks);

        chkCoOp.setChecked(localPrefs.prefCoOpGames);
        chkCasual.setChecked(localPrefs.prefCasualGames);
        chkAnime.setChecked(localPrefs.prefAnimeMovies);
        chkHorror.setChecked(localPrefs.prefHorrorMovies);
        chkRomance.setChecked(localPrefs.prefRomanceMovies);
        chkCafes.setChecked(localPrefs.prefCafes);
        chkDining.setChecked(localPrefs.prefDining);
        chkParks.setChecked(localPrefs.prefOutdoorParks);

        new MaterialAlertDialogBuilder(this)
                .setView(view)
                .setPositiveButton("Save Preferences", (dialog, which) -> {
                    localPrefs.prefCoOpGames = chkCoOp.isChecked();
                    localPrefs.prefCasualGames = chkCasual.isChecked();
                    localPrefs.prefAnimeMovies = chkAnime.isChecked();
                    localPrefs.prefHorrorMovies = chkHorror.isChecked();
                    localPrefs.prefRomanceMovies = chkRomance.isChecked();
                    localPrefs.prefCafes = chkCafes.isChecked();
                    localPrefs.prefDining = chkDining.isChecked();
                    localPrefs.prefOutdoorParks = chkParks.isChecked();

                    dbRef.child("profiles").child(userId).child("datePreferences").setValue(localPrefs);
                    Toast.makeText(this, "Preferences saved!", Toast.LENGTH_SHORT).show();
                    loadPreferencesAndBuildDeck();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void resetDeck() {
        currentCardIndex = 0;
        loadPreferencesAndBuildDeck();
        Toast.makeText(this, "Deck shuffled!", Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        ThemeUtils.applyAccentToActivity(this);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
