package org.ntlab.graffiti.graffiti;

import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.opengl.GLES30;
import android.opengl.GLSurfaceView;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.constraintlayout.widget.ConstraintLayout;

import com.google.ar.core.ArCoreApk;
import com.google.ar.core.Camera;
import com.google.ar.core.Config;
import com.google.ar.core.Frame;
import com.google.ar.core.HitResult;
import com.google.ar.core.Plane;
import com.google.ar.core.PointCloud;
import com.google.ar.core.Pose;
import com.google.ar.core.Session;
import com.google.ar.core.Trackable;
import com.google.ar.core.TrackingFailureReason;
import com.google.ar.core.TrackingState;
import com.google.ar.core.exceptions.CameraNotAvailableException;
import com.google.ar.core.exceptions.UnavailableApkTooOldException;
import com.google.ar.core.exceptions.UnavailableArcoreNotInstalledException;
import com.google.ar.core.exceptions.UnavailableDeviceNotCompatibleException;
import com.google.ar.core.exceptions.UnavailableSdkTooOldException;
import com.google.ar.core.exceptions.UnavailableUserDeclinedInstallationException;

import org.ntlab.graffiti.R;
import org.ntlab.graffiti.common.drawer.CircleDrawer;
import org.ntlab.graffiti.common.drawer.RectangleDrawer;
import org.ntlab.graffiti.common.drawer.TextureDrawer;
import org.ntlab.graffiti.common.helpers.CameraPermissionHelper;
import org.ntlab.graffiti.common.helpers.DepthSettings;
import org.ntlab.graffiti.common.helpers.DisplayRotationHelper;
import org.ntlab.graffiti.common.helpers.FullScreenHelper;
import org.ntlab.graffiti.common.helpers.InstantPlacementSettings;
import org.ntlab.graffiti.common.helpers.MusicPlayerHelper;
import org.ntlab.graffiti.common.helpers.RendererHelper;
import org.ntlab.graffiti.common.helpers.SnackbarHelper;
import org.ntlab.graffiti.common.helpers.TapHelper;
import org.ntlab.graffiti.common.helpers.TimeoutHelper;
import org.ntlab.graffiti.common.helpers.TrackingStateHelper;
import org.ntlab.graffiti.common.rendering.BackgroundRenderer;
import org.ntlab.graffiti.common.rendering.Framebuffer;
import org.ntlab.graffiti.common.rendering.GraffitiRenderer;
import org.ntlab.graffiti.common.rendering.PlaneRenderer;
import org.ntlab.graffiti.common.views.Arc;
import org.ntlab.graffiti.common.views.PlaneDetectController;
import org.ntlab.graffiti.graffiti.states.CountDownState;
import org.ntlab.graffiti.graffiti.states.GameRankingState;
import org.ntlab.graffiti.graffiti.states.GameReadyState;
import org.ntlab.graffiti.graffiti.states.GameResultState;
import org.ntlab.graffiti.graffiti.states.PlaneDetectState;
import org.ntlab.graffiti.graffiti.states.State;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/**
 * GraffitiTimeAttack Game Activity.
 * Created by a-hongo on 25,2月,2021
 * @author a-hongo
 */
public class GraffitiTimeAttackActivity extends GameActivity {
    private static final String TAG = GraffitiTimeAttackActivity.class.getSimpleName();

    private static final float Z_NEAR = 0.1f;
    private static final float Z_FAR = 100f;

    private boolean installRequested;

    private Session session;
    private final SnackbarHelper messageSnackbarHelper = new SnackbarHelper();
    private DisplayRotationHelper displayRotationHelper;
    private final TrackingStateHelper trackingStateHelper = new TrackingStateHelper(this);
    private TapHelper tapHelper;
    private final RendererHelper rendererHelper = new RendererHelper();

    private final BackgroundRenderer backgroundRenderer = new BackgroundRenderer();
//    private final BackgroundOcclusionRenderer backgroundOcclusionRenderer = new BackgroundOcclusionRenderer();
    private final GraffitiRenderer graffitiRenderer = new GraffitiRenderer();
    private Framebuffer virtualSceneFramebuffer;
    private boolean hasSetTextureNames = false;

