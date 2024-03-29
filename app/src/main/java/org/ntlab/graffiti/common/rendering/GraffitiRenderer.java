package org.ntlab.graffiti.common.rendering;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.PorterDuff;
import android.opengl.GLES20;
import android.opengl.GLUtils;
import android.opengl.Matrix;
import android.util.Log;

import com.google.ar.core.Camera;
import com.google.ar.core.Frame;
import com.google.ar.core.Plane;
import com.google.ar.core.Pose;
import com.google.ar.core.Trackable;
import com.google.ar.core.TrackingState;

import org.ntlab.graffiti.common.drawer.TextureDrawer;
import org.ntlab.graffiti.common.geometry.Vector;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/**
 * This class renders the AR Graffiti.
 * Created by a-hongo on 01,4月,2021
 * @author n-nitta, a-hongo
 */
public class GraffitiRenderer {
    private static final String TAG = GraffitiRenderer.class.getSimpleName();

    // Shader names.
    private static final String VERTEX_SHADER_NAME = "shaders/graffiti.vert";
    private static final String FRAGMENT_SHADER_NAME = "shaders/graffiti.frag";

    private static final int BYTES_PER_FLOAT = Float.SIZE / 8;
    private static final int BYTES_PER_SHORT = Short.SIZE / 8;
    private static final int COORDS_PER_VERTEX = 3; // x, z, alpha

    private static final int VERTS_PER_BOUNDARY_VERT = 2;
    private static final int INDICES_PER_BOUNDARY_VERT = 3;
    private static final int INITIAL_BUFFER_BOUNDARY_VERTS = 64;

    private static final int INITIAL_VERTEX_BUFFER_SIZE_BYTES = BYTES_PER_FLOAT * COORDS_PER_VERTEX * VERTS_PER_BOUNDARY_VERT * INITIAL_BUFFER_BOUNDARY_VERTS;

    private static final int INITIAL_INDEX_BUFFER_SIZE_BYTES =
            BYTES_PER_SHORT
                    * INDICES_PER_BOUNDARY_VERT
                    * INDICES_PER_BOUNDARY_VERT
                    * INITIAL_BUFFER_BOUNDARY_VERTS;

    private static final float FADE_RADIUS_M = 0.25f;

    // texture size
    private static final float DOTS_PER_METER = 0.2f;

    // Using the "signed distance field" approach to render sharp lines and circles.
    // {dotThreshold, lineThreshold, lineFadeSpeed, occlusionScale}
    // dotThreshold/lineThreshold: red/green intensity above which dots/lines are present
    // lineFadeShrink:  lines will fade in between alpha = 1-(1/lineFadeShrink) and 1.0
    // occlusionShrink: occluded planes will fade out between alpha = 0 and 1/occlusionShrink
    private static final float[] GRID_CONTROL = {0.2f, 0.4f, 2.0f, 1.5f};

    private int planeobjectProgram;

    private int planeobjectXZPositionAlphaAttribute;

    private int planeobjectModelViewProjectionUniform;
    private int textureUniform;
    private int lineColorUniform;
    private int dotColorUniform;
    private int gridControlUniform;
    private int planeobjectUvMatrixUniform;

    private FloatBuffer vertexBuffer =
            ByteBuffer.allocateDirect(INITIAL_VERTEX_BUFFER_SIZE_BYTES)
                    .order(ByteOrder.nativeOrder())
                    .asFloatBuffer();
    private ShortBuffer indexBuffer =
            ByteBuffer.allocateDirect(INITIAL_INDEX_BUFFER_SIZE_BYTES)
                    .order(ByteOrder.nativeOrder())
                    .asShortBuffer();

    // Temporary lists/matrices allocated here to reduce number of allocations for each frame.
    private final float[] modelMatrix = new float[16];
    private final float[] modelViewMatrix = new float[16];
    private final float[] modelViewProjectionMatrix = new float[16];
    private final float[] planeobjectColor = new float[4];
    private final float[] planeobjectAngleUvMatrix = new float[4]; // 2x2 rotation matrix applied to uv coords.

    private final Map<Plane, Integer> planeobjectIndexMap = new HashMap<>();

