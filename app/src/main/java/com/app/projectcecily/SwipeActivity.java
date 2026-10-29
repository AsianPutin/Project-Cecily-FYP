package com.app.projectcecily;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class SwipeActivity extends AppCompatActivity {

    private CardView swipeCard;
    private TextView tvCardTitle, tvCardLabel, tvMatchDetails, tvLocation, tvPriceRange, tvFoodList;
    private ImageView ivFoodImage;
    private LinearLayout layoutMatchOverlay;
    private ImageButton btnPass, btnLike, btnFavorite, btnHardNo;

    private String userId, partnerId;
    private DatabaseReference swipeRef;

    private final String[] establishments = {"Chong Choon Cafe", "Lau Ya Keng Food Court", "Top Spot Food Court", "Sharing Downtown"};
    private final String[] categories = {"Coffee Shop", "Heritage Food Court", "Seafood Center", "Western Fusion"};
    private final String[] addressList = {"Abell Road, Kuching", "Carpenter Street, Kuching", "Off Jalan Bukit Mata, Kuching", "Jalan Padungan, Kuching"};
    private final String[] priceList = {"$", "$", "$$$", "$$"};
    private final String[] dishLists = {
        "• Sarawak Laksa\n• Teh C Peng Special\n• Kolo Mee",
        "• Pork Satay\n• Fish Ball Soup\n• Kueh Chap",
        "• Butter Prawns\n• Midin (Jungle Fern)\n• Oyster Pancake",
        "• Chicken Chop\n• Lamb Shoulder\n• Beef Burger"
    };
    
    private int currentDeckIndex = 0;
    private float initialX, initialY;
    private final float SWIPE_THRESHOLD = 300f;
    private boolean isAnimating = false;

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_swipe);

        userId = UserManager.getUserId(this);
        partnerId = UserManager.getPartnerId(this);
        swipeRef = FirebaseDatabase.getInstance("https://i-must-finish-this-in-2026-default-rtdb.asia-southeast1.firebasedatabase.app/").getReference("swipes");

        // Clear previous session swipes to prevent Ghost Matches for demo
        swipeRef.child(userId).removeValue();

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Swipe Food");
        }

        swipeCard = findViewById(R.id.swipeCard);
        tvCardTitle = findViewById(R.id.tvCardTitle);
        tvCardLabel = findViewById(R.id.tvCardLabel);
        tvLocation = findViewById(R.id.tvLocation);
        tvPriceRange = findViewById(R.id.tvPriceRange);
        tvFoodList = findViewById(R.id.tvFoodList);
        ivFoodImage = findViewById(R.id.ivFoodImage);
        layoutMatchOverlay = findViewById(R.id.layoutMatchOverlay);
        tvMatchDetails = findViewById(R.id.tvMatchDetails);
        
        btnPass = findViewById(R.id.btnPass);
        btnLike = findViewById(R.id.btnLike);
        btnFavorite = findViewById(R.id.btnFavorite);
        btnHardNo = findViewById(R.id.btnHardNo);
        
        Button btnDismissMatch = findViewById(R.id.btnDismissMatch);

        UIUtils.applyBottomMargin(findViewById(R.id.actionButtonsContainer));

        updateCardContent();

        swipeCard.setOnTouchListener((view, event) -> {
            if (isAnimating) return false;
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    initialX = event.getRawX();
                    initialY = event.getRawY();
                    return true;
                case MotionEvent.ACTION_MOVE:
                    float deltaX = event.getRawX() - initialX;
                    float deltaY = event.getRawY() - initialY;
                    view.setTranslationX(deltaX);
                    view.setTranslationY(deltaY);
                    view.setRotation(deltaX / 20f);
                    return true;
                case MotionEvent.ACTION_UP:
                    float finalDeltaX = event.getRawX() - initialX;
                    if (Math.abs(finalDeltaX) > SWIPE_THRESHOLD) {
                        animateAndSwipe(finalDeltaX > 0 ? "LIKE" : "PASS");
                    } else {
                        view.animate().translationX(0).translationY(0).rotation(0).setDuration(200).start();
                    }
                    return true;
            }
            return false;
        });

        btnLike.setOnClickListener(v -> animateAndSwipe("LIKE"));
        btnPass.setOnClickListener(v -> animateAndSwipe("PASS"));
        btnFavorite.setOnClickListener(v -> animateAndSwipe("FAVORITE"));
        btnHardNo.setOnClickListener(v -> animateAndSwipe("HARD_NO"));

        btnDismissMatch.setOnClickListener(v -> {
            layoutMatchOverlay.setVisibility(View.GONE);
            advanceDeck();
        });
    }

    private void animateAndSwipe(String decision) {
        if (isAnimating) return;
        isAnimating = true;
        float translation = decision.equals("LIKE") || decision.equals("FAVORITE") ? 1500f : -1500f;
        swipeCard.animate()
                .translationX(translation)
                .rotation(translation / 20f)
                .alpha(0f)
                .setDuration(400)
                .withEndAction(() -> {
                    isAnimating = false;
                    swipeCard.setTranslationX(0);
                    swipeCard.setTranslationY(0);
                    swipeCard.setRotation(0);
                    swipeCard.setAlpha(1f);
                    handleSwipeDecision(decision);
                }).start();
    }

    private void handleSwipeDecision(String decision) {
        String currentName = establishments[currentDeckIndex];
        
        // 1. Record your decision immediately to Firebase
        swipeRef.child(userId).child(currentName).setValue(decision);

        // 2. ONLY check for match if you swiped LIKE or FAVORITE
        if (decision.equals("LIKE") || decision.equals("FAVORITE")) {
            swipeRef.child(partnerId).child(currentName).get().addOnSuccessListener(snapshot -> {
                if (snapshot.exists()) {
                    String partnerValue = snapshot.getValue(String.class);
                    // Match ONLY if partner also LIKED or FAVORITED in this session
                    if ("LIKE".equals(partnerValue) || "FAVORITE".equals(partnerValue)) {
                        tvMatchDetails.setText("You both want " + currentName + "!");
                        layoutMatchOverlay.setVisibility(View.VISIBLE);
                    } else {
                        advanceDeck();
                    }
                } else {
                    advanceDeck();
                }
            }).addOnFailureListener(e -> advanceDeck());
        } else {
            advanceDeck();
        }
    }

    private void advanceDeck() {
        currentDeckIndex++;
        if (currentDeckIndex < establishments.length) {
            updateCardContent();
        } else {
            tvCardTitle.setText("No More Options!");
            tvCardLabel.setText("Check back later.");
            tvFoodList.setText(""); tvLocation.setText(""); tvPriceRange.setText("");
            swipeCard.setOnTouchListener(null);
            disableButtons();
        }
    }

    private void updateCardContent() {
        tvCardTitle.setText(establishments[currentDeckIndex]);
        tvCardLabel.setText(categories[currentDeckIndex]);
        tvLocation.setText(addressList[currentDeckIndex]);
        tvPriceRange.setText(priceList[currentDeckIndex]);
        tvFoodList.setText(dishLists[currentDeckIndex]);
    }

    private void disableButtons() {
        btnLike.setEnabled(false); btnPass.setEnabled(false); btnFavorite.setEnabled(false); btnHardNo.setEnabled(false);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) { finish(); return true; }
        return super.onOptionsItemSelected(item);
    }
}