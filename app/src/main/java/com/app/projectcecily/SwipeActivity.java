package com.app.projectcecily;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
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

    private float initialX, initialY;
    private static final float SWIPE_THRESHOLD = 220f;

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

        btnPass.setOnClickListener(v -> animateCardAndSwipe(false));
        btnLike.setOnClickListener(v -> animateCardAndSwipe(true));
        btnResetDeck.setOnClickListener(v -> resetDeck());
        btnViewMatches.setOnClickListener(v -> showMatchesDialog());
        btnEditDatePreferences.setOnClickListener(v -> showPreferencesDialog());

        setupSwipeTouchListener();
        resolveCityAndStart();
        listenForRealTimeMatches();
    }

    @SuppressLint("ClickableViewAccessibility")
    private void setupSwipeTouchListener() {
        if (cardSwipeContainer == null) return;

        cardSwipeContainer.setOnTouchListener((v, event) -> {
            if (currentCardIndex >= currentDeck.size()) return false;

            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    initialX = event.getRawX();
                    initialY = event.getRawY();
                    return true;

                case MotionEvent.ACTION_MOVE:
                    float deltaX = event.getRawX() - initialX;
                    float deltaY = event.getRawY() - initialY;
                    cardSwipeContainer.setTranslationX(deltaX);
                    cardSwipeContainer.setTranslationY(deltaY * 0.3f);
                    cardSwipeContainer.setRotation(deltaX / 25f);
                    return true;

                case MotionEvent.ACTION_UP:
                    float totalDeltaX = event.getRawX() - initialX;
                    if (totalDeltaX > SWIPE_THRESHOLD) {
                        animateCardAndSwipe(true);
                    } else if (totalDeltaX < -SWIPE_THRESHOLD) {
                        animateCardAndSwipe(false);
                    } else {
                        cardSwipeContainer.animate()
                                .translationX(0f)
                                .translationY(0f)
                                .rotation(0f)
                                .setDuration(200)
                                .start();
                    }
                    return true;
            }
            return false;
        });
    }

    private void animateCardAndSwipe(boolean isLike) {
        if (cardSwipeContainer == null) {
            handleSwipe(isLike);
            return;
        }

        float targetX = isLike ? 1200f : -1200f;
        float targetRotation = isLike ? 30f : -30f;

        cardSwipeContainer.animate()
                .translationX(targetX)
                .rotation(targetRotation)
                .setDuration(220)
                .withEndAction(() -> {
                    cardSwipeContainer.setTranslationX(0f);
                    cardSwipeContainer.setTranslationY(0f);
                    cardSwipeContainer.setRotation(0f);
                    handleSwipe(isLike);
                })
                .start();
    }

    private void resolveCityAndStart() {
        tvSwipeModeHeader.setText("Detecting partner proximity...");

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
                    .diskCacheStrategy(com.bumptech.glide.load.engine.DiskCacheStrategy.ALL)
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
        androidx.recyclerview.widget.RecyclerView rvMatches = new androidx.recyclerview.widget.RecyclerView(this);
        rvMatches.setLayoutManager(new androidx.recyclerview.widget.LinearLayoutManager(this));
        rvMatches.setPadding(0, 16, 0, 16);
        rvMatches.setClipToPadding(false);

        AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                .setTitle("Your Mutual Date Matches")
                .setView(rvMatches)
                .setPositiveButton("Close", null)
                .create();

        dbRef.child("matches").child(pairKey).get().addOnSuccessListener(snapshot -> {
            List<DateCard> matchCards = new ArrayList<>();
            if (snapshot.exists()) {
                for (DataSnapshot ds : snapshot.getChildren()) {
                    DateCard card = ds.getValue(DateCard.class);
                    if (card != null) {
                        matchCards.add(card);
                    }
                }
            }

            MatchesAdapter adapter = new MatchesAdapter(matchCards, card -> deleteMatch(card, matchCards, rvMatches));
            rvMatches.setAdapter(adapter);

            androidx.recyclerview.widget.ItemTouchHelper.SimpleCallback swipeCallback =
                    new androidx.recyclerview.widget.ItemTouchHelper.SimpleCallback(0, androidx.recyclerview.widget.ItemTouchHelper.LEFT | androidx.recyclerview.widget.ItemTouchHelper.RIGHT) {
                        @Override
                        public boolean onMove(@NonNull androidx.recyclerview.widget.RecyclerView recyclerView, @NonNull androidx.recyclerview.widget.RecyclerView.ViewHolder viewHolder, @NonNull androidx.recyclerview.widget.RecyclerView.ViewHolder target) {
                            return false;
                        }

                        @Override
                        public void onSwiped(@NonNull androidx.recyclerview.widget.RecyclerView.ViewHolder viewHolder, int direction) {
                            int pos = viewHolder.getAdapterPosition();
                            if (pos >= 0 && pos < matchCards.size()) {
                                DateCard card = matchCards.get(pos);
                                deleteMatch(card, matchCards, rvMatches);
                            }
                        }
                    };

            new androidx.recyclerview.widget.ItemTouchHelper(swipeCallback).attachToRecyclerView(rvMatches);
        });

        dialog.show();
    }

    private void deleteMatch(DateCard card, List<DateCard> matchCards, androidx.recyclerview.widget.RecyclerView rvMatches) {
        int index = matchCards.indexOf(card);
        if (index >= 0) {
            matchCards.remove(index);
            if (rvMatches.getAdapter() != null) {
                rvMatches.getAdapter().notifyItemRemoved(index);
            }
        }

        // Reset/delete swipe preference on deleter's end in Firebase
        dbRef.child("swipes").child(pairKey).child(card.id).child(userId).removeValue();
        // Remove mutual match from pair key in Firebase
        dbRef.child("matches").child(pairKey).child(card.id).removeValue();
    }

    private static class MatchesAdapter extends androidx.recyclerview.widget.RecyclerView.Adapter<MatchesAdapter.ViewHolder> {

        private final List<DateCard> items;
        private final OnMatchDeleteListener deleteListener;

        private interface OnMatchDeleteListener {
            void onDelete(DateCard card);
        }

        MatchesAdapter(List<DateCard> items, OnMatchDeleteListener deleteListener) {
            this.items = items;
            this.deleteListener = deleteListener;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_date_match, parent, false);
            return new ViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            DateCard card = items.get(position);
            if ("EMPTY".equals(card.id)) {
                holder.tvMatchTitle.setText(card.title);
                holder.btnDeleteMatch.setVisibility(View.GONE);
            } else {
                holder.tvMatchTitle.setText("★ " + card.title + " (" + card.category + ")");
                holder.btnDeleteMatch.setVisibility(View.VISIBLE);
                holder.btnDeleteMatch.setOnClickListener(v -> deleteListener.onDelete(card));
            }
        }

        @Override
        public int getItemCount() {
            if (items.isEmpty()) {
                DateCard emptyCard = new DateCard("EMPTY", "No mutual matches yet! Keep swiping.", "", "", "", false);
                items.add(emptyCard);
            }
            return items.size();
        }

        static class ViewHolder extends androidx.recyclerview.widget.RecyclerView.ViewHolder {
            TextView tvMatchTitle;
            ImageView btnDeleteMatch;

            ViewHolder(View itemView) {
                super(itemView);
                tvMatchTitle = itemView.findViewById(R.id.tvMatchTitle);
                btnDeleteMatch = itemView.findViewById(R.id.btnDeleteMatch);
            }
        }
    }

    private void showPreferencesDialog() {
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_date_preferences, null);

        // Together Mode Checkboxes
        MaterialCheckBox chkLocalFood = view.findViewById(R.id.chkLocalFood);
        MaterialCheckBox chkAsianFood = view.findViewById(R.id.chkAsianFood);
        MaterialCheckBox chkWesternFood = view.findViewById(R.id.chkWesternFood);
        MaterialCheckBox chkMiddleEasternFood = view.findViewById(R.id.chkMiddleEasternFood);
        MaterialCheckBox chkCafes = view.findViewById(R.id.chkCafes);

        MaterialCheckBox chkOutdoorNature = view.findViewById(R.id.chkOutdoorNature);
        MaterialCheckBox chkEntertainment = view.findViewById(R.id.chkEntertainment);
        MaterialCheckBox chkCozyCreative = view.findViewById(R.id.chkCozyCreative);

        // LDR Mode Checkboxes
        MaterialCheckBox chkCoOp = view.findViewById(R.id.chkCoOpGames);
        MaterialCheckBox chkCasual = view.findViewById(R.id.chkCasualGames);
        MaterialCheckBox chkAnime = view.findViewById(R.id.chkAnimeMovies);
        MaterialCheckBox chkHorror = view.findViewById(R.id.chkHorrorMovies);
        MaterialCheckBox chkRomance = view.findViewById(R.id.chkRomanceMovies);

        chkLocalFood.setChecked(localPrefs.prefLocalFood);
        chkAsianFood.setChecked(localPrefs.prefAsianFood);
        chkWesternFood.setChecked(localPrefs.prefWesternFood);
        chkMiddleEasternFood.setChecked(localPrefs.prefMiddleEasternFood);
        chkCafes.setChecked(localPrefs.prefCafes);

        chkOutdoorNature.setChecked(localPrefs.prefOutdoorNature);
        chkEntertainment.setChecked(localPrefs.prefEntertainment);
        chkCozyCreative.setChecked(localPrefs.prefCozyCreative);

        chkCoOp.setChecked(localPrefs.prefCoOpGames);
        chkCasual.setChecked(localPrefs.prefCasualGames);
        chkAnime.setChecked(localPrefs.prefAnimeMovies);
        chkHorror.setChecked(localPrefs.prefHorrorMovies);
        chkRomance.setChecked(localPrefs.prefRomanceMovies);

        new MaterialAlertDialogBuilder(this)
                .setView(view)
                .setPositiveButton("Save Preferences", (dialog, which) -> {
                    localPrefs.prefLocalFood = chkLocalFood.isChecked();
                    localPrefs.prefAsianFood = chkAsianFood.isChecked();
                    localPrefs.prefWesternFood = chkWesternFood.isChecked();
                    localPrefs.prefMiddleEasternFood = chkMiddleEasternFood.isChecked();
                    localPrefs.prefCafes = chkCafes.isChecked();

                    localPrefs.prefOutdoorNature = chkOutdoorNature.isChecked();
                    localPrefs.prefEntertainment = chkEntertainment.isChecked();
                    localPrefs.prefCozyCreative = chkCozyCreative.isChecked();

                    localPrefs.prefCoOpGames = chkCoOp.isChecked();
                    localPrefs.prefCasualGames = chkCasual.isChecked();
                    localPrefs.prefAnimeMovies = chkAnime.isChecked();
                    localPrefs.prefHorrorMovies = chkHorror.isChecked();
                    localPrefs.prefRomanceMovies = chkRomance.isChecked();

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
