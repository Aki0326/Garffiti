package org.ntlab.graffiti.entities;

import java.util.ArrayList;
import java.util.Collection;

/**
 * Entity PlaneJSON
 * @author a-hongo
 */
public class PlaneJSON {
    Collection<PointPlane2D> polygon = new ArrayList<>();

    public PlaneJSON() {
    }

    public PlaneJSON(Collection<PointPlane2D> polygon) {
        this.polygon = polygon;
    }

    public Collection<PointPlane2D> getPolygon() {
        return polygon;
    }

    public void setPolygon(Collection<PointPlane2D> polygon) {
        this.polygon = polygon;
    }
}
