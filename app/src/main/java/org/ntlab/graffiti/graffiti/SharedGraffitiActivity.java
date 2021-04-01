/*
 * Copyright 2018 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.ntlab.graffiti.graffiti;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.media.Image;
import android.opengl.GLES30;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.DialogFragment;

import com.google.ar.core.Anchor;
import com.google.ar.core.Anchor.CloudAnchorState;
import com.google.ar.core.ArCoreApk;
import com.google.ar.core.Camera;
import com.google.ar.core.Config;
import com.google.ar.core.Frame;
import com.google.ar.core.HitResult;
import com.google.ar.core.Plane;
import com.google.ar.core.Point;
import com.google.ar.core.PointCloud;
import com.google.ar.core.Pose;
import com.google.ar.core.Session;
import com.google.ar.core.Trackable;
import com.google.ar.core.TrackingState;
import com.google.ar.core.exceptions.CameraNotAvailableException;
import com.google.ar.core.exceptions.NotYetAvailableException;
import com.google.ar.core.exceptions.UnavailableApkTooOldException;
import com.google.ar.core.exceptions.UnavailableArcoreNotInstalledException;
import com.google.ar.core.exceptions.UnavailableDeviceNotCompatibleException;
import com.google.ar.core.exceptions.UnavailableSdkTooOldException;
import com.google.common.base.Preconditions;

import org.ntlab.graffiti.R;
import org.ntlab.graffiti.common.drawer.CircleDrawer;
import org.ntlab.graffiti.common.drawer.RectangleDrawer;
import org.ntlab.graffiti.common.drawer.TextureDrawer;
import org.ntlab.graffiti.common.geometry.GeometryUtil;
import org.ntlab.graffiti.common.helpers.CameraPermissionHelper;
import org.ntlab.graffiti.common.helpers.DepthSettings;
import org.ntlab.graffiti.common.helpers.DisplayRotationHelper;
import org.ntlab.graffiti.common.helpers.FullScreenHelper;
import org.ntlab.graffiti.common.helpers.InstantPlacementSettings;
import org.ntlab.graffiti.common.helpers.RendererHelper;
import org.ntlab.graffiti.common.helpers.SnackbarHelper;
import org.ntlab.graffiti.common.helpers.TapHelper;
import org.ntlab.graffiti.common.helpers.TrackingStateHelper;
import org.ntlab.graffiti.common.rendering.BackgroundOcclusionRenderer;
import org.ntlab.graffiti.common.rendering.Framebuffer;
import org.ntlab.graffiti.common.rendering.GraffitiOcclusionRenderer;
import org.ntlab.graffiti.common.rendering.ObjectRenderer;
import org.ntlab.graffiti.common.rendering.ObjectRenderer.BlendMode;
import org.ntlab.graffiti.common.rendering.PlaneRenderer;
import org.ntlab.graffiti.common.rendering.PointCloudRenderer;
import org.ntlab.graffiti.common.views.PlaneDetectController;
import org.ntlab.graffiti.entities.CloudAnchor;
import org.ntlab.graffiti.entities.PointTex2D;
import org.ntlab.graffiti.graffiti.PrivacyNoticeDialogFragment.HostResolveListener;
import org.ntlab.graffiti.graffiti.PrivacyNoticeDialogFragment.NoticeDialogListener;
import org.ntlab.graffiti.graffiti.anchorMatch.AnchorMatchingManager;
import org.ntlab.graffiti.graffiti.anchorMatch.MatchedAnchor;
import org.ntlab.graffiti.graffiti.anchorMatch.MergedPlane;
import org.ntlab.graffiti.graffiti.controls.AnchorManager;
import org.ntlab.graffiti.graffiti.controls.WebServiceManager;

import java.io.IOException;
import java.nio.FloatBuffer;
import java.util.List;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/**
 * Main Activity for the Cloud Anchor Example
 *
 * This is a simple example that shows how to host and resolve anchors using ARCore Cloud Anchors
 * API calls. This app only has at most one anchor at a time, to focus more on the cloud aspect of
 * anchors.
 * @author a-hongo
 */
public class SharedGraffitiActivity extends AppCompatActivity implements GLSurfaceView.Renderer, NoticeDialogListener {
    private static final String TAG = SharedGraffitiActivity.class.getSimpleName();
    private static final String TAGTEST = SharedGraffitiActivity.class.getSimpleName() + "Shared";
    private static final String TAGPLANE = SharedGraffitiActivity.class.getSimpleName() + "Plane";
    private static final String TAGANCHOR = SharedGraffitiActivity.class.getSimpleName() + "Anchor";
    private static final String TAGSTROKE = SharedGraffitiActivity.class.getSimpleName() + "Stroke";

//    private static final float[] OBJECT_COLOR = new float[] {139.0f, 195.0f, 74.0f, 255.0f};

//    private enum HostResolveMode {
//        NONE,
//        HOSTING,
//        RESOLVING,
//    }