    private final int backgroundColor = Color.TRANSPARENT;      // テクスチャ背景色
    private Bitmap textureBitmap = null;
    private Bitmap textureBitmapToRecycle = null;
    private ArrayList<Bitmap> textureBitmaps = new ArrayList<Bitmap>();
    private ArrayList<Integer> textures = new ArrayList<Integer>(); //テキスチャID
    private HashMap<Plane, Integer> planeNo = new HashMap<Plane, Integer>();
    private HashMap<Plane, Pose> planePose = new HashMap<>();

    private int diffColoredPxs;

    private boolean isVisibility = false;

    public GraffitiRenderer() {}

    /**
     * Allocates and initializes OpenGL resources needed by the plane renderer. Must be called on the
     * OpenGL thread, typically in {@link android.opengl.GLSurfaceView.Renderer#onSurfaceCreated(GL10, EGLConfig)}.
     *
     * @param context Needed to access shader source and texture PNG.
     * @param gridDistanceTextureName Name of the PNG file containing the grid texture.
     */
    public void createOnGlThread(Context context, String gridDistanceTextureName)
            throws IOException {
        ShaderUtil.checkGLError(TAG, "before create");

        int vertexShader =
                ShaderUtil.loadGLShader(TAG, context, GLES20.GL_VERTEX_SHADER, VERTEX_SHADER_NAME);
        int passthroughShader =
                ShaderUtil.loadGLShader(TAG, context, GLES20.GL_FRAGMENT_SHADER, FRAGMENT_SHADER_NAME);

        planeobjectProgram = GLES20.glCreateProgram();
        GLES20.glAttachShader(planeobjectProgram, vertexShader);
        GLES20.glAttachShader(planeobjectProgram, passthroughShader);
        GLES20.glLinkProgram(planeobjectProgram);
        GLES20.glUseProgram(planeobjectProgram);

        ShaderUtil.checkGLError(TAG, "Program creation");

        // Read the texture.
        textureBitmap = BitmapFactory.decodeStream(context.getAssets().open(gridDistanceTextureName));
        textureBitmapToRecycle = textureBitmap.copy(textureBitmap.getConfig(),true);
        textureBitmapToRecycle.eraseColor(backgroundColor);

        ShaderUtil.checkGLError(TAG, "Texture loading");

        planeobjectXZPositionAlphaAttribute = GLES20.glGetAttribLocation(planeobjectProgram, "a_XZPositionAlpha");

        planeobjectModelViewProjectionUniform =
                GLES20.glGetUniformLocation(planeobjectProgram, "u_ModelViewProjection");
        textureUniform = GLES20.glGetUniformLocation(planeobjectProgram, "u_Texture");
        lineColorUniform = GLES20.glGetUniformLocation(planeobjectProgram, "u_lineColor");
        dotColorUniform = GLES20.glGetUniformLocation(planeobjectProgram, "u_dotColor");
        gridControlUniform = GLES20.glGetUniformLocation(planeobjectProgram, "u_gridControl");
        planeobjectUvMatrixUniform = GLES20.glGetUniformLocation(planeobjectProgram, "u_PlaneUvMatrix");

        ShaderUtil.checkGLError(TAG, "Program parameters");
    }

    /**
     * Sum up part of colored pixels.
     *
     * @param color 集計する色
     */
    public long getPartColoredPixels(Plane plane, int x, int y, int r, int color) {
        long colorPixels = 0; // color色pixelの総計
        if (textureBitmap != null) {
            Integer coloredPlaneObjectTextureNo = planeNo.get(plane);
            Log.d(TAG, "no: " + coloredPlaneObjectTextureNo);
            if (coloredPlaneObjectTextureNo != null && coloredPlaneObjectTextureNo < textureBitmaps.size()) {
                Bitmap bitmap = textureBitmaps.get(coloredPlaneObjectTextureNo);
                int w = bitmap.getWidth();
                int h = bitmap.getHeight();
                int[] pixels = new int[w * h];
                bitmap.getPixels(pixels, 0, w, 0, 0, w, h);
                int pixelX = (int)((x * DOTS_PER_METER + 0.5) * w);
                int pixelY = (int)((y * DOTS_PER_METER + 0.5) * h);
                pixelX = Math.floorMod(pixelX, w);
                pixelY = Math.floorMod(pixelY, h);
                for (int i = pixelX - r; i < pixelX + r; i++) {
                    for (int j = pixelY - r; j < pixelY + r; j++) {
                        if (pixels[i + (j * w)] == color) {
                            colorPixels++;
                        }
                    }
                }
            }
        }
        Log.d(TAG, "partColor: " + colorPixels);
        return colorPixels;
    }

