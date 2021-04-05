package org.ntlab.graffiti.entities;

/**
 * Entity PointTex2D
 * @author a-hongo
 */
public class PointTex2D {
    float x;
    float y;

    public PointTex2D() {
    }

    public PointTex2D(float x, float y) {
        this.x = x;
        this.y = y;
    }

    public float getX() {
        return x;
    }

    public void setX(float x) {
        this.x = x;
    }

    public float getY() {
        return y;
    }

    public void setY(float y) {
        this.y = y;
    }
}
