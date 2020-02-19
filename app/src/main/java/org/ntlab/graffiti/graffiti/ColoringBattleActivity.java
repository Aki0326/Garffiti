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
import android.graphics.PointF;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.GuardedBy;
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
import com.google.ar.core.Point.OrientationMode;
import com.google.ar.core.PointCloud;
import com.google.ar.core.Pose;
import com.google.ar.core.Session;
import com.google.ar.core.Trackable;
import com.google.ar.core.TrackingState;

import org.ntlab.graffiti.R;
import org.ntlab.graffiti.common.drawer.CircleDrawer;
import org.ntlab.graffiti.common.drawer.RectangleDrawer;
import org.ntlab.graffiti.common.drawer.TextureDrawer;
import org.ntlab.graffiti.common.helpers.TapHelper;
import org.ntlab.graffiti.common.rendering.GraffitiRenderer;
import org.ntlab.graffiti.common.views.PlaneDiscoveryController;
import org.ntlab.graffiti.graffiti.PrivacyNoticeDialogFragment.HostResolveListener;
import org.ntlab.graffiti.graffiti.PrivacyNoticeDialogFragment.NoticeDialogListener;
import org.ntlab.graffiti.common.helpers.CameraPermissionHelper;
import org.ntlab.graffiti.common.helpers.DisplayRotationHelper;
import org.ntlab.graffiti.common.helpers.FullScreenHelper;
import org.ntlab.graffiti.common.helpers.SnackbarHelper;
import org.ntlab.graffiti.common.helpers.TrackingStateHelper;
import org.ntlab.graffiti.common.rendering.BackgroundRenderer;
import org.ntlab.graffiti.common.rendering.ObjectRenderer;
import org.ntlab.graffiti.common.rendering.ObjectRenderer.BlendMode;
import org.ntlab.graffiti.common.rendering.PlaneRenderer;
import org.ntlab.graffiti.common.rendering.PointCloudRenderer;
import com.google.ar.core.exceptions.CameraNotAvailableException;
import com.google.ar.core.exceptions.UnavailableApkTooOldException;
import com.google.ar.core.exceptions.UnavailableArcoreNotInstalledException;
import com.google.ar.core.exceptions.UnavailableSdkTooOldException;
import com.google.common.base.Preconditions;
//import com.google.firebase.database.DatabaseError;
import java.io.IOException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import static java.util.stream.Collectors.toList;

/**
 * Main Activity for the Cloud Anchor Example
 *
 * <p>This is a simple example that shows how to host and resolve anchors using ARCore Cloud Anchors
 * API calls. This app only has at most one anchor at a time, to focus more on the cloud aspect of
 * anchors.
 */
public class ColoringBattleActivity extends AppCompatActivity implements GLSurfaceView.Renderer, NoticeDialogListener {
    private static final String TAG = ColoringBattleActivity.class.getSimpleName();
//    private static final float[] OBJECT_COLOR = new float[] {139.0f, 195.0f, 74.0f, 255.0f};

//    private enum HostResolveMode {
//        NONE,
//        HOSTING,
//        RESOLVING,
//    }

    // Rendering. The Renderers are created here, and initialized when the GL surface is created.
    private GLSurfaceView surfaceView;
    private final BackgroundRenderer backgroundRenderer = new BackgroundRenderer();
    private final ObjectRenderer virtualObject = new ObjectRenderer();
    private final ObjectRenderer virtualObjectShadow = new ObjectRenderer();
    private final PlaneRenderer planeRenderer = new PlaneRenderer();
    private final PointCloudRenderer pointCloudRenderer = new PointCloudRenderer();
    private final GraffitiRenderer graffitiRenderer = new GraffitiRenderer();


    private boolean installRequested;

    // Temporary matrices allocated here to reduce number of allocations for each frame.
//    private final float[] anchorMatrix = new float[16];
    private final float[] viewMatrix = new float[16];
    private final float[] projectionMatrix = new float[16];

    // Locks needed for synchronization
    private final Object anchorLock = new Object();

    // Tap handling and UI.
    private final SnackbarHelper snackbarHelper = new SnackbarHelper();
    private final SnackbarHelper planeDiscoverySnackbarHelper = new SnackbarHelper();
    private DisplayRotationHelper displayRotationHelper;
    private TapHelper tapHelper;
    private final TrackingStateHelper trackingStateHelper = new TrackingStateHelper(this);
//    private Button hostButton;
//    private Button resolveButton;
    private TextView roomCodeText;
    private SharedPreferences sharedPreferences;
    private static final String PREFERENCE_FILE_KEY = "allow_sharing_images";
    private static final String ALLOW_SHARE_IMAGES_KEY = "ALLOW_SHARE_IMAGES";