    /**
     * Sum up part of colored pixels.
     *
     * @param color 集計する色
     */
    public long getColoredPixels(Plane plane, int color) {
        long colorPixels = 0; // color色pixelの総計
        if (textureBitmap != null) {
            Integer coloredPlaneObjectTextureNo = planeNo.get(plane);
            Log.d(TAG, "no: " + coloredPlaneObjectTextureNo);
            if (coloredPlaneObjectTextureNo != null && coloredPlaneObjectTextureNo < textureBitmaps.size()) {
                Bitmap bitmap = textureBitmaps.get(coloredPlaneObjectTextureNo);
                int w = bitmap.getWidth();
                int h = bitmap.getHeight();
                int[] pixels = new int[w * h];
                bitmap.getPixels(pixels, 0, w, 0, 0, w, h);
                for (int j = 0; j < w * h; j++) {
                    if (pixels[j] == color) {
                        colorPixels++;
                    }
                }
            }
        }
        Log.d(TAG, "color: " + colorPixels);
        return colorPixels;
    }

    /**
         * Sum up colored pixels.
         *
         * @param color 集計する色
         */
    public long getTotalColoredPixels(int color) {
        long colorPixels = 0; // color色pixelの総計
        for (int i = 0; i < textureBitmaps.size(); i++) {
            Bitmap bitmap = textureBitmaps.get(i);
            int w = bitmap.getWidth();
            int h = bitmap.getHeight();
            int[] pixels = new int[w * h];
            bitmap.getPixels(pixels, 0, w, 0, 0, w, h);
            for (int j = 0; j < w * h; j++) {
                if (pixels[j] == color) {
                    colorPixels++;
                }
            }
        }
        return colorPixels;
    }

    public int getDiffColoredPixels() {
        return diffColoredPxs;
    }

    /**
     * Sum up difference colored pixels between preBitmap and curBitmap.
     *
     * @param color 集計する色
     * @param preBitmap previous bitmap
     * @param curBitmap current bitmap
     */
    private int diffColoredPixels(int color, Bitmap preBitmap, Bitmap curBitmap) {
        if (color == -1) {
            // TODO Correspond color
            color = Color.BLUE;
        }
        int coloredPixels = 0; // color色pixelの総計
        if (preBitmap.getWidth() == curBitmap.getWidth()
                && preBitmap.getHeight() == curBitmap.getHeight()) {
            int w = preBitmap.getWidth();
            int h = preBitmap.getHeight();
            int[] prePixels = new int[w * h];
            int[] curPixels = new int[w * h];
            preBitmap.getPixels(prePixels, 0, w, 0, 0, w, h);
            curBitmap.getPixels(curPixels, 0, w, 0, 0, w, h);
            for (int i = 0; i < w * h; i++) {
                if (prePixels[i] == color) {
                    if (curPixels[i] != color) coloredPixels--; // prePixels[i]だけがcolor
                } else if (curPixels[i] == color) { // curPixels[i]だけがcolor
                    coloredPixels++;
                }
            }
        }
        return coloredPixels;
    }

