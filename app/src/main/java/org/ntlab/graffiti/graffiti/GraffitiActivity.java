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

import androidx.appcompat.app.AppCompatActivity;

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
import org.ntlab.graffiti.common.rendering.BackgroundRenderer;
import org.ntlab.graffiti.common.rendering.Framebuffer;
import org.ntlab.graffiti.common.rendering.GraffitiRenderer;
import org.ntlab.graffiti.common.rendering.PlaneRenderer;
import org.ntlab.graffiti.common.views.BrushSizeSelector;
import org.ntlab.graffiti.common.views.ColorSelector;
import org.ntlab.graffiti.common.views.PlaneDiscoveryController;

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
public class GraffitiActivity extends AppCompatActivity implements GLSurfaceView.Renderer {
    private static final String TAG = GraffitiActivity.class.getSimpleName();

    private static final String SEARCHING_PLANE_MESSAGE = "端末を持ち上げて、カメラに映った壁や床にタッチしてらくがきして下さい。";
//    private static final String WAITING_FOR_TAP_MESSAGE = "タッチしてらくがきして下さい。";

    private static final float Z_NEAR = 0.1f;
    private static final float Z_FAR = 100f;

    // Rendering. The Renderers are created here, and initialized when the GL surface is created.
    private GLSurfaceView surfaceView;

    private boolean installRequested;

    private Session session;
    private final SnackbarHelper messageSnackbarHelper = new SnackbarHelper();
    private DisplayRotationHelper displayRotationHelper;
    private final TrackingStateHelper trackingStateHelper = new TrackingStateHelper(this);
    private TapHelper tapHelper;
    private final RendererHelper rendererHelper = new RendererHelper();

    private final BackgroundRenderer backgroundRenderer = new BackgroundRenderer();
//    private final PlaneRenderer planeRenderer = new PlaneRenderer();
//    private final PointCloudRenderer pointCloudRenderer = new PointCloudRenderer();
//    private final ObjectRenderer virtualObject = new ObjectRenderer();
//    private final ObjectRenderer virtualObjectShadow = new ObjectRenderer();
//    private PlaneRendererOcculusion planeRendererOcculusion;
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

    private PlaneDiscoveryController planeDiscoveryController;

    private ColorSelector colorSelector;
    private BrushSizeSelector brushSizeSelector;

    private static final int REQUEST_MEDIA_PROJECTION = 1001;
    private MediaProjectionManager mpManager;
    private MediaProjection mProjection;

    private int displayWidth, displayHeight;
    private ImageReader imageReader;
    private VirtualDisplay virtualDisplay;
    private int screenDensity;
    private ImageView imageView;
    private ImageView cameraButton;

    private static final long START_TIME = 10000;
    private CountDownTimer countDownTimer;
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
        planeDiscoveryController = new PlaneDiscoveryController(instructionsView);

        // Set up the ColorSelector View.
        colorSelector = findViewById(R.id.color_selector_view);

        // Set up the brushSizeSelector View.
        brushSizeSelector = findViewById(R.id.brush_size_selector);