    private Session session;

    @GuardedBy("anchorLock")
    private Anchor anchor;

    private PointF coordinate;
    private Set<Anchor> anchors = new HashSet<>();

    // Cloud Anchor Components.
//    private FirebaseManager firebaseManager;
    private ServiceManager serviceManager;
    private final ColoringBattleManager cloudManager = new ColoringBattleManager();
//    private HostResolveMode currentMode;
    private CloudAnchorListener hostListener;

    private PlaneDiscoveryController planeDiscoveryController;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate()");
        setContentView(R.layout.activity_coloring_battle);
        surfaceView = findViewById(R.id.surfaceview);
        displayRotationHelper = new DisplayRotationHelper(this);

        // Set up tap listener.
        tapHelper = new TapHelper(this, ColoringBattleActivity.this);
        surfaceView.setOnTouchListener(tapHelper);

        // Set up renderer.
        surfaceView.setPreserveEGLContextOnPause(true);
        surfaceView.setEGLContextClientVersion(2);
        surfaceView.setEGLConfigChooser(8, 8, 8, 8, 16, 0); // Alpha used for plane blending.
        surfaceView.setRenderer(this);
        surfaceView.setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);
        surfaceView.setWillNotDraw(false);
        installRequested = false;

        // Set up the HandMotion View.
        LayoutInflater inflater = LayoutInflater.from(this);
        FrameLayout handmotion = findViewById(R.id.plane_discovery_view);
        FrameLayout instructionsView = (FrameLayout)inflater.inflate(R.layout.view_plane_discovery, handmotion, true);
        planeDiscoveryController = new PlaneDiscoveryController(instructionsView);

        // Initialize UI components.
//        hostButton = findViewById(R.id.host_button);
//        hostButton.setOnClickListener((view) -> onHostButtonPress());
//        resolveButton = findViewById(R.id.resolve_button);
//        resolveButton.setOnClickListener((view) -> onResolveButtonPress());
        roomCodeText = findViewById(R.id.room_code_text);

        // Initialize Cloud Anchor variables.
//        firebaseManager = new FirebaseManager(this);
        serviceManager = new ServiceManager();