    /** Updates the plane model transform matrix and extents. */
    public void updatePlaneObjectParamaters(float[] planeobjectMatrix, float extentX, float extentZ, FloatBuffer boundary) {
        System.arraycopy(planeobjectMatrix, 0, modelMatrix, 0, 16);
        if (boundary == null) {
            vertexBuffer.limit(0);
            indexBuffer.limit(0);
            return;
        }

        // Generate a new set of vertices and a corresponding triangle strip index set so that
        // the plane boundary polygon has a fading edge. This is done by making a copy of the
        // boundary polygon vertices and scaling it down around center to push it inwards. Then
        // the index buffer is setup accordingly.
        boundary.rewind();
        int boundaryVertices = boundary.limit() / 2;
        int numVertices;
        int numIndices;

        numVertices = boundaryVertices * VERTS_PER_BOUNDARY_VERT;

        // drawn as GL_TRIANGLE_STRIP with 3n-2 triangles (n-2 for fill, 2n for perimeter).
        numIndices = boundaryVertices * INDICES_PER_BOUNDARY_VERT;

        if (vertexBuffer.capacity() < numVertices * COORDS_PER_VERTEX) {
            int size = vertexBuffer.capacity();
            while (size < numVertices * COORDS_PER_VERTEX) {
                size *= 2;
            }
            vertexBuffer =
                    ByteBuffer.allocateDirect(BYTES_PER_FLOAT * size)
                            .order(ByteOrder.nativeOrder())
                            .asFloatBuffer();
        }
        vertexBuffer.rewind();
        vertexBuffer.limit(numVertices * COORDS_PER_VERTEX);

        if (indexBuffer.capacity() < numIndices) {
            int size = indexBuffer.capacity();
            while (size < numIndices) {
                size *= 2;
            }
            indexBuffer =
                    ByteBuffer.allocateDirect(BYTES_PER_SHORT * size)
                            .order(ByteOrder.nativeOrder())
                            .asShortBuffer();
        }
        indexBuffer.rewind();
        indexBuffer.limit(numIndices);

        // Note: when either dimension of the bounding box is smaller than 2*FADE_RADIUS_M we
        // generate a bunch of 0-area triangles.  These don't get rendered though so it works
        // out ok.
        float xScale = Math.max((extentX - 2 * FADE_RADIUS_M) / extentX, 1.0f);
        float zScale = Math.max((extentZ - 2 * FADE_RADIUS_M) / extentZ, 1.0f);

        while (boundary.hasRemaining()) {
            float x = boundary.get();
            float z = boundary.get();
            vertexBuffer.put(x);
            vertexBuffer.put(z);
            vertexBuffer.put(0.0f);
            vertexBuffer.put(x * xScale);
            vertexBuffer.put(z * zScale);
            vertexBuffer.put(1.0f);
        }

        // step 1, perimeter（外部）
        for (int i = 0; i < boundaryVertices; ++i) {
            indexBuffer.put((short) (i * 2 + 1));
        }
        indexBuffer.put((short) 1);
        // This leaves us on the interior edge of the perimeter between the inset vertices
        // for boundary verts n-1 and 0.
    }

