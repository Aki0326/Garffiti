package com.google.ar.core.examples.java.common.geometry;

/**
 * Geometry to compute vector.
 */
public class Vector {
    public static float dot(float[] vec1, float[] vec2) {
        if (vec1.length == 2 && vec2.length == 2) {
            return vec1[0] * vec2[0] + vec1[1] * vec2[1];
        } else if (vec1.length == 3 && vec2.length == 3) {
            return vec1[0] * vec2[0] + vec1[1] * vec2[1] + vec1[2] * vec2[2];
        }
        return 0.0f;
    }

    public static float[] cross(float[] vec1, float[] vec2) {
        if (vec1.length == 2 && vec2.length == 2) {
            return new float[]{vec1[0] * vec2[1] - vec1[1] * vec2[0]};
        } else if (vec1.length == 3 && vec2.length == 3) {
            return new float[]{vec1[1] * vec2[2] - vec1[2] * vec2[1], vec1[2] * vec2[0] - vec1[0] * vec2[2], vec1[0] * vec2[1] - vec1[1] * vec2[0]};
        }
        return null;
    }

    public static float[] add(float[] vec1, float[] vec2) {
        if (vec1.length == 2 && vec2.length == 2) {
            return new float[]{vec1[0] + vec2[0], vec1[1] + vec2[1]};
        } else if (vec1.length == 3 && vec2.length == 3) {
            return new float[]{vec1[0] + vec2[0], vec1[1] + vec2[1], vec1[2] + vec2[2]};
        }
        return null;
    }

    public static float[] minus(float[] vec1, float[] vec2) {
        if (vec1.length == 2 && vec2.length == 2) {
            return new float[]{vec1[0] - vec2[0], vec1[1] - vec2[1]};
        } else if (vec1.length == 3 && vec2.length == 3) {
            return new float[]{vec1[0] - vec2[0], vec1[1] - vec2[1], vec1[2] - vec2[2]};
        }
        return null;
    }

    public static float squareLength(float[] vec) {
        if (vec.length == 2) {
            return vec[0] * vec[0] + vec[1] * vec[1];
        } else if (vec.length == 3) {
            return vec[0] * vec[0] + vec[1] * vec[1] + vec[2] * vec[2];
        }
        return 0.0f;
    }

    public static float length(float[] vec) {
        return (float)Math.sqrt(squareLength(vec));
    }

    public static float[] normalize(float[] vec) {
        float l = length(vec);
        return scale(vec, 1.0f / l);
    }

    public static float[] scale(float[] vec, float s) {
        if (vec.length == 2) {
            return new float[]{vec[0] * s, vec[1] * s};
        } else if (vec.length == 3) {
            return new float[]{vec[0] * s, vec[1] * s, vec[2] * s};
        }
        return null;
    }
}
