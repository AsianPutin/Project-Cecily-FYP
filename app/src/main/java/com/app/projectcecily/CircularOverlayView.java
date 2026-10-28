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

public class CircularOverlayView extends View {

    private Paint maskPaint;
    private Paint borderPaint;
    private Paint clearPaint;
    private final RectF circleRect = new RectF();

    public CircularOverlayView(Context context) {
        super(context);
        init();
    }

    public CircularOverlayView(Context context, AttributeSet attrs) {
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
        float diameter = Math.min(width, height) * 0.82f;
        float radius = diameter / 2f;

        float cx = width / 2f;
        float cy = height / 2f;

        circleRect.set(cx - radius, cy - radius, cx + radius, cy + radius);

        // Draw dark translucent mask
        canvas.drawRect(0, 0, width, height, maskPaint);

        // Cut out clear circle
        canvas.drawCircle(cx, cy, radius, clearPaint);

        // Draw white border ring
        canvas.drawCircle(cx, cy, radius, borderPaint);
    }

    public RectF getCircleRect() {
        return circleRect;
    }
}