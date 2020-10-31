package org.ntlab.graffiti.common.rendering;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.GLUtils;
import android.opengl.Matrix;

import com.google.ar.core.Camera;
import com.google.ar.core.Plane;
import com.google.ar.core.Pose;
import com.google.ar.core.TrackingState;

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

public class Test {
    private static final String TAG = Test.class.getSimpleName();

    // Shader names.
    private static final String VERTEX_SHADER_NAME = "shaders/test.vert";
    private static final String FRAGMENT_SHADER_NAME = "shaders/test.frag";

    private static final int BYTES_PER_FLOAT = Float.SIZE / 8;
    private static final int BYTES_PER_SHORT = Short.SIZE / 8;
    private static final int COORDS_PER_VERTEX = 3; // x, z, alpha

    private static final int VERTS_PER_BOUNDARY_VERT = 2;
    private static final int INDICES_PER_BOUNDARY_VERT = 3;
    private static final int INITIAL_BUFFER_BOUNDARY_VERTS = 64;

    private static final int INITIAL_VERTEX_BUFFER_SIZE_BYTES =
            BYTES_PER_FLOAT * COORDS_PER_VERTEX * VERTS_PER_BOUNDARY_VERT * INITIAL_BUFFER_BOUNDARY_VERTS;

    private static final int INITIAL_INDEX_BUFFER_SIZE_BYTES =
            BYTES_PER_SHORT
                    * INDICES_PER_BOUNDARY_VERT
                    * INDICES_PER_BOUNDARY_VERT
                    * INITIAL_BUFFER_BOUNDARY_VERTS;

    private static final float FADE_RADIUS_M = 0.25f;

    // texture size
//    private static final float DOTS_PER_METER = 20.0f;
    private static final float DOTS_PER_METER = 0.5f;
//    private static final float EQUILATERAL_TRIANGLE_SCALE = (float) (1 / Math.sqrt(3));

    // Using the "signed distance field" approach to render sharp lines and circles.
    // {dotThreshold, lineThreshold, lineFadeSpeed, occlusionScale}
    // dotThreshold/lineThreshold: red/green intensity above which dots/lines are present
    // lineFadeShrink:  lines will fade in between alpha = 1-(1/lineFadeShrink) and 1.0
    // occlusionShrink: occluded planes will fade out between alpha = 0 and 1/occlusionShrink
    private static final float[] GRID_CONTROL = {0.2f, 0.4f, 2.0f, 1.5f};


    private int planeobjectProgram;
    private final int[] textures = new int[1];

    private int planeobjectXZPositionAlphaAttribute;

    private int planeobjectModelUniform;
    private int planeobjectNormalUniform;
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
    //    private final float[] planeobjectColor = new float[] {255.0f, 255.0f, 0.0f, 0.0f}; // Yeallow
//    private final float[] planeobjectColor = new float[] {0.0f, 0.0f, 0.0f, 0.0f}; //透明？
    private final float[] planeobjectAngleUvMatrix =
            new float[4]; // 2x2 rotation matrix applied to uv coords.

    private final Map<Plane, Integer> planeobjectIndexMap = new HashMap<>();

    public Test() {}

