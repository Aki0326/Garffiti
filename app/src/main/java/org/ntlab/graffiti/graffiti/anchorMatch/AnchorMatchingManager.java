package org.ntlab.graffiti.graffiti.anchorMatch;

import android.util.Log;

import com.google.ar.core.Anchor;
import com.google.ar.core.Plane;
import com.google.ar.core.Pose;
import com.google.ar.core.TrackingState;

import org.ntlab.graffiti.common.geometry.GeometryUtil;
import org.ntlab.graffiti.common.geometry.Vector;
import org.ntlab.graffiti.entities.CloudAnchor;
import org.ntlab.graffiti.entities.PointTex2D;

import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Manager for the process of matching cloud anchors.
 * @author a-hongo
 */
public class AnchorMatchingManager {
    private static final String TAG_TEST = AnchorMatchingManager.class.getSimpleName();
    private static final String TAG_STROKE = AnchorMatchingManager.class.getSimpleName() + "Stroke";
    private static final String TAG_ANCHOR = AnchorMatchingManager.class.getSimpleName() + "Anchor";

    private static Map<Anchor, Plane> pendingAnchors = new HashMap<>();
    private static Map<Anchor, Plane> myAnchors = new ConcurrentHashMap<>();
    private static List<Anchor> partnerAnchors = new CopyOnWriteArrayList<>();
    private static Map<String, MatchedAnchor> matchedAnchors = new HashMap<>();
    private static Map<Plane, MatchedAnchor> planeAnchors = new HashMap<>();

    public interface UpdateAnchorListener {

        public Anchor createAnchor(Plane plane);

        public void onAnchorMatched(MatchedAnchor matchedAnchor);
    }

    public interface UpdatePlaneListener {

        public void onUpdatePlaneAfterMatched(MatchedAnchor matchedAnchor, Plane plane);
    }

    public Plane getmergedPlaneByPlane(Plane plane) {
        // Check if the hit was within the plane's or mergedPlane's polygon.
        MatchedAnchor matchedAnchor = planeAnchors.get(plane);
        if (matchedAnchor == null || matchedAnchor.getmergedPlane() == null) {
            return plane;
        } else {
            Plane mergedPlane = matchedAnchor.getmergedPlane();
            return mergedPlane;
        }
    }

    public Anchor getMyAnchorByPlane(Plane hitPlane) {
        // Check if the hit was within the plane's polygon.
        if(hitPlane instanceof MergedPlane) {
            MatchedAnchor matchedAnchor = planeAnchors.get(hitPlane);
            return matchedAnchor.getMyAnchor();
        } else {
            if (myAnchors.containsValue(hitPlane)) {
                for (Anchor anchor : myAnchors.keySet()) {
                    Plane plane = myAnchors.get(anchor);
                    if (plane.equals(hitPlane)) {
                        return anchor;
                    }
                }
            }
            return null;
        }
    }

    public Collection<MergedPlane> getDrawnPlanes() {
        Collection<MergedPlane> mergedPlanes = new ArrayList<>();
        for (MatchedAnchor matchedAnchor : matchedAnchors.values()) {
            // TODO BUG when simply plane
            if (matchedAnchor.getmergedPlane() instanceof MergedPlane) {
                MergedPlane mergedPlane = (MergedPlane) matchedAnchor.getmergedPlane();
                mergedPlanes.add(mergedPlane);
            }
        }
        return mergedPlanes;
    }