    // Rendering. The Renderers are created here, and initialized when the GL surface is created.
    private GLSurfaceView surfaceView;
    private final BackgroundOcclusionRenderer backgroundOcclusionRenderer = new BackgroundOcclusionRenderer();
    private final ObjectRenderer virtualObject = new ObjectRenderer();
    private final ObjectRenderer virtualObjectShadow = new ObjectRenderer();
    private final PlaneRenderer planeRenderer = new PlaneRenderer();
    private final PointCloudRenderer pointCloudRenderer = new PointCloudRenderer();
    private final GraffitiOcclusionRenderer graffitiOcclusionRenderer = new GraffitiOcclusionRenderer();
    private Framebuffer virtualSceneFramebuffer;
    private boolean hasSetTextureNames = false;

    private final DepthSettings depthSettings = new DepthSettings();
    private final InstantPlacementSettings instantPlacementSettings = new InstantPlacementSettings();

    private boolean installRequested;

    // Temporary matrices allocated here to reduce number of allocations for each frame.
//    private final float[] anchorMatrix = new float[16];
    private final float[] viewMatrix = new float[16];
    private final float[] projectionMatrix = new float[16];

    // Tap handling and UI.
    private final SnackbarHelper messageSnackbarHelper = new SnackbarHelper();
    private final SnackbarHelper planeDiscoverySnackbarHelper = new SnackbarHelper();
    private DisplayRotationHelper displayRotationHelper;
    private TapHelper tapHelper;
    private final RendererHelper rendererHelper = new RendererHelper();

    private final TrackingStateHelper trackingStateHelper = new TrackingStateHelper(this);
//    private Button hostButton;
//    private Button resolveButton;
    private TextView roomCodeText;
    private SharedPreferences sharedPreferences;
    private static final String PREFERENCE_FILE_KEY = "allow_sharing_images";
    private static final String ALLOW_SHARE_IMAGES_KEY = "ALLOW_SHARE_IMAGES";

    private Session session;

    // Cloud Anchor Components.
    private WebServiceManager webServiceManager;
    private final AnchorManager anchorManager = new AnchorManager();
//    private HostResolveMode currentMode;
    private AnchorListener anchorListener;

    private AnchorMatchingManager anchorMatchingManager = new AnchorMatchingManager();

    private PlaneDetectController planeDetectController;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate()");
        setContentView(R.layout.activity_shared_graffiti);

        surfaceView = findViewById(R.id.surfaceview);
        displayRotationHelper = new DisplayRotationHelper(/*context=*/this);

        // Set up tap listener.
        tapHelper = new TapHelper(this, SharedGraffitiActivity.this);
        surfaceView.setOnTouchListener(tapHelper);

        // Set up renderer.
        surfaceView.setPreserveEGLContextOnPause(true);
        surfaceView.setEGLContextClientVersion(2);
        surfaceView.setEGLConfigChooser(8, 8, 8, 8, 16, 0); // Alpha used for plane blending.
        surfaceView.setRenderer(this);
        surfaceView.setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);
        surfaceView.setWillNotDraw(false);

        installRequested = false;

        depthSettings.onCreate(this);
        instantPlacementSettings.onCreate(this);

        // Set up the HandMotion View.
        LayoutInflater inflater = LayoutInflater.from(this);
        FrameLayout handmotion = findViewById(R.id.plane_discovery_view);
        FrameLayout instructionsView = (FrameLayout)inflater.inflate(R.layout.view_plane_discovery, handmotion, true);
        planeDetectController = new PlaneDetectController(instructionsView);

        // Initialize UI components.
//        hostButton = findViewById(R.id.host_button);
//        hostButton.setOnClickListener((view) -> onHostButtonPress());
//        resolveButton = findViewById(R.id.resolve_button);
//        resolveButton.setOnClickListener((view) -> onResolveButtonPress());
        roomCodeText = findViewById(R.id.room_code_text);

        // Initialize Cloud Anchor variables.
        webServiceManager = new WebServiceManager();
