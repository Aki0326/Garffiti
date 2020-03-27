package org.ntlab.graffiti.entities;

import com.google.ar.core.Anchor;
import com.google.ar.core.Plane;

import java.util.Collection;
import java.util.List;

public class SharedAnchor {
    private Anchor myAnchor;
    private Anchor partnerAnchor;
    private Plane margedPlane;

    public SharedAnchor(Anchor myAnchor, Anchor partnerAnchor, Plane margedPlane) {
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
        if (!(margedPlane instanceof SharedPlane)) {
            margedPlane = new SharedPlane(margedPlane);
        }
        ((SharedPlane) margedPlane).margePolygon(polygon, partnerAnchor.getPose());
    }

    public void updatePlane(Plane myNewPlane) {
//        if (!(margedPlane instanceof SharedPlane)) {
//            margedPlane = new SharedPlane(margedPlane);
//        }
        ((SharedPlane) margedPlane).setCurrentPlane(myNewPlane);
        ((SharedPlane) margedPlane).updatePolygon(myNewPlane.getPolygon());
    }

    public void updatePolygon() {
        ((SharedPlane) margedPlane).updatePolygon(((SharedPlane) margedPlane).getCurrentPlane().getPolygon());
    }

    public Plane getMargedPlane() {
        return margedPlane;
    }
}
