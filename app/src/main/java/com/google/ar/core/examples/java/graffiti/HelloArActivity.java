/*
 * Copyright 2017 Google Inc. All Rights Reserved.
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

package com.google.ar.core.examples.java.graffiti;

import android.annotation.SuppressLint;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.Image;
import android.media.ImageReader;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.Toast;
import com.google.ar.core.Anchor;
import com.google.ar.core.ArCoreApk;
import com.google.ar.core.Camera;
import com.google.ar.core.Frame;
import com.google.ar.core.HitResult;
import com.google.ar.core.Plane;
import com.google.ar.core.PointCloud;
import com.google.ar.core.Pose;
import com.google.ar.core.Session;
import com.google.ar.core.Trackable;
import com.google.ar.core.TrackingState;
import com.google.ar.core.examples.java.common.helpers.CameraPermissionHelper;
import com.google.ar.core.examples.java.common.helpers.DisplayRotationHelper;
import com.google.ar.core.examples.java.common.helpers.FullScreenHelper;
import com.google.ar.core.examples.java.common.helpers.MusicPlayerHelper;
import com.google.ar.core.examples.java.common.helpers.SnackbarHelper;
import com.google.ar.core.examples.java.common.helpers.TapHelper;
import com.google.ar.core.examples.java.common.helpers.TimeoutHelper;
import com.google.ar.core.examples.java.common.rendering.BackgroundRenderer;
import com.google.ar.core.examples.java.common.rendering.GraffitiRenderer;
import com.google.ar.core.examples.java.common.rendering.LineShaderRenderer;
import com.google.ar.core.examples.java.common.rendering.ObjectRenderer;
import com.google.ar.core.examples.java.common.rendering.ObjectRenderer.BlendMode;
import com.google.ar.core.examples.java.common.rendering.PlaneObjectRenderer;
import com.google.ar.core.examples.java.common.rendering.PlaneRenderer;
import com.google.ar.core.examples.java.common.rendering.PointCloudRenderer;
import com.google.ar.core.examples.java.common.rendering.Test;
import com.google.ar.core.examples.java.common.view.ColorSelector;
import com.google.ar.core.examples.java.common.view.PlaneDiscoveryController;
import com.google.ar.core.exceptions.CameraNotAvailableException;
import com.google.ar.core.exceptions.UnavailableApkTooOldException;
import com.google.ar.core.exceptions.UnavailableArcoreNotInstalledException;
import com.google.ar.core.exceptions.UnavailableDeviceNotCompatibleException;
import com.google.ar.core.exceptions.UnavailableSdkTooOldException;
import com.google.ar.core.exceptions.UnavailableUserDeclinedInstallationException;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.text.SimpleDateFormat;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Date;
import java.util.Queue;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/**
 * This is a simple example that shows how to create an augmented reality (AR) application using the
 * ARCore API. The application will display any detected planes and will allow the user to tap on a
 * plane to place a 3d model of the Android robot.
 */
public class HelloArActivity extends AppCompatActivity implements GLSurfaceView.Renderer {
  private static final String TAG = HelloArActivity.class.getSimpleName();

  // Rendering. The Renderers are created here, and initialized when the GL surface is created.
  private GLSurfaceView surfaceView;

  private boolean installRequested;

  private Session session;
  private final SnackbarHelper messageSnackbarHelper = new SnackbarHelper();
  private DisplayRotationHelper displayRotationHelper;
  private TapHelper tapHelper;

  private final BackgroundRenderer backgroundRenderer = new BackgroundRenderer();
  private final ObjectRenderer virtualObject = new ObjectRenderer();
  private final ObjectRenderer virtualObjectShadow = new ObjectRenderer();
  private final PlaneRenderer planeRenderer = new PlaneRenderer();
  private final PointCloudRenderer pointCloudRenderer = new PointCloudRenderer();
  //  private final RaycastRenderer raycastRenderer = new RaycastRenderer();
  private final PlaneObjectRenderer planeObjectRenderer = new PlaneObjectRenderer();
  private final LineShaderRenderer lineShaderRenderer = new LineShaderRenderer();
  private final Test testRenderer = new Test();
  private final GraffitiRenderer graffitiRenderer = new GraffitiRenderer();

  // Temporary matrix allocated here to reduce number of allocations for each frame.
  private final float[] anchorMatrix = new float[16];
  private static final float[] DEFAULT_COLOR = new float[] {0f, 0f, 0f, 0f};
  private Queue<IGLDrawListener> glDrawListenerQueue = new ArrayDeque<>();
  private Handler glHandler = null;

  // Anchors created from taps used for object placing with a given color.
  private static class ColoredAnchor {
    public final Anchor anchor;
    public final float[] color;

    public ColoredAnchor(Anchor a, float[] color4f) {
      this.anchor = a;
      this.color = color4f;
    }
  }

  private final ArrayList<ColoredAnchor> anchors = new ArrayList<>();