//        currentMode = HostResolveMode.NONE;
        sharedPreferences = getSharedPreferences(PREFERENCE_FILE_KEY, Context.MODE_PRIVATE);
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume()");

        if (sharedPreferences.getBoolean(ALLOW_SHARE_IMAGES_KEY, false)) {
            createSession();
            planeDetectController.show();
            planeDiscoverySnackbarHelper.showMessage(this, getString(R.string.searching_plane));
        } else {
            messageSnackbarHelper.showMessage(this, getString(R.string.unavailable_mode));
            finish();
        }
    }

    private void createSession() {
        if (session == null) {
            Exception exception = null;
            int messageId = -1;
            try {
                switch (ArCoreApk.getInstance().requestInstall(this, !installRequested)) {
                    case INSTALL_REQUESTED:
                        installRequested = true;
                        return;
                    case INSTALLED:
                        break;
                }
                // ARCore requires camera permissions to operate. If we did not yet obtain runtime
                // permission on Android M and above, now is a good time to ask the user for it.
                if (!CameraPermissionHelper.hasCameraPermission(this)) {
                    CameraPermissionHelper.requestCameraPermission(this);
                    return;
                }

                // Create the session.
                session = new Session(/* context= */this);
            } catch (UnavailableArcoreNotInstalledException e) {
                messageId = R.string.snackbar_arcore_unavailable;
                exception = e;
            } catch (UnavailableApkTooOldException e) {
                messageId = R.string.snackbar_arcore_too_old;
                exception = e;
            } catch (UnavailableSdkTooOldException e) {
                messageId = R.string.snackbar_arcore_sdk_too_old;
                exception = e;
            } catch (UnavailableDeviceNotCompatibleException e) {
                messageId = R.string.snackbar_arcore_not_compatible;
                exception = e;
            } catch (Exception e) {
                messageId = R.string.snackbar_arcore_exception;
                exception = e;
            }

            if (exception != null) {
                messageSnackbarHelper.showError(this, getString(messageId));
                Log.e(TAG, "Exception creating session", exception);
                return;
            }

            // Enable depth-based occlusion.
            boolean isDepthSupported = session.isDepthModeSupported(Config.DepthMode.AUTOMATIC);
//            if (isDepthSupported) {
                depthSettings.setUseDepthForOcclusion(false);
                depthSettings.setDepthColorVisualizationEnabled(false);
//            }
        }

        // Note that order matters - see the note in onPause(), the reverse applies here.
        try {
            configureSession();

            // Setting the session in the HostManager.
            anchorManager.setSession(session);

            // To record a live camera session for later playback, call
            // `session.startRecording(recorderConfig)` at anytime. To playback a previously recorded AR
            // session instead of using the live camera feed, call
            // `session.setPlaybackDataset(playbackDatasetPath)` before calling `session.resume()`. To
            // learn more about recording and playback, see:
            // https://developers.google.com/ar/develop/java/recording-and-playback
            session.resume();
        } catch (CameraNotAvailableException e) {
            // In some cases (such as another camera app launching) the camera may be given to
            // a different app instead. Handle this properly by showing a message and recreate the
            // session at the next iteration.
            messageSnackbarHelper.showError(this, getString(R.string.snackbar_camera_unavailable));
            session = null;
            return;
        }

        surfaceView.onResume();
        displayRotationHelper.onResume();
    }

    /**
     * Configures the session with feature settings.
     */
    private void configureSession() {
        Config config = session.getConfig();
//        config.setLightEstimationMode(Config.LightEstimationMode.ENVIRONMENTAL_HDR);
        config.setLightEstimationMode(Config.LightEstimationMode.DISABLED);
////        config.setLightEstimationMode(Config.LightEstimationMode.AMBIENT_INTENSITY);
        if (session.isDepthModeSupported(Config.DepthMode.AUTOMATIC)) {
            config.setDepthMode(Config.DepthMode.AUTOMATIC);
        } else {
            config.setDepthMode(Config.DepthMode.DISABLED);
        }
        if (instantPlacementSettings.isInstantPlacementEnabled()) {
            config.setInstantPlacementMode(Config.InstantPlacementMode.LOCAL_Y_UP);
        } else {
            config.setInstantPlacementMode(Config.InstantPlacementMode.DISABLED);
        }
        config.setCloudAnchorMode(Config.CloudAnchorMode.ENABLED);
        session.configure(config);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] results) {
        super.onRequestPermissionsResult(requestCode, permissions, results);
        if (!CameraPermissionHelper.hasCameraPermission(this)) {
            // Use toast instead of snackbar here since the activity will exit.
            Toast.makeText(this, "Camera permission is needed to run this application", Toast.LENGTH_LONG)
                    .show();
            if (!CameraPermissionHelper.shouldShowRequestPermissionRationale(this)) {
                // Permission denied with checking "Do not ask again".
                CameraPermissionHelper.launchPermissionSettings(this);
            }
            finish();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        Log.d(TAG, "onPause()");
        if (session != null) {
            // Note that the order matters - GLSurfaceView is paused first so that it does not try
            // to query the session. If Session is paused before GLSurfaceView, GLSurfaceView may
            // still call session.update() and get a SessionPausedException.
            displayRotationHelper.onPause();
            surfaceView.onPause();
            session.pause();
        }
    }

    @Override
    public void onStop() {
        Log.d(TAGTEST, "onStop()");
        if (session != null) {
            Log.d(TAGTEST, "PlaneSize:" + session.getAllTrackables(Plane.class).size());
            int cnt = 0;
            for (Plane plane : session.getAllTrackables(Plane.class)) {
                if (plane.getSubsumedBy() == null) {
                    Log.d(TAGTEST, "NotSubsumedPlane:" + plane);
                    cnt++;
                } else {
                    Log.d(TAGTEST, "SubsumedPlane:" + plane + " subsumed by " + plane.getSubsumedBy());

                }
            }
            Log.d(TAGTEST, "NotSubsumedPlaneSize:" + cnt);
            anchorMatchingManager.endLog();
        }
        super.onStop();
    }

    @Override
    public void onDestroy() {
        // Clear all registered listeners.
        resetMode();

        if (session != null) {
            // Explicitly close ARCore Session to release native resources.
            // Review the API reference for important considerations before calling close() in apps with
            // more complicated lifecycle requirements:
            // https://developers.google.com/ar/reference/java/arcore/reference/com/google/ar/core/Session#close()
            session.close();
            session = null;
        }

        Log.d(TAG, "onDestroy()");
        super.onDestroy();
    }

    @Override
    public void onBackPressed(){
        resetMode();
        // Activity を終了し, 前のページへ
        finish();
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        FullScreenHelper.setFullScreenOnWindowFocusChanged(this, hasFocus);
    }


    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        rendererHelper.enableBlend();
        GLES30.glClearColor(0.1f, 0.1f, 0.1f, 1.0f);

        // Prepare the rendering objects. This involves reading shaders, so may throw an IOException.
        try {
            // Create the texture and pass it to ARCore session to be filled during update().
            backgroundOcclusionRenderer.createOnGlThread(this);
            // Update BackgroundRenderer state to match the depth settings.
            backgroundOcclusionRenderer.setUseOcclusion(this, depthSettings.useDepthForOcclusion());

            planeRenderer.createOnGlThread(this, "models/trigrid.png");
            pointCloudRenderer.createOnGlThread(this);

            virtualObject.createOnGlThread(this, "models/andy.obj", "models/andy.png");
            virtualObject.setMaterialProperties(0.0f, 2.0f, 0.5f, 6.0f);

            virtualObjectShadow.createOnGlThread(this, "models/andy_shadow.obj", "models/andy_shadow.png");
            virtualObjectShadow.setBlendMode(BlendMode.Shadow);
            virtualObjectShadow.setMaterialProperties(1.0f, 0.0f, 0.0f, 1.0f);

            graffitiOcclusionRenderer.createOnGlThread(this,"models/plane.png");
            // Update BackgroundRenderer state to match the depth settings.
            graffitiOcclusionRenderer.setUseOcclusion(this, depthSettings.useDepthForOcclusion());

            virtualSceneFramebuffer = new Framebuffer(/*width=*/ 1, /*height=*/ 1);
        } catch (IOException ex) {
            Log.e(TAG, "Failed to read an asset file", ex);
        }
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        displayRotationHelper.onSurfaceChanged(width, height);
        virtualSceneFramebuffer.resize(width, height);
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        // Clear screen to notify driver it should not load any pixels from previous frame.
        GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT | GLES30.GL_DEPTH_BUFFER_BIT);
        virtualSceneFramebuffer.clear();
        rendererHelper.clear(0f, 0f, 0f, 1f);

        if (session == null) {
            return;
        }

        // Texture names should only be set once on a GL thread unless they change. This is done during
        // onDrawFrame rather than onSurfaceCreated since the session is not guaranteed to have been
        // initialized during the execution of onSurfaceCreated.
        if (!hasSetTextureNames) {
//      session.setCameraTextureNames(
//              new int[] {backgroundRenderer.getCameraColorTexture().getTextureId()});
            session.setCameraTextureName(backgroundOcclusionRenderer.getCameraColorTexture().getTextureId());
            hasSetTextureNames = true;
        }

        // -- Update per-frame state

        // Notify ARCore session that the view size changed so that the perspective matrix and
        // the video background can be properly adjusted.
        displayRotationHelper.updateSessionIfNeeded(session);

        // Obtain the current frame from ARSession. When the configuration is set to
        // UpdateMode.BLOCKING (it is by default), this will throttle the rendering to the
        // camera framerate.
        Frame frame;
        try {
            frame = session.update();
        } catch (CameraNotAvailableException e) {
            Log.e(TAG, "Camera not available during onDrawFrame. Try restarting the app.", e);
            return;
        }

        Camera camera = frame.getCamera();
        TrackingState cameraTrackingState = camera.getTrackingState();

        // Notify the anchorManager of all the updates.
        anchorManager.update();

        // BackgroundRenderer.updateDisplayGeometry must be called every frame to update the coordinates
        // used to draw the background camera image.
        backgroundOcclusionRenderer.updateDisplayGeometry(frame);
        graffitiOcclusionRenderer.updateDisplayGeometry(frame);

        if (cameraTrackingState == TrackingState.TRACKING
                && (depthSettings.useDepthForOcclusion()
                /*|| depthSettings.depthColorVisualizationEnabled()*/)) {
            // Retrieve the depth map for the current frame, if available.
            try {
                Image depthImage = frame.acquireDepthImage();
                backgroundOcclusionRenderer.updateCameraDepthTexture(depthImage);
//                graffitiRenderer.updateCameraDepthTexture(depthImage);
                graffitiOcclusionRenderer.setDepthTexture(backgroundOcclusionRenderer.getCameraDepthTexture().getTextureId(), depthImage.getWidth(), depthImage.getHeight());
            } catch (NotYetAvailableException e) {
                // This means that depth data is not available yet.
                // Depth data will not be available if there are no tracked
                // feature points. This can happen when there is no motion, or when the
                // camera loses its ability to track objects in the surrounding
                // environment.
//                Log.e(TAG, "NotYetAvailableException", e);
            }
        }

        // Handle user input.
        handleTap(frame, cameraTrackingState);

        // Keep the screen unlocked while tracking, but allow it to lock when tracking stops.
        trackingStateHelper.updateKeepScreenOnFlag(camera.getTrackingState());

        // Check if we detected at least one plane. If so, hide the loading message.
        if (planeDiscoverySnackbarHelper.isShowing()) {
            for (Plane plane : session.getAllTrackables(Plane.class)) {
                if (plane.getTrackingState() == TrackingState.TRACKING) {
                    planeDiscoverySnackbarHelper.hide(this);
                    planeDetectController.hide();
                    onEnterRoom();
                    break;
                }
            }
        }

        // -- Draw background

        // Suppress rendering if the camera did not produce the first frame yet. This is to avoid
        // drawing possible leftover data from previous sessions if the texture is reused.
        virtualSceneFramebuffer.clear();
        backgroundOcclusionRenderer.draw(frame);

        // If not tracking, don't draw 3d objects.
        if (cameraTrackingState == TrackingState.PAUSED) {
            return;
        }

        // -- Draw non-occluded virtual objects (planes, point cloud)

        // Get camera and projection matrices.
        camera.getViewMatrix(viewMatrix, 0);
        camera.getProjectionMatrix(projectionMatrix, 0, 0.1f, 100.0f);

        for (Plane newPlane : frame.getUpdatedTrackables(Plane.class)) {
            Log.d(TAGPLANE, "UpdatePlane:" + newPlane + ", " + newPlane.getCenterPose() + ", " + newPlane.getSubsumedBy());
        }

        if (anchorListener != null) {
            anchorMatchingManager.updateState(frame.getUpdatedTrackables(Plane.class),
                    new AnchorMatchingManager.UpdateAnchorListener() {

                        @Override
                        public Anchor createAnchor(Plane plane) {
                            Anchor hostAnchor = anchorManager.hostAnchor(plane.createAnchor(plane.getCenterPose()), anchorListener);
                            messageSnackbarHelper.showMessageWithDismiss(SharedGraffitiActivity.this, "pendingAnchor put.");
                            return hostAnchor;
                        }

                        @Override
                        public void onAnchorMatched(MatchedAnchor matchedAnchor) {
                            messageSnackbarHelper.showMessageWithDismiss(SharedGraffitiActivity.this, "Shared Plane.");
                        }
                    },
                    new AnchorMatchingManager.UpdatePlaneListener() {
                        @Override
                        public void onUpdatePlaneAfterMatched(MatchedAnchor matchedAnchor, Plane plane) {
                            storePolygon(matchedAnchor.getMyAnchor(), plane);
                        }
                    });
        }

        anchorMatchingManager.updateLog();

        for (Anchor anchor: session.getAllAnchors()) {
            Log.d(TAGANCHOR,  ", getAllAnchors, " + anchor.getCloudAnchorId() + ", " + anchor.getPose() + ", (" + anchor.getPose().getXAxis().toString() + ", " + anchor.getPose().getZAxis().toString() + ")");
        }

        // Visualize tracked points.
        // Use try-with-resources to automatically release the point cloud.
        try (PointCloud pointCloud = frame.acquirePointCloud()) {
            pointCloudRenderer.update(pointCloud);
            pointCloudRenderer.draw(viewMatrix, projectionMatrix);
            // Application is responsible for releasing the point cloud resources after
            // using it.
            pointCloud.release();
        }

        // Visualize planes.
        virtualSceneFramebuffer.clear();
        planeRenderer.drawPlanes(session.getAllTrackables(Plane.class), camera.getDisplayOrientedPose(), projectionMatrix);

        // -- Draw occluded virtual objects

        // Visualize graffiti.
        virtualSceneFramebuffer.clear();
        graffitiOcclusionRenderer.adjustTextureAxis(frame, camera);
        graffitiOcclusionRenderer.draw(session.getAllTrackables(Plane.class), camera.getDisplayOrientedPose(), projectionMatrix);

        for (MergedPlane mergedPlane : anchorMatchingManager.getDrawnPlanes()) {
            List<PointTex2D> stroke = mergedPlane.getStroke();
            if (stroke.size() > mergedPlane.getDrawnStrokeIndex()) {
                for (int i = mergedPlane.getDrawnStrokeIndex(); i < stroke.size(); i++) {
                    graffitiOcclusionRenderer.drawTexture(stroke.get(i).getX(), stroke.get(i).getY(), 4, mergedPlane, new CircleDrawer(Color.RED));
                }
                mergedPlane.drawnStroke(stroke.size());
            }
        }

    }

    /**
     * Handles the most recent user tap.
     *
     * <p>We only ever handle one tap at a time, since this app only allows for a single anchor.
     *
     * @param frame the current AR frame
     * @param cameraTrackingState the current camera tracking state
     */
    private void handleTap(Frame frame, TrackingState cameraTrackingState) {
        // Handle taps. Handling only one tap per frame, as taps are usually low frequency
        // compared to frame rate.
        // Only handle a tap if the anchor is currently null, the queued tap is non-null and the
        // camera is currently tracking.
        MotionEvent tap = tapHelper.poll();
        if (tap != null && cameraTrackingState == TrackingState.TRACKING) {
//                Preconditions.checkState(currentMode == HostResolveMode.HOSTING, "We should only be creating an anchor in hosting mode.");
            for (HitResult hit : frame.hitTest(tap)) {
                Plane plane = getPlaneWithHit(hit);
                if (plane != null && plane.getSubsumedBy() == null) {
                    // Check if any plane was hit, and if it was hit inside the plane polygon
                    Pose planePose = plane.getCenterPose();
                    // ワールド座標系から平面のローカル座標への変換
                    float[] hitOnPlaneCoord = GeometryUtil.worldToLocal(hit.getHitPose().getTranslation(), planePose.getTranslation(), planePose.getXAxis(), planePose.getZAxis());
                    int drawerStyle = 1;
                    int color = Color.BLUE;
                    TextureDrawer drawer = null;
                    switch (drawerStyle) {
                        case 1:
                            drawer = new CircleDrawer(color);
                            break;
                        case 2:
                            drawer = new RectangleDrawer(color);
                            break;
                    }
                    Preconditions.checkNotNull(anchorListener, "The anchorlistener cannot be null.");
                    messageSnackbarHelper.showMessage(this, getString(R.string.snackbar_anchor_placed));
                    if (color == Color.TRANSPARENT) {
                        graffitiOcclusionRenderer.drawTexture(hitOnPlaneCoord[0], -hitOnPlaneCoord[1], 9, plane, drawer);
                    } else {
                        graffitiOcclusionRenderer.drawTexture(hitOnPlaneCoord[0], -hitOnPlaneCoord[1], 4, plane, drawer);
                    }
                    storeStroke(plane, hit.getHitPose().getTranslation());
                    Log.d(TAGSTROKE, "hit.getHitPose().getTranslation(): " + hit.getHitPose().getTranslation());
                    Log.d(TAGSTROKE, "hitOnPlaneCoord: " + hitOnPlaneCoord[0] + ", " + -hitOnPlaneCoord[1]);
                    break; // Only handle the first valid hit.
                }
            }
        }
    }

    /** Returns {@code Plane} if and only if the hit can be used to create an Anchor reliably. */
    private Plane getPlaneWithHit(HitResult hit) {
        Trackable trackable = hit.getTrackable();
        if (trackable instanceof Plane) {
            // Check if the hit was within the plane's or mergedPlane's polygon.
            Plane plane = anchorMatchingManager.getmergedPlaneByPlane((Plane) trackable);
            if (plane.isPoseInPolygon(hit.getHitPose())) {
                return plane;
            } else {
                return null;
            }
        } else if (trackable instanceof Point) {
            // Check if the hit was against an oriented point.
            return null;
        }
        return null;
    }


    private void storePolygon(Anchor anchor, Plane plane) {
        String cloudAnchorId = anchor.getCloudAnchorId();
        FloatBuffer polygon = plane.getPolygon();
        if (cloudAnchorId == null) {
            return;
        }
        float[] planePolygon = polygon.array();
        float[] polyArray = new float[planePolygon.length];
        Pose planeCenter = plane.getCenterPose();
        float[] planePosition = planeCenter.getTranslation();
        float[] planeXAxis = planeCenter.getXAxis();
        float[] planeZAxis = planeCenter.getZAxis();
        Pose anchorPose = anchor.getPose();
        float[] anchorPosition = anchorPose.getTranslation();
        float[] anchorXAxis = anchorPose.getXAxis();
        float[] anchorZAxis = anchorPose.getZAxis();

        for (int i = 0; i < planePolygon.length; i += 2) {
            float[] world = GeometryUtil.localToWorld(planePolygon[i], planePolygon[i + 1], planePosition, planeXAxis, planeZAxis);
            float[] anchorLocal = GeometryUtil.worldToLocal(world, anchorPosition, anchorXAxis, anchorZAxis);
            polyArray[i] = anchorLocal[0];
            polyArray[i + 1] = anchorLocal[1];
        }
        Log.d(TAGTEST, "storePolygonInRoom:" + cloudAnchorId);
        webServiceManager.storePolygonInRoom(cloudAnchorId, polyArray);
        messageSnackbarHelper.showMessageWithDismiss(SharedGraffitiActivity.this, "Stored Polygon.");
    }

    private void storeStroke(Plane hitPlane, float[] hitPosition) {
        Anchor myAnchor = anchorMatchingManager.getMyAnchorByPlane(hitPlane);
        Pose myAnchorPose;
        float[] hitOnMyAnchorCoord;
        String cloudAnchorId;
        if (myAnchor != null) {
            myAnchorPose = myAnchor.getPose();
            cloudAnchorId = myAnchor.getCloudAnchorId();
            // ワールド座標系から平面のローカル座標への変換
            hitOnMyAnchorCoord = GeometryUtil.worldToLocal(hitPosition, myAnchorPose.getTranslation(), myAnchorPose.getXAxis(), myAnchorPose.getZAxis());
            webServiceManager.storeStrokeInRoom(cloudAnchorId, hitOnMyAnchorCoord[0], hitOnMyAnchorCoord[1]);
            messageSnackbarHelper.showMessageWithDismiss(SharedGraffitiActivity.this, "Stored Stroke.");
        } else {
            messageSnackbarHelper.showMessageWithDismiss(SharedGraffitiActivity.this, "No Store Stroke.");
        }
    }

    /** Callback function invoked when the Host Button is pressed. */