    private final DepthSettings depthSettings = new DepthSettings();
    private final InstantPlacementSettings instantPlacementSettings = new InstantPlacementSettings();
    // Assumed distance from the device camera to the surface on which user will try to place objects.
    // This value affects the apparent scale of objects while the tracking method of the
    // Instant Placement point is SCREENSPACE_WITH_APPROXIMATE_DISTANCE.
    // Values in the [0.2, 2.0] meter range are a good choice for most AR experiences. Use lower
    // values for AR experiences where users are expected to place objects on surfaces close to the
    // camera. Use larger values for experiences where the user will likely be standing and trying to
    // place an object on the ground or floor in front of them.
    private static final float APPROXIMATE_DISTANCE_METERS = 2.0f;

    private Queue<IGLDrawListener> glDrawListenerQueue = new ArrayDeque<>();

    private PlaneDetectState planeDetectState;
    private GameReadyState gameReadyState;
    private CountDownState countDownState;
    private GameResultState gameResultState;
    private GameRankingState gameRankingState;

    private PlaneDetectController planeDetectController;

    private Arc arcView;

    private TextView myResultText;

    private Button retryButton;
    private Button quitButton;
    private View.OnClickListener readyButtonClickListener;
    private View.OnClickListener quitButtonClickListener;

    private MusicPlayerHelper graffitiClickSE = new MusicPlayerHelper();
    private boolean isLoop = false;

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_graffiti_time_attack);

        surfaceView = findViewById(R.id.surfaceview);
        displayRotationHelper = new DisplayRotationHelper(/*context=*/ this);

        // Set up tap listener.
        tapHelper = new TapHelper(this, GraffitiTimeAttackActivity.this);
        surfaceView.setOnTouchListener(tapHelper);

        // Set up renderer.
        surfaceView.setPreserveEGLContextOnPause(true);
        surfaceView.setEGLContextClientVersion(3);
        surfaceView.setEGLConfigChooser(8, 8, 8, 8, 16, 0); // Alpha used for plane blending.
        surfaceView.setRenderer(this);
        surfaceView.setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);
        surfaceView.setWillNotDraw(false);

        installRequested = false;

        depthSettings.onCreate(this);
        instantPlacementSettings.onCreate(this);

        // Set up the HandMotion View & planeDetectState.
        LayoutInflater inflater = LayoutInflater.from(this);
        FrameLayout handmotion = findViewById(R.id.plane_discovery_view);
        FrameLayout instructionsView = (FrameLayout) inflater.inflate(R.layout.view_plane_discovery, handmotion, true);
        planeDetectController = new PlaneDetectController(instructionsView);
        planeDetectState = new PlaneDetectState();
        changeState(planeDetectState);

        // Set up the Arc View.
        FrameLayout arc = findViewById(R.id.arc_view);
        inflater.inflate(R.layout.view_arc, arc, true);
        arcView = findViewById(R.id.arc);

        // Set up the ReadyGo View.
        TextView readyText = findViewById(R.id.ready_text);
        TextView goText = findViewById(R.id.go_text);
        gameReadyState = new GameReadyState(this, readyText, goText, arcView);

        // Set up the timerView.
        TextView timerText = findViewById(R.id.timer_text);
        ImageView timerBgImage = findViewById(R.id.timer_bg_image);
        countDownState = new CountDownState(timerText, timerBgImage);

        // Set up the resultView.
        myResultText = findViewById(R.id.my_result_text);
        gameResultState = new GameResultState(this, myResultText);

        ConstraintLayout constraintLayout = findViewById(R.id.draw_container);
        gameRankingState = new GameRankingState(this, constraintLayout);
        gameRankingState.setGameRankingBg(getResources().getDrawable(R.drawable.bg_sketchbook, null));
        List<Drawable> rankBgs = new ArrayList<>();
        rankBgs.add(getResources().getDrawable(R.drawable.crown_gold, null));
        rankBgs.add(getResources().getDrawable(R.drawable.crown_silver, null));
        rankBgs.add(getResources().getDrawable(R.drawable.crown_bronze, null));
        gameRankingState.setRankBgs(getResources(), rankBgs, false);
        Typeface textFont = getResources().getFont(R.font.mplus_rounded1c_bold);
        gameRankingState.setTextFont(Typeface.create(textFont, Typeface.BOLD));

        // Set up the retry & quit button
        retryButton = findViewById(R.id.retry_button);
        retryButton.setVisibility(View.INVISIBLE);
        readyButtonClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                retryButton.setClickable(false);
                retryButton.setVisibility(View.INVISIBLE);
                quitButton.setClickable(false);
                quitButton.setVisibility(View.INVISIBLE);
                myResultText.clearAnimation();
                myResultText.setVisibility(View.INVISIBLE);
                graffitiRenderer.clearTexture();