    /**
     * Draw the texture.
     * @param x
     * @param y
     * @param r
     * @param trackable
     * @param drawer
     */
    public PointF drawTexture(float x, float y, int r, Trackable trackable, TextureDrawer drawer) {
        if (textureBitmap != null) {
            Integer hitPlaneObjectTextureNo = planeNo.get(trackable);

            Log.d(TAG, "No. " + hitPlaneObjectTextureNo);
            if (hitPlaneObjectTextureNo == null || textureBitmaps.size() <= hitPlaneObjectTextureNo)
                return null;
            int w = textureBitmaps.get(hitPlaneObjectTextureNo).getWidth();
            int h = textureBitmaps.get(hitPlaneObjectTextureNo).getHeight();
            int pixelX = (int)((x * DOTS_PER_METER + 0.5) * w);
            int pixelY = (int)((y * DOTS_PER_METER + 0.5) * h);
            pixelX = Math.floorMod(pixelX, w);
            pixelY = Math.floorMod(pixelY, h);

            Bitmap bitmap = textureBitmaps.get(hitPlaneObjectTextureNo);
            Canvas canvas = new Canvas(bitmap);
            Bitmap preBitmap = Bitmap.createBitmap(bitmap, pixelX - r, pixelY - r, r * 2, r * 2);
            Bitmap miniBitmap;

            drawer.draw(pixelX, pixelY, r, canvas);

            miniBitmap = Bitmap.createBitmap(bitmap, pixelX - r, pixelY - r, r * 2, r * 2);
            diffColoredPxs = diffColoredPixels(-1, preBitmap, miniBitmap);

            GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textures.get(hitPlaneObjectTextureNo));
            GLUtils.texSubImage2D(GLES20.GL_TEXTURE_2D, 0, pixelX - r, pixelY - r, miniBitmap, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE);
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
            return new PointF(pixelX, pixelY);
        }
        return null;
    }

    /**
     * Adjust the texture axis.
     *
     * @param frame
     * @param camera
     */
    public void adjustTextureAxis(Frame frame, Camera camera) {
        Pose cameraPose = camera.getPose();
        Pose worldToCameraLocal = cameraPose.inverse();

        for (Plane p: frame.getUpdatedTrackables(Plane.class)) {
            Pose newPose = p.getCenterPose();
            Pose oldPose = planePose.get(p);

            if (!planeNo.containsKey(p)) return;

            // 1. テクスチャを平面上で回転＆移動させるための行列の作成
            Bitmap bitmap = textureBitmaps.get(planeNo.get(p));
            textureBitmapToRecycle.eraseColor(backgroundColor);
            Canvas canvas = new Canvas(textureBitmapToRecycle);

            //  回転成分
            android.graphics.Matrix adjustMatrix = new android.graphics.Matrix();
            float[] newAxisX = newPose.getXAxis();
            float[] newAxisY = newPose.getYAxis();
            float[] newAxisZ = newPose.getZAxis();
            float[] oldAxisX = oldPose.getXAxis();
            float cosTheta = Vector.dot(newAxisX, oldAxisX);
            float sinTheta = Vector.dot(newAxisY, Vector.cross(newAxisX, oldAxisX));
            float theta = (float) (Math.atan2(sinTheta, cosTheta) / Math.PI * 180.0);
            adjustMatrix.setRotate(theta, 0.5f * bitmap.getWidth(), 0.5f * bitmap.getHeight());

            // 並行移動成分
            float[] newToOld = Vector.minus(oldPose.getTranslation(), newPose.getTranslation());
            float pixelTransX = (Vector.dot(newToOld, newAxisX) * DOTS_PER_METER) * bitmap.getWidth();
            float pixelTransY = (-Vector.dot(newToOld, newAxisZ) * DOTS_PER_METER) * bitmap.getHeight();
            adjustMatrix.postTranslate(pixelTransX, pixelTransY);

            // 2. 適用
            canvas.drawBitmap(bitmap, adjustMatrix, new Paint());

            // 3. 転送
            GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textures.get(planeNo.get(p)));
            GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, textureBitmapToRecycle, 0);
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);

            textureBitmaps.set(planeNo.get(p), textureBitmapToRecycle);
            textureBitmapToRecycle = bitmap;        //  リサイクルに回す
            planePose.put(p, newPose);
        }
    }

    /**
     * Clear one textureBitmap.
     */
    public void clearTexture(Plane plane) {
        if (textureBitmap != null) {
            Integer coloredPlaneObjectTextureNo = planeNo.get(plane);
            if (coloredPlaneObjectTextureNo != null && textureBitmaps.size() > coloredPlaneObjectTextureNo) {
                Bitmap bitmap = textureBitmaps.get(coloredPlaneObjectTextureNo);
                Canvas canvas = new Canvas(bitmap);
                canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
            }
        }
    }

    /**
     * Clear all textureBitmap.
     */
    public void clearAllTexture() {
        for (Plane p: planeNo.keySet()) {
            Bitmap bitmap = textureBitmaps.get(planeNo.get(p));
            Canvas canvas = new Canvas(bitmap);
            canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
        }
    }

    /**
     * Draw the Garffiti.
     *
     * @param cameraView
     * @param cameraPerspective
     * @param planeNormal
     */
    private void draw(float[] cameraView, float[] cameraPerspective, float[] planeNormal) {
        // Build the ModelView and ModelViewProjection matrices
        // for calculating cube position and light.
        Matrix.multiplyMM(modelViewMatrix, 0, cameraView, 0, modelMatrix, 0);
        Matrix.multiplyMM(modelViewProjectionMatrix, 0, cameraPerspective, 0, modelViewMatrix, 0);

        // Set the position of the plane
        vertexBuffer.rewind();
        GLES20.glVertexAttribPointer(
                planeobjectXZPositionAlphaAttribute,
                COORDS_PER_VERTEX,
                GLES20.GL_FLOAT,
                false,
                BYTES_PER_FLOAT * COORDS_PER_VERTEX,
                vertexBuffer);

        // Set the Model and ModelViewProjection matrices in the shader.
        GLES20.glUniformMatrix4fv(
                planeobjectModelViewProjectionUniform, 1, false, modelViewProjectionMatrix, 0);

        // Draw the Model.
        indexBuffer.rewind();
        GLES20.glDrawElements(
                GLES20.GL_TRIANGLE_FAN, indexBuffer.limit(), GLES20.GL_UNSIGNED_SHORT, indexBuffer);
        ShaderUtil.checkGLError(TAG, "Drawing plane");
    }

    static class SortablePlaneObject {
        final float distance;
        final Plane plane;

        SortablePlaneObject(float distance, Plane plane) {
            this.distance = distance;
            this.plane = plane;
        }
    }

    /**
     * Draws the collection of tracked planes, with closer planes hiding more distant ones.
     *
     * @param updateObjectPlanes The collection of planes to draw.
     * @param cameraPose The pose of the camera, as returned by {@link Camera#getPose()}
     * @param cameraPerspective The projection matrix, as returned by {@link
     *     Camera#getProjectionMatrix(float[], int, float, float)}
     */
    public void draw(Collection<Plane> updateObjectPlanes, Pose cameraPose, float[] cameraPerspective) {
        // Planes must be sorted by distance from camera so that we draw closer planes first, and
        // they occlude the farther planes.
        List<SortablePlaneObject> sortedPlaneObjects = new ArrayList<>();

        for (Plane plane : updateObjectPlanes) {
            if (plane.getTrackingState() != TrackingState.TRACKING /*|| plane.getSubsumedBy() != null*/) {
                continue;
            }

            float distance = calculateDistanceToPlane(plane.getCenterPose(), cameraPose);
            if (distance < 0) { // PlaneJSON is back-facing.
                continue;
            }
            sortedPlaneObjects.add(new SortablePlaneObject(distance, plane));
        }
        Collections.sort(
                sortedPlaneObjects,
                new Comparator<SortablePlaneObject>() {
                    @Override
                    public int compare(GraffitiRenderer.SortablePlaneObject a, GraffitiRenderer.SortablePlaneObject b) {
                        return Float.compare(a.distance, b.distance);
                    }
                });

        float[] cameraView = new float[16];
        cameraPose.inverse().toMatrix(cameraView, 0);

        // Planes are drawn with additive blending, masked by the alpha channel for occlusion.

        // Start by clearing the alpha channel of the color buffer to 1.0.
        GLES20.glClearColor(1, 1, 1, 1);
        GLES20.glColorMask(false, false, false, true);
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
        GLES20.glColorMask(true, true, true, true);

        // Disable depth write.
        GLES20.glDepthMask(false);

        // Additive blending, masked by alpha channel, clearing alpha channel.
        GLES20.glEnable(GLES20.GL_BLEND);
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);

        // Set up the shader.
        GLES20.glUseProgram(planeobjectProgram);

        // Shared fragment uniforms.
        GLES20.glUniform4fv(gridControlUniform, 1, GRID_CONTROL, 0);

        // Enable vertex arrays
        GLES20.glEnableVertexAttribArray(planeobjectXZPositionAlphaAttribute);

        ShaderUtil.checkGLError(TAG, "Setting up to draw planes");

        for (GraffitiRenderer.SortablePlaneObject sortedPlaneObject : sortedPlaneObjects) {
            Plane plane = sortedPlaneObject.plane;
            float[] planeobjectMatrix = new float[16];
            plane.getCenterPose().toMatrix(planeobjectMatrix, 0);

            float[] normal = new float[3];
            // Get transformed Y axis of plane's coordinate system.
            plane.getCenterPose().getTransformedAxis(1, 1.0f, normal, 0);

            updatePlaneObjectParamaters(
                    planeobjectMatrix, plane.getExtentX(), plane.getExtentZ(), plane.getPolygon());

            // Get plane index. Keep a map to assign same indices to same planes.
            Integer planeobjectIndex = planeobjectIndexMap.get(plane);
            if (planeobjectIndex == null) {
                planeobjectIndex = planeobjectIndexMap.size();
                planeobjectIndexMap.put(plane, planeobjectIndex);

                planeNo.put(plane, planeobjectIndex);
                planePose.put(plane, plane.getCenterPose());

                GLES20.glActiveTexture(GLES20.GL_TEXTURE0);

                int textureArray[] = new int[1];

                GLES20.glGenTextures(textureArray.length, textureArray, 0);
                textures.add(textureArray[0]);
                GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textures.get(planeobjectIndex));
                GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR_MIPMAP_LINEAR);
                GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);

                Bitmap tmp = textureBitmap.copy(textureBitmap.getConfig(), true);
                tmp.eraseColor(backgroundColor);
                textureBitmaps.add(tmp);
                GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, textureBitmaps.get(planeobjectIndex), 0);
                GLES20.glGenerateMipmap(GLES20.GL_TEXTURE_2D);
                GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
            }

            // Attach the texture.
            GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textures.get(planeobjectIndex));
            GLES20.glUniform1i(textureUniform, 0);

            // Set plane color. Computed deterministically from the PlaneJSON index.
            int colorIndex = planeobjectIndex % PLANE_COLORS_RGBA.length;
            colorRgbaToFloat(planeobjectColor, PLANE_COLORS_RGBA[colorIndex]);
            GLES20.glUniform4fv(lineColorUniform, 1, planeobjectColor, 0);
            GLES20.glUniform4fv(dotColorUniform, 1, planeobjectColor, 0);

            // Each plane will have its own angle offset from others, to make them easier to
            // distinguish. Compute a 2x2 rotation matrix from the angle.
            float angleRadians = 0.0f;   //planeobjectIndex * 0.1f/*44*/;
            float uScale = DOTS_PER_METER;
            float vScale = DOTS_PER_METER /* EQUILATERAL_TRIANGLE_SCALE*/;
            planeobjectAngleUvMatrix[0] = +(float) Math.cos(angleRadians) * uScale;
            planeobjectAngleUvMatrix[1] = -(float) Math.sin(angleRadians) * vScale;
            planeobjectAngleUvMatrix[2] = +(float) Math.sin(angleRadians) * uScale;
            planeobjectAngleUvMatrix[3] = +(float) Math.cos(angleRadians) * vScale;

            GLES20.glUniformMatrix2fv(planeobjectUvMatrixUniform, 1, false, planeobjectAngleUvMatrix, 0);

            draw(cameraView, cameraPerspective, normal);
        }

        // Clean up the state we set
        GLES20.glDisableVertexAttribArray(planeobjectXZPositionAlphaAttribute);
        GLES20.glDisable(GLES20.GL_BLEND);
        GLES20.glDepthMask(true);
        GLES20.glClearColor(0.1f, 0.1f, 0.1f, 1.0f);

        ShaderUtil.checkGLError(TAG, "Cleaning up after drawing planes");
    }

    // Calculate the normal distance to plane from cameraPose, the given planePose should have y axis
    // parallel to plane's normal, for example plane's center pose or hit test pose.
    public static float calculateDistanceToPlane(Pose planePose, Pose cameraPose) {
        float[] normal = new float[3];
        float cameraX = cameraPose.tx();
        float cameraY = cameraPose.ty();
        float cameraZ = cameraPose.tz();
        // Get transformed Y axis of plane's coordinate system.
        planePose.getTransformedAxis(1, 1.0f, normal, 0);
        // Compute dot product of plane's normal with vector from camera to plane center.
        return (cameraX - planePose.tx()) * normal[0]
                + (cameraY - planePose.ty()) * normal[1]
                + (cameraZ - planePose.tz()) * normal[2];
    }

    public boolean isTotalColoredPixels(int up, int color) {
        long colorPixels = 0; // color色pixelの総計
        for (int i = 0; i < textureBitmaps.size(); i++) {
            Bitmap bitmap = textureBitmaps.get(i);
            int w = bitmap.getWidth();
            int h = bitmap.getHeight();
            int[] pixels = new int[w * h];
            bitmap.getPixels(pixels, 0, w, 0, 0, w, h);
            for (int j = 0; j < w * h; j++) {
                if (pixels[j] == color) {
                    colorPixels++;
                }
                if (colorPixels >= up) return true;
            }
        }
        return false;
    }

    private static void colorRgbaToFloat(float[] planeColor, int colorRgba) {
        planeColor[0] = ((float) ((colorRgba >> 24) & 0xff)) / 255.0f;
        planeColor[1] = ((float) ((colorRgba >> 16) & 0xff)) / 255.0f;
        planeColor[2] = ((float) ((colorRgba >> 8) & 0xff)) / 255.0f;
        planeColor[3] = ((float) ((colorRgba >> 0) & 0xff)) / 255.0f;
    }
    private static final int[] PLANE_COLORS_RGBA = {
            0xFFFFFFFF,
            0xF44336FF,
            0xE91E63FF,
            0x9C27B0FF,
            0x673AB7FF,
            0x3F51B5FF,
            0x2196F3FF,
            0x03A9F4FF,
            0x00BCD4FF,
            0x009688FF,
            0x4CAF50FF,
            0x8BC34AFF,
            0xCDDC39FF,
            0xFFEB3BFF,
            0xFFC107FF,
            0xFF9800FF,
    };
}