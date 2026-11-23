package com.app.projectcecily;

import android.content.Context;
import android.graphics.Color;

public class IconManager {

    public static int getNotificationSmallIcon() {
        return R.drawable.ic_notification_small;
    }

    public static android.graphics.Bitmap getNotificationLargeIcon(Context context) {
        if (context == null) return null;
        try {
            int size = 192;
            android.graphics.Bitmap bitmap = android.graphics.Bitmap.createBitmap(size, size, android.graphics.Bitmap.Config.ARGB_8888);
            android.graphics.Canvas canvas = new android.graphics.Canvas(bitmap);

            android.graphics.Paint bgPaint = new android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG);
            bgPaint.setColor(Color.WHITE);
            canvas.drawRoundRect(0, 0, size, size, size * 0.22f, size * 0.22f, bgPaint);

            android.graphics.drawable.Drawable drawable = context.getDrawable(R.drawable.ic_app_logo);
            if (drawable != null) {
                int padding = (int) (size * 0.12f);
                drawable.setBounds(padding, padding, size - padding, size - padding);
                drawable.draw(canvas);
            }
            return bitmap;
        } catch (Exception e) {
            return null;
        }
    }
}