    public void updateState(Collection<Plane> updatePlanes, UpdateAnchorListener updateAnchorListener, UpdatePlaneListener updatePlaneListener) {
        for (Plane newPlane : updatePlanes) {
            Log.d(TAG_TEST,  "UpdatePlane:" + newPlane + ", " + newPlane.getCenterPose() + ", " + newPlane.getSubsumedBy());
            if (newPlane.getTrackingState() == TrackingState.TRACKING && newPlane.getSubsumedBy() == null) {
                // newPlane 一番外側のPlaneのみ
                boolean flag = false;
                Plane oldPlane = null;
                for (Map.Entry<Plane, MatchedAnchor> planeAnchorEntry : planeAnchors.entrySet()) {
                    MatchedAnchor matchedAnchor = planeAnchorEntry.getValue();
                    Plane plane = matchedAnchor.getmergedPlane();
                    if (plane.getSubsumedBy() != null && plane.getSubsumedBy().equals(newPlane)) {
                        Log.d(TAG_TEST, "planeAnchorSubsumed." + newPlane);
                        flag = true;
                        // 既にMatchedAnchorsに入っているplaneがmergedPlaneだったときもPlaneだったときも
                        // 座標変換 myAnchor座標系でのnewPlaneの位置を求めたい newPlane->myAnchor + margePlane
                        plane = matchedAnchor.updatePlane(newPlane);
                        FloatBuffer currentPolygon = ((MergedPlane) plane).getCurrentPolygon();
                        updatePlaneListener.onUpdatePlaneAfterMatched(matchedAnchor, plane);
                        matchedAnchor.setPrevPolygon(currentPolygon);
                        oldPlane = planeAnchorEntry.getKey();
                        break;
                    }
                }
                if (oldPlane != null) planeAnchors.put(newPlane, planeAnchors.remove(oldPlane));

                HashMap<Anchor, Plane> cloneMyAnchors = new HashMap<>(myAnchors);
                for (Map.Entry<Anchor, Plane> myAnchorEntry : cloneMyAnchors.entrySet()) {
                    if (myAnchorEntry.getValue() != null && myAnchorEntry.getValue().getSubsumedBy() != null && myAnchorEntry.getValue().getSubsumedBy().equals(newPlane)) {
//                        if (!flag) {
                        Log.d(TAG_TEST, "myAnchorSubsumed." + newPlane);
                        flag = true;
                        // 座標変換 myAnchor座標系でのnewPlaneの位置を求めたい newPlane->myAnchor + margePlane
                        Plane plane = myAnchorEntry.getValue();
                        if (!(plane instanceof MergedPlane)) {
                            plane = new MergedPlane(plane);
                        }
                        ((MergedPlane) plane).setCurrentPlane(newPlane);
                        ((MergedPlane) plane).updatePolygon(newPlane.getPolygon());
                        myAnchors.put(myAnchorEntry.getKey(), plane);
//                                } else {
//                                    Log.d(TAGTEST, "myAnchors remove:" + myAnchorEntry.getKey());
//                                    myAnchors.remove(myAnchorEntry.getKey());
//                                }
                        // 座標変換
//                                    Anchor myAnchor = sharedAnchorEntry.getValue().getMyAnchor();
//                                    FloatBuffer newPlanePolygon = newPlane.getPolygon();
//                                    Pose myPose = myAnchor.getPose();
//                                    Pose newPlanePose = newPlane.getCenterPose();
//                                    Pose myInversePose = myPose.inverse();
//                                    FloatBuffer polygon = FloatBuffer.allocate(newPlanePolygon.capacity());
//                                    for (int i = 0; i < newPlanePolygon.capacity(); i += 2) {
//                                        PointPlane2D newPlaneLocal = new PointPlane2D(newPlanePolygon.get(i), newPlanePolygon.get(i+1));
//                                        float[] newPlaneRotated = newPlanePose.rotateVector(new float[]{newPlaneLocal.getX(), 0f, newPlaneLocal.getZ()});
//                                        float[] world = newPlanePose.transformPoint(newPlaneRotated);
//                                        float[] myTransformedPose = myInversePose.transformPoint(world);
//                                        float[] myLocal = myInversePose.rotateVector(myTransformedPose);
//                                        polygon.put(myLocal[0]);
//                                        polygon.put(myLocal[2]);
//                                    }
//                                    polygon.rewind();
                    }
                }
                HashMap<Anchor, Plane> clonePendingAnchors = new HashMap<>(pendingAnchors);
                for (Map.Entry<Anchor, Plane> pendingAnchorEntry : clonePendingAnchors.entrySet()) {
                    if (pendingAnchorEntry.getValue().getSubsumedBy() != null && pendingAnchorEntry.getValue().getSubsumedBy().equals(newPlane)) {
//                                if (!flag) {
//                                    Log.d(TAGTEST, "pendingAnchorSubsumed." + newPlane);
                        flag = true;
                        // 座標変換
//                                    Plane plane = pendingAnchorEntry.getValue();
//                                    if (!(plane instanceof MergedPlane)) {
//                                        plane = new MergedPlane(plane);
//                                    }
//                                    ((MergedPlane) plane).setCurrentPlane(newPlane);
//                                    ((MergedPlane) plane).updatePolygon(newPlane.getPolygon());
//                                        pendingAnchorEntry.setValue(plane);
//                                    pendingAnchors.put(pendingAnchorEntry.getKey(), plane);
//                                } else {
                        Log.d(TAG_TEST, ", pendingAnchors.remove, " + pendingAnchorEntry.getValue());
                        pendingAnchors.remove(pendingAnchorEntry.getKey());
//                                }
                    }
                }
                if (!pendingAnchors.values().contains(newPlane) && !myAnchors.values().contains(newPlane)) {
                    if (!planeAnchors.keySet().contains(newPlane)) {
                        if (!flag) {
                            // ホストするのに時間が掛かるため, hostListenerを渡し、後にcallbackする
                            Anchor hostAnchor = updateAnchorListener.createAnchor(newPlane);
                            pendingAnchors.put(hostAnchor, newPlane);
                            Log.d(TAG_TEST, ", pendingAnchors.put, " + hostAnchor.getCloudAnchorId() + ", " + newPlane + ", " + newPlane.getSubsumedBy());
                        }
                    } else {
                        // 既にplaneAnchors含まれている同じ平面のpolygon情報のみが更新された場合
                        MatchedAnchor matchedAnchor = planeAnchors.get(newPlane);
                        if (!matchedAnchor.getPrevPolygon().equals(newPlane.getPolygon())) {
                            updatePlaneListener.onUpdatePlaneAfterMatched(matchedAnchor, newPlane);
                            matchedAnchor.setPrevPolygon(newPlane.getPolygon());
                        }
                        // REST merged?
                    }
                }
            }
        }

        for (Anchor myAnchor : myAnchors.keySet()) {
            for (Anchor partnerAnchor : partnerAnchors) {
                Pose myPose = myAnchor.getPose();
                Pose partnerPose = partnerAnchor.getPose();
                if (Vector.dot(myPose.getYAxis(), partnerPose.getYAxis()) > 0.95) {
                    float[] sub = Vector.minus(partnerPose.getTranslation(), myPose.getTranslation());
                    if (Math.abs(Vector.dot(sub, myPose.getYAxis())) < 0.15 && Vector.length(sub) < 1.0) {
                        Plane myPlane = myAnchors.get(myAnchor);
                        String partnerAnchorId = partnerAnchor.getCloudAnchorId();
                        MatchedAnchor matchedAnchor = new MatchedAnchor(myAnchor, partnerAnchor, myPlane);
                        matchedAnchors.put(partnerAnchorId, matchedAnchor);
                        Log.d(TAG_TEST, "matchedAnchors.put:" + partnerAnchorId);
                        if (!(myPlane instanceof MergedPlane)) {
                            planeAnchors.put(myPlane, matchedAnchor);
                        } else {
                            planeAnchors.put(((MergedPlane) myPlane).getCurrentPlane(), matchedAnchor);
                        }
                        Log.d(TAG_TEST, "planeAnchors.put:" + myAnchor.getCloudAnchorId());
                        myAnchors.remove(myAnchor);
                        partnerAnchors.remove(partnerAnchor);
                        updateAnchorListener.onAnchorMatched(matchedAnchor);
                        updatePlaneListener.onUpdatePlaneAfterMatched(matchedAnchor, myPlane);
                        matchedAnchors.get(partnerAnchorId).setPrevPolygon(myPlane.getPolygon());
                    }
                }
            }
        }
    }