//        currentMode = HostResolveMode.NONE;
        sharedPreferences = getSharedPreferences(PREFERENCE_FILE_KEY, Context.MODE_PRIVATE);
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume()");

        if (sharedPreferences.getBoolean(ALLOW_SHARE_IMAGES_KEY, false)) {
            createSession();
        }
        surfaceView.onResume();
        displayRotationHelper.onResume();
        planeDiscoveryController.show();
        planeDiscoverySnackbarHelper.showMessage(this, "端末を持ち上げて、カメラに映った壁や床を写してください。");
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
                session = new Session(this);
            } catch (UnavailableArcoreNotInstalledException e) {
                messageId = R.string.snackbar_arcore_unavailable;
                exception = e;
            } catch (UnavailableApkTooOldException e) {
                messageId = R.string.snackbar_arcore_too_old;
                exception = e;
            } catch (UnavailableSdkTooOldException e) {
                messageId = R.string.snackbar_arcore_sdk_too_old;
                exception = e;
            } catch (Exception e) {
                messageId = R.string.snackbar_arcore_exception;
                exception = e;
            }

            if (exception != null) {
                snackbarHelper.showError(this, getString(messageId));
                Log.e(TAG, "Exception creating session", exception);
                return;
            }

            // Create default config and check if supported.
            Config config = new Config(session);
            config.setCloudAnchorMode(Config.CloudAnchorMode.ENABLED);
            session.configure(config);

            // Setting the session in the HostManager.
            cloudManager.setSession(session);
        }

        // Note that order matters - see the note in onPause(), the reverse applies here.
        try {
            session.resume();
        } catch (CameraNotAvailableException e) {
            snackbarHelper.showError(this, getString(R.string.snackbar_camera_unavailable));
            session = null;
            return;
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
        Log.d(TAG, "onStop()");
        super.onStop();
    }

    @Override
    public void onDestroy() {
        resetMode();
        Log.d(TAG, "onDestroy()");
        super.onDestroy();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] results) {
        if (!CameraPermissionHelper.hasCameraPermission(this)) {
            Toast.makeText(this, "Camera permission is needed to run this application", Toast.LENGTH_LONG).show();
            if (!CameraPermissionHelper.shouldShowRequestPermissionRationale(this)) {
                // Permission denied with checking "Do not ask again".
                CameraPermissionHelper.launchPermissionSettings(this);
            }
            finish();
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        FullScreenHelper.setFullScreenOnWindowFocusChanged(this, hasFocus);
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
        synchronized (anchorLock) {
            // Only handle a tap if the anchor is currently null, the queued tap is non-null and the
            // camera is currently tracking.
            MotionEvent tap = tapHelper.poll();
            if (tap != null && cameraTrackingState == TrackingState.TRACKING) {
//                Preconditions.checkState(currentMode == HostResolveMode.HOSTING, "We should only be creating an anchor in hosting mode.");
                for (HitResult hit : frame.hitTest(tap)) {
                    if (shouldCreateAnchorWithHit(hit)) {
                        // Check if any plane was hit, and if it was hit inside the plane polygon
                        Trackable trackable = hit.getTrackable();
                        Pose planePose = ((Plane) trackable).getCenterPose();
                        for(Anchor anchor: ((Plane) trackable).getAnchors()) {
                                Log.d(TAG, "getAnchor: " + anchor + ", " + anchor.getCloudAnchorId());
                        }
                        float hitMinusCenterX = hit.getHitPose().tx() - planePose.tx();
                        float hitMinusCenterY = hit.getHitPose().ty() - planePose.ty();
                        float hitMinusCenterZ = hit.getHitPose().tz() - planePose.tz();
                        float hitOnPlaneCoordX = planePose.getXAxis()[0] * hitMinusCenterX + planePose.getXAxis()[1] * hitMinusCenterY + planePose.getXAxis()[2] * hitMinusCenterZ;
                        float hitOnPlaneCoordZ = planePose.getZAxis()[0] * hitMinusCenterX + planePose.getZAxis()[1] * hitMinusCenterY + planePose.getZAxis()[2] * hitMinusCenterZ;
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
                        Preconditions.checkNotNull(hostListener, "The host listener cannot be null.");
                        PointF newCoordinate = new PointF(hitOnPlaneCoordX, -hitOnPlaneCoordZ);
                        Anchor newAnchor = null;
                        List<Anchor> anchor = trackable.getAnchors().stream().collect(toList());
                        if(anchor.size() == 0) {
                             newAnchor = hit.createAnchor();
                        } else {
                            newAnchor = anchor.get(0);
                            if (anchors.contains(anchor.get(0))) {
                                for (Iterator<Anchor> i = anchors.iterator(); i.hasNext();) {
                                    Log.d(TAG, "anchors:" + i.next() + i.next().getCloudAnchorId());
                                }
                            }
                        }
                        Log.d(TAG, "newAnchor:" + newAnchor);
                        cloudManager.hostCloudAnchor(newAnchor, hostListener, newCoordinate);
                        setNewAnchor(newAnchor, newCoordinate);
                        anchors.add(newAnchor);
                        snackbarHelper.showMessage(this, getString(R.string.snackbar_anchor_placed));
                        Log.d(TAG, "trackable: " + trackable);
                        Log.d(TAG, "trackableCenterPose: " + ((Plane) trackable).getCenterPose());
//                        Log.d(TAG, "anchor: " + hit.getTrackable().createAnchor(planePose));
//                        PointF newCoordinate;
                        if (color == Color.TRANSPARENT) {
//                            newCoordinate = graffitiRenderer.drawTexture(hitOnPlaneCoordX, -hitOnPlaneCoordZ, 9, trackable, drawer);
                            graffitiRenderer.drawTexture(hitOnPlaneCoordX, -hitOnPlaneCoordZ, 9, trackable, drawer);
                        } else {
//                            newCoordinate = graffitiRenderer.drawTexture(hitOnPlaneCoordX, -hitOnPlaneCoordZ, 4, trackable, drawer);
                            graffitiRenderer.drawTexture(hitOnPlaneCoordX, -hitOnPlaneCoordZ, 4, trackable, drawer);
                        }
//                        cloudManager.hostCloudAnchor(newAnchor, hostListener, newCoordinate);
//                        setNewAnchor(newAnchor, newCoordinate);
//                        anchors.add(newAnchor);
//                        snackbarHelper.showMessage(this, getString(R.string.snackbar_anchor_placed));
                        Log.d(TAG, hitOnPlaneCoordX + ", " + -hitOnPlaneCoordZ);
                        break; // Only handle the first valid hit.
                    }
                }
            }
        }
    }

    /** Returns {@code true} if and only if the hit can be used to create an Anchor reliably. */
    private static boolean shouldCreateAnchorWithHit(HitResult hit) {
        Trackable trackable = hit.getTrackable();
        if (trackable instanceof Plane) {
            // Check if the hit was within the plane's polygon.
            return ((Plane) trackable).isPoseInPolygon(hit.getHitPose());
        } else if (trackable instanceof Point) {
            // Check if the hit was against an oriented point.
            return ((Point) trackable).getOrientationMode() == OrientationMode.ESTIMATED_SURFACE_NORMAL;
        }
        return false;
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        GLES20.glClearColor(0.1f, 0.1f, 0.1f, 1.0f);

        // Prepare the rendering objects. This involves reading shaders, so may throw an IOException.
        try {
            // Create the texture and pass it to ARCore session to be filled during update().
            backgroundRenderer.createOnGlThread(this);
            planeRenderer.createOnGlThread(this, "models/trigrid.png");
            pointCloudRenderer.createOnGlThread(this);

            virtualObject.createOnGlThread(this, "models/andy.obj", "models/andy.png");
            virtualObject.setMaterialProperties(0.0f, 2.0f, 0.5f, 6.0f);

            virtualObjectShadow.createOnGlThread(this, "models/andy_shadow.obj", "models/andy_shadow.png");
            virtualObjectShadow.setBlendMode(BlendMode.Shadow);
            virtualObjectShadow.setMaterialProperties(1.0f, 0.0f, 0.0f, 1.0f);

            graffitiRenderer.createOnGlThread(this,"models/plane.png");

        } catch (IOException ex) {
            Log.e(TAG, "Failed to read an asset file", ex);
        }
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        displayRotationHelper.onSurfaceChanged(width, height);
        GLES20.glViewport(0, 0, width, height);
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        // Clear screen to notify driver it should not load any pixels from previous frame.
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);

        if (session == null) {
            return;
        }
        // Notify ARCore session that the view size changed so that the perspective matrix and
        // the video background can be properly adjusted.
        displayRotationHelper.updateSessionIfNeeded(session);

        try {
            session.setCameraTextureName(backgroundRenderer.getTextureId());

            // Obtain the current frame from ARSession. When the configuration is set to
            // UpdateMode.BLOCKING (it is by default), this will throttle the rendering to the
            // camera framerate.
            Frame frame = session.update();
            Camera camera = frame.getCamera();
            TrackingState cameraTrackingState = camera.getTrackingState();

            // Notify the cloudManager of all the updates.
            cloudManager.onUpdate();

            // Handle user input.
            handleTap(frame, cameraTrackingState);

            // If frame is ready, render camera preview image to the GL surface.
            backgroundRenderer.draw(frame);

            // Keep the screen unlocked while tracking, but allow it to lock when tracking stops.
            trackingStateHelper.updateKeepScreenOnFlag(camera.getTrackingState());

            // If not tracking, don't draw 3d objects.
            if (cameraTrackingState == TrackingState.PAUSED) {
                return;
            }

            // Get camera and projection matrices.
            camera.getViewMatrix(viewMatrix, 0);
            camera.getProjectionMatrix(projectionMatrix, 0, 0.1f, 100.0f);

            // Check if we detected at least one plane. If so, hide the loading message.
            if (planeDiscoverySnackbarHelper.isShowing()) {
                for (Plane plane : session.getAllTrackables(Plane.class)) {
                    if (plane.getTrackingState() == TrackingState.TRACKING) {
                        planeDiscoverySnackbarHelper.hide(this);
                        Handler handler = new Handler(Looper.getMainLooper());
                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                planeDiscoveryController.hide();
                            }
                        });
                        onEnterRoom();
                        break;
                    }
                }
            }

            // Visualize tracked points.
            // Use try-with-resources to automatically release the point cloud.
            try (PointCloud pointCloud = frame.acquirePointCloud()) {
                pointCloudRenderer.update(pointCloud);
                pointCloudRenderer.draw(viewMatrix, projectionMatrix);
            }

            // Visualize planes.
            planeRenderer.drawPlanes(session.getAllTrackables(Plane.class), camera.getDisplayOrientedPose(), projectionMatrix);

            // Visualize graffiti.
            graffitiRenderer.adjustTextureAxis(frame, camera);
            graffitiRenderer.draw(session.getAllTrackables(Plane.class), camera.getDisplayOrientedPose(), projectionMatrix);

            // Check if the anchor can be visualized or not, and get its pose if it can be.
            boolean shouldDrawAnchor = false;
            synchronized (anchorLock) {
                if(anchor != null) {
                    if (!anchor.getCloudAnchorId().isEmpty() && anchor.getTrackingState() == TrackingState.TRACKING && !anchors.contains(anchor)) {
                        // Get the current pose of an Anchor in world space. The Anchor pose is updated
                        // during calls to session.update() as ARCore refines its estimate of the world.
                        anchors.add(anchor);
                        shouldDrawAnchor = true;
                    }
                }
            }

            // Visualize anchor.
            if (shouldDrawAnchor) {
                for(Plane plane: session.getAllTrackables(Plane.class)) {
//                    if (plane.getAnchors().contains(anchor)) {
                    if(plane.isPoseInPolygon(anchor.getPose())) {
                        graffitiRenderer.drawTexture(coordinate.x, coordinate.y, 4, plane, new CircleDrawer(Color.BLUE));
                        Log.d(TAG, "onDrawFrame: " + anchor.getCloudAnchorId());
                        Log.d(TAG, "onDrawFrame: " + coordinate.x + ", " + coordinate.y);
                        break;
                    }
                }
            }
        } catch (Throwable t) {
            // Avoid crashing the application due to unhandled exceptions.
            Log.e(TAG, "Exception on the OpenGL thread", t);
        }
    }

    /** Sets the new value of the current anchor. Detaches the old anchor, if it was non-null. */
    private void setNewAnchor(Anchor newAnchor, PointF newCoordinate) {
        synchronized (anchorLock) {
            if (anchor != null) {
                anchor.detach();
            }
            anchor = newAnchor;
            if (newAnchor != null) {
                Log.d(TAG, "CloudAnchorId: " + newAnchor.getCloudAnchorId());
            }
            setNewCoordinate(newCoordinate);
        }
    }

    /** Sets the new value of the current coordinate. Detaches the old coordiate, if it was non-null. */
    private void setNewCoordinate(PointF newCoordinate) {
        coordinate = newCoordinate;
        if (coordinate != null) {
            Log.d(TAG, coordinate.x + ", " + coordinate.y);
        }
    }

    @Override
    public void onBackPressed(){
        resetMode();
        // Activity を終了し, 前のページへ
        finish();
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
//        if (hostListener != null) {
//            return;
//        }
//        resolveButton.setEnabled(false);
//        hostButton.setText(R.string.cancel);
//        snackbarHelper.showMessageWithDismiss(this, getString(R.string.snackbar_on_host));
//
//        hostListener = new RoomCodeAndCloudAnchorIdListener();
//        firebaseManager.getNewRoomCode(hostListener);
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
//    private void resetMode() {
//        hostButton.setText(R.string.host_button_text);
//        hostButton.setEnabled(true);
//        resolveButton.setText(R.string.resolve_button_text);
//        resolveButton.setEnabled(true);
//        roomCodeText.setText(R.string.initial_room_code);
//        currentMode = HostResolveMode.NONE;
//        firebaseManager.clearRoomListener();
//        hostListener = null;
//        setNewAnchor(null, null);
//        snackbarHelper.hide(this);
//        cloudManager.clearListeners();
//    }

    /** Resets the mode of the app to its initial state and removes the anchors. */
    private void resetMode() {
        roomCodeText.setText(null);
//        firebaseManager.clearRoomListener();
        serviceManager.clearRoomListener();
        hostListener = null;
        setNewAnchor(null, null);
        snackbarHelper.hide(this);
        cloudManager.clearListeners();
    }

//    /** Callback function invoked when the user presses the OK button in the Resolve Dialog. */
//    private void onRoomCodeEntered(Long roomCode) {
//        currentMode = HostResolveMode.RESOLVING;
//        hostButton.setEnabled(false);
//        resolveButton.setText(R.string.cancel);
//        roomCodeText.setText(String.valueOf(roomCode));
//        snackbarHelper.showMessageWithDismiss(this, getString(R.string.snackbar_on_resolve));
//
//        // Register a new listener for the given room.
//        firebaseManager.registerNewListenerForRoom(roomCode, (cloudAnchorId,coordinate) -> {
//                    // When the cloud anchor ID is available from Firebase.
//                    CloudAnchorResolveStateListener resolveListener = new CloudAnchorResolveStateListener(roomCode);
//                    Preconditions.checkNotNull(resolveListener, "The resolve listener cannot be null.");
//                    cloudManager.resolveCloudAnchor(cloudAnchorId, resolveListener, SystemClock.uptimeMillis(), coordinate);
//                });
//    }

    /** Callback function invoked when the user presses the OK button in the Resolve Dialog. */
    private void onRoomCodeEntered(Long roomCode) {
        CloudAnchorListener cloudAnchorListener = new CloudAnchorListener();
        hostListener = cloudAnchorListener;
        cloudAnchorListener.onNewRoomCode(roomCode);

        // Register a new listener for the given room.
//        firebaseManager.registerNewListenerForRoom(roomCode, (cloudAnchorId, coordinate) -> {
//            // When the cloud anchor ID is available from Firebase.
//            Preconditions.checkNotNull(cloudAnchorListener, "The Cloud Anchor listener cannot be null.");
//            cloudManager.resolveCloudAnchor(cloudAnchorId, cloudAnchorListener, SystemClock.uptimeMillis(), coordinate);
//        });
        serviceManager.registerNewListenerForRoom(roomCode, (cloudAnchorId, coordinate) -> {
            // When the cloud anchor ID is available from Firebase.
            Preconditions.checkNotNull(cloudAnchorListener, "The Cloud Anchor listener cannot be null.");
            cloudManager.resolveCloudAnchor(cloudAnchorId, cloudAnchorListener, SystemClock.uptimeMillis(), coordinate);
        });

    }

    /**
     * Listens for both a new room code and an anchor ID, and shares the anchor ID in Firebase with
     * the room code when both are available.
     */
//    private final class RoomCodeAndCloudAnchorIdListener implements ColoringBattleManager.CloudAnchorHostListener, FirebaseManager.RoomCodeListener {
//
//        private Long roomCode;
//        private String cloudAnchorId;
//        PointF coordinate = new PointF(1.5f, 1.8f);
//
//        @Override
//        public void onNewRoomCode(Long newRoomCode) {
//            Preconditions.checkState(roomCode == null, "The room code cannot have been set before.");
//            roomCode = newRoomCode;
//            checkAndMaybeShare();
//            // Change currentMode to HOSTING after receiving the room code (not when the 'Host' button
//            // is tapped), to prevent an anchor being placed before we know the room code and able to
//            // share the anchor ID.
//            currentMode = HostResolveMode.HOSTING;
//        }
//
//        @Override
//        public void onError(DatabaseError error) {
//            Log.w(TAG, "A Firebase database error happened.", error.toException());
//            snackbarHelper.showError(ColoringBattleActivity.this, getString(R.string.snackbar_firebase_error));
//        }
//
//        @Override
//        public void onCloudHostTaskComplete(Anchor anchor, PointF coordinate) {
//            Anchor.CloudAnchorState cloudState = anchor.getCloudAnchorState();
//            if (cloudState.isError()) {
//                Log.e(TAG, "Error hosting a cloud anchor, state " + cloudState);
//                snackbarHelper.showMessageWithDismiss(ColoringBattleActivity.this, getString(R.string.snackbar_host_error, cloudState));
//                return;
//            }
//            Preconditions.checkState(cloudAnchorId == null, "The cloud anchor ID cannot have been set before.");
//            anchors.add(anchor);
//            cloudAnchorId = anchor.getCloudAnchorId();
//            this.coordinate = coordinate;
//            setNewAnchor(anchor, coordinate);
//            checkAndMaybeShare();
//        }
//
//        private void checkAndMaybeShare() {
//            if (roomCode == null || cloudAnchorId == null) {
//                return;
//            }
//            firebaseManager.storeAnchorIdInRoom(roomCode, cloudAnchorId, coordinate);
//            snackbarHelper.showMessageWithDismiss(ColoringBattleActivity.this, getString(R.string.snackbar_cloud_id_shared));
//        }
//    }

//    private final class CloudAnchorResolveStateListener implements ColoringBattleManager.CloudAnchorResolveListener {
//        private final long roomCode;
//
//        CloudAnchorResolveStateListener(long roomCode) {
//            this.roomCode = roomCode;
//        }
//
//        @Override
//        public void onCloudResolveTaskComplete(Anchor anchor, PointF coordinate) {
//            // When the anchor has been resolved, or had a final error state.
//            CloudAnchorState cloudState = anchor.getCloudAnchorState();
//            if (cloudState.isError()) {
//                Log.w(TAG, "The anchor in room " + roomCode + " could not be resolved. The error state was " + cloudState);
//                snackbarHelper.showMessageWithDismiss(ColoringBattleActivity.this, getString(R.string.snackbar_resolve_error, cloudState));
//                return;
//            }
//            snackbarHelper.showMessageWithDismiss(ColoringBattleActivity.this, getString(R.string.snackbar_resolve_success));
//            setNewAnchor(anchor, coordinate);
//        }
//
//        @Override
//        public void onShowResolveMessage() {
//            snackbarHelper.setMaxLines(4);
//            snackbarHelper.showMessageWithDismiss(ColoringBattleActivity.this, getString(R.string.snackbar_resolve_no_result_yet));
//        }
//    }

    /**
     * Listens for both a new room code and an anchor ID, and shares the anchor ID in Firebase with
     * the room code when both are available.
     */
    public final class CloudAnchorListener implements ColoringBattleManager.CloudAnchorHostListener, ServiceManager.RoomCodeListener, ColoringBattleManager.CloudAnchorResolveListener {

        private Long roomCode;
        private String cloudAnchorId;
        PointF coordinate = new PointF(1.5f, 1.8f);

        @Override
        public void onNewRoomCode(Long newRoomCode) {
            roomCode = newRoomCode;
            roomCodeText.setText(String.valueOf(roomCode));
            snackbarHelper.showMessageWithDismiss(ColoringBattleActivity.this, "Now in No. " + roomCode + " room. Press Cancel to Exit.");
            checkAndMaybeShare();
        }

//        @Override
//        public void onError(DatabaseError error) {
//            Log.w(TAG, "A Firebase database error happened.", error.toException());
//            snackbarHelper.showError(ColoringBattleActivity.this, getString(R.string.snackbar_firebase_error));
//        }

        @Override
        public void onCloudHostTaskComplete(Anchor anchor, PointF coordinate) {
            Anchor.CloudAnchorState cloudState = anchor.getCloudAnchorState();
            if (cloudState.isError()) {
                Log.e(TAG, "Error hosting a cloud anchor, state " + cloudState);
                snackbarHelper.showMessageWithDismiss(ColoringBattleActivity.this, getString(R.string.snackbar_host_error, cloudState));
                return;
            }
            Preconditions.checkState(cloudAnchorId == null, "The cloud anchor ID cannot have been set before.");
            anchors.add(anchor);
            cloudAnchorId = anchor.getCloudAnchorId();
            this.coordinate = coordinate;
            setNewAnchor(anchor, coordinate);
            checkAndMaybeShare();
        }

        private void checkAndMaybeShare() {
            if (roomCode == null || cloudAnchorId == null) {
                return;
            }
//            firebaseManager.storeAnchorIdInRoom(roomCode, cloudAnchorId, coordinate);
            serviceManager.storeAnchorIdInRoom(roomCode, cloudAnchorId, coordinate);
            snackbarHelper.showMessageWithDismiss(ColoringBattleActivity.this, getString(R.string.snackbar_cloud_id_shared));
            cloudAnchorId = null;
        }

        @Override
        public void onCloudResolveTaskComplete(Anchor anchor, PointF coordinate) {
            // When the anchor has been resolved, or had a final error state.
            CloudAnchorState cloudState = anchor.getCloudAnchorState();
            if (cloudState.isError()) {
                Log.w(TAG, "The anchor in room " + roomCode + " could not be resolved. The error state was " + cloudState);
                snackbarHelper.showMessageWithDismiss(ColoringBattleActivity.this, getString(R.string.snackbar_resolve_error, cloudState));
                return;
            }
            snackbarHelper.showMessageWithDismiss(ColoringBattleActivity.this, getString(R.string.snackbar_resolve_success));
            setNewAnchor(anchor, coordinate);
        }

        @Override
        public void onShowResolveMessage() {
            snackbarHelper.setMaxLines(4);
            snackbarHelper.showMessageWithDismiss(ColoringBattleActivity.this, getString(R.string.snackbar_resolve_no_result_yet));
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