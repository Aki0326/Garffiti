package org.ntlab.graffiti.common.views;

import android.view.View;

import androidx.annotation.Nullable;

/**
 * This class control the plane detection.
 * @author a-hongo
 */
public class PlaneDetectController {
    private @Nullable
    View planeDiscoveryView;

    public PlaneDetectController(@Nullable View planeDiscoveryView) {
        this.planeDiscoveryView = planeDiscoveryView;
    }

    /** Set the instructions view to present over the Sceneform view. */
    public void setInstructionView(View view) {
        planeDiscoveryView = view;
    }

    /** Show the plane discovery UX instructions for finding a plane. */
    public void show() {
        if (planeDiscoveryView == null || planeDiscoveryView.isShown()) {
            return;
        }

        planeDiscoveryView.post(() -> planeDiscoveryView.setVisibility(View.VISIBLE));
    }

    /** Hide the plane discovery UX instructions. */
    public void hide() {
        if (planeDiscoveryView == null || !planeDiscoveryView.isShown()) {
            return;
        }

        planeDiscoveryView.post(() -> planeDiscoveryView.setVisibility(View.GONE));
    }
}
