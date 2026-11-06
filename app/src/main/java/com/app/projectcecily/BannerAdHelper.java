package com.app.projectcecily;

import android.os.Handler;
import android.os.Looper;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class BannerAdHelper {

    private static final String[] MOCK_ADS = new String[]{
        "Sponsored: Explore Couple Travel Deals & Flights ✈️",
        "Sponsored: Send Secret Gifts & Flowers Overseas 💐",
        "Sponsored: Book Long-Distance Date Night Packages 🎟️",
        "Sponsored: Unlock Unlimited High-Speed eSIM Roaming 🌐"
    };

    public interface OnAdDismissedListener {
        void onDismissed();
    }

    public static void setupBanner(AppCompatActivity activity, View cardBannerAd, View btnDismissAd, TextView tvAdText, OnAdDismissedListener dismissListener) {
        if (activity == null || cardBannerAd == null || tvAdText == null) return;

        if (SubscriptionDialogHelper.isPremiumUnlocked(activity)) {
            cardBannerAd.setVisibility(View.GONE);
            if (dismissListener != null) {
                dismissListener.onDismissed();
            }
            return;
        }

        cardBannerAd.setVisibility(View.VISIBLE);

        List<String> randomizedAdsList = new ArrayList<>(Arrays.asList(MOCK_ADS));
        Collections.shuffle(randomizedAdsList);
        if (!randomizedAdsList.isEmpty()) {
            tvAdText.setText(randomizedAdsList.get(0));
        }

        Handler handler = new Handler(Looper.getMainLooper());
        final int[] currentIndex = {0};
        final Runnable[] runnableWrapper = new Runnable[1];

        Runnable resetTimer = new Runnable() {
            @Override
            public void run() {
                if (runnableWrapper[0] != null) {
                    handler.removeCallbacks(runnableWrapper[0]);
                }
                runnableWrapper[0] = new Runnable() {
                    @Override
                    public void run() {
                        if (randomizedAdsList.isEmpty()) return;
                        currentIndex[0] = (currentIndex[0] + 1) % randomizedAdsList.size();
                        String nextText = randomizedAdsList.get(currentIndex[0]);

                        tvAdText.animate()
                                .translationX(-tvAdText.getWidth())
                                .alpha(0f)
                                .setDuration(350)
                                .withEndAction(() -> {
                                    tvAdText.setText(nextText);
                                    tvAdText.setTranslationX(tvAdText.getWidth());
                                    tvAdText.animate()
                                            .translationX(0f)
                                            .alpha(1f)
                                            .setDuration(350)
                                            .start();
                                })
                                .start();

                        handler.postDelayed(this, 5000);
                    }
                };
                handler.postDelayed(runnableWrapper[0], 5000);
            }
        };

        if (btnDismissAd != null) {
            btnDismissAd.setOnClickListener(v -> {
                if (runnableWrapper[0] != null) {
                    handler.removeCallbacks(runnableWrapper[0]);
                }
                cardBannerAd.setVisibility(View.GONE);
                Toast.makeText(activity, "Ad dismissed! Unlock permanent Ad-Free Premium in Settings.", Toast.LENGTH_SHORT).show();
                if (dismissListener != null) {
                    dismissListener.onDismissed();
                }
            });
        }

        GestureDetector gestureDetector = new GestureDetector(activity, new GestureDetector.SimpleOnGestureListener() {
            private static final int SWIPE_THRESHOLD = 50;
            private static final int SWIPE_VELOCITY_THRESHOLD = 50;

            @Override
            public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
                if (e1 == null || e2 == null) return false;
                float diffX = e2.getX() - e1.getX();
                float diffY = e2.getY() - e1.getY();
                if (Math.abs(diffX) > Math.abs(diffY) && Math.abs(diffX) > SWIPE_THRESHOLD && Math.abs(velocityX) > SWIPE_VELOCITY_THRESHOLD) {
                    boolean next = diffX < 0;
                    if (randomizedAdsList.isEmpty()) return false;
                    currentIndex[0] = next ? (currentIndex[0] + 1) % randomizedAdsList.size() : (currentIndex[0] - 1 + randomizedAdsList.size()) % randomizedAdsList.size();
                    String newText = randomizedAdsList.get(currentIndex[0]);

                    float exitX = next ? -tvAdText.getWidth() : tvAdText.getWidth();
                    float enterX = next ? tvAdText.getWidth() : -tvAdText.getWidth();

                    tvAdText.animate().translationX(exitX).alpha(0f).setDuration(350).withEndAction(() -> {
                        tvAdText.setText(newText);
                        tvAdText.setTranslationX(enterX);
                        tvAdText.animate().translationX(0f).alpha(1f).setDuration(350).start();
                    }).start();

                    resetTimer.run();
                    return true;
                }
                return false;
            }
        });

        cardBannerAd.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_UP) {
                v.performClick();
            }
            gestureDetector.onTouchEvent(event);
            return true;
        });

        resetTimer.run();
    }
}
