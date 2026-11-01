package com.app.projectcecily;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ComposeShader;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import androidx.annotation.Nullable;

public class ColorPickerView extends View {

    private Paint paint;
    private Paint selectorPaint;
    private float hue = 120f;
    private float sat = 1f;
    private float val = 1f;

    private float selectorX = 0f;
    private float selectorY = 0f;

    public interface OnColorChangeListener {
        void OnColorChanged(int color, float hue, float sat, float val);
    }

    private OnColorChangeListener listener;

    public ColorPickerView(Context context) {
        super(context);
        init();
    }

    public ColorPickerView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public ColorPickerView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        selectorPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        selectorPaint.setStyle(Paint.Style.STROKE);
        selectorPaint.setStrokeWidth(6f);
        selectorPaint.setColor(Color.WHITE);
    }

    public void setOnColorChangeListener(OnColorChangeListener listener) {
        this.listener = listener;
    }

    public void setHue(float hue) {
        this.hue = hue;
        invalidate();
        notifyColorChanged();
    }

    public void setColor(int color) {
        float[] hsv = new float[3];
        Color.colorToHSV(color, hsv);
        this.hue = hsv[0];
        this.sat = hsv[1];
        this.val = hsv[2];

        if (getWidth() > 0 && getHeight() > 0) {
            selectorX = sat * getWidth();
            selectorY = (1f - val) * getHeight();
        }
        invalidate();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        selectorX = sat * w;
        selectorY = (1f - val) * h;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int pureHueColor = Color.HSVToColor(new float[]{hue, 1f, 1f});

        Shader valShader = new LinearGradient(0, 0, 0, getHeight(),
                Color.WHITE, Color.BLACK, Shader.TileMode.CLAMP);

        Shader satShader = new LinearGradient(0, 0, getWidth(), 0,
                Color.WHITE, pureHueColor, Shader.TileMode.CLAMP);

        ComposeShader composeShader = new ComposeShader(satShader, valShader, PorterDuff.Mode.MULTIPLY);
        paint.setShader(composeShader);

        canvas.drawRoundRect(0, 0, getWidth(), getHeight(), 16f, 16f, paint);

        // Draw selector circle
        canvas.drawCircle(selectorX, selectorY, 16f, selectorPaint);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_MOVE:
                selectorX = Math.max(0, Math.min(event.getX(), getWidth()));
                selectorY = Math.max(0, Math.min(event.getY(), getHeight()));

                sat = selectorX / (float) getWidth();
                val = 1f - (selectorY / (float) getHeight());

                invalidate();
                notifyColorChanged();
                return true;
        }
        return super.onTouchEvent(event);
    }

    private void notifyColorChanged() {
        if (listener != null) {
            int currentColor = Color.HSVToColor(new float[]{hue, sat, val});
            listener.OnColorChanged(currentColor, hue, sat, val);
        }
    }
}
