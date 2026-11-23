package com.app.projectcecily;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;
import androidx.annotation.NonNull;

public class SquircleOverlayView extends View {

    private Paint maskPaint;
    private Paint borderPaint;
    private Paint clearPaint;
    private final RectF cropRect = new RectF();

    public SquircleOverlayView(Context context) {
        super(context);
        init();
    }

    public SquircleOverlayView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        setLayerType(LAYER_TYPE_SOFTWARE, null);

        maskPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        maskPaint.setColor(Color.parseColor("#A0000000")); // 60% translucent dark overlay

        borderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        borderPaint.setStyle(Paint.Style.STROKE);
        borderPaint.setColor(Color.WHITE);
        borderPaint.setStrokeWidth(4f);

        clearPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        clearPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        super.onDraw(canvas);

        int width = getWidth();
        int height = getHeight();

        // Size matches dashboard photo card proportions
        float size = Math.min(width, height) * 0.82f;
        float cx = width / 2f;
        float cy = height / 2f;

        float halfW = size / 2f;
        float halfH = size / 2f;

        cropRect.set(cx - halfW, cy - halfH, cx + halfW, cy + halfH);
        float cornerRadius = size * 0.12f; // ~20dp squircle radius

        // Draw dark translucent mask
        canvas.drawRect(0, 0, width, height, maskPaint);

        // Cut out clear squircle
        canvas.drawRoundRect(cropRect, cornerRadius, cornerRadius, clearPaint);

        // Draw white border outline
        canvas.drawRoundRect(cropRect, cornerRadius, cornerRadius, borderPaint);
    }

    public RectF getCropRect() {
        return cropRect;
    }
}