    public boolean isPendingSubmission(Anchor anchor) {
        if (pendingAnchors.get(anchor) != null) return true;
        return false;
    }

    public Plane submit(Anchor anchor) {
        Plane plane = pendingAnchors.remove(anchor);
        myAnchors.put(anchor, plane);
        return plane;
    }

    public void storePartner(Anchor anchor) {
        boolean flag = false;
        for (Anchor myAnchor: myAnchors.keySet()) {
            if (myAnchor.getCloudAnchorId().equals(anchor.getCloudAnchorId())) {
                flag = true;
                break;
            }
        }
        if (!flag) {
            for (Anchor partnerAnchor : partnerAnchors) {
                if (partnerAnchor.getCloudAnchorId().equals(anchor.getCloudAnchorId())) {
                    flag = true;
                    break;
                }
            }
        }
        if (!flag) {
            partnerAnchors.add(anchor);
            Log.d(TAG_TEST, "partnerAnchors.put:" + anchor + ", " + anchor.getCloudAnchorId() + ", (" + anchor.getPose().getTranslation()[0] + ", " + anchor.getPose().getTranslation()[1] + ", " + anchor.getPose().getTranslation()[2] + ")");
        } else {
            Log.d(TAG_TEST, "partnerAnchors.notput:" + anchor + ", " + anchor.getCloudAnchorId() + ", (" + anchor.getPose().getTranslation()[0] + ", " + anchor.getPose().getTranslation()[1] + ", " + anchor.getPose().getTranslation()[2] + ")");
        }
    }

