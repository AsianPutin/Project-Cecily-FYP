package com.app.projectcecily;

import android.app.Activity;
import android.graphics.Color;
import android.os.Build;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;

public class UIUtils {
    
    /**
     * Enables Edge-to-Edge and handles insets for the given view.
     */
    public static void makeEdgeToEdge(Activity activity, View view, boolean usePadding) {
        Window window = activity.getWindow();
        WindowCompat.setDecorFitsSystemWindows(window, false);
        window.setStatusBarColor(Color.TRANSPARENT);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.setNavigationBarColor(Color.TRANSPARENT);
        }

        ViewCompat.setOnApplyWindowInsetsListener(view, (v, windowInsets) -> {
            Insets insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
            if (usePadding) {
                v.setPadding(v.getPaddingLeft(), insets.top, v.getPaddingRight(), insets.bottom);
            } else {
                ViewGroup.MarginLayoutParams mlp = (ViewGroup.MarginLayoutParams) v.getLayoutParams();
                mlp.topMargin = insets.top;
                mlp.bottomMargin = insets.bottom + (int) (16 * v.getContext().getResources().getDisplayMetrics().density);
                v.setLayoutParams(mlp);
            }
            return windowInsets;
        });
    }

    /**
     * Adds top margin to a view to keep it comfortably below the status/action bar.
     */
    public static void applyTopInsetMargin(View view) {
        if (view == null) return;
        if (!(view.getLayoutParams() instanceof ViewGroup.MarginLayoutParams)) return;

        ViewCompat.setOnApplyWindowInsetsListener(view, (v, windowInsets) -> {
            Insets insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
            ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams) v.getLayoutParams();
            int density = (int) v.getContext().getResources().getDisplayMetrics().density;
            lp.topMargin = insets.top + (16 * density);
            v.setLayoutParams(lp);
            return windowInsets;
        });
    }

    /**
     * Adds top padding to a view to keep its content below the status/action bar.
     */
    public static void applyTopInsetPadding(View view) {
        if (view == null) return;
        ViewCompat.setOnApplyWindowInsetsListener(view, (v, windowInsets) -> {
            Insets insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
            int density = (int) v.getContext().getResources().getDisplayMetrics().density;
            v.setPadding(v.getPaddingLeft(), insets.top + (16 * density), v.getPaddingRight(), v.getPaddingBottom());
            return windowInsets;
        });
    }

    /**
     * Adds bottom padding to a view to keep its content above the system navigation bar.
     */
    public static void applyBottomPadding(View view) {
        if (view == null) return;
        ViewCompat.setOnApplyWindowInsetsListener(view, (v, windowInsets) -> {
            Insets insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(v.getPaddingLeft(), v.getPaddingTop(), v.getPaddingRight(), insets.bottom);
            return windowInsets;
        });
    }

    /**
     * Adds bottom margin to a view to keep the entire view above the system navigation bar.
     */
    public static void applyBottomMargin(View view) {
        if (view == null) return;
        if (!(view.getLayoutParams() instanceof ViewGroup.MarginLayoutParams)) return;
        
        ViewCompat.setOnApplyWindowInsetsListener(view, (v, windowInsets) -> {
            Insets insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
            ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams) v.getLayoutParams();
            int density = (int) v.getContext().getResources().getDisplayMetrics().density;
            lp.bottomMargin = insets.bottom + (16 * density);
            v.setLayoutParams(lp);
            return windowInsets;
        });
    }
    
    public static void handleInsets(View view) {
        applyBottomPadding(view);
    }
}
