package org.ntlab.graffiti.graffiti;

import android.annotation.SuppressLint;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.PixelFormat;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.Image;
import android.media.ImageReader;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.opengl.GLES30;
import android.opengl.GLSurfaceView;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.Toast;

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
import com.google.ar.core.exceptions.NotYetAvailableException;
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
import org.ntlab.graffiti.common.rendering.BackgroundOcclusionRenderer;
import org.ntlab.graffiti.common.rendering.Framebuffer;
import org.ntlab.graffiti.common.rendering.GraffitiOcclusionRenderer;
import org.ntlab.graffiti.common.rendering.PlaneRenderer;
import org.ntlab.graffiti.common.views.BrushSizeSelector;
import org.ntlab.graffiti.common.views.ColorSelector;
import org.ntlab.graffiti.common.views.PlaneDetectController;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.text.SimpleDateFormat;
import java.util.ArrayDeque;
import java.util.Date;
import java.util.Queue;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/**
 * This is a simple example that shows how to create an augmented reality (AR) application using the
 * ARCore API. The application will display any detected planes and will allow the user to tap on a
 * plane to place a 3d model of the Android robot.
 *
 * @author a-hongo, n-nitta
 */
public class GraffitiActivity extends ArActivity {
    private static final String TAG = GraffitiActivity.class.getSimpleName();

    private static final float Z_NEAR = 0.1f;
    private static final float Z_FAR = 100f;

    private boolean installRequested;

    private Session session;
    private final SnackbarHelper messageSnackbarHelper = new SnackbarHelper();
    private DisplayRotationHelper displayRotationHelper;
    private final TrackingStateHelper trackingStateHelper = new TrackingStateHelper(this);
    private TapHelper tapHelper;
    private final RendererHelper rendererHelper = new RendererHelper();

    private final BackgroundOcclusionRenderer backgroundOcclusionRenderer = new BackgroundOcclusionRenderer();
    private final GraffitiOcclusionRenderer graffitiOcclusionRenderer = new GraffitiOcclusionRenderer();
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

    private PlaneDetectController planeDetectController;

    private ColorSelector colorSelector;
    private BrushSizeSelector brushSizeSelector;

    private static final int REQUEST_MEDIA_PROJECTION = 1001;
    private MediaProjectionManager mpManager;
    private MediaProjection mProjection;

    private int displayWidth, displayHeight;
    private ImageReader imageReader;
    private VirtualDisplay virtualDisplay;
    private int screenDensity;
    private ImageView screenshotImageView;
    private ImageView cameraButton;

    private static final long START_TIME = 10000; //10s
    private CountDownTimer showScreenshotTimer;
    private long timeLeftInMillis = START_TIME;