    public void updateMatchedAnchor(String cloudAnchorId, CloudAnchor cloudAnchor) {
        MatchedAnchor matchedAnchor = matchedAnchors.get(cloudAnchorId);
        List<PointTex2D> newStroke = cloudAnchor.getStroke();
        if (matchedAnchor != null) {
            //TODO BUG strokeも同時に入っている
            if (cloudAnchor.getPlane() != null) matchedAnchor.margePlane(cloudAnchor.getPlane().getPolygon());
            if(matchedAnchor.getmergedPlane() instanceof MergedPlane) {
                // 座標変換 stroke
                Anchor partnerAnchor = matchedAnchor.getPartnerAnchor();
                Pose myPlanePose = ((MergedPlane) matchedAnchor.getmergedPlane()).getCurrentPlane().getCenterPose();
                float[] myCenter = myPlanePose.getTranslation();
                float[] myXAxis = myPlanePose.getXAxis();
                float[] myZAxis = myPlanePose.getZAxis();
                Pose partnerAnchorPose = partnerAnchor.getPose();
                float[] partnerCenter = partnerAnchorPose.getTranslation();
                float[] pertnerXAxis = partnerAnchorPose.getXAxis();
                float[] partnerZAxis = partnerAnchorPose.getZAxis();
                MergedPlane mergedPlane = (MergedPlane) matchedAnchor.getmergedPlane();
                if (newStroke.size() > mergedPlane.getStroke().size()) {
                    for (int i = mergedPlane.getStroke().size(); i < newStroke.size(); i++) {
                        PointTex2D partnerLocal = newStroke.get(i);
                        Log.d(TAG_STROKE, i + " partnerLocal: " + partnerLocal.getX() + ", " + partnerLocal.getY());
                        float[] world = GeometryUtil.localToWorld(partnerLocal.getX(), partnerLocal.getY(), partnerCenter, pertnerXAxis, partnerZAxis);
                        float[] planeLocal = GeometryUtil.worldToLocal(world, myCenter, myXAxis, myZAxis);
                        mergedPlane.addStroke(planeLocal[0], -planeLocal[1]);
                        Log.d(TAG_STROKE, i + " myLocal: " + planeLocal[0] + ", " + -planeLocal[1]);
                    }
                }
            }
        } else {
            //myAnchor
            for (Anchor anchor: partnerAnchors) {
                anchor.getCloudAnchorId().equals(cloudAnchorId);
            }
        }
    }

