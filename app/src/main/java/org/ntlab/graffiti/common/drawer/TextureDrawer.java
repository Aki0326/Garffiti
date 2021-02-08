package org.ntlab.graffiti.common.drawer;

import android.graphics.Canvas;

/**
 * Abstract class of drawing shape.
 * @author a-hongo
 */
abstract public class TextureDrawer {
    protected int color;

    public TextureDrawer(int color) {
        this.color = color;
    }

    abstract public void draw(int pixelX, int pixelY, int r, Canvas canvas);
}
