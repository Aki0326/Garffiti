package org.ntlab.graffiti.graffiti.anchorMatch;

import com.google.ar.core.Anchor;
import com.google.ar.core.Plane;

import org.ntlab.graffiti.entities.PointPlane2D;

import java.nio.FloatBuffer;
import java.util.Collection;

/**
 * MatchedAnchor Model
 * @author a-hongo
 */
public class MatchedAnchor {
    private Anchor myAnchor;
    private Anchor partnerAnchor;
    private Plane mergedPlane;
    private FloatBuffer prevPolygon;

    public MatchedAnchor(Anchor myAnchor, Anchor partnerAnchor, Plane mergedPlane) {
        this.myAnchor = myAnchor;
        this.partnerAnchor = partnerAnchor;
        this.mergedPlane = mergedPlane;
    }
    public Anchor getMyAnchor() {
        return myAnchor;
    }

    public void setMyAnchor(Anchor myAnchor) {
        this.myAnchor = myAnchor;
    }

    public Anchor getPartnerAnchor() {
        return partnerAnchor;
    }

    public void setPartnerAnchor(Anchor partnerAnchor) {
        this.partnerAnchor = partnerAnchor;
    }

    public void margePlane(Collection<PointPlane2D> polygon) {
        if (!(mergedPlane instanceof MergedPlane)) {
            mergedPlane = new MergedPlane(mergedPlane);
        }
        ((MergedPlane) mergedPlane).mergePolygon(polygon, partnerAnchor.getPose());
    }

    public Plane updatePlane(Plane myNewPlane) {
        if (!(mergedPlane instanceof MergedPlane)) {
            mergedPlane = new MergedPlane(mergedPlane);
        }
        ((MergedPlane) mergedPlane).setCurrentPlane(myNewPlane);
        ((MergedPlane) mergedPlane).updatePolygon(myNewPlane.getPolygon());
        return mergedPlane;
    }

    public void updatePolygon() {
        ((MergedPlane) mergedPlane).updatePolygon(((MergedPlane) mergedPlane).getCurrentPlane().getPolygon());
    }

    public Plane getmergedPlane() {
        return mergedPlane;
    }
    public FloatBuffer getPrevPolygon() {
        return prevPolygon;
    }

    public void setPrevPolygon(FloatBuffer prevPolygon) {
        this.prevPolygon = prevPolygon;
    }

}
