package org.ntlab.graffiti.common.views;

import android.view.View;

import androidx.annotation.Nullable;

/**
 * This class controll the plane discovery.
 */
public class PlaneDiscoveryController{
    private @Nullable
    View planeDiscoveryView;

    public PlaneDiscoveryController(@Nullable View planeDiscoveryView) {
        this.planeDiscoveryView = planeDiscoveryView;
    }

    /** Set the instructions view to present over the Sceneform view. */
    public void setInstructionView(View view) {
        planeDiscoveryView = view;
    }

    /** Show the plane discovery UX instructions for finding a plane. */
    public void show() {
        if (planeDiscoveryView == null) {
            return;
        }

        planeDiscoveryView.setVisibility(View.VISIBLE);
    }

    /** Hide the plane discovery UX instructions. */
    public void hide() {
        if (planeDiscoveryView == null) {
            return;
        }

        planeDiscoveryView.setVisibility(View.GONE);
    }
}