  private PlaneDiscoveryController planeDiscoveryController;

  private ColorSelector colorSelector;

  private MediaProjectionManager mpManager;
  private MediaProjection mProjection;
  private static final int REQUEST_MEDIA_PROJECTION = 1001;

  private int displayWidth, displayHeight;
  private ImageReader imageReader;
  private VirtualDisplay virtualDisplay;
  private int screenDensity;
  private ImageView imageView;
  private ImageView cameraButton;

  private CountDownTimer countDownTimer;
  private static final long START_TIME = 10000;
  private long timeLeftInMillis = START_TIME;

  private MusicPlayerHelper helloArClickSE = new MusicPlayerHelper();
  private boolean isLoop = false;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);
    surfaceView = findViewById(R.id.surfaceview);
    displayRotationHelper = new DisplayRotationHelper(/*context=*/ this);

    // Set up tap listener.
    tapHelper = new TapHelper(/*context=*/ this, HelloArActivity.this);
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

    // Set up the ColorSelector View.
    colorSelector = findViewById(R.id.color_selector_view);

    // Set up the Screen Shot View.
    // 撮影したスクリーンを表示するImageView
    imageView = findViewById(R.id.image_view);
    imageView.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        startActivity(new Intent(HelloArActivity.this, PhotoGalleryActivity.class));
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
          helloArClickSE.musicPlay(HelloArActivity.this, "musics/se/camera-shutter.mp3", isLoop);
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

    }

    // Note that order matters - see the note in onPause(), the reverse applies here.
    try {
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

//    messageSnackbarHelper.showMessage(this, "Searching for surfaces...");
    planeDiscoveryController.show();

    TimeoutHelper.startTimer(HelloArActivity.this);
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
    if (!CameraPermissionHelper.hasCameraPermission(this)) {
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
    if (REQUEST_MEDIA_PROJECTION == requestCode) {
      if (resultCode != RESULT_OK) {
        // 拒否された
        Toast.makeText(this,
                "User cancelled", Toast.LENGTH_LONG).show();
        return;
      }
      // 許可された結果を受け取る
      setUpMediaProjection(resultCode, data);
    }
  }

  private void setUpMediaProjection(int code, Intent intent) {
    mProjection = mpManager.getMediaProjection(code, intent);
    setUpVirtualDisplay();
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
      Log.d("debug","release VirtualDisplay");
      virtualDisplay.release();
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
    GLES20.glClearColor(0.1f, 0.1f, 0.1f, 1.0f);

    // Prepare the rendering objects. This involves reading shaders, so may throw an IOException.
    try {
      // Create the texture and pass it to ARCore session to be filled during update().
      backgroundRenderer.createOnGlThread(/*context=*/ this);
      planeRenderer.createOnGlThread(/*context=*/ this, "models/trigrid.png");
      pointCloudRenderer.createOnGlThread(/*context=*/ this);
      virtualObject.createOnGlThread(/*context=*/ this, "models/fluid_01.obj", "models/fluid_01.png");
      virtualObject.setMaterialProperties(0.0f, 2.0f, 0.5f, 6.0f);

      virtualObjectShadow.createOnGlThread(/*context=*/ this, "models/andy_shadow.obj", "models/andy_shadow.png");
      virtualObjectShadow.setBlendMode(BlendMode.Shadow);
      virtualObjectShadow.setMaterialProperties(1.0f, 0.0f, 0.0f, 1.0f);

//      raycastRenderer.createOnGlThread(this);
      planeObjectRenderer.createOnGlThread(this,"models/nambo.png");
      lineShaderRenderer.createOnGlThread(/*context=*/ this,"models/linecap.png");

      testRenderer.createOnGlThread(this,"models/nambo.png");
      graffitiRenderer.createOnGlThread(this,"models/plane.png");

    } catch (IOException e) {
      Log.e(TAG, "Failed to read an asset file", e);
    }
  }

  @Override
  public void onSurfaceChanged(GL10 gl, int width, int height) {
    displayRotationHelper.onSurfaceChanged(width, height);
    GLES20.glViewport(0, 0, width, height);
  }

  @Override
  public void onDrawFrame(GL10 gl) {
    IGLDrawListener l = glDrawListenerQueue.poll();
    if (l != null) l.onDraw(gl);

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

      // Handle one tap per frame.
      handleTap(frame, camera);

      // Draw background.
      backgroundRenderer.draw(frame);

      // If not tracking, don't draw 3d objects.
      if (camera.getTrackingState() == TrackingState.PAUSED) {
        return;
      }

      // Get projection matrix.
      float[] projmtx = new float[16];
      camera.getProjectionMatrix(projmtx, 0, 0.1f, 100.0f);

      // Get camera matrix and draw.
      float[] viewmtx = new float[16];
      camera.getViewMatrix(viewmtx, 0);

      // Compute lighting from average intensity of the image.
      // The first three components are color scaling factors.
      // The last one is the average pixel intensity in gamma space.
      final float[] colorCorrectionRgba = new float[4];
      frame.getLightEstimate().getColorCorrection(colorCorrectionRgba, 0);

      // Visualize tracked points.
      PointCloud pointCloud = frame.acquirePointCloud();
      pointCloudRenderer.update(pointCloud);
//      pointCloudRenderer.draw(viewmtx, projmtx);

      // Application is responsible for releasing the point cloud resources after
      // using it.
      pointCloud.release();

      // Check if we detected at least one plane. If so, hide the loading message.
//      if (messageSnackbarHelper.isShowing()) {
      for (Plane plane : session.getAllTrackables(Plane.class)) {
        if (plane.getTrackingState() == TrackingState.TRACKING) {
//            messageSnackbarHelper.hide(this);
          planeDiscoveryController.hide();
          break;
        }
      }
//      }

      // Visualize planes.
//      planeRenderer.drawPlanes(session.getAllTrackables(Plane.class), camera.getDisplayOrientedPose(), projmtx);

//      planeObjectRenderer.draw(session.getAllTrackables(Plane.class)/*session.update().getUpdatedTrackables(Plane.class)*/, camera.getDisplayOrientedPose(), projmtx);
//      testRenderer.draw(session.getAllTrackables(Plane.class)/*session.update().getUpdatedTrackables(Plane.class)*/, camera.getDisplayOrientedPose(), projmtx);

      graffitiRenderer.adjustTextureAxis(frame, camera);
      graffitiRenderer.draw(session.getAllTrackables(Plane.class)/*session.update().getUpdatedTrackables(Plane.class)*/, camera.getDisplayOrientedPose(), projmtx);

//      raycastRenderer.draw(viewmtx,projmtx);

      // Visualize anchors created by touch.
      float scaleFactor = 1.0f;
      for (ColoredAnchor coloredAnchor : anchors) {
        if (coloredAnchor.anchor.getTrackingState() != TrackingState.TRACKING) {
          continue;
        }
        // Get the current pose of an Anchor in world space. The Anchor pose is updated
        // during calls to session.update() as ARCore refines its estimate of the world.
        coloredAnchor.anchor.getPose().toMatrix(anchorMatrix, 0);

        // Update and draw the model and its shadow.
//        virtualObject.updateModelMatrix(anchorMatrix, scaleFactor);
//        virtualObjectShadow.updateModelMatrix(anchorMatrix, scaleFactor);
//        virtualObject.draw(viewmtx, projmtx, colorCorrectionRgba, coloredAnchor.color);
//        virtualObjectShadow.draw(viewmtx, projmtx, colorCorrectionRgba, coloredAnchor.color);
        lineShaderRenderer.updateModelMatrix(anchorMatrix, scaleFactor);
//        lineShaderRenderer.draw(viewmtx, projmtx);
      }

    } catch (Throwable t) {
      // Avoid crashing the application due to unhandled exceptions.
      Log.e(TAG, "Exception on the OpenGL thread", t);
    }
  }

  // Handle only one tap per frame, as taps are usually low frequency compared to frame rate.
  private void handleTap(Frame frame, Camera camera) {
    MotionEvent tap = tapHelper.poll();
    if (tap != null && camera.getTrackingState() == TrackingState.TRACKING) {
      for (HitResult hit : frame.hitTest(tap)) {
        // Check if any plane was hit, and if it was hit inside the plane polygon
        Trackable trackable = hit.getTrackable();
        // Creates an anchor if a plane or an oriented point was hit.
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
          graffitiRenderer.drawCircle(hitOnPlaneCoordX, -hitOnPlaneCoordZ, colorSelector.getSelectedLineColor(), 4, trackable);
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
        GLES20.glGetIntegerv(GLES20.GL_VIEWPORT, IntBuffer.wrap(viewportDim));
        int width = viewportDim[2];
        int height = viewportDim[3];
        ByteBuffer buffer = ByteBuffer.allocate(width * height * 4);
        buffer.position(0);
        GLES20.glReadPixels(0, 0, width, height, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, buffer);
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
    ContentResolver contentResolver = HelloArActivity.this.getContentResolver();
    contentValues.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
    contentValues.put("_data", file);
    contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            contentValues);
  }

  public void setImageView(Bitmap bitmap) {
    cameraButton.setClickable(false);
    imageView.setImageBitmap(bitmap);
    imageView.setClickable(true);
    resetTimer();
    startTimer();
  }

  private void startTimer(){
    countDownTimer = new CountDownTimer(timeLeftInMillis,START_TIME) {
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

  private void resetTimer(){
      if(countDownTimer != null) {
          countDownTimer.cancel();
          timeLeftInMillis = START_TIME;
      }
  }

  private interface IGLDrawListener {
    public void onDraw(GL10 gl);
  }
}