//                graffitiOcclusionRenderer.clearTexture();
                changeState(gameReadyState);
            }
        };
        retryButton.setOnClickListener(readyButtonClickListener);
        retryButton.setClickable(false);
        quitButton = findViewById(R.id.quit_button);
        quitButton.setVisibility(View.INVISIBLE);
        quitButtonClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        };
        quitButton.setOnClickListener(quitButtonClickListener);
        quitButton.setClickable(false);
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        restartCurState();
    }

    @Override
    protected void onResume() {
        super.onResume();

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
                session = new Session(/* context= */ this);
            } catch (UnavailableArcoreNotInstalledException
                    | UnavailableUserDeclinedInstallationException e) {
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
//            boolean isDepthSupported = session.isDepthModeSupported(Config.DepthMode.AUTOMATIC);
//            if (isDepthSupported) {
//                depthSettings.setUseDepthForOcclusion(true);
//                depthSettings.setDepthColorVisualizationEnabled(true);
//            }
//            depthSettings.setUseDepthForOcclusion(false);
//            depthSettings.setDepthColorVisualizationEnabled(false);
        }

        // Note that order matters - see the note in onPause(), the reverse applies here.
        try {
            configureSession();
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

        TimeoutHelper.startTimer(GraffitiTimeAttackActivity.this);
    }

    /**
     * Configures the session with feature settings.
     */
    private void configureSession() {
        Config config = session.getConfig();
//        config.setLightEstimationMode(Config.LightEstimationMode.ENVIRONMENTAL_HDR);
        config.setLightEstimationMode(Config.LightEstimationMode.DISABLED);
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
        if (session != null) {
            // Note that the order matters - GLSurfaceView is paused first so that it does not try
            // to query the session. If Session is paused before GLSurfaceView, GLSurfaceView may
            // still call session.update() and get a SessionPausedException.
            displayRotationHelper.onPause();
            surfaceView.onPause();
            session.pause();
        }
        pauseCurState();
        TimeoutHelper.resetTimer();
    }

    @Override
    protected void onDestroy() {
        if (session != null) {
            // Explicitly close ARCore Session to release native resources.
            // Review the API reference for important considerations before calling close() in apps with
            // more complicated lifecycle requirements:
            // https://developers.google.com/ar/reference/java/arcore/reference/com/google/ar/core/Session#close()
            session.close();
            session = null;
        }

        super.onDestroy();
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
            backgroundRenderer.createOnGlThread(this);
//            backgroundOcclusionRenderer.createOnGlThread(this);
            // Update backgroundOcclusionRenderer state to match the depth settings.
//            backgroundOcclusionRenderer.setUseOcclusion(this, depthSettings.useDepthForOcclusion());

            graffitiRenderer.createOnGlThread(this, "models/plane.png");
//            graffitiOcclusionRenderer.createOnGlThread(this, "models/plane.png");
            // Update backgroundOcclusionRenderer state to match the depth settings.
//            graffitiOcclusionRenderer.setUseOcclusion(this, depthSettings.useDepthForOcclusion());
//            graffitiOcclusionRenderer.setUseOcclusion(this, false);

            virtualSceneFramebuffer = new Framebuffer(/*width=*/ 1, /*height=*/ 1);
        } catch (IOException e) {
            Log.e(TAG, "Failed to read a required asset file", e);
        }
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        displayRotationHelper.onSurfaceChanged(width, height);
        virtualSceneFramebuffer.resize(width, height);
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        IGLDrawListener l = glDrawListenerQueue.poll();
        if (l != null) l.onDraw(gl);

        // Clear screen to notify driver it should not load any pixels from previous frame.
