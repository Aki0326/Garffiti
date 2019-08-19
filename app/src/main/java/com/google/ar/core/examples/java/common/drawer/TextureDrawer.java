package com.google.ar.core.examples.java.common.drawer;

import android.graphics.Canvas;

abstract public class TextureDrawer {
    protected int color;

    public TextureDrawer(int color) {
        this.color = color;
    }

    abstract public void draw(int pixelX, int pixelY, int r, Canvas canvas);
}
