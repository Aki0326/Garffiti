package org.ntlab.graffiti.entities;

/**
 * Entity PointPlane2D
 * @author a-hongo
 */
public class PointPlane2D {
    private float x;
    private float z;

    public PointPlane2D() {
    }

    public PointPlane2D(float x, float z) {
        this.setX(x);
        this.setZ(z);
    }

    public float getX() {
        return x;
    }

    public void setX(float x) {
        this.x = x;
    }

    public float getZ() {
        return z;
    }

    public void setZ(float z) {
        this.z = z;
    }


    public float[] array() {
        return new float[]{getX(), getZ()};
    }
}