////        clear(/*framebuffer=*/ null, 0f, 0f, 0f, 1f);
        virtualSceneFramebuffer.clear();
        rendererHelper.clear(0f, 0f, 0f, 1f);

        if (session == null) {
            return;
        }

        // Texture names should only be set once on a GL thread unless they change. This is done during
        // onDrawFrame rather than onSurfaceCreated since the session is not guaranteed to have been
        // initialized during the execution of onSurfaceCreated.
        if (!hasSetTextureNames) {
            session.setCameraTextureName(backgroundRenderer.getTextureId());
//      session.setCameraTextureNames(
//              new int[] {backgroundOcclusionRenderer.getCameraColorTexture().getTextureId()});
//            session.setCameraTextureName(backgroundOcclusionRenderer.getCameraColorTexture().getTextureId());
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

        // Update backgroundOcclusionRenderer state to match the depth settings. Call onSurfaceCreated().
//      try {
//          backgroundOcclusionRenderer.setUseOcclusion(depthSettings.useDepthForOcclusion());
//      } catch (IOException e) {
//          Log.e(TAG, "Failed to read a required asset file", e);
//          return;
//      }

        // backgroundOcclusionRenderer.updateDisplayGeometry must be called every frame to update the coordinates
        // used to draw the background camera image.
//        backgroundOcclusionRenderer.updateDisplayGeometry(frame);
//        graffitiOcclusionRenderer.updateDisplayGeometry(frame);

        if (camera.getTrackingState() == TrackingState.TRACKING
//                && (depthSettings.useDepthForOcclusion()
                /*|| depthSettings.depthColorVisualizationEnabled())*/) {
            // Retrieve the depth map for the current frame, if available.
//            try {
//                Image depthImage = frame.acquireDepthImage();
//                backgroundOcclusionRenderer.updateCameraDepthTexture(depthImage);
//                graffitiOcclusionRenderer.updateCameraDepthTexture(depthImage);
//                graffitiOcclusionRenderer.setDepthTexture(backgroundOcclusionRenderer.getCameraDepthTexture().getTextureId(), depthImage.getWidth(), depthImage.getHeight());
//            } catch (NotYetAvailableException e) {
                // This means that depth data is not available yet.
                // Depth data will not be available if there are no tracked
                // feature points. This can happen when there is no motion, or when the
                // camera loses its ability to track objects in the surrounding
                // environment.
//                Log.e(TAG, "NotYetAvailableException", e);
//            }
        }

        // Handle user input.
        handleTap(frame, camera);

        // Keep the screen unlocked while tracking, but allow it to lock when tracking stops.
        trackingStateHelper.updateKeepScreenOnFlag(camera.getTrackingState());

        // Show a message based on whether tracking has failed, if planes are detected, and if the user
        // has placed any objects.
        String message = null;
        if (camera.getTrackingState() == TrackingState.PAUSED) {
            if (camera.getTrackingFailureReason() == TrackingFailureReason.NONE) {
                message = getString(R.string.searching_plane);
            } else {
                message = TrackingStateHelper.getTrackingFailureReasonString(camera);
            }
        } else if (hasTrackingPlane()) {
            // TODO Visualize temporarily plane
//            if (anchors.isEmpty()) {
//                message = getString(R.string.waiting_for_tap);;
//            }
        } else {
            message = getString(R.string.searching_plane);
        }
        if (messageSnackbarHelper.isShowing() && message == null) {
            messageSnackbarHelper.hide(this);
            planeDetectController.hide();
            startTimeAttack();
        } else if (!messageSnackbarHelper.isShowing() && message != null) {
            if (getCurState() instanceof PlaneDetectState) {
                messageSnackbarHelper.showMessage(this, message);
                planeDetectController.show();
            }
        }

        // -- Draw background

//        if (frame.getTimestamp() != 0) {
        // Suppress rendering if the camera did not produce the first frame yet. This is to avoid
        // drawing possible leftover data from previous sessions if the texture is reused.
        virtualSceneFramebuffer.clear();
        backgroundRenderer.draw(frame);
//        backgroundOcclusionRenderer.draw(frame);
//        }

        // If not tracking, don't draw 3D objects.
        if (camera.getTrackingState() == TrackingState.PAUSED) {
            return;
        }

        // -- Draw non-occluded virtual objects (planes, point cloud)

        // Get projection matrix.
        float[] projmtx = new float[16];
        camera.getProjectionMatrix(projmtx, 0, Z_NEAR, Z_FAR);

        // Get camera matrix and draw.
        float[] viewmtx = new float[16];
        camera.getViewMatrix(viewmtx, 0);

        // Visualize tracked points.
        // Use try-with-resources to automatically release the point cloud.
//      try (PointCloud pointCloud = frame.acquirePointCloud()) {
//        if (pointCloud.getTimestamp() > lastPointCloudTimestamp) {
//          pointCloudVertexBuffer.set(pointCloud.getPoints());
//          lastPointCloudTimestamp = pointCloud.getTimestamp();
//        }
//        android.opengl.Matrix.multiplyMM(modelViewProjectionMatrix, 0, projectionMatrix, 0, viewMatrix, 0);
//        pointCloudShader.setMat4("u_ModelViewProjection", modelViewProjectionMatrix);
//        render.draw(pointCloudMesh, pointCloudShader);
//      }

        PointCloud pointCloud = frame.acquirePointCloud();
//        pointCloudRenderer.update(pointCloud);
//      pointCloudRenderer.draw(viewmtx, projmtx);
        // Application is responsible for releasing the point cloud resources after
        // using it.
        pointCloud.release();

        // Visualize planes.
//        virtualSceneFramebuffer.clear();
//        planeRendererOcculusion.drawPlanes(
//              session.getAllTrackables(Plane.class),
//              camera.getDisplayOrientedPose(),
//              projectionMatrix);

//      planeRenderer.drawPlanes(session.getAllTrackables(PlaneJSON.class), camera.getDisplayOrientedPose(), projmtx);
//      planeObjectRenderer.draw(session.getAllTrackables(PlaneJSON.class)/*session.update().getUpdatedTrackables(PlaneJSON.class)*/, camera.getDisplayOrientedPose(), projmtx);
//      testRenderer.draw(session.getAllTrackables(PlaneJSON.class)/*session.update().getUpdatedTrackables(PlaneJSON.class)*/, camera.getDisplayOrientedPose(), projmtx);

        // -- Draw occluded virtual objects
        virtualSceneFramebuffer.clear();
        graffitiRenderer.adjustTextureAxis(frame, camera);
//        graffitiOcclusionRenderer.adjustTextureAxis(frame, camera);

        // Update lighting parameters in the shader
//        graffitiOcclusionRenderer.updateLightEstimation(frame.getLightEstimate(), viewmtx);

        graffitiRenderer.draw(session.getAllTrackables(Plane.class)/*session.update().getUpdatedTrackables(PlaneJSON.class)*/, camera.getDisplayOrientedPose(), projmtx);
//        graffitiOcclusionRenderer.draw(session.getAllTrackables(Plane.class)/*session.update().getUpdatedTrackables(PlaneJSON.class)*/, camera.getDisplayOrientedPose(), projmtx);

        // Compute lighting from average intensity of the image.
        // The first three components are color scaling factors.
        // The last one is the average pixel intensity in gamma space.
//        final float[] colorCorrectionRgba = new float[4];
//        frame.getLightEstimate().getColorCorrection(colorCorrectionRgba, 0);

        // Visualize anchors created by touch.
////      render.clear(virtualSceneFramebuffer, 0f, 0f, 0f, 0f);
//        virtualSceneFramebuffer.use();
//        rendererHelper.clear(0f, 0f, 0f, 0f);
//
//        for (Anchor anchor : anchors) {
//            if (anchor.getTrackingState() != TrackingState.TRACKING) {
//                continue;
//            }
//
//            // Get the current pose of an Anchor in world space. The Anchor pose is updated
//            // during calls to session.update() as ARCore refines its estimate of the world.
//            anchor.getPose().toMatrix(modelMatrix, 0);
//
//            // Calculate model/view/projection matrices
//            android.opengl.Matrix.multiplyMM(modelViewMatrix, 0, viewMatrix, 0, modelMatrix, 0);
//            android.opengl.Matrix.multiplyMM(modelViewProjectionMatrix, 0, projectionMatrix, 0, modelViewMatrix, 0);
//
//            // Update shader properties and draw
//            virtualObjectShader.setMat4("u_ModelView", modelViewMatrix);
//            virtualObjectShader.setMat4("u_ModelViewProjection", modelViewProjectionMatrix);
//            virtualSceneFramebuffer.use();
//            virtualObjectShader.lowLevelUse();
//            virtualObjectMesh.lowLevelDraw();
//        }

//        virtualObject.draw(viewmtx, projmtx, colorCorrectionRgba, coloredAnchor.color);
//        virtualObjectShadow.draw(viewmtx, projmtx, colorCorrectionRgba, coloredAnchor.color);

        // Compose the virtual scene with the background. (not use)
//        virtualSceneFramebuffer.clear();
//        backgroundRenderer.drawVirtualScene(virtualSceneFramebuffer, Z_NEAR, Z_FAR);
//        } catch (Throwable t) {
////             Avoid crashing the application due to unhandled exceptions.
//            Log.e(TAG, "Exception on the OpenGL thread", t);
//        }
    }

    // Handle only one tap per frame, as taps are usually low frequency compared to frame rate.
    private void handleTap(Frame frame, Camera camera) {
        MotionEvent tap = tapHelper.poll();
        if (tap != null && camera.getTrackingState() == TrackingState.TRACKING) {
//            List<HitResult> hitResultList;
//            if (instantPlacementSettings.isInstantPlacementEnabled()) {
//                hitResultList =
//                        frame.hitTestInstantPlacement(tap.getX(), tap.getY(), APPROXIMATE_DISTANCE_METERS);
//            } else {
//                hitResultList = frame.hitTest(tap);
//            }

            if (arcView.getAngle() > 0.0f) {
                for (HitResult hit : frame.hitTest(tap)) {
                    // If any plane, Oriented Point, or Instant Placement Point was hit, create an anchor.
                    Trackable trackable = hit.getTrackable();
                    // If a plane was hit, check that it was hit inside the plane polygon.
                    if (trackable instanceof Plane
                            && ((Plane) trackable).isPoseInPolygon(hit.getHitPose())
                            && (PlaneRenderer.calculateDistanceToPlane(hit.getHitPose(), camera.getPose()) > 0)
                            && ((Plane) trackable).getSubsumedBy() == null) {
//                        || (trackable instanceof Point
//                        && ((Point) trackable).getOrientationMode()
//                        == Point.OrientationMode.ESTIMATED_SURFACE_NORMAL)
//                        || (trackable instanceof InstantPlacementPoint)) {

                        Pose planePose = ((Plane) trackable).getCenterPose();
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

                        if (color == Color.TRANSPARENT) {
                            graffitiRenderer.drawTexture(hitOnPlaneCoordX, -hitOnPlaneCoordZ, 9, trackable, drawer);
//                            graffitiOcclusionRenderer.drawTexture(hitOnPlaneCoordX, -hitOnPlaneCoordZ, 9, trackable, drawer);
                        } else {
                            graffitiRenderer.drawTexture(hitOnPlaneCoordX, -hitOnPlaneCoordZ, 4, trackable, drawer);
//                            graffitiOcclusionRenderer.drawTexture(hitOnPlaneCoordX, -hitOnPlaneCoordZ, 4, trackable, drawer);
                        }
                        int diffColoredPxs = graffitiRenderer.getDiffColoredPixels();
//                        int diffColoredPxs = graffitiOcclusionRenderer.getDiffColoredPixels();
                        Log.d(TAG, "angle " + arcView.getAngle() + ", diffColoredPxs " + diffColoredPxs);
                        if (diffColoredPxs > 0) {
                            arcView.addAngleQueue(diffColoredPxs / 10);
                        }
                    }
                }
            }
        }
    }

    /**
     * Checks if we detected at least one plane.
     */
    private boolean hasTrackingPlane() {
        for (Plane plane : session.getAllTrackables(Plane.class)) {
            if (plane.getTrackingState() == TrackingState.TRACKING) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void onExitState(State state) {
        if (state instanceof PlaneDetectState) {
            surfaceView.setEnabled(false);
            arcView.setArcColor(Color.BLUE);
            changeState(gameReadyState);
        } else if (state instanceof GameReadyState) {
            changeState(countDownState);
            surfaceView.setEnabled(true);
        } else if (state instanceof CountDownState) {
            surfaceView.setEnabled(false);
            long score = graffitiRenderer.getTotalColoredPixels(Color.BLUE);
//            long score = graffitiOcclusionRenderer.getTotalColoredPixels(Color.BLUE);
            Log.d(TAG, "Point: " + score + "p");
            gameResultState.setScore(score);
            gameRankingState.setScore(score);
            changeState(gameResultState);
        } else if (state instanceof GameResultState) {
            changeState(gameRankingState);
        } else if (state instanceof GameRankingState) {
            retryButton.setVisibility(View.VISIBLE);
            retryButton.setClickable(true);
            quitButton.setVisibility(View.VISIBLE);
            quitButton.setClickable(true);
        }
    }

    private interface IGLDrawListener {
        public void onDraw(GL10 gl);
    }

    private void startTimeAttack() {
        this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                onExitState(getCurState());
            }
        });
    }

    private void finishTimeAttack() {
    }

}
