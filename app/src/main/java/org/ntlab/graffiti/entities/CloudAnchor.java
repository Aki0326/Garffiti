package org.ntlab.graffiti.entities;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

public class CloudAnchor {

    String displayName;
    Timestamp updateTimestamp;
    List<Point2D> draw = new ArrayList<>();

    public CloudAnchor() {
    }

    public CloudAnchor(String displayName, Point2D coordinate) {
        this.displayName = displayName;
        this.updateTimestamp = new Timestamp(System.currentTimeMillis());
        this.draw.add(coordinate);
    }

    public CloudAnchor(String displayName, Timestamp updateTimestamp, List<Point2D> draw) {
        this.displayName = displayName;
        this.updateTimestamp = updateTimestamp;
        this.draw = draw;
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

    public List<Point2D> getDraw() {
        return draw;
    }

    public void setDraw(List<Point2D> draw) {
        this.draw = draw;
    }

}
