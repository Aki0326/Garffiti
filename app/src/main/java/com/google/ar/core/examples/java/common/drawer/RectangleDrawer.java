package com.google.ar.core.examples.java.common.drawer;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;

public class RectangleDrawer extends TextureDrawer {

    public RectangleDrawer(int color) {
        super(color);
    }

    public void draw(int pixelX, int pixelY, int r, Canvas canvas) {
        Paint paint = new Paint();
        if (color == Color.TRANSPARENT) {
//                paint.setColor(Color.TRANSPARENT);
            paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
        } else {
            paint.setColor(color);
        }
        paint.setStyle(Paint.Style.FILL);
        canvas.drawCircle(pixelX, pixelY, r, paint);
    }
}
