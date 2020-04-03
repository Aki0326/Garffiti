package org.ntlab.graffiti.graffiti.anchorMatch;

import android.util.Log;

import com.google.ar.core.Anchor;
import com.google.ar.core.Plane;
import com.google.ar.core.Pose;

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

public class AnchorMatchingManager {
    private static final String TAGTEST = AnchorMatchingManager.class.getSimpleName();
    private static final String TAGSTROKE = AnchorMatchingManager.class.getSimpleName() + "Stroke";
    private static final String TAGANCHOR = AnchorMatchingManager.class.getSimpleName() + "Anchor";

    private static Map<Anchor, Plane> pendingAnchors = new HashMap<>();
    private static Map<Anchor, Plane> myAnchors = new HashMap<>();
    private static List<Anchor> partnerAnchors = new ArrayList<>();
    private static Map<String, MatchedAnchor> matchedAnchors = new HashMap<>();
    private static Map<Plane, MatchedAnchor> planeAnchors = new HashMap<>();

    public interface UpdateAnchorListener {

        public Anchor createAnchor(Plane plane);

        public void onAnchorMatched(MatchedAnchor matchedAnchor);
    }

    public interface UpdatePlaneListener {

        public void onUpdatePlaneAfterMatched(MatchedAnchor matchedAnchor, Plane plane);
    }

    public Plane getMargedPlaneByPlane(Plane plane) {
        // Check if the hit was within the plane's or margedPlane's polygon.
        MatchedAnchor matchedAnchor = planeAnchors.get(plane);
        if (matchedAnchor == null || matchedAnchor.getMargedPlane() == null) {
            return plane;
        } else {
            Plane margedPlane = matchedAnchor.getMargedPlane();
            return margedPlane;
        }
    }