        // Set up the Screen Shot View.
        // 撮影したスクリーンを表示するImageView
        imageView = findViewById(R.id.image_view);
        imageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(GraffitiActivity.this, PhotoGalleryActivity.class));
                imageView.setClickable(false);
                cameraButton.setClickable(false);
                imageView.setImageBitmap(null);
            }
        });
        imageView.setClickable(false);

        cameraButton = findViewById(R.id.camera_image);
        // ボタンタップでスクリーンショットを撮る
        cameraButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try {
                    isLoop = false;
                    graffitiClickSE.musicPlay(GraffitiActivity.this, "musics/se/camera-shutter.mp3", isLoop);
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
            String message = null;
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
                message = "Please install ARCore";
                exception = e;
            } catch (UnavailableApkTooOldException e) {
                message = "Please update ARCore";
                exception = e;
            } catch (UnavailableSdkTooOldException e) {
                message = "Please update this app";
                exception = e;
            } catch (UnavailableDeviceNotCompatibleException e) {
                message = "This device does not support AR";
                exception = e;
            } catch (Exception e) {
                message = "Failed to create AR session";
                exception = e;
            }

            if (message != null) {
                messageSnackbarHelper.showError(this, message);
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
            messageSnackbarHelper.showError(this, "Camera not available. Please restart the app.");
            session = null;
            return;
        }

        surfaceView.onResume();
        displayRotationHelper.onResume();

        TimeoutHelper.startTimer(GraffitiActivity.this);
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

    // ユーザーの許可を受け取る（スクリーンショット）
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
//            Intent intent = new Intent(this, GraffitiActivity.class);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//                startForegroundService(intent);
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
//        GLES30.glClearColor(0.1f, 0.1f, 0.1f, 1.0f);

        // Prepare the rendering objects. This involves reading shaders, so may throw an IOException.
        try {
            // Create the texture and pass it to ARCore session to be filled during update().
            backgroundRenderer.createOnGlThread(this);
            // Update BackgroundRenderer state to match the depth settings.
            backgroundRenderer.setUseOcclusion(this, depthSettings.useDepthForOcclusion());

//            planeRenderer.createOnGlThread(this, "models/trigrid.png");
//            pointCloudRenderer.createOnGlThread(this);
//            virtualObject.createOnGlThread(this, "models/fluid_01.obj", "models/fluid_01.png");
//            virtualObject.setMaterialProperties(0.0f, 2.0f, 0.5f, 6.0f);
//            virtualObjectShadow.createOnGlThread(this, "models/andy_shadow.obj", "models/andy_shadow.png");
//            virtualObjectShadow.setBlendMode(BlendMode.Shadow);
//            virtualObjectShadow.setMaterialProperties(1.0f, 0.0f, 0.0f, 1.0f);
//            planeObjectRenderer.createOnGlThread(this, "models/nambo.png");
            graffitiRenderer.createOnGlThread(this, "models/plane.png");
            // Update BackgroundRenderer state to match the depth settings.
            graffitiRenderer.setUseOcclusion(this, depthSettings.useDepthForOcclusion());
//            graffitiRenderer.setUseOcclusion(this, false);

//            planeRendererOcculusion = new PlaneRendererOcculusion(this);
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

//        try {
        // Texture names should only be set once on a GL thread unless they change. This is done during
        // onDrawFrame rather than onSurfaceCreated since the session is not guaranteed to have been
        // initialized during the execution of onSurfaceCreated.
        if (!hasSetTextureNames) {
//      session.setCameraTextureNames(
//              new int[] {backgroundRenderer.getCameraColorTexture().getTextureId()});
            session.setCameraTextureName(backgroundRenderer.getCameraColorTexture().getTextureId());
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

        // Update BackgroundRenderer state to match the depth settings. Call onSurfaceCreated().
//      try {
//          backgroundRenderer.setUseOcclusion(depthSettings.useDepthForOcclusion());
//      } catch (IOException e) {
//          Log.e(TAG, "Failed to read a required asset file", e);
//          return;
//      }

        // BackgroundRenderer.updateDisplayGeometry must be called every frame to update the coordinates
        // used to draw the background camera image.
        backgroundRenderer.updateDisplayGeometry(frame);
        graffitiRenderer.updateDisplayGeometry(frame);

        if (camera.getTrackingState() == TrackingState.TRACKING
                && (depthSettings.useDepthForOcclusion()
                /*|| depthSettings.depthColorVisualizationEnabled()*/)) {
            // Retrieve the depth map for the current frame, if available.
            try {
                Image depthImage = frame.acquireDepthImage();
                backgroundRenderer.updateCameraDepthTexture(depthImage);
//                graffitiRenderer.updateCameraDepthTexture(depthImage);
                graffitiRenderer.setDepthTexture(backgroundRenderer.getCameraDepthTexture().getTextureId(), depthImage.getWidth(), depthImage.getHeight());
            } catch (NotYetAvailableException e) {
                // This means that depth data is not available yet.
                // Depth data will not be available if there are no tracked
                // feature points. This can happen when there is no motion, or when the
                // camera loses its ability to track objects in the surrounding
                // environment.
//                Log.e(TAG, "NotYetAvailableException", e);
            }
        }

        // Handle one tap per frame.
        handleTap(frame, camera);

        // Keep the screen unlocked while tracking, but allow it to lock when tracking stops.
        trackingStateHelper.updateKeepScreenOnFlag(camera.getTrackingState());

        // Show a message based on whether tracking has failed, if planes are detected, and if the user
        // has placed any objects.
        String message = null;
        if (camera.getTrackingState() == TrackingState.PAUSED) {
            if (camera.getTrackingFailureReason() == TrackingFailureReason.NONE) {
                message = SEARCHING_PLANE_MESSAGE;
            } else {
                message = TrackingStateHelper.getTrackingFailureReasonString(camera);
            }
        } else if (hasTrackingPlane()) {
//            if (anchors.isEmpty()) {
//                message = WAITING_FOR_TAP_MESSAGE;
//            }
        } else {
            message = SEARCHING_PLANE_MESSAGE;
        }
        if (messageSnackbarHelper.isShowing() && message == null) {
            messageSnackbarHelper.hide(this);
            planeDiscoveryController.hide();
        } else if (!messageSnackbarHelper.isShowing() && message != null) {
            messageSnackbarHelper.showMessage(this, message);
            planeDiscoveryController.show();
        }

        // -- Draw background

//        if (frame.getTimestamp() != 0) {
        // Suppress rendering if the camera did not produce the first frame yet. This is to avoid
        // drawing possible leftover data from previous sessions if the texture is reused.
        virtualSceneFramebuffer.clear();
        backgroundRenderer.draw(frame);
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

        // Update lighting parameters in the shader
        graffitiRenderer.updateLightEstimation(frame.getLightEstimate(), viewmtx);

        graffitiRenderer.draw(session.getAllTrackables(Plane.class)/*session.update().getUpdatedTrackables(PlaneJSON.class)*/, camera.getDisplayOrientedPose(), projmtx);

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
                        graffitiRenderer.drawTexture(hitOnPlaneCoordX, -hitOnPlaneCoordZ, 9, trackable, drawer);
                    } else {
                        graffitiRenderer.drawTexture(hitOnPlaneCoordX, -hitOnPlaneCoordZ, brushSizeSelector.getSelectedLineWidth(), trackable, drawer);
                    }
                }
            }
        }
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
                        setImageView(bitmap2);
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

    public void setImageView(Bitmap bitmap) {
        imageView.setImageBitmap(bitmap);
        imageView.setClickable(true);
        resetTimer();
        startTimer();
    }

    private void startTimer() {
        countDownTimer = new CountDownTimer(timeLeftInMillis, START_TIME) {
            @Override
            public void onTick(long millisUntilFinished) {
                timeLeftInMillis = millisUntilFinished;
            }

            @Override
            public void onFinish() {
                imageView.setClickable(false);
                cameraButton.setClickable(true);
                imageView.setImageBitmap(null);
            }
        }.start();
    }

    private void resetTimer() {
        if (countDownTimer != null) {
            countDownTimer.cancel();
            timeLeftInMillis = START_TIME;
        }
    }

    private interface IGLDrawListener {
        public void onDraw(GL10 gl);
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
        session.configure(config);
    }

}
