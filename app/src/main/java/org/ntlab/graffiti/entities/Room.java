package org.ntlab.graffiti.entities;

import java.util.HashMap;
import java.util.Map;

public class Room {
    String roomId;
    Map<String, CloudAnchor> cloudAnchors = new HashMap<>();

    public Room() {
    }

    public Room(String roomId, String cloudAnchorId, CloudAnchor cloudAnchor) {
        this.roomId = roomId;
        this.cloudAnchors.put(cloudAnchorId, cloudAnchor);
    }

    public Room(String roomId, Map<String, CloudAnchor> cloudAnchors) {
        this.roomId = roomId;
        this.cloudAnchors = cloudAnchors;
    }

    public String getRoomId() {
        return roomId;
    }

    public void setRoomId(String roomId) {
        this.roomId = roomId;
    }

    public Map<String, CloudAnchor> getCloudAnchors() {
        return cloudAnchors;
    }

    public void setCloudAnchors(Map<String, CloudAnchor> cloudAnchors) {
        this.cloudAnchors = cloudAnchors;
    }
}
