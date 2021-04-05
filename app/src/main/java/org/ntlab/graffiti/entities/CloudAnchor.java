package org.ntlab.graffiti.entities;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

/**
 * CloudAnchor Entity
 * @author a-hongo
 */
public class CloudAnchor {

    String displayName;
    Timestamp updateTimestamp;
    PlaneJSON plane;
    List<PointTex2D> stroke = new ArrayList<>();

    public CloudAnchor() {
    }

    public CloudAnchor(String displayName, Timestamp updateTimestamp) {
        this.displayName = displayName;
        this.updateTimestamp = updateTimestamp;
    }

    public CloudAnchor(String displayName, PointTex2D coordinate) {
        this.displayName = displayName;
        this.updateTimestamp = new Timestamp(System.currentTimeMillis());
        this.stroke.add(coordinate);
    }

    public CloudAnchor(String displayName, Timestamp updateTimestamp, List<PointTex2D> stroke) {
        this.displayName = displayName;
        this.updateTimestamp = updateTimestamp;
        this.stroke = stroke;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public Timestamp getUpdateTimestamp() {
        return updateTimestamp;
    }

    public void setUpdateTimestamp(Timestamp updateTimestamp) {
        this.updateTimestamp = updateTimestamp;
    }

    public PlaneJSON getPlane() {
        return plane;
    }

    public void setPlane(PlaneJSON plane) {
        this.plane = plane;
    }

    public List<PointTex2D> getStroke() {
        return stroke;
    }

    public void setStroke(List<PointTex2D> stroke) {
        this.stroke = stroke;
    }

}
