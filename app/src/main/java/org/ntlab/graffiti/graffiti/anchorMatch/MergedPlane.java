package org.ntlab.graffiti.graffiti.anchorMatch;

import com.google.ar.core.Anchor;
import com.google.ar.core.Plane;
import com.google.ar.core.Pose;

import org.ntlab.graffiti.common.geometry.GeometryUtil;
import org.ntlab.graffiti.common.geometry.Vector;
import org.ntlab.graffiti.entities.PointPlane2D;
import org.ntlab.graffiti.entities.PointTex2D;

import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class MergedPlane extends Plane {
    private Plane originalPlane;
    private Plane currentPlane;
    private FloatBuffer mergedPolygon = null;
    private FloatBuffer currentPolygon = null;
    private List<PointTex2D> stroke = new ArrayList<>();
    private int drawnStrokeIndex = 0;

    public MergedPlane(Plane originalPlane) {
        this.originalPlane = originalPlane;
        this.currentPlane = originalPlane;
    }

    public Plane getOriginalPlane() {
        return originalPlane;
    }

    public Pose getCenterPose() {
        return originalPlane.getCenterPose();
    }

    public Collection<Anchor> getAnchors() {
        return originalPlane.getAnchors();
    }

    public Anchor createAnchor(Pose pose) {
        return originalPlane.createAnchor(pose);
    }

    public Plane getSubsumedBy() {
        return currentPlane.getSubsumedBy();
    }

    public void setCurrentPlane(Plane myNewPlane) {
        currentPlane = myNewPlane;
    }

    public Plane getCurrentPlane() {
        return currentPlane;
    }

    public void mergePolygon(Collection<PointPlane2D> partnerPolygon, Pose partnerPose) {
        ArrayList<PointPlane2D> mergedPoints = new ArrayList<>();
        float[] partnerCenter = partnerPose.getTranslation();
        float[] partnerXAxis = partnerPose.getXAxis();
        float[] partnerZAxis = partnerPose.getZAxis();
        for (PointPlane2D p: partnerPolygon) {
            float[] target3D = GeometryUtil.localToWorld(p.getX(), p.getZ(), partnerCenter, partnerXAxis, partnerZAxis);
            float[] target2D = getPlaneLocal2D(target3D);
            mergedPoints.add(new PointPlane2D(target2D[0], target2D[1]));
        }
        float[] myPolygon = originalPlane.getPolygon().array();
        for (int i = 0; i < myPolygon.length; i += 2) {
            mergedPoints.add(new PointPlane2D(myPolygon[i], myPolygon[i+1]));
        }
        mergePolygonSub(mergedPoints);
    }

    public void updatePolygon(FloatBuffer myNewPolygonBuf) {
        Pose currentPose = currentPlane.getCenterPose();
        ArrayList<PointPlane2D> mergedPoints = new ArrayList<>();
        float[] currentCenter = currentPose.getTranslation();
        float[] currentXAxis = currentPose.getXAxis();
        float[] currentZAxis = currentPose.getZAxis();
        float[] currentPolygon = myNewPolygonBuf.array();
        this.currentPolygon = FloatBuffer.allocate(currentPolygon.length * 2);
        for (int i = 0; i < currentPolygon.length; i += 2) {
            float[] target3D = GeometryUtil.localToWorld(currentPolygon[i], currentPolygon[i+1], currentCenter, currentXAxis, currentZAxis);
            float[] target2D = getPlaneLocal2D(target3D);
            mergedPoints.add(new PointPlane2D(target2D[0], target2D[1]));
            this.currentPolygon.put(target2D[0]);
            this.currentPolygon.put(target2D[1]);
        }
        this.currentPolygon.position(0);

        FloatBuffer oldPolygon = null;
        if (mergedPolygon == null) {
            oldPolygon = originalPlane.getPolygon();
        } else {
            oldPolygon = mergedPolygon;
        }
        float[] myPolygon = oldPolygon.array();
        for (int i = 0; i < myPolygon.length; i += 2) {
            mergedPoints.add(new PointPlane2D(myPolygon[i], myPolygon[i+1]));
        }
        mergePolygonSub(mergedPoints);
    }

    private void mergePolygonSub(ArrayList<PointPlane2D> mergedPoints) {
        ArrayList<PointPlane2D> convexFull = new ArrayList<>();
        PointPlane2D minPoint = null;
        for (PointPlane2D p: mergedPoints) {
            if (minPoint == null || p.getZ() < minPoint.getZ() || (p.getZ() == minPoint.getZ() && p.getX() < minPoint.getX())) {
                minPoint = p;
            }
        }
        do {
            convexFull.add(minPoint);
            PointPlane2D nextPoint = mergedPoints.get(0);
            for (PointPlane2D p: mergedPoints) {
                if (p != nextPoint) {
                    float[] ab = Vector.minus(nextPoint.array(), minPoint.array());
                    float[] ac = Vector.minus(p.array(), minPoint.array());
                    float v = Vector.cross(ab, ac)[0];
                    if (v > 0 || (v == 0 && Vector.length(ac) > Vector.length(ab))) {
                        nextPoint = p;
                    }
                }
            }
            minPoint = nextPoint;
            mergedPoints.remove(minPoint);
        } while (minPoint != convexFull.get(0));

        mergedPolygon = FloatBuffer.allocate(convexFull.size() * 2);
        Collections.reverse(convexFull);
        for (PointPlane2D p: convexFull) {
            mergedPolygon.put(p.getX());
            mergedPolygon.put(p.getZ());
        }
        mergedPolygon.position(0);
    }

    public FloatBuffer getPolygon() {
        if (mergedPolygon == null) {
            return currentPlane.getPolygon();
        }
        return mergedPolygon;
    }

    public FloatBuffer getCurrentPolygon() {
        if (currentPolygon == null) return originalPlane.getPolygon();
        return currentPolygon;
    }


    public boolean isPoseInPolygon(Pose target) {
        float[] targetPos3D = target.getTranslation();
        float[] targetPos2D = getPlaneLocal2D(targetPos3D);
        for (int i = 0; i < mergedPolygon.capacity(); i += 2) {
            float[] curVertex = new float[]{mergedPolygon.get(i), mergedPolygon.get(i + 1)};
            float[] nextVertex = null;
            if (i == mergedPolygon.capacity() - 2) {
                nextVertex = new float[]{mergedPolygon.get(0), mergedPolygon.get(0)};
            } else {
                nextVertex = new float[]{mergedPolygon.get(i + 2), mergedPolygon.get(i + 3)};
            }
            float[] curToNext = Vector.minus(nextVertex, curVertex);
            float[] curToTarget = Vector.minus(targetPos2D, curVertex);
            float v = Vector.cross(curToNext, curToTarget)[0];
            if (v < 0) return false;
        }
        return true;
    }

    public float[] getPlaneLocal2D(float[] targetPos3D) {
        float[] planePos3D = originalPlane.getCenterPose().getTranslation();
        float[] planeAxisX = originalPlane.getCenterPose().getXAxis();
        float[] planeAxisZ = originalPlane.getCenterPose().getZAxis();
        return GeometryUtil.worldToLocal(targetPos3D, planePos3D, planeAxisX, planeAxisZ);
    }

    public boolean equals(Object another) {
        return currentPlane.equals(another);
    }

    public int hashCode() {
        return currentPlane.hashCode();
    }

    public List<PointTex2D> getStroke() {
        return stroke;
    }

    public void addStroke(float x, float z) {
        stroke.add(new PointTex2D(x, z));
    }

    public int getDrawnStrokeIndex() {
        return drawnStrokeIndex;
    }

    public void drawnStroke(int drawnStrokeIndex) {
        this.drawnStrokeIndex = drawnStrokeIndex;
    }
}