    public void updateLog() {
        for (Anchor anchor: myAnchors.keySet()) {
            Log.d(TAG_ANCHOR,  ", myAnchorPose, " + anchor.getCloudAnchorId() + ", " + anchor.getPose() + ", (" + anchor.getPose().getXAxis().toString() + ", " + anchor.getPose().getZAxis().toString() + ")");
        }
        for (Anchor anchor: partnerAnchors) {
            Log.d(TAG_ANCHOR,  ", partnerPose, " + anchor.getCloudAnchorId() + ", " + anchor.getPose() + ", (" + anchor.getPose().getXAxis().toString() + ", " + anchor.getPose().getZAxis().toString() + ")");
        }
    }

    public void endLog() {
        int cnt = 0;

        for (Anchor anchor: myAnchors.keySet()) {
            Log.d(TAG_TEST,  "myAnchors:" + anchor + ", " + anchor.getCloudAnchorId() + ", " + myAnchors.get(anchor));
        }
        for (Anchor anchor: partnerAnchors) {
            Log.d(TAG_TEST,  "partnerAnchors:" + anchor + ", " + anchor.getCloudAnchorId());
        }

        cnt = 0;
        for (Plane plane: planeAnchors.keySet()) {
            if (planeAnchors.get(plane).getmergedPlane() instanceof MergedPlane) {
                float[] planeXAxis = plane.getCenterPose().getXAxis();
                float[] planeZAxis = plane.getCenterPose().getZAxis();
                float[] mergedMyPlaneXAxis = planeAnchors.get(plane).getMyAnchor().getPose().getXAxis();
                float[] mergedMyPlaneZAxis = planeAnchors.get(plane).getMyAnchor().getPose().getZAxis();
                float[] mergedPartnerPlaneXAxis = planeAnchors.get(plane).getPartnerAnchor().getPose().getXAxis();
                float[] mergedPartnerPlaneZAxis = planeAnchors.get(plane).getPartnerAnchor().getPose().getZAxis();
                Log.d(TAG_STROKE,  "mergedMyPlane: " + plane + ", " + planeAnchors.get(plane).getMyAnchor().getCloudAnchorId() + ", (" + mergedMyPlaneXAxis[0] + ", " + mergedMyPlaneXAxis[1] + ", " + mergedMyPlaneXAxis[2] + "), "+ "(" + mergedMyPlaneZAxis[0] + ", " + mergedMyPlaneZAxis[1] + ", " + mergedMyPlaneZAxis[2] + "), (" + planeXAxis[0] + ", " + planeXAxis[1] + ", " + planeXAxis[2] + "), (" + planeZAxis[0] + ", " + planeZAxis[1] + ", " + planeZAxis[2] + "), ");
                Log.d(TAG_STROKE,  "mergedPartnerPlane: " + plane + ", " + planeAnchors.get(plane).getPartnerAnchor().getCloudAnchorId() + ", (" + mergedPartnerPlaneXAxis[0] + ", " + mergedPartnerPlaneXAxis[1] + ", " + mergedPartnerPlaneXAxis[2] + "), "+ "(" + mergedPartnerPlaneZAxis[0] + ", " + mergedPartnerPlaneZAxis[1] + ", " + mergedPartnerPlaneZAxis[2] + ")");
                cnt++;
            }
        }
        Log.d(TAG_TEST,  "mergedPlaneSize:" + cnt);

    }


}