    /**
     * Allocates and initializes OpenGL resources needed by the plane renderer. Must be called on the
     * OpenGL thread, typically in {@link GLSurfaceView.Renderer#onSurfaceCreated(GL10, EGLConfig)}.
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
        Bitmap textureBitmap =
                BitmapFactory.decodeStream(context.getAssets().open(gridDistanceTextureName));

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glGenTextures(textures.length, textures, 0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textures[0]);

        GLES20.glTexParameteri(
                GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR_MIPMAP_LINEAR);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);

//        GLES20.glTexParameteri(
//                GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
//        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
        GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, textureBitmap, 0);
        GLES20.glGenerateMipmap(GLES20.GL_TEXTURE_2D);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);

        ShaderUtil.checkGLError(TAG, "Texture loading");

        planeobjectXZPositionAlphaAttribute = GLES20.glGetAttribLocation(planeobjectProgram, "a_XZPositionAlpha");

        planeobjectModelUniform = GLES20.glGetUniformLocation(planeobjectProgram, "u_Model");
        planeobjectNormalUniform = GLES20.glGetUniformLocation(planeobjectProgram, "u_Normal");
        planeobjectModelViewProjectionUniform =
                GLES20.glGetUniformLocation(planeobjectProgram, "u_ModelViewProjection");
        textureUniform = GLES20.glGetUniformLocation(planeobjectProgram, "u_Texture");
        lineColorUniform = GLES20.glGetUniformLocation(planeobjectProgram, "u_lineColor");
        dotColorUniform = GLES20.glGetUniformLocation(planeobjectProgram, "u_dotColor");
        gridControlUniform = GLES20.glGetUniformLocation(planeobjectProgram, "u_gridControl");
        planeobjectUvMatrixUniform = GLES20.glGetUniformLocation(planeobjectProgram, "u_PlaneUvMatrix");

        ShaderUtil.checkGLError(TAG, "Program parameters");
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

//        planeobjectAngleUvMatrix[0] = xScale;
//        planeobjectAngleUvMatrix[1] = zScale;
//        planeobjectAngleUvMatrix[2] = xScale;
//        planeobjectAngleUvMatrix[3] = zScale;
//        float[] scaleMatrix = new float[16];
//        Matrix.setIdentityM(scaleMatrix, 0);
//        scaleMatrix[0] = 10.1f;
//        scaleMatrix[5] = 10.1f;
//        scaleMatrix[10] = 10.1f;
//        Matrix.multiplyMM(this.modelMatrix, 0, modelMatrix, 0, scaleMatrix, 0);

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
//        indexBuffer.put((short) ((boundaryVertices - 1) * 2));
        for (int i = 0; i < boundaryVertices; ++i) {
//            indexBuffer.put((short) (i * 2));
            indexBuffer.put((short) (i * 2 + 1));
        }
        indexBuffer.put((short) 1);
        // This leaves us on the interior edge of the perimeter between the inset vertices
        // for boundary verts n-1 and 0.

        // step 2, interior:（内部）
//        for (int i = 1; i < boundaryVertices / 2; ++i) {
//            indexBuffer.put((short) ((boundaryVertices - 1 - i) * 2 + 1));
//            indexBuffer.put((short) (i * 2 + 1));
//        }
//        if (boundaryVertices % 2 != 0) {
//            indexBuffer.put((short) ((boundaryVertices / 2) * 2 + 1));
//        }
    }

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
        GLES20.glUniformMatrix4fv(planeobjectModelUniform, 1, false, modelMatrix, 0);
        GLES20.glUniform3f(planeobjectNormalUniform, planeNormal[0], planeNormal[1], planeNormal[2]);
        GLES20.glUniformMatrix4fv(
                planeobjectModelViewProjectionUniform, 1, false, modelViewProjectionMatrix, 0);

        // Draw the Model.
        indexBuffer.rewind();
//        GLES20.glLineWidth(25f);
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
        List<PlaneObjectRenderer.SortablePlaneObject> sortedPlaneObjects = new ArrayList<>();

        for (Plane plane : updateObjectPlanes) {
            if (plane.getTrackingState() != TrackingState.TRACKING || plane.getSubsumedBy() != null) {
                continue;
            }

            float distance = calculateDistanceToPlane(plane.getCenterPose(), cameraPose);
            if (distance < 0) { // Plane is back-facing.
                continue;
            }
            sortedPlaneObjects.add(new PlaneObjectRenderer.SortablePlaneObject(distance, plane));
        }
        Collections.sort(
                sortedPlaneObjects,
                new Comparator<PlaneObjectRenderer.SortablePlaneObject>() {
                    @Override
                    public int compare(PlaneObjectRenderer.SortablePlaneObject a, PlaneObjectRenderer.SortablePlaneObject b) {
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
        GLES20.glBlendFuncSeparate(
                GLES20.GL_DST_ALPHA, GLES20.GL_ONE, // RGB (src, dest)
                GLES20.GL_ZERO, GLES20.GL_ONE_MINUS_SRC_ALPHA); // ALPHA (src, dest)

        // Set up the shader.
        GLES20.glUseProgram(planeobjectProgram);

        // Attach the texture.
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textures[0]);
        GLES20.glUniform1i(textureUniform, 0);

        // Shared fragment uniforms.
        GLES20.glUniform4fv(gridControlUniform, 1, GRID_CONTROL, 0);

        // Enable vertex arrays
        GLES20.glEnableVertexAttribArray(planeobjectXZPositionAlphaAttribute);

        ShaderUtil.checkGLError(TAG, "Setting up to draw planes");

        for (PlaneObjectRenderer.SortablePlaneObject sortedPlaneObject : sortedPlaneObjects) {
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
            }

            // Set plane color. Computed deterministically from the Plane index.
            int colorIndex = planeobjectIndex % PLANE_COLORS_RGBA.length;
            colorRgbaToFloat(planeobjectColor, PLANE_COLORS_RGBA[colorIndex]);
            GLES20.glUniform4fv(lineColorUniform, 1, planeobjectColor, 0);
            GLES20.glUniform4fv(dotColorUniform, 1, planeobjectColor, 0);

            // Each plane will have its own angle offset from others, to make them easier to
            // distinguish. Compute a 2x2 rotation matrix from the angle.
            float angleRadians = planeobjectIndex * 0.1f/*44*/;
            float uScale = DOTS_PER_METER;
            float vScale = DOTS_PER_METER /* EQUILATERAL_TRIANGLE_SCALE*/;
            planeobjectAngleUvMatrix[0] = +(float) Math.cos(angleRadians) * uScale;
            planeobjectAngleUvMatrix[1] = -(float) Math.sin(angleRadians) * vScale;
            planeobjectAngleUvMatrix[2] = +(float) Math.sin(angleRadians) * uScale;
            planeobjectAngleUvMatrix[3] = +(float) Math.cos(angleRadians) * vScale;
//            planeobjectAngleUvMatrix[0] = +(float) Math.cos(angleRadians);
//            planeobjectAngleUvMatrix[1] = -(float) Math.sin(angleRadians);
//            planeobjectAngleUvMatrix[2] = +(float) Math.sin(angleRadians);
//            planeobjectAngleUvMatrix[3] = +(float) Math.cos(angleRadians);
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