    public Anchor getMyAnchorByPlane(Plane hitPlane) {
        // Check if the hit was within the plane's polygon.
        if(hitPlane instanceof MargedPlane) {
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

    public Collection<MargedPlane> getDrawnPlanes() {
        Collection<MargedPlane> margedPlanes = new ArrayList<>();
        for (MatchedAnchor matchedAnchor : matchedAnchors.values()) {
            // BUG when simply plane
            if (matchedAnchor.getMargedPlane() instanceof MargedPlane) {
                MargedPlane margedPlane = (MargedPlane) matchedAnchor.getMargedPlane();
                margedPlanes.add(margedPlane);
            }
        }
        return margedPlanes;
    }

    public void updateState(Collection<Plane> updatePlanes, UpdateAnchorListener updateAnchorListener, UpdatePlaneListener updatePlaneListener) {
        for (Plane newPlane : updatePlanes) {
            Log.d(TAGTEST,  "UpdatePlane:" + newPlane + ", " + newPlane.getCenterPose() + ", " + newPlane.getSubsumedBy());
            if (newPlane.getSubsumedBy() == null) {
                // newPlane 一番外側のPlaneのみ
                boolean flag = false;
                Plane oldPlane = null;
                for (Map.Entry<Plane, MatchedAnchor> planeAnchorEntry : planeAnchors.entrySet()) {
                    MatchedAnchor matchedAnchor = planeAnchorEntry.getValue();
                    Plane plane = matchedAnchor.getMargedPlane();
                    if (plane.getSubsumedBy() != null && plane.getSubsumedBy().equals(newPlane)) {
                        Log.d(TAGTEST, "planeAnchorSubsumed." + newPlane);
                        flag = true;
                        // 既にMatchedAnchorsに入っているplaneがMargedPlaneだったときもPlaneだったときも
                        // 座標変換 myAnchor座標系でのnewPlaneの位置を求めたい newPlane->myAnchor + margePlane
                        plane = matchedAnchor.updatePlane(newPlane);
                        FloatBuffer currentPolygon = ((MargedPlane) plane).getCurrentPolygon();
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
                        Log.d(TAGTEST, "myAnchorSubsumed." + newPlane);
                        flag = true;
                        // 座標変換 myAnchor座標系でのnewPlaneの位置を求めたい newPlane->myAnchor + margePlane
                        Plane plane = myAnchorEntry.getValue();
                        if (!(plane instanceof MargedPlane)) {
                            plane = new MargedPlane(plane);
                        }
                        ((MargedPlane) plane).setCurrentPlane(newPlane);
                        ((MargedPlane) plane).updatePolygon(newPlane.getPolygon());
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
//                                    if (!(plane instanceof MargedPlane)) {
//                                        plane = new MargedPlane(plane);
//                                    }
//                                    ((MargedPlane) plane).setCurrentPlane(newPlane);
//                                    ((MargedPlane) plane).updatePolygon(newPlane.getPolygon());
//                                        pendingAnchorEntry.setValue(plane);
//                                    pendingAnchors.put(pendingAnchorEntry.getKey(), plane);
//                                } else {
                        Log.d(TAGTEST, ", pendingAnchors.remove, " + pendingAnchorEntry.getValue());
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
                            Log.d(TAGTEST, ", pendingAnchors.put, " + hostAnchor.getCloudAnchorId() + ", " + newPlane + ", " + newPlane.getSubsumedBy());
                        }
                    } else {
                        // 既にplaneAnchors含まれている同じ平面のpolygon情報のみが更新された場合
                        MatchedAnchor matchedAnchor = planeAnchors.get(newPlane);
                        if (!matchedAnchor.getPrevPolygon().equals(newPlane.getPolygon())) {
                            updatePlaneListener.onUpdatePlaneAfterMatched(matchedAnchor, newPlane);
                            matchedAnchor.setPrevPolygon(newPlane.getPolygon());
                        }
//                                Log.d(TAGTEST, "else");
                        // REST marged?
                    }
                }
            }
        }

        for (Anchor myAnchor: myAnchors.keySet()) {
            for (Anchor partnerAnchor : partnerAnchors) {
                Pose myPose = myAnchor.getPose();
                Pose patnerPose = partnerAnchor.getPose();
//                        Log.d(TAGTEST, "?matchedAnchors?" + Vector.dot(myPose.getYAxis(), patnerPose.getYAxis()));
                if (Vector.dot(myPose.getYAxis(), patnerPose.getYAxis()) > 0.95) {
                    float[] sub = Vector.minus(patnerPose.getTranslation(), myPose.getTranslation());
//                            Log.d(TAGTEST, "?matchedAnchors?" + Math.abs(Vector.dot(sub, myPose.getYAxis())) + ", " + Vector.length(sub));
                    if (Math.abs(Vector.dot(sub, myPose.getYAxis())) < 0.15 && Vector.length(sub) < 1.0) {
                        Plane myPlane = myAnchors.get(myAnchor);
                        String partnerAnchorId = partnerAnchor.getCloudAnchorId();
                        MatchedAnchor matchedAnchor = new MatchedAnchor(myAnchor, partnerAnchor, myPlane);
                        matchedAnchors.put(partnerAnchorId, matchedAnchor);
                        Log.d(TAGTEST, "matchedAnchors.put:" + partnerAnchorId);
                        if (!(myPlane instanceof MargedPlane)) {
                            planeAnchors.put(myPlane, matchedAnchor);
                        } else {
                            planeAnchors.put(((MargedPlane) myPlane).getCurrentPlane(), matchedAnchor);
                        }
                        Log.d(TAGTEST, "planeAnchors.put:" + myAnchor.getCloudAnchorId());
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
            Log.d(TAGTEST, "partnerAnchors.put:" + anchor + ", " + anchor.getCloudAnchorId() + ", (" + anchor.getPose().getTranslation()[0] + ", " + anchor.getPose().getTranslation()[1] + ", " + anchor.getPose().getTranslation()[2] + ")");
        } else {
            Log.d(TAGTEST, "partnerAnchors.notput:" + anchor + ", " + anchor.getCloudAnchorId() + ", (" + anchor.getPose().getTranslation()[0] + ", " + anchor.getPose().getTranslation()[1] + ", " + anchor.getPose().getTranslation()[2] + ")");
        }
    }

    public void updateMatchedAnchor(String cloudAnchorId, CloudAnchor cloudAnchor) {
        MatchedAnchor matchedAnchor = matchedAnchors.get(cloudAnchorId);
        List<PointTex2D> newStroke = cloudAnchor.getStroke();
        if (matchedAnchor != null) {
            //BUG strokeも同時に入っている
            if (cloudAnchor.getPlane() != null) matchedAnchor.margePlane(cloudAnchor.getPlane().getPolygon());
            if(matchedAnchor.getMargedPlane() instanceof MargedPlane) {
                // 座標変換 stroke
                Anchor partnerAnchor = matchedAnchor.getPartnerAnchor();
                Pose myPlanePose = ((MargedPlane) matchedAnchor.getMargedPlane()).getCurrentPlane().getCenterPose();
                float[] myCenter = myPlanePose.getTranslation();
                float[] myXAxis = myPlanePose.getXAxis();
                float[] myZAxis = myPlanePose.getZAxis();
                Pose partnerAnchorPose = partnerAnchor.getPose();
                float[] partnerCenter = partnerAnchorPose.getTranslation();
                float[] pertnerXAxis = partnerAnchorPose.getXAxis();
                float[] partnerZAxis = partnerAnchorPose.getZAxis();
                MargedPlane margedPlane = (MargedPlane) matchedAnchor.getMargedPlane();
                if (newStroke.size() > margedPlane.getStroke().size()) {
                    for (int i = margedPlane.getStroke().size(); i < newStroke.size(); i++) {
                        PointTex2D partnerLocal = newStroke.get(i);
                        Log.d(TAGSTROKE, i + " partnerLocal: " + partnerLocal.getX() + ", " + partnerLocal.getY());
                        float[] world = GeometryUtil.localToWorld(partnerLocal.getX(), partnerLocal.getY(), partnerCenter, pertnerXAxis, partnerZAxis);
                        float[] planeLocal = GeometryUtil.worldToLocal(world, myCenter, myXAxis, myZAxis);
                        margedPlane.addStroke(planeLocal[0], -planeLocal[1]);
                        Log.d(TAGSTROKE, i + " myLocal: " + planeLocal[0] + ", " + -planeLocal[1]);
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
            Log.d(TAGANCHOR,  ", myAnchorPose, " + anchor.getCloudAnchorId() + ", " + anchor.getPose() + ", (" + anchor.getPose().getXAxis().toString() + ", " + anchor.getPose().getZAxis().toString() + ")");
        }
        for (Anchor anchor: partnerAnchors) {
            Log.d(TAGANCHOR,  ", partnerPose, " + anchor.getCloudAnchorId() + ", " + anchor.getPose() + ", (" + anchor.getPose().getXAxis().toString() + ", " + anchor.getPose().getZAxis().toString() + ")");
        }
    }

    public void endLog() {
        int cnt = 0;

//        Log.d(TAGTEST,  "pendingAnchorsSize:" + pendingAnchors.size() + ", myAcnhorsSize:" + myAnchors.size() + ", partnerAnchorsSize:" + partnerAnchors.size() + ", sharedAnchorsSize:" + matchedAnchors.size());
//        for (Anchor anchor: pendingAnchors.keySet()) {
//            Log.d(TAGTEST,  "pendingAnchors:" + anchor + ", " + pendingAnchors.get(anchor));
//        }

        for (Anchor anchor: myAnchors.keySet()) {
            Log.d(TAGTEST,  "myAnchors:" + anchor + ", " + anchor.getCloudAnchorId() + ", " + myAnchors.get(anchor));
        }
        for (Anchor anchor: partnerAnchors) {
            Log.d(TAGTEST,  "partnerAnchors:" + anchor + ", " + anchor.getCloudAnchorId());
        }

//        cnt = 0;
//        for (MatchedAnchor sharedAnchor: matchedAnchors.values()) {
//            if (sharedAnchor.getMargedPlane() instanceof MargedPlane) {
//                Log.d(TAGTEST,  "margedPlane:" + sharedAnchor.getMyAnchor().getCloudAnchorId() + ", " + sharedAnchor.getPartnerAnchor().getCloudAnchorId());
//                cnt++;
//            }
//        }
        cnt = 0;
        for (Plane plane: planeAnchors.keySet()) {
            if (planeAnchors.get(plane).getMargedPlane() instanceof MargedPlane) {
                float[] planeXAxis = plane.getCenterPose().getXAxis();
                float[] planeZAxis = plane.getCenterPose().getZAxis();
                float[] margedMyPlaneXAxis = planeAnchors.get(plane).getMyAnchor().getPose().getXAxis();
                float[] margedMyPlaneZAxis = planeAnchors.get(plane).getMyAnchor().getPose().getZAxis();
                float[] margedPartnerPlaneXAxis = planeAnchors.get(plane).getPartnerAnchor().getPose().getXAxis();
                float[] margedPartnerPlaneZAxis = planeAnchors.get(plane).getPartnerAnchor().getPose().getZAxis();
                Log.d(TAGSTROKE,  "margedMyPlane: " + plane + ", " + planeAnchors.get(plane).getMyAnchor().getCloudAnchorId() + ", (" + margedMyPlaneXAxis[0] + ", " + margedMyPlaneXAxis[1] + ", " + margedMyPlaneXAxis[2] + "), "+ "(" + margedMyPlaneZAxis[0] + ", " + margedMyPlaneZAxis[1] + ", " + margedMyPlaneZAxis[2] + "), (" + planeXAxis[0] + ", " + planeXAxis[1] + ", " + planeXAxis[2] + "), (" + planeZAxis[0] + ", " + planeZAxis[1] + ", " + planeZAxis[2] + "), ");
                Log.d(TAGSTROKE,  "margedPartnerPlane: " + plane + ", " + planeAnchors.get(plane).getPartnerAnchor().getCloudAnchorId() + ", (" + margedPartnerPlaneXAxis[0] + ", " + margedPartnerPlaneXAxis[1] + ", " + margedPartnerPlaneXAxis[2] + "), "+ "(" + margedPartnerPlaneZAxis[0] + ", " + margedPartnerPlaneZAxis[1] + ", " + margedPartnerPlaneZAxis[2] + ")");
                cnt++;
            }
        }
        Log.d(TAGTEST,  "MargedPlaneSize:" + cnt);

    }


}
