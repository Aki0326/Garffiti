package org.ntlab.graffiti.common.geometry;

/**
 * Transform the coordinates of the local coordinate system and the world coordinate system
 * @author a-hongo
 */
public class GeometryUtil {
    public static float[] localToWorld(float localX, float localZ, float[] localCenter, float[] localXAxis, float[] localZAxis) {
        return Vector.add(Vector.add(localCenter, Vector.scale(localXAxis, localX)), Vector.scale(localZAxis, localZ));
    }

    public static float[] worldToLocal(float[] world, float[] localCenter, float[] localXAxis, float[] localZAxis) {
        float[] worldMinusLocalCenter = Vector.minus(world, localCenter);
        float localX = Vector.dot(worldMinusLocalCenter, localXAxis);
        float localZ = Vector.dot(worldMinusLocalCenter, localZAxis);
        return new float[]{localX, localZ};
    }
}
