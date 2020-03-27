package org.ntlab.graffiti.entities;

import com.google.ar.core.Anchor;
import com.google.ar.core.Plane;
import com.google.ar.core.Point;
import com.google.ar.core.Pose;

import org.ntlab.graffiti.common.geometry.Vector;

import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class SharedPlane extends Plane {
    private Plane originalPlane;
    private Plane currentPlane;
    private FloatBuffer margedPolygon = null;
    private FloatBuffer currentPolygon = null;
    private List<PointTex2D> stroke = new ArrayList<>();
    private int drawnStrokeIndex = 0;

    public SharedPlane(Plane originalPlane) {
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

    public void margePolygon(Collection<PointPlane2D> partnerPolygon, Pose partnerPose) {
        ArrayList<PointPlane2D> margedPoints = new ArrayList<>();
        float[] partnerCenter = partnerPose.getTranslation();
        float[] partnerXAxis = partnerPose.getXAxis();
        float[] partnerZAxis = partnerPose.getZAxis();
        for (PointPlane2D p: partnerPolygon) {
            float[] target3D = Vector.add(Vector.add(partnerCenter, Vector.scale(partnerXAxis, p.x)), Vector.scale(partnerZAxis, p.z));
            float[] target2D = getPlaneLocal2D(target3D);
            margedPoints.add(new PointPlane2D(target2D[0], target2D[1]));
        }
        float[] myPolygon = originalPlane.getPolygon().array();
        for (int i = 0; i < myPolygon.length; i += 2) {
            margedPoints.add(new PointPlane2D(myPolygon[i], myPolygon[i+1]));
        }
        margePolygonSub(margedPoints);
    }

    public void updatePolygon(FloatBuffer myNewPolygonBuf) {
        Pose currentPose = currentPlane.getCenterPose();
        ArrayList<PointPlane2D> margedPoints = new ArrayList<>();
        float[] currentCenter = currentPose.getTranslation();
        float[] currentXAxis = currentPose.getXAxis();
        float[] currentZAxis = currentPose.getZAxis();
        float[] currentPolygon = myNewPolygonBuf.array();
        this.currentPolygon = FloatBuffer.allocate(currentPolygon.length * 2);
        for (int i = 0; i < currentPolygon.length; i += 2) {
            float[] target3D = Vector.add(Vector.add(currentCenter, Vector.scale(currentXAxis, currentPolygon[i])), Vector.scale(currentZAxis, currentPolygon[i+1]));
            float[] target2D = getPlaneLocal2D(target3D);
            margedPoints.add(new PointPlane2D(target2D[0], target2D[1]));
            this.currentPolygon.put(target2D[0]);
            this.currentPolygon.put(target2D[1]);
        }
        this.currentPolygon.position(0);

        FloatBuffer oldPolygon = null;
        if (margedPolygon == null) {
            oldPolygon = originalPlane.getPolygon();
        } else {
            oldPolygon = margedPolygon;
        }
        float[] myPolygon = oldPolygon.array();
        for (int i = 0; i < myPolygon.length; i += 2) {
            margedPoints.add(new PointPlane2D(myPolygon[i], myPolygon[i+1]));
        }
        margePolygonSub(margedPoints);
    }

    private void margePolygonSub(ArrayList<PointPlane2D> margedPoints) {
        ArrayList<PointPlane2D> convexFull = new ArrayList<>();
        PointPlane2D minPoint = null;
        for (PointPlane2D p: margedPoints) {
            if (minPoint == null || p.z < minPoint.z || (p.z == minPoint.z && p.x < minPoint.x)) {
                minPoint = p;
            }
        }
        do {
            convexFull.add(minPoint);
            PointPlane2D nextPoint = margedPoints.get(0);
            for (PointPlane2D p: margedPoints) {
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
            margedPoints.remove(minPoint);
        } while (minPoint != convexFull.get(0));

        margedPolygon = FloatBuffer.allocate(convexFull.size() * 2);
        Collections.reverse(convexFull);
        for (PointPlane2D p: convexFull) {
            margedPolygon.put(p.x);
            margedPolygon.put(p.z);
        }
        margedPolygon.position(0);
    }

    public FloatBuffer getPolygon() {
        if (margedPolygon == null) {
            return currentPlane.getPolygon();
        }
        return margedPolygon;
    }

    public FloatBuffer getCurrentPolygon() {
        if (currentPolygon == null) return originalPlane.getPolygon();
        return currentPolygon;
    }


    public boolean isPoseInPolygon(Pose target) {
        float[] targetPos3D = target.getTranslation();
        float[] targetPos2D = getPlaneLocal2D(targetPos3D);
        for (int i = 0; i < margedPolygon.capacity(); i += 2) {
            float[] curVertex = new float[]{margedPolygon.get(i), margedPolygon.get(i + 1)};
            float[] nextVertex = null;
            if (i == margedPolygon.capacity() - 2) {
                nextVertex = new float[]{margedPolygon.get(0), margedPolygon.get(0)};
            } else {
                nextVertex = new float[]{margedPolygon.get(i + 2), margedPolygon.get(i + 3)};
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
        float[] planeToTarget3D = Vector.minus(targetPos3D, planePos3D);
        return new float[]{Vector.dot(planeToTarget3D, planeAxisX), Vector.dot(planeToTarget3D, planeAxisZ)};
    }

    public boolean equals(Object another) {
        return originalPlane.equals(another);
    }

    public int hashCode() {
        return originalPlane.hashCode();
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