//    private void onHostButtonPress() {
//        if (currentMode == HostResolveMode.HOSTING) {
//            resetMode();
//            return;
//        }
//
//        if (!sharedPreferences.getBoolean(ALLOW_SHARE_IMAGES_KEY, false)) {
//            showNoticeDialog(this::onPrivacyAcceptedForHost);
//        } else {
//            onPrivacyAcceptedForHost();
//        }
//    }

//    private void onPrivacyAcceptedForHost() {
//        if (anchorListener != null) {
//            return;
//        }
//        resolveButton.setEnabled(false);
//        hostButton.setText(R.string.cancel);
//        snackbarHelper.showMessageWithDismiss(this, getString(R.string.snackbar_on_host));
//        anchorListener = new RoomCodeAndCloudAnchorIdListener();
//    }

    /** Callback function invoked when the Resolve Button is pressed. */
//    private void onResolveButtonPress() {
//        if (currentMode == HostResolveMode.RESOLVING) {
//            resetMode();
//            return;
//        }
//
//        if (!sharedPreferences.getBoolean(ALLOW_SHARE_IMAGES_KEY, false)) {
//            showNoticeDialog(this::onPrivacyAcceptedForResolve);
//        } else {
//            onPrivacyAcceptedForResolve();
//        }
//    }