    private MusicPlayerHelper graffitiClickSE = new MusicPlayerHelper();
    private boolean isLoop = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_graffiti);

        surfaceView = findViewById(R.id.surfaceview);
        displayRotationHelper = new DisplayRotationHelper(/*context=*/ this);

        // Set up tap listener.
        tapHelper = new TapHelper(this, GraffitiActivity.this);
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

        // Set up the HandMotion View.
        LayoutInflater inflater = LayoutInflater.from(this);
        FrameLayout handmotion = findViewById(R.id.plane_discovery_view);
        FrameLayout instructionsView = (FrameLayout) inflater.inflate(R.layout.view_plane_discovery, handmotion, true);
        planeDetectController = new PlaneDetectController(instructionsView);

        // Set up the ColorSelector View.
        colorSelector = findViewById(R.id.color_selector);

        // Set up the brushSizeSelector View.
        brushSizeSelector = findViewById(R.id.brush_size_selector);

        // Set up the Screen Shot View.
        // 撮影したスクリーンを表示するImageView
        screenshotImageView = findViewById(R.id.screenshot_view);
        screenshotImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(GraffitiActivity.this, PhotoGalleryActivity.class));
                screenshotImageView.setClickable(false);
                cameraButton.setClickable(false);
                screenshotImageView.setImageBitmap(null);
            }
        });
        screenshotImageView.setClickable(false);

        cameraButton = findViewById(R.id.camera_image);
        // ボタンタップでスクリーンショットを撮る
        cameraButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try {
                    isLoop = false;
                    graffitiClickSE.playMusic(GraffitiActivity.this, "musics/se/camera-shutter.mp3", isLoop);
                    cameraButton.setClickable(false);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                getScreenshot();
            }
        });

        // 画面の縦横サイズとdpを取得
        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        screenDensity = displayMetrics.densityDpi;
        displayWidth = displayMetrics.widthPixels;
        displayHeight = displayMetrics.heightPixels;
        mpManager = (MediaProjectionManager) getSystemService(MEDIA_PROJECTION_SERVICE);

        // permissionを確認するintentを投げ、ユーザーの許可・不許可を受け取る
        if (mpManager != null) {
            startActivityForResult(mpManager.createScreenCaptureIntent(), REQUEST_MEDIA_PROJECTION);
        }

    }

    @Override
    protected void onRestart() {
        super.onRestart();
        resetTimer();
        cameraButton.setClickable(true);
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
            boolean isDepthSupported = session.isDepthModeSupported(Config.DepthMode.AUTOMATIC);
            if (isDepthSupported) {
                depthSettings.setUseDepthForOcclusion(true);
                depthSettings.setDepthColorVisualizationEnabled(true);
            }
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

        TimeoutHelper.startTimer(GraffitiActivity.this);
    }

    /**
     * Configures the session with feature settings.
     */
    private void configureSession() {
        Config config = session.getConfig();
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

    /*
     * ユーザーのスクリーンショットの許可を受け取る
     */
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (REQUEST_MEDIA_PROJECTION == requestCode) {
            if (resultCode != RESULT_OK) {
                // 拒否された
                Toast.makeText(this,
                        "User cancelled", Toast.LENGTH_LONG).show();
                return;
            }
            // 許可された結果を受け取る
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                setUpMediaProjection(resultCode, data);
            }
        }
    }

    private void setUpMediaProjection(int code, Intent intent) {
        if (intent != null) {
            mProjection = mpManager.getMediaProjection(code, intent);
            setUpVirtualDisplay();
        }
    }

    private void setUpVirtualDisplay() {
        imageReader = ImageReader.newInstance(
                displayWidth, displayHeight, PixelFormat.RGBA_8888, 2);

        virtualDisplay = mProjection.createVirtualDisplay("ScreenCapture",
                displayWidth, displayHeight, screenDensity,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                imageReader.getSurface(), null, null);
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
        resetTimer();
        TimeoutHelper.resetTimer();
    }

    @Override
    protected void onDestroy() {
        if (virtualDisplay != null) {
            Log.d("debug", "release VirtualDisplay");
            virtualDisplay.release();
        }
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
            backgroundOcclusionRenderer.createOnGlThread(this);
            // Update BackgroundRenderer state to match the depth settings.
//            backgroundOcclusionRenderer.setUseOcclusion(this, depthSettings.useDepthForOcclusion());
            backgroundOcclusionRenderer.setUseOcclusion(this, false);

            graffitiOcclusionRenderer.createOnGlThread(this, "models/plane.png");
            // Update BackgroundRenderer state to match the depth settings.
//            graffitiOcclusionRenderer.setUseOcclusion(this, depthSettings.useDepthForOcclusion());
            backgroundOcclusionRenderer.setUseOcclusion(this, false);

            virtualSceneFramebuffer = new Framebuffer(/*width=*/ 1, /*height=*/ 1);
        } catch (IOException e) {
            String message = getString(R.string.snackbar_asset_error);
            Log.e(TAG, message, e);
            messageSnackbarHelper.showError(this, message + ": " + e);
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
        virtualSceneFramebuffer.clear();
        rendererHelper.clear(0f, 0f, 0f, 1f);

        if (session == null) {
            return;
        }

        // Texture names should only be set once on a GL thread unless they change. This is done during
        // onDrawFrame rather than onSurfaceCreated since the session is not guaranteed to have been
        // initialized during the execution of onSurfaceCreated.
        if (!hasSetTextureNames) {
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

        // BackgroundRenderer.updateDisplayGeometry must be called every frame to update the coordinates
        // used to draw the background camera image.
        backgroundOcclusionRenderer.updateDisplayGeometry(frame);
        graffitiOcclusionRenderer.updateDisplayGeometry(frame);

        if (camera.getTrackingState() == TrackingState.TRACKING
                && (depthSettings.useDepthForOcclusion())) {
            // Retrieve the depth map for the current frame, if available.
            try {
                Image depthImage = frame.acquireDepthImage();
                backgroundOcclusionRenderer.updateCameraDepthTexture(depthImage);
                graffitiOcclusionRenderer.setDepthTexture(backgroundOcclusionRenderer.getCameraDepthTexture().getTextureId(), depthImage.getWidth(), depthImage.getHeight());
            } catch (NotYetAvailableException e) {
                // This means that depth data is not available yet.
                // Depth data will not be available if there are no tracked
                // feature points. This can happen when there is no motion, or when the
                // camera loses its ability to track objects in the surrounding
                // environment.
            }
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
            // TODO waiting for tap.
        } else {
            message = getString(R.string.searching_plane);
        }
        if (message == null) {
            messageSnackbarHelper.hide(this);
            planeDetectController.hide();
        } else {
            messageSnackbarHelper.showMessage(this, message);
            planeDetectController.show();
        }

        // -- Draw background

        // Suppress rendering if the camera did not produce the first frame yet. This is to avoid
        // drawing possible leftover data from previous sessions if the texture is reused.
        virtualSceneFramebuffer.clear();
        backgroundOcclusionRenderer.draw(frame);

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
        PointCloud pointCloud = frame.acquirePointCloud();
        // Application is responsible for releasing the point cloud resources after
        // using it.
        pointCloud.release();

        // -- Draw occluded virtual objects
        virtualSceneFramebuffer.clear();
        graffitiOcclusionRenderer.adjustTextureAxis(frame, camera);

        // Update lighting parameters in the shader
        graffitiOcclusionRenderer.updateLightEstimation(frame.getLightEstimate(), viewmtx);

        graffitiOcclusionRenderer.draw(session.getAllTrackables(Plane.class)/*session.update().getUpdatedTrackables(PlaneJSON.class)*/, camera.getDisplayOrientedPose(), projmtx);
    }

    // Handle only one tap per frame, as taps are usually low frequency compared to frame rate.
    private void handleTap(Frame frame, Camera camera) {
        MotionEvent tap = tapHelper.poll();
        if (tap != null && camera.getTrackingState() == TrackingState.TRACKING) {

            for (HitResult hit : frame.hitTest(tap)) {
                // If any plane, Oriented Point, or Instant Placement Point was hit, create an anchor.
                Trackable trackable = hit.getTrackable();
                // If a plane was hit, check that it was hit inside the plane polygon.
                if (trackable instanceof Plane
                        && ((Plane) trackable).isPoseInPolygon(hit.getHitPose())
                        && (PlaneRenderer.calculateDistanceToPlane(hit.getHitPose(), camera.getPose()) > 0)
                        && ((Plane) trackable).getSubsumedBy() == null) {

                    Pose planePose = ((Plane) trackable).getCenterPose();
                    float hitMinusCenterX = hit.getHitPose().tx() - planePose.tx();
                    float hitMinusCenterY = hit.getHitPose().ty() - planePose.ty();
                    float hitMinusCenterZ = hit.getHitPose().tz() - planePose.tz();
                    float hitOnPlaneCoordX = planePose.getXAxis()[0] * hitMinusCenterX + planePose.getXAxis()[1] * hitMinusCenterY + planePose.getXAxis()[2] * hitMinusCenterZ;
                    float hitOnPlaneCoordZ = planePose.getZAxis()[0] * hitMinusCenterX + planePose.getZAxis()[1] * hitMinusCenterY + planePose.getZAxis()[2] * hitMinusCenterZ;
                    int drawerStyle = 1;
                    int color = colorSelector.getSelectedLineColor();
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
                        graffitiOcclusionRenderer.drawTexture(hitOnPlaneCoordX, -hitOnPlaneCoordZ, 9, trackable, drawer);
                    } else {
                        graffitiOcclusionRenderer.drawTexture(hitOnPlaneCoordX, -hitOnPlaneCoordZ, brushSizeSelector.getSelectedLineWidth(), trackable, drawer);
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

    private void getScreenshot() {
        glDrawListenerQueue.offer(new IGLDrawListener() {
            @Override
            public void onDraw(GL10 gl) {
                final String filename = generateFilename();
                // バッファからBitmapを生成
                int[] viewportDim = new int[4];
                GLES30.glGetIntegerv(GLES30.GL_VIEWPORT, IntBuffer.wrap(viewportDim));
                int width = viewportDim[2];
                int height = viewportDim[3];
                ByteBuffer buffer = ByteBuffer.allocate(width * height * 4);
                buffer.position(0);
                GLES30.glReadPixels(0, 0, width, height, GLES30.GL_RGBA, GLES30.GL_UNSIGNED_BYTE, buffer);
                Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
                bitmap.copyPixelsFromBuffer(buffer);
                Matrix mat = new Matrix();
                mat.preScale(1.0f, -1.0f);
                Bitmap bitmap2 = Bitmap.createBitmap(bitmap, 0, 0, width, height, mat, false);
                try {
                    saveBitmapToDisk(bitmap2, filename);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                Handler handler = new Handler(Looper.getMainLooper());
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        setScreenshotImageView(bitmap2);
                    }
                });
            }
        });
    }

    private String generateFilename() {
        String date =
                new SimpleDateFormat("yyyyMMddHHmmss", java.util.Locale.getDefault()).format(new Date());
        return Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES) + File.separator + "AR/" + date + "_screenshot.jpg";
    }

    @SuppressLint("WrongThread")
    private void saveBitmapToDisk(Bitmap bitmap, String filename) throws IOException {

        File out = new File(filename);
        if (!out.getParentFile().exists()) {
            out.getParentFile().mkdirs();
        }
        try (FileOutputStream outputStream = new FileOutputStream(filename);
             ByteArrayOutputStream outputData = new ByteArrayOutputStream()) {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputData);
            outputData.writeTo(outputStream);
            outputStream.flush();
            outputStream.close();
            registerDatabase(filename);
        } catch (IOException ex) {
            throw new IOException("Failed to save bitmap to disk", ex);
        }
    }

    // アンドロイドのデータベースへ登録する
    private void registerDatabase(String file) {
        ContentValues contentValues = new ContentValues();
        ContentResolver contentResolver = GraffitiActivity.this.getContentResolver();
        contentValues.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
        contentValues.put("_data", file);
        contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                contentValues);
    }

    public void setScreenshotImageView(Bitmap bitmap) {
        screenshotImageView.setImageBitmap(bitmap);
        screenshotImageView.setClickable(true);
        resetTimer();
        startTimer();
    }

    private void startTimer() {
        showScreenshotTimer = new CountDownTimer(timeLeftInMillis, START_TIME) {
            @Override
            public void onTick(long millisUntilFinished) {
                timeLeftInMillis = millisUntilFinished;
            }

            @Override
            public void onFinish() {
                screenshotImageView.setClickable(false);
                cameraButton.setClickable(true);
                screenshotImageView.setImageBitmap(null);
            }
        }.start();
    }

    private void resetTimer() {
        if (showScreenshotTimer != null) {
            showScreenshotTimer.cancel();
            timeLeftInMillis = START_TIME;
        }
    }

    private interface IGLDrawListener {
        public void onDraw(GL10 gl);
    }
}
