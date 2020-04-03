package org.ntlab.graffiti.graffiti.anchorMatch;

import com.google.ar.core.Anchor;
import com.google.ar.core.Plane;

import org.ntlab.graffiti.entities.PointPlane2D;

import java.nio.FloatBuffer;
import java.util.Collection;

public class MatchedAnchor {
    private Anchor myAnchor;
    private Anchor partnerAnchor;
    private Plane margedPlane;
    private FloatBuffer prevPolygon;

    public MatchedAnchor(Anchor myAnchor, Anchor partnerAnchor, Plane margedPlane) {
        this.myAnchor = myAnchor;
        this.partnerAnchor = partnerAnchor;
        this.margedPlane = margedPlane;
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
        if (!(margedPlane instanceof MargedPlane)) {
            margedPlane = new MargedPlane(margedPlane);
        }
        ((MargedPlane) margedPlane).margePolygon(polygon, partnerAnchor.getPose());
    }

    public Plane updatePlane(Plane myNewPlane) {
        if (!(margedPlane instanceof MargedPlane)) {
            margedPlane = new MargedPlane(margedPlane);
        }
        ((MargedPlane) margedPlane).setCurrentPlane(myNewPlane);
        ((MargedPlane) margedPlane).updatePolygon(myNewPlane.getPolygon());
        return margedPlane;
    }

    public void updatePolygon() {
        ((MargedPlane) margedPlane).updatePolygon(((MargedPlane) margedPlane).getCurrentPlane().getPolygon());
    }

    public Plane getMargedPlane() {
        return margedPlane;
    }
    public FloatBuffer getPrevPolygon() {
        return prevPolygon;
    }

    public void setPrevPolygon(FloatBuffer prevPolygon) {
        this.prevPolygon = prevPolygon;
    }

}
