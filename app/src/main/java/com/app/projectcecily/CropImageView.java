package com.app.projectcecily;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.PointF;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import androidx.appcompat.widget.AppCompatImageView;

public class CropImageView extends AppCompatImageView {

    private final Matrix matrix = new Matrix();
    private final Matrix savedMatrix = new Matrix();

    private static final int NONE = 0;
    private static final int DRAG = 1;
    private static final int ZOOM = 2;
    private int mode = NONE;

    private final PointF start = new PointF();
    private final PointF mid = new PointF();
    private float oldDist = 1f;

    private ScaleGestureDetector scaleDetector;

    public CropImageView(Context context) {
        super(context);
        init(context);
    }

    public CropImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    private void init(Context context) {
        setScaleType(ScaleType.MATRIX);
        scaleDetector = new ScaleGestureDetector(context, new ScaleListener());
    }

    public void setImageAndReset(Bitmap bitmap) {
        setImageBitmap(bitmap);
        matrix.reset();
        if (bitmap != null) {
            float viewWidth = getWidth();
            float viewHeight = getHeight();
            if (viewWidth <= 0 || viewHeight <= 0) {
                post(() -> setImageAndReset(bitmap));
                return;
            }
            float scale = Math.max(viewWidth / bitmap.getWidth(), viewHeight / bitmap.getHeight());
            matrix.postScale(scale, scale);
            float dx = (viewWidth - bitmap.getWidth() * scale) / 2f;
            float dy = (viewHeight - bitmap.getHeight() * scale) / 2f;
            matrix.postTranslate(dx, dy);
        }
        setImageMatrix(matrix);
    }

    public void rotate90() {
        if (getDrawable() == null) return;
        matrix.postRotate(90, getWidth() / 2f, getHeight() / 2f);
        setImageMatrix(matrix);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        scaleDetector.onTouchEvent(event);

        switch (event.getAction() & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_DOWN:
                savedMatrix.set(matrix);
                start.set(event.getX(), event.getY());
                mode = DRAG;
                break;
            case MotionEvent.ACTION_POINTER_DOWN:
                oldDist = spacing(event);
                if (oldDist > 10f) {
                    savedMatrix.set(matrix);
                    midPoint(mid, event);
                    mode = ZOOM;
                }
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_POINTER_UP:
                mode = NONE;
                break;
            case MotionEvent.ACTION_MOVE:
                if (mode == DRAG) {
                    matrix.set(savedMatrix);
                    float dx = event.getX() - start.x;
                    float dy = event.getY() - start.y;
                    matrix.postTranslate(dx, dy);
                }
                break;
        }

        setImageMatrix(matrix);
        return true;
    }

    private float spacing(MotionEvent event) {
        float x = event.getX(0) - event.getX(1);
        float y = event.getY(0) - event.getY(1);
        return (float) Math.sqrt(x * x + y * y);
    }

    private void midPoint(PointF point, MotionEvent event) {
        float x = event.getX(0) + event.getX(1);
        float y = event.getY(0) + event.getY(1);
        point.set(x / 2f, y / 2f);
    }

    private class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            float scaleFactor = detector.getScaleFactor();
            matrix.postScale(scaleFactor, scaleFactor, detector.getFocusX(), detector.getFocusY());
            setImageMatrix(matrix);
            return true;
        }
    }

    public Bitmap getCroppedCircleBitmap(RectF circleRect) {
        setDrawingCacheEnabled(true);
        Bitmap viewBitmap = Bitmap.createBitmap(getDrawingCache());
        setDrawingCacheEnabled(false);

        if (viewBitmap == null) return null;

        int left = Math.max(0, (int) circleRect.left);
        int top = Math.max(0, (int) circleRect.top);
        int width = Math.min(viewBitmap.getWidth() - left, (int) circleRect.width());
        int height = Math.min(viewBitmap.getHeight() - top, (int) circleRect.height());

        if (width <= 0 || height <= 0) return null;

        Bitmap croppedSquare = Bitmap.createBitmap(viewBitmap, left, top, width, height);

        // Scale down to standard 300x300 for profile picture saving
        return Bitmap.createScaledBitmap(croppedSquare, 300, 300, true);
    }
}