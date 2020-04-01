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
import com.google.ar.core.PointCloud;
import com.google.ar.core.Pose;
import com.google.ar.core.Session;
import com.google.ar.core.Trackable;
import com.google.ar.core.TrackingState;

import org.ntlab.graffiti.R;
import org.ntlab.graffiti.common.drawer.CircleDrawer;
import org.ntlab.graffiti.common.drawer.RectangleDrawer;
import org.ntlab.graffiti.common.drawer.TextureDrawer;
import org.ntlab.graffiti.common.geometry.Vector;
import org.ntlab.graffiti.common.helpers.TapHelper;
import org.ntlab.graffiti.common.rendering.GraffitiRenderer;
import org.ntlab.graffiti.common.views.PlaneDiscoveryController;
import org.ntlab.graffiti.entities.PointTex2D;
import org.ntlab.graffiti.entities.SharedAnchor;
import org.ntlab.graffiti.entities.SharedPlane;
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
import java.io.IOException;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
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
    private static final String TAGTEST = ColoringBattleActivity.class.getSimpleName() + "Shared";
    private static final String TAGPLANE = ColoringBattleActivity.class.getSimpleName() + "Plane";
    private static final String TAGANCHOR = ColoringBattleActivity.class.getSimpleName() + "Anchor";
    private static final String TAGSTROKE = ColoringBattleActivity.class.getSimpleName() + "Stroke";
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

//    private Map<PlaneJSON, Anchor> pendingAnchors = new HashMap<>();
    private static Map<Anchor, Plane> pendingAnchors = new HashMap<>();
    private static Map<Anchor, Plane> myAnchors = new HashMap<>();
    private static List<Anchor> partnerAnchors = new ArrayList<>();
    private static Map<String, SharedAnchor> sharedAnchors = new HashMap<>();
    private static Map<Plane, SharedAnchor> planeAnchors = new HashMap<>();

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
        Log.d(TAGTEST, "onStop()");
        Log.d(TAGTEST,  "PlaneSize:" + session.getAllTrackables(Plane.class).size());
        int cnt = 0;
        for (Plane plane : session.getAllTrackables(Plane.class)) {
            if (plane.getSubsumedBy() == null) {
                Log.d(TAGTEST,  "NotSubsumedPlane:" + plane);
                cnt++;
            } else {
                Log.d(TAGTEST,  "SubsumedPlane:" + plane + " subsumed by " + plane.getSubsumedBy());

            }
        }
        Log.d(TAGTEST,  "NotSubsumedPlaneSize:" + cnt);
//        Log.d(TAGTEST,  "pendingAnchorsSize:" + pendingAnchors.size() + ", myAcnhorsSize:" + myAnchors.size() + ", partnerAnchorsSize:" + partnerAnchors.size() + ", sharedAnchorsSize:" + sharedAnchors.size());
//        for (Anchor anchor: pendingAnchors.keySet()) {
//            Log.d(TAGTEST,  "pendingAnchors:" + anchor + ", " + pendingAnchors.get(anchor));
//        }
        for (Anchor anchor: myAnchors.keySet()) {
            Log.d(TAGTEST,  "myAnchors:" + anchor + ", " + anchor.getCloudAnchorId() + ", " + myAnchors.get(anchor));
        }
        for (Anchor anchor: partnerAnchors) {
            Log.d(TAGTEST,  "partnerAnchors:" + anchor + ", " + anchor.getCloudAnchorId());
        }

//        cnt = 0;
//        for (SharedAnchor sharedAnchor: sharedAnchors.values()) {
//            if (sharedAnchor.getMargedPlane() instanceof SharedPlane) {
//                Log.d(TAGTEST,  "margedPlane:" + sharedAnchor.getMyAnchor().getCloudAnchorId() + ", " + sharedAnchor.getPartnerAnchor().getCloudAnchorId());
//                cnt++;
//            }
//        }
        cnt = 0;
        for (Plane plane: planeAnchors.keySet()) {
            if (planeAnchors.get(plane).getMargedPlane() instanceof SharedPlane) {
                float[] margedMyPlaneXAxis = planeAnchors.get(plane).getMyAnchor().getPose().getXAxis();
                float[] margedMyPlaneZAxis = planeAnchors.get(plane).getMyAnchor().getPose().getZAxis();
                float[] margedPartnerPlaneXAxis = planeAnchors.get(plane).getPartnerAnchor().getPose().getXAxis();
                float[] margedPartnerPlaneZAxis = planeAnchors.get(plane).getPartnerAnchor().getPose().getZAxis();
                Log.d(TAGSTROKE,  "margedMyPlane: " + plane + ", " + planeAnchors.get(plane).getMyAnchor().getCloudAnchorId() + ", (" + margedMyPlaneXAxis[0] + ", " + margedMyPlaneXAxis[1] + ", " + margedMyPlaneXAxis[2] + "), "+ ", (" + margedMyPlaneZAxis[0] + ", " + margedMyPlaneZAxis[1] + ", " + margedMyPlaneZAxis[2] + ")");
                Log.d(TAGSTROKE,  "margedPartnerPlane: " + plane + ", " + planeAnchors.get(plane).getPartnerAnchor().getCloudAnchorId() + ", (" + margedPartnerPlaneXAxis[0] + ", " + margedPartnerPlaneXAxis[1] + ", " + margedPartnerPlaneXAxis[2] + "), "+ ", (" + margedPartnerPlaneZAxis[0] + ", " + margedPartnerPlaneZAxis[1] + ", " + margedPartnerPlaneZAxis[2] + ")");
                cnt++;
            }
        }
        Log.d(TAGTEST,  "MargedPlaneSize:" + cnt);
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
                    Plane plane = shouldCreateAnchorWithHit(hit);
                    if (plane != null && plane.getSubsumedBy() == null) {
                        // Check if any plane was hit, and if it was hit inside the plane polygon
                        Pose planePose = plane.getCenterPose();
                        for(Anchor anchor: plane.getAnchors()) {
                                Log.d(TAG, "getAnchor: " + anchor + ", " + anchor.getCloudAnchorId());
                        }
                        // ワールド座標系から平面のローカル座標への変換
                        float[] hitMinusPlaneCenter = Vector.minus(hit.getHitPose().getTranslation(), planePose.getTranslation());
                        float hitOnPlaneCoordX = Vector.dot(planePose.getXAxis(), hitMinusPlaneCenter);
                        float hitOnPlaneCoordZ = Vector.dot(planePose.getZAxis(), hitMinusPlaneCenter);
//                        float hitMinusCenterX = hit.getHitPose().tx() - planePose.tx();
//                        float hitMinusCenterY = hit.getHitPose().ty() - planePose.ty();
//                        float hitMinusCenterZ = hit.getHitPose().tz() - planePose.tz();
//                        float hitOnPlaneCoordX = planePose.getXAxis()[0] * hitMinusCenterX + planePose.getXAxis()[1] * hitMinusCenterY + planePose.getXAxis()[2] * hitMinusCenterZ;
//                        float hitOnPlaneCoordZ = planePose.getZAxis()[0] * hitMinusCenterX + planePose.getZAxis()[1] * hitMinusCenterY + planePose.getZAxis()[2] * hitMinusCenterZ;
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
//                        Anchor newAnchor = null;
                        List<Anchor> anchor = plane.getAnchors().stream().collect(toList());
                        if(anchor.size() == 0) {
//                             newAnchor = hit.createAnchor();
                            Log.d(TAG, "PlaneJSON#createAnchor: " + ((Plane) plane).createAnchor(hit.getHitPose()));
                        } else {
//                            newAnchor = anchor.get(0);
                            if (anchors.contains(anchor.get(0))) {
                                for (Iterator<Anchor> i = anchors.iterator(); i.hasNext();) {
                                    Log.d(TAG, "anchors:" + i.next() + i.next().getCloudAnchorId());
                                }
                            }
                        }
//                        Log.d(TAG, "newAnchor:" + newAnchor);
//                        cloudManager.hostCloudAnchor(newAnchor, hostListener, newCoordinate);
//                        setNewAnchor(newAnchor, newCoordinate);
//                        anchors.add(newAnchor);
                        snackbarHelper.showMessage(this, getString(R.string.snackbar_anchor_placed));
                        if (color == Color.TRANSPARENT) {
//                            newCoordinate = graffitiRenderer.drawTexture(hitOnPlaneCoordX, -hitOnPlaneCoordZ, 9, trackable, drawer);
                            graffitiRenderer.drawTexture(hitOnPlaneCoordX, -hitOnPlaneCoordZ, 9, plane, drawer);
                        } else {
//                            newCoordinate = graffitiRenderer.drawTexture(hitOnPlaneCoordX, -hitOnPlaneCoordZ, 4, trackable, drawer);
                            graffitiRenderer.drawTexture(hitOnPlaneCoordX, -hitOnPlaneCoordZ, 4, plane, drawer);
                        }
                        hostListener.onStoreStroke(plane, hitOnPlaneCoordX, -hitOnPlaneCoordZ);

//                        cloudManager.hostCloudAnchor(newAnchor, hostListener, newCoordinate);
//                        setNewAnchor(newAnchor, newCoordinate);
//                        anchors.add(newAnchor);
//                        snackbarHelper.showMessage(this, getString(R.string.snackbar_anchor_placed));
                        Log.d(TAGSTROKE, "hit.getHitPose().getTranslation(): " + hit.getHitPose().getTranslation());
                        Log.d(TAGSTROKE, "hitOnPlaneCoord: " + hitOnPlaneCoordX + ", " + -hitOnPlaneCoordZ);
                        break; // Only handle the first valid hit.
                    }
                }
            }
        }
    }

    /** Returns {@code true} if and only if the hit can be used to create an Anchor reliably. */
    private static Plane shouldCreateAnchorWithHit(HitResult hit) {
        Trackable trackable = hit.getTrackable();
        if (trackable instanceof Plane) {
            // Check if the hit was within the plane's polygon.
            SharedAnchor sharedAnchor = planeAnchors.get((Plane) trackable);
            if (sharedAnchor == null || sharedAnchor.getMargedPlane() == null) {
                if (((Plane) trackable).isPoseInPolygon(hit.getHitPose())) {
                    return (Plane) trackable;
                } else {
                    return null;
                }
            } else {
                Plane sharedPlane = sharedAnchor.getMargedPlane();
                if (sharedPlane.isPoseInPolygon(hit.getHitPose())) {
                    return sharedPlane;
                } else {
                    return null;
                }
            }
        } else if (trackable instanceof Point) {
            // Check if the hit was against an oriented point.
            return null;
//            return ((Point) trackable).getOrientationMode() == OrientationMode.ESTIMATED_SURFACE_NORMAL;
        }
        return null;
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

//            Log.d(TAG,  "PlaneSize:" + session.getAllTrackables(Plane.class).size());
//            int cnt = 0;
//            for (Plane plane : session.getAllTrackables(Plane.class)) {
//                if (plane.getSubsumedBy() == null) {
//                    cnt++;
//                }
//            }
//            Log.d(TAG,  "NotSubsumedPlaneSize:" + cnt);
//            Log.d(TAG,  "pendingAnchorsSize:" + pendingAnchors.size() + ", myAcnhorsSize:" + myAnchors.size() + ", partnerAnchorsSize:" + partnerAnchors.size() + ", sharedAnchorsSize:" + sharedAnchors.size());
//            cnt = 0;
//            for (SharedAnchor sharedAnchor: sharedAnchors.values()) {
//                if (sharedAnchor.getMargedPlane() instanceof SharedPlane) {
//                    cnt++;
//                }
//            }
//            Log.d(TAG,  "MargedPlaneSize:" + cnt);


//            for (Plane plane : session.getAllTrackables(Plane.class)) {
//                Log.d(TAGPLANE,  "Plane:" + plane + ", " + plane.getCenterPose() + ", " + plane.getSubsumedBy());
//            }

            for (Plane newPlane : frame.getUpdatedTrackables(Plane.class)) {
                Log.d(TAGPLANE, "UpdatePlane:" + newPlane + ", " + newPlane.getCenterPose() + ", " + newPlane.getSubsumedBy());
            }
            Log.d(TAGPLANE, "\n");

//            Log.d(TAG,  "UpdatePlaneSize:" + frame.getUpdatedTrackables(PlaneJSON.class).size());
            if (hostListener != null) {
                for (Plane newPlane : frame.getUpdatedTrackables(Plane.class)) {
//                Log.d(TAGTEST,  "UpdatePlane:" + newPlane + ", " + newPlane.getCenterPose() + ", " + newPlane.getSubsumedBy());
                    if (newPlane.getSubsumedBy() == null) {
                        boolean flag = false;
                        Plane oldPlane = null;
                        for (Map.Entry<Plane, SharedAnchor> planeAnchorEntry : planeAnchors.entrySet()) {
                            SharedAnchor sharedAnchor = planeAnchorEntry.getValue();
                            Plane plane = sharedAnchor.getMargedPlane();
                            if (plane.getSubsumedBy() != null && plane.getSubsumedBy().equals(newPlane)) {
                                Log.d(TAGTEST, "planeAnchorSubsumed." + newPlane);
                                flag = true;
                                // 既にSharedAnchorsに入っているplaneがSharedPlaneだったときもPlaneだったときも
                                // 座標変換 myAnchor座標系でのnewPlaneの位置を求めたい newPlane->myAnchor + margePlane
                                sharedAnchor.updatePlane(newPlane);
                                FloatBuffer currentPolygon = ((SharedPlane) sharedAnchor.getMargedPlane()).getCurrentPolygon();
                                hostListener.onStorePolygon(sharedAnchor.getMyAnchor().getCloudAnchorId(), currentPolygon);
                                sharedAnchor.setPrevPolygon(currentPolygon);
                                oldPlane = planeAnchorEntry.getKey();
                                break;
                            }
                        }
                        if (oldPlane != null) planeAnchors.put(newPlane, planeAnchors.remove(oldPlane));

                        HashMap<Anchor, Plane> cloneMyAnchors = new HashMap<>(myAnchors);
                        for (Map.Entry<Anchor, Plane> myAnchorEntry : cloneMyAnchors.entrySet()) {
                            if (myAnchorEntry.getValue() != null && myAnchorEntry.getValue().getSubsumedBy() != null && myAnchorEntry.getValue().getSubsumedBy().equals(newPlane)) {
//                                if (!flag) {
                                    Log.d(TAGTEST, "myAnchorSubsumed." + newPlane);
                                    flag = true;
                                    // 座標変換 myAnchor座標系でのnewPlaneの位置を求めたい newPlane->myAnchor + margePlane
                                    Plane plane = myAnchorEntry.getValue();
                                    if (!(plane instanceof SharedPlane)) {
                                        plane = new SharedPlane(plane);
                                    }
                                    ((SharedPlane) plane).setCurrentPlane(newPlane);
                                    ((SharedPlane) plane).updatePolygon(newPlane.getPolygon());
//                                        myAnchorEntry.setValue(plane);
                                    myAnchors.put(myAnchorEntry.getKey(), plane);
//                                } else {
//                                    Log.d(TAGTEST, "myAnchors remove:" + myAnchorEntry.getKey());
//                                    myAnchors.remove(myAnchorEntry.getKey());
//                                }
                                // 座標変換
//                                    Anchor myAnchor = sharedAnchorEntry.getValue().getMyAnchor();
//                                    FloatBuffer newPlanePolygon = newPlane.getPolygon();
//                                    Pose myPose = myAnchor.getPose();
//                                    Pose newPlanePose = newPlane.getCenterPose();
//                                    Pose myInversePose = myPose.inverse();
//                                    FloatBuffer polygon = FloatBuffer.allocate(newPlanePolygon.capacity());
//                                    for (int i = 0; i < newPlanePolygon.capacity(); i += 2) {
//                                        PointPlane2D newPlaneLocal = new PointPlane2D(newPlanePolygon.get(i), newPlanePolygon.get(i+1));
//                                        float[] newPlaneRotated = newPlanePose.rotateVector(new float[]{newPlaneLocal.getX(), 0f, newPlaneLocal.getZ()});
//                                        float[] world = newPlanePose.transformPoint(newPlaneRotated);
//                                        float[] myTransformedPose = myInversePose.transformPoint(world);
//                                        float[] myLocal = myInversePose.rotateVector(myTransformedPose);
//                                        polygon.put(myLocal[0]);
//                                        polygon.put(myLocal[2]);
//                                    }
//                                    polygon.rewind();
                            }
                        }
                        HashMap<Anchor, Plane> clonePendingAnchors = new HashMap<>(pendingAnchors);
                        for (Map.Entry<Anchor, Plane> pendingAnchorEntry : clonePendingAnchors.entrySet()) {
                            if (pendingAnchorEntry.getValue().getSubsumedBy() != null && pendingAnchorEntry.getValue().getSubsumedBy().equals(newPlane)) {
//                                if (!flag) {
//                                    Log.d(TAGTEST, "pendingAnchorSubsumed." + newPlane);
                                    flag = true;
                                    // 座標変換
//                                    Plane plane = pendingAnchorEntry.getValue();
//                                    if (!(plane instanceof SharedPlane)) {
//                                        plane = new SharedPlane(plane);
//                                    }
//                                    ((SharedPlane) plane).setCurrentPlane(newPlane);
//                                    ((SharedPlane) plane).updatePolygon(newPlane.getPolygon());
//                                        pendingAnchorEntry.setValue(plane);
//                                    pendingAnchors.put(pendingAnchorEntry.getKey(), plane);
//                                } else {
                                    Log.d(TAGTEST, ", pendingAnchors.remove, " + pendingAnchorEntry.getValue());
                                    pendingAnchors.remove(pendingAnchorEntry.getKey());
//                                }
                            }
                        }
                        if (!pendingAnchors.values().contains(newPlane) && !myAnchors.values().contains(newPlane)) {
                            // newPlane 一番外側のPlaneのみ
                            if (!planeAnchors.keySet().contains(newPlane)) {
                                if (!flag) {
                                    Anchor hostAnchor = cloudManager.hostCloudAnchor(newPlane.createAnchor(newPlane.getCenterPose()), hostListener, null);
                                    pendingAnchors.put(hostAnchor, newPlane);
                                    Log.d(TAGTEST, ", pendingAnchors.put, " + hostAnchor.getCloudAnchorId() + ", " + newPlane + ", " + newPlane.getSubsumedBy());
                                    snackbarHelper.showMessageWithDismiss(ColoringBattleActivity.this, "pendingAnchor put.");
                                }
                            } else {
                                // 既にplaneAnchors含まれている同じ平面のpolygon情報のみが更新された場合
                                SharedAnchor sharedAnchor = planeAnchors.get(newPlane);
                                if (!sharedAnchor.getPrevPolygon().equals(newPlane.getPolygon())) {
                                    hostListener.onStorePolygon(planeAnchors.get(newPlane).getMyAnchor().getCloudAnchorId(), newPlane.getPolygon());
                                    sharedAnchor.setPrevPolygon(newPlane.getPolygon());
                                }
//                                Log.d(TAGTEST, "else");
                                // REST marged?
                            }
                        }
                    }
                }

                for (Anchor anchor: myAnchors.keySet()) {
                    Log.d(TAGANCHOR,  ", myAnchorPose, " + anchor.getCloudAnchorId() + ", " + anchor.getPose() + ", (" + anchor.getPose().getXAxis().toString() + ", " + anchor.getPose().getZAxis().toString() + ")");
                }
                for (Anchor anchor: partnerAnchors) {
                    Log.d(TAGANCHOR,  ", partnerPose, " + anchor.getCloudAnchorId() + ", " + anchor.getPose() + ", (" + anchor.getPose().getXAxis().toString() + ", " + anchor.getPose().getZAxis().toString() + ")");
                }
                for (Anchor anchor: session.getAllAnchors()) {
                    Log.d(TAGANCHOR,  ", getAllAnchors, " + anchor.getCloudAnchorId() + ", " + anchor.getPose() + ", (" + anchor.getPose().getXAxis().toString() + ", " + anchor.getPose().getZAxis().toString() + ")");
                }

                for (Anchor myAnchor: myAnchors.keySet()) {
                    for (Anchor partnerAnchor : partnerAnchors) {
                        Pose myPose = myAnchor.getPose();
                        Pose patnerPose = partnerAnchor.getPose();
//                        Log.d(TAGTEST, "?sharedAnchors?" + Vector.dot(myPose.getYAxis(), patnerPose.getYAxis()));
                        if (Vector.dot(myPose.getYAxis(), patnerPose.getYAxis()) > 0.95) {
                            float[] sub = Vector.minus(patnerPose.getTranslation(), myPose.getTranslation());
//                            Log.d(TAGTEST, "?sharedAnchors?" + Math.abs(Vector.dot(sub, myPose.getYAxis())) + ", " + Vector.length(sub));
                            if (Math.abs(Vector.dot(sub, myPose.getYAxis())) < 0.15 && Vector.length(sub) < 1.0) {
                                Plane myPlane = myAnchors.get(myAnchor);
                                String partnerAnchorId = partnerAnchor.getCloudAnchorId();
                                SharedAnchor sharedAnchor = new SharedAnchor(myAnchor, partnerAnchor, myPlane);
                                sharedAnchors.put(partnerAnchorId, sharedAnchor);
                                Log.d(TAGTEST, "sharedAnchors.put:" + partnerAnchorId);
                                if (!(myPlane instanceof SharedPlane)) {
                                    planeAnchors.put(myPlane, sharedAnchor);
                                } else {
                                    planeAnchors.put(((SharedPlane) myPlane).getCurrentPlane(), sharedAnchor);
                                }
                                Log.d(TAGTEST, "planeAnchors.put:" + myAnchor.getCloudAnchorId());
                                myAnchors.remove(myAnchor);
                                partnerAnchors.remove(partnerAnchor);
                                snackbarHelper.showMessageWithDismiss(ColoringBattleActivity.this, "Shared Plane.");
                                hostListener.onStorePolygon(myAnchor.getCloudAnchorId(), myPlane.getPolygon());
                                sharedAnchors.get(partnerAnchorId).setPrevPolygon(myPlane.getPolygon());
                            }
                        }
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
//            synchronized (anchorLock) {
//                if(anchor != null) {
//                    if (!anchor.getCloudAnchorId().isEmpty() && anchor.getTrackingState() == TrackingState.TRACKING && !anchors.contains(anchor)) {
//                        // Get the current pose of an Anchor in world space. The Anchor pose is updated
//                        // during calls to session.update() as ARCore refines its estimate of the world.
//                        anchors.add(anchor);
//                        shouldDrawAnchor = true;
//                    }
//                }
//            }

            for (SharedAnchor sharedAnchor: sharedAnchors.values()) {
                // BUG when simply plane
                if (sharedAnchor.getMargedPlane() instanceof  SharedPlane) {
                    SharedPlane margedPlane = (SharedPlane) sharedAnchor.getMargedPlane();
                    List<PointTex2D> stroke = margedPlane.getStroke();
                    if (stroke.size() > margedPlane.getDrawnStrokeIndex()) {
                        for (int i = margedPlane.getDrawnStrokeIndex(); i < stroke.size(); i++) {
                            graffitiRenderer.drawTexture(stroke.get(i).getX(), stroke.get(i).getY(), 4, margedPlane, new CircleDrawer(Color.RED));
                        }
                        margedPlane.drawnStroke(stroke.size());
                    }
                }
            }

            // Visualize anchor.
//            if (shouldDrawAnchor) {
//                for(Plane plane: session.getAllTrackables(Plane.class)) {
//                    if (plane.getAnchors().contains(anchor)) {
//                    if(plane.isPoseInPolygon(anchor.getPose())) {
//                        graffitiRenderer.drawTexture(coordinate.x, coordinate.y, 4, plane, new CircleDrawer(Color.BLUE));
//                        Log.d(TAG, "onDrawFrame: " + anchor.getCloudAnchorId());
//                        Log.d(TAG, "onDrawFrame: " + coordinate.x + ", " + coordinate.y);
//                        break;
//                    }
//                }
//            }
        } catch (Throwable t) {
//             Avoid crashing the application due to unhandled exceptions.
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

//        for (Anchor anchor: pendingAnchors.keySet()) {
//            cloudManager.hostCloudAnchor(anchor, hostListener, null);
//            setNewAnchor(anchor, null);
//            anchors.add(anchor);
//        }

        serviceManager.registerNewListenerForRoom(roomCode,
                (cloudAnchorId, cloudAnchor, coordinate) -> {
                    // When the cloud anchor ID is available from Firebase.
                    Preconditions.checkNotNull(cloudAnchorListener, "The Cloud Anchor listener cannot be null.");
                    cloudManager.resolveCloudAnchor(cloudAnchorId, cloudAnchorListener, SystemClock.uptimeMillis(), coordinate);
                },
                (cloudAnchorId, cloudAnchor, coordinate) -> {
                    // When the cloud anchor ID is available from Firebase.
                    Preconditions.checkNotNull(cloudAnchorListener, "The Cloud Anchor listener cannot be null.");
                    SharedAnchor sharedAnchor = sharedAnchors.get(cloudAnchorId);
                    List<PointTex2D> newStroke = cloudAnchor.getStroke();
                    if (sharedAnchor != null) {
                        //BUG strokeも同時に入っている
                        if (cloudAnchor.getPlane() != null) sharedAnchor.margePlane(cloudAnchor.getPlane().getPolygon());
                        if(sharedAnchor.getMargedPlane() instanceof SharedPlane) {
                            // 座標変換 stroke
                            Anchor myAnchor = sharedAnchor.getMyAnchor();
                            Anchor partnerAnchor = sharedAnchor.getPartnerAnchor();
                            Pose myPose = myAnchor.getPose();
                            Pose partnerPose = partnerAnchor.getPose();
//                            Pose myInversePose = myPose.inverse();
                            SharedPlane margedPlane = (SharedPlane) sharedAnchor.getMargedPlane();
                            if (newStroke.size() > margedPlane.getStroke().size()) {
                                for (int i = margedPlane.getStroke().size(); i < newStroke.size(); i++) {
                                    PointTex2D partnerLocal = newStroke.get(i);
                                    Log.d(TAGSTROKE, i + " partnerLocal: " + partnerLocal.getX() + ", " + partnerLocal.getY());
//                                    float[] partnerRotated = partnerPose.rotateVector(new float[]{partnerLocal.getX(), 0f, partnerLocal.getY()});
//                                    float[] world = partnerPose.transformPoint(partnerRotated);
//                                    float[] myTransformedPose = myInversePose.transformPoint(world);
//                                    float[] myLocal = myInversePose.rotateVector(myTransformedPose);
//                                    margedPlane.addStroke(myLocal[0], myLocal[2]);
                                    float[] partnerRotated = Vector.add(
                                            Vector.scale(partnerPose.getXAxis(), partnerLocal.getX()),
                                            Vector.scale(partnerPose.getZAxis(), partnerLocal.getY()));
                                    float[] world = Vector.add(partnerRotated, partnerPose.getTranslation());
                                    float[] myCenter = myPose.getTranslation();
                                    float[] myAxisX = myPose.getXAxis();
                                    float[] myAxisZ = myPose.getZAxis();
                                    float[] relativeToMe = Vector.minus(world, myCenter);
                                    float myLocalX = Vector.dot(relativeToMe, myAxisX);
                                    float myLocalZ = Vector.dot(relativeToMe, myAxisZ);
                                    margedPlane.addStroke(myLocalX, myLocalZ);
                                    Log.d(TAGSTROKE, i + " myLocal: " + myLocalX + ", " + myLocalZ);

                                }
                            }
                        }
                    } else {
                        //myAnchor
                        for (Anchor anchor: partnerAnchors) {
                            anchor.getCloudAnchorId().equals(cloudAnchorId);
                        }
                    }
//                        for (PointTex2D partnerLocal: stroke) {
//                            float[] partnerRotated = partnerPose.rotateVector(new float[] {partnerLocal.getX(), 0f, partnerLocal.getY()});
//                            float[] world = partnerPose.transformPoint(partnerRotated);
//                            float[] myTransformedPose = myInversePose.transformPoint(world);
//                            float[] myLocal = myInversePose.rotateVector(myTransformedPose);
//                            SharedPlane sharedPlane = (SharedPlane) sharedAnchor.getMargedPlane();
//                            sharedPlane.addStroke(myLocal[0], myLocal[2]);
//                        }
                }
        );

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
            snackbarHelper.showMessageWithDismiss(ColoringBattleActivity.this, getString(R.string.snackbar_on_room_code_available, roomCode));
//            checkAndMaybeShare();
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
            if (pendingAnchors.get(anchor) != null) {
                checkAndMaybeShare();
                myAnchors.put(anchor, pendingAnchors.remove(anchor));
            }
            cloudAnchorId = null;
            Log.d(TAGTEST, "myAnchors.put:" + anchor + ", " + anchor.getCloudAnchorId() + ", (" + anchor.getPose().getTranslation()[0] + ", " + anchor.getPose().getTranslation()[1] + ", " + anchor.getPose().getTranslation()[2] + ") ," + myAnchors.get(anchor));
        }

        private void checkAndMaybeShare() {
            if (roomCode == null || cloudAnchorId == null) {
                return;
            }
//            firebaseManager.storeAnchorIdInRoom(roomCode, cloudAnchorId, coordinate);
            serviceManager.storeAnchorIdInRoom(roomCode, cloudAnchorId);
//            snackbarHelper.showMessageWithDismiss(ColoringBattleActivity.this, getString(R.string.snackbar_cloud_id_shared));
            snackbarHelper.showMessageWithDismiss(ColoringBattleActivity.this, "myAnchor put.");
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
            boolean flag = false;
            for (Anchor myAnchor: myAnchors.keySet()) {
//            if (!myAnchors.containsKey(anchor)) {
                if (myAnchor.getCloudAnchorId().equals(anchor.getCloudAnchorId())) {
                    flag = true;
                    break;
                }
            }
            if (!flag) {
                for (Anchor partnerAnchor : partnerAnchors) {
                    if (partnerAnchor.getCloudAnchorId().equals(anchor.getCloudAnchorId())) {
                        flag = true;
                        break;
                    }
                }
            }
            if (!flag) {
                partnerAnchors.add(anchor);
                Log.d(TAGTEST, "partnerAnchors.put:" + anchor + ", " + anchor.getCloudAnchorId() + ", (" + anchor.getPose().getTranslation()[0] + ", " + anchor.getPose().getTranslation()[1] + ", " + anchor.getPose().getTranslation()[2] + ")");
            } else {
                Log.d(TAGTEST, "partnerAnchors.notput:" + anchor + ", " + anchor.getCloudAnchorId() + ", (" + anchor.getPose().getTranslation()[0] + ", " + anchor.getPose().getTranslation()[1] + ", " + anchor.getPose().getTranslation()[2] + ")");
            }
        }

        @Override
        public void onShowResolveMessage() {
            snackbarHelper.setMaxLines(4);
            snackbarHelper.showMessageWithDismiss(ColoringBattleActivity.this, getString(R.string.snackbar_resolve_no_result_yet));
        }

        private void onStorePolygon(String cloudAnchorId, FloatBuffer polygon) {
            if (roomCode == null || cloudAnchorId == null) {
                return;
            }
            float[] polyArray = polygon.array();
//            Log.d(TAGTEST, "storePolygonInRoom:" + cloudAnchorId);
            serviceManager.storePolygonInRoom(roomCode, cloudAnchorId, polyArray);
            snackbarHelper.showMessageWithDismiss(ColoringBattleActivity.this, "Stored Polygon.");
        }

        private void onStoreStroke(Plane hitPlane, float texX, float texY) {
            if (roomCode == null) {
                return;
            }

            String cloudAnchorId = null;
            // Check if the hit was within the plane's polygon.
            if(hitPlane instanceof SharedPlane) {
                SharedAnchor sharedAnchor = planeAnchors.get(hitPlane);
                cloudAnchorId = sharedAnchor.getMyAnchor().getCloudAnchorId();
            } else {
                if (myAnchors.containsValue(hitPlane)) {
                    for (Anchor anchor : myAnchors.keySet()) {
                        Plane plane = myAnchors.get(anchor);
                        if (plane.equals(hitPlane)) {
                            cloudAnchorId = anchor.getCloudAnchorId();
                            break;
                        }
                    }
                } else {
                    snackbarHelper.showMessageWithDismiss(ColoringBattleActivity.this, "No Store Stroke.");
                    return;
                }
            }

            serviceManager.storeStrokeInRoom(roomCode, cloudAnchorId, texX, texY);
            snackbarHelper.showMessageWithDismiss(ColoringBattleActivity.this, "Stored Stroke.");
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