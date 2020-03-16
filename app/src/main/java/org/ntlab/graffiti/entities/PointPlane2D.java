package org.ntlab.graffiti.entities;

public class PointPlane2D {
    float x;
    float z;

    public PointPlane2D() {
    }

    public PointPlane2D(float x, float z) {
        this.x = x;
        this.z = z;
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
        return new float[]{x, z};
    }
}