//    private void onPrivacyAcceptedForResolve() {
//        ResolveDialogFragment dialogFragment = new ResolveDialogFragment();
//        dialogFragment.setOkListener(this::onRoomCodeEntered);
//        dialogFragment.show(getSupportFragmentManager(), "ResolveDialog");
//    }

    /** Call method when plane find. */
    private void onEnterRoom() {
        if (!sharedPreferences.getBoolean(ALLOW_SHARE_IMAGES_KEY, false)) {
            showNoticeDialog(this::onPrivacyAcceptedForEnterRoom);
        } else {
            onPrivacyAcceptedForEnterRoom();
        }
    }

    private void onPrivacyAcceptedForEnterRoom() {
        EnterRoomDialogFragment dialogFragment = new EnterRoomDialogFragment();
        dialogFragment.setOkListener(this::onRoomCodeEntered);
        dialogFragment.show(getSupportFragmentManager(), "EnterRoomDialog");
    }

    /** Resets the mode of the app to its initial state and removes the anchors. */
    private void resetMode() {
        roomCodeText.setText(null);
        webServiceManager.clearRoomListener();
        anchorManager.clearListeners();
        anchorListener = null;
        messageSnackbarHelper.hide(this);
    }

    /** Callback function invoked when the user presses the OK button in the Resolve Dialog. */
    private void onRoomCodeEntered(Long roomCode) {
//        currentMode = HostResolveMode.RESOLVING;
//        hostButton.setEnabled(false);
//        resolveButton.setText(R.string.cancel);

        AnchorListener cloudAnchorListener = new AnchorListener();
        anchorListener = cloudAnchorListener;
        WebServiceUpdateListener webServiceListener = new WebServiceUpdateListener(cloudAnchorListener);
//        Preconditions.checkState(roomCode == null, "The room code cannot have been set before.");
        roomCodeText.setText(String.valueOf(roomCode));
        messageSnackbarHelper.showMessageWithDismiss(SharedGraffitiActivity.this, getString(R.string.snackbar_on_room_code_available, roomCode));
        webServiceManager.createRoom(roomCode, webServiceListener);
    }

    /**
     * Listens for both a new room code and an anchor ID, and shares the anchor ID in Firebase with
     * the room code when both are available.
     */
    public final class WebServiceUpdateListener implements WebServiceManager.RoomUpdateListener {
        private AnchorListener anchorListener;

        public WebServiceUpdateListener(AnchorListener anchorListener) {
            this.anchorListener = anchorListener;
        }

        @Override
        public void onNewCloudAnchor(String cloudAnchorId, CloudAnchor cloudAnchor) {
            // When the cloud anchor ID is available from Firebase.
            Preconditions.checkNotNull(anchorListener, "The Cloud Anchor listener cannot be null.");
            anchorManager.resolveAnchor(cloudAnchorId, anchorListener, SystemClock.uptimeMillis());
        }

        @Override
        public void onUpdateCloudAnchor(String cloudAnchorId, CloudAnchor cloudAnchor) {
            // When the cloud anchor ID is available from Firebase.
            Preconditions.checkNotNull(anchorListener, "The Cloud Anchor listener cannot be null.");
            anchorMatchingManager.updateMatchedAnchor(cloudAnchorId, cloudAnchor);
        }
    }

    /**
     * Listens for both a new room code and an anchor ID, and shares the anchor ID in Firebase with
     * the room code when both are available.
     */
    public final class AnchorListener implements AnchorManager.AnchorHostListener, AnchorManager.AnchorResolveListener {

        @Override
        public void onHostTaskComplete(Anchor anchor) {
            Anchor.CloudAnchorState cloudState = anchor.getCloudAnchorState();
            if (cloudState.isError()) {
                Log.e(TAG, "Error hosting a cloud anchor, state " + cloudState);
                messageSnackbarHelper.showMessageWithDismiss(SharedGraffitiActivity.this, getString(R.string.snackbar_host_error, cloudState));
                return;
            }
            String cloudAnchorId = anchor.getCloudAnchorId();
//            Preconditions.checkState(cloudAnchorId == null, "The cloud anchor ID cannot have been set before.");
            if (anchorMatchingManager.isPendingSubmission(anchor) && cloudAnchorId != null) {
                webServiceManager.storeAnchorIdInRoom(cloudAnchorId);
                Plane plane = anchorMatchingManager.submit(anchor);
                messageSnackbarHelper.showMessageWithDismiss(SharedGraffitiActivity.this, "myAnchor put.");
//                    snackbarHelper.showMessageWithDismiss(ColoringBattleActivity.this, getString(R.string.snackbar_cloud_id_shared));
                Log.d(TAGTEST, "myAnchors.put:" + anchor + ", " + anchor.getCloudAnchorId() + ", (" + anchor.getPose().getTranslation()[0] + ", " + anchor.getPose().getTranslation()[1] + ", " + anchor.getPose().getTranslation()[2] + ") ," + plane);
            }
        }

        @Override
        public void onResolveTaskComplete(Anchor anchor) {
            // When the anchor has been resolved, or had a final error state.
            CloudAnchorState cloudState = anchor.getCloudAnchorState();
            if (cloudState.isError()) {
                messageSnackbarHelper.showMessageWithDismiss(SharedGraffitiActivity.this, getString(R.string.snackbar_resolve_error, cloudState));
                return;
            }
            messageSnackbarHelper.showMessageWithDismiss(SharedGraffitiActivity.this, getString(R.string.snackbar_resolve_success));
            anchorMatchingManager.storePartner(anchor);
        }

        @Override
        public void onShowResolveMessage() {
            messageSnackbarHelper.setMaxLines(4);
            messageSnackbarHelper.showMessageWithDismiss(SharedGraffitiActivity.this, getString(R.string.snackbar_resolve_no_result_yet));
        }

    }

    public void showNoticeDialog(HostResolveListener listener) {
        DialogFragment dialog = PrivacyNoticeDialogFragment.createDialog(listener);
        dialog.show(getSupportFragmentManager(), PrivacyNoticeDialogFragment.class.getName());
    }

    @Override
    public void onDialogPositiveClick(DialogFragment dialog) {
        if (!sharedPreferences.edit().putBoolean(ALLOW_SHARE_IMAGES_KEY, true).commit()) {
            throw new AssertionError("Could not save the user preference to SharedPreferences!");
        }
        createSession();
    }
}