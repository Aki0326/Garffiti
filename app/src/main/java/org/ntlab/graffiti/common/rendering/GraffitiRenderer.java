package org.ntlab.graffiti.common.rendering;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PointF;
import android.opengl.GLES30;
import android.opengl.GLSurfaceView;
import android.opengl.GLUtils;
import android.opengl.Matrix;
import android.util.Log;

import com.google.ar.core.Camera;
import com.google.ar.core.Coordinates2d;
import com.google.ar.core.Frame;
import com.google.ar.core.LightEstimate;
import com.google.ar.core.Plane;
import com.google.ar.core.Pose;
import com.google.ar.core.Session;
import com.google.ar.core.Trackable;
import com.google.ar.core.TrackingState;

import org.ntlab.graffiti.common.drawer.TextureDrawer;
import org.ntlab.graffiti.common.geometry.Vector;
import org.ntlab.graffiti.common.rendering.arcore.SpecularCubemapFilter;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This class renders the AR Graffiti.
 *
 * @author n-nitta, a-hongo
 */
public class GraffitiRenderer {
    private static final String TAG = GraffitiRenderer.class.getSimpleName();

    // Shader names.
    private static final String VERTEX_SHADER_NAME = "shaders/graffiti.vert";
    private static final String FRAGMENT_SHADER_NAME = "shaders/graffiti.frag";

    private static final int BYTES_PER_FLOAT = Float.SIZE / 8;
    private static final int BYTES_PER_INT = Integer.SIZE / 8;
    //    private static final int BYTES_PER_SHORT = Short.SIZE / 8;
    private static final int COORDS_PER_VERTEX = 3; // x, z, alpha

    private static final int VERTS_PER_BOUNDARY_VERT = 2;
    private static final int INDICES_PER_BOUNDARY_VERT = 3;
    private static final int INITIAL_BUFFER_BOUNDARY_VERTS = 64;

    private static final int INITIAL_VERTEX_BUFFER_SIZE_BYTES =
            BYTES_PER_FLOAT * COORDS_PER_VERTEX * VERTS_PER_BOUNDARY_VERT * INITIAL_BUFFER_BOUNDARY_VERTS;

    private static final int INITIAL_INDEX_BUFFER_SIZE_BYTES =
            BYTES_PER_INT
                    * INDICES_PER_BOUNDARY_VERT
                    * INDICES_PER_BOUNDARY_VERT
                    * INITIAL_BUFFER_BOUNDARY_VERTS;
//    private static final int INITIAL_INDEX_BUFFER_SIZE_BYTES =
//            BYTES_PER_SHORT
//                    * INDICES_PER_BOUNDARY_VERT
//                    * INDICES_PER_BOUNDARY_VERT
//                    * INITIAL_BUFFER_BOUNDARY_VERTS;

    private static final float FADE_RADIUS_M = 0.25f;
    // texture size
//    private static final float DOTS_PER_METER = 20.0f;
    private static final float DOTS_PER_METER = 0.2f;

    // Using the "signed distance field" approach to render sharp lines and circles.
    // {dotThreshold, lineThreshold, lineFadeSpeed, occlusionScale}
    // dotThreshold/lineThreshold: red/green intensity above which dots/lines are present
    // lineFadeShrink:  lines will fade in between alpha = 1-(1/lineFadeShrink) and 1.0
    // occlusionShrink: occluded planes will fade out between alpha = 0 and 1/occlusionShrink
    private static final float[] GRID_CONTROL = {0.2f, 0.4f, 2.0f, 1.5f};

    private Mesh mesh;
    private IndexBuffer indexBufferObject;
    private VertexBuffer vertexBufferObject;
//    private Shader shader;
//    private Texture cameraDepthTexture;

    private int planeObjectProgram;

    private int planeObjectXZPositionAlphaAttribute;

    private int planeObjectModelViewUniform;
    private int planeObjectModelViewProjectionUniform;
    private int textureUniform;
//    private int lineColorUniform;
//    private int dotColorUniform;
//    private int gridControlUniform;
    private int planeObjectUvMatrixUniform;

    private int cubeMapUniform;
    private int dfgTextureUniform;
    private int viewInverseUniform;
    private int viewLightDirectionUniform;
    private int lightIntensityUniform;
    private int sphericalHarmonicsCoefficientsUniform;
    private int isLightEstimateUniform;

    // Shader location: depth texture.
    private int depthTextureUniform;
    // Shader location: transform to depth uvs.
    private int depthUvTransformUniform;
    // Shader location: the aspect ratio of the depth texture.
    private int depthAspectRatioUniform;

    private FloatBuffer vertexBuffer =
            ByteBuffer.allocateDirect(INITIAL_VERTEX_BUFFER_SIZE_BYTES)
                    .order(ByteOrder.nativeOrder())
                    .asFloatBuffer();
    private IntBuffer indexBuffer =
            ByteBuffer.allocateDirect(INITIAL_INDEX_BUFFER_SIZE_BYTES)
                    .order(ByteOrder.nativeOrder())
                    .asIntBuffer();
//    private ShortBuffer indexBuffer =
//            ByteBuffer.allocateDirect(INITIAL_INDEX_BUFFER_SIZE_BYTES)
//                    .order(ByteOrder.nativeOrder())
//                    .asShortBuffer();

    // Plane Object vertex buffer variables.
//    private int vertexBufferId;
//    private int verticesBaseAddress;
//    private int indexBufferId;
//    private int indexCount;

    // Environmental HDR
    private Texture dfgTexture;
    private SpecularCubemapFilter cubemapFilter;

    // Temporary lists/matrices allocated here to reduce number of allocations for each frame.
//    private final float[] viewMatrix = new float[16];
    private final float[] modelMatrix = new float[16];
    private final float[] modelViewMatrix = new float[16];
    private final float[] modelViewProjectionMatrix = new float[16];
    private final float[] planeObjectColor = new float[4];
    private final float[] planeObjectAngleUvMatrix = new float[4]; // 2x2 rotation matrix applied to uv coords.
    private final float[] sphericalHarmonicsCoefficients = new float[9 * 3];
    private final float[] viewInverseMatrix = new float[16];
    private final float[] worldLightDirection = {0.0f, 0.0f, 0.0f, 0.0f};
    private final float[] viewLightDirection = new float[4]; // view x world light direction

    // See the definition of updateSphericalHarmonicsCoefficients for an explanation of these
    // constants.
    private static final float[] sphericalHarmonicFactors = {
            0.282095f,
            -0.325735f,
            0.325735f,
            -0.325735f,
            0.273137f,
            -0.273137f,
            0.078848f,
            -0.273137f,
            0.136569f,
    };

    private static final int CUBEMAP_RESOLUTION = 16;
    private static final int CUBEMAP_NUMBER_OF_IMPORTANCE_SAMPLES = 32;

    private final Map<Plane, Integer> planeObjectIndexMap = new HashMap<>();

    private Bitmap textureBitmap = null;
    private Bitmap textureBitmapToRecycle = null;
    private ArrayList<Bitmap> textureBitmaps = new ArrayList<Bitmap>();
    private ArrayList<Integer> textures = new ArrayList<Integer>(); //テキスチャID
    private HashMap<Plane, Integer> planeNo = new HashMap<Plane, Integer>();
    private HashMap<Plane, Pose> planePose = new HashMap<>();
    private final int backgroundColor = Color.TRANSPARENT;      // テクスチャ背景色

    private boolean calculateUVTransform = true;
    private boolean useOcclusion = false;
    private int depthTextureId;
    private float aspectRatio = 0.0f;
    private float[] uvTransform = null;

    public GraffitiRenderer() {
    }

    /**
     * Allocates and initializes OpenGL resources needed by the plane renderer. Must be called on the
     * OpenGL thread, typically in {@link GLSurfaceView.Renderer#onSurfaceCreated(GL10, EGLConfig)}.
     *
     * @param context          Needed to access shader source and texture PNG.
     * @param planeTextureName Name of the PNG file containing the plane texture.
     */
    public void createOnGlThread(Context context, String planeTextureName)
            throws IOException {
        // Read the texture.
        textureBitmap = BitmapFactory.decodeStream(context.getAssets().open(planeTextureName));
        textureBitmapToRecycle = textureBitmap.copy(textureBitmap.getConfig(), true);
//        textureBitmapToRecycle.eraseColor(backgroundColor);
//        ShaderUtil.checkGLError(TAG, "Failed to load texture");

        // Generate the background texture.
//        cameraDepthTexture =
//                new Texture(
//                        GLES30.GL_TEXTURE_2D,
//                        GLES30.GL_CLAMP_TO_EDGE,
//                        /*useMipmaps=*/ false);

            cubemapFilter =
                    new SpecularCubemapFilter(
                            context, CUBEMAP_RESOLUTION, CUBEMAP_NUMBER_OF_IMPORTANCE_SAMPLES);
            // Load DFG lookup table for environmental lighting
            dfgTexture =
                    new Texture(
                            GLES30.GL_TEXTURE_2D,
                            GLES30.GL_CLAMP_TO_EDGE,
                            /*useMipmaps=*/ false);
            // The dfg.raw file is a raw half-float texture with two channels.
            final int dfgResolution = 64;
            final int dfgChannels = 2;
            final int halfFloatSize = 2;

            ByteBuffer buffer =
                    ByteBuffer.allocateDirect(dfgResolution * dfgResolution * dfgChannels * halfFloatSize);
            try (InputStream is = context.getAssets().open("models/dfg.raw")) {
                is.read(buffer.array());
            }
            // Render abstraction leaks here.
            GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, dfgTexture.getTextureId());
            GLError.maybeThrowGLException("Failed to bind DFG texture", "glBindTexture");
            GLES30.glTexImage2D(
                    GLES30.GL_TEXTURE_2D,
                    /*level=*/ 0,
                    GLES30.GL_RG16F,
                    /*width=*/ dfgResolution,
                    /*height=*/ dfgResolution,
                    /*border=*/ 0,
                    GLES30.GL_RG,
                    GLES30.GL_HALF_FLOAT,
                    buffer);
            GLError.maybeThrowGLException("Failed to populate DFG texture", "glTexImage2D");

//        HashMap<String, String> defines = new HashMap<>();
//        defines.put("USE_OCCLUSION", useOcclusion ? "1" : "0");
        // This loads the shader code, and must be called on the GL thread.
//        shader =
//                Shader.createFromAssets(context, VERTEX_SHADER_NAME, FRAGMENT_SHADER_NAME, /*defines=*/ defines)
////                        .setTexture("u_Texture", texture)
////                        .setVec4("u_gridControl", GRID_CONTROL)
////                        .setBlend(
////                                GLES30.GL_DST_ALPHA, // RGB (src)
////                                GLES30.GL_ONE, // RGB (dest)
////                                GLES30.GL_ZERO, // ALPHA (src)
////                                GLES30.GL_ONE_MINUS_SRC_ALPHA) // ALPHA (dest)
////                        .setBlend(
////                                GLES30.GL_SRC_ALPHA, // RGBA (src)
////                                GLES30.GL_ONE_MINUS_SRC_ALPHA) // RGBA (dest)
//                        .setBlend(
//                                GLES30.GL_ONE, // RGBA (src)
//                                GLES30.GL_ONE_MINUS_SRC_ALPHA); // RGBA (dest)
////                        .setDepthWrite(false);
        HashMap<String, Integer> defines = new HashMap<>();
        defines.put("USE_OCCLUSION", useOcclusion ? 1 : 0);
        defines.put("NUMBER_OF_MIPMAP_LEVELS", cubemapFilter.getNumberOfMipmapLevels());

        int vertexShader =
                ShaderUtil.loadGLShader(TAG, context, GLES30.GL_VERTEX_SHADER, VERTEX_SHADER_NAME);
        int fragmentSahder =
                ShaderUtil.loadGLShader(TAG, context, GLES30.GL_FRAGMENT_SHADER, FRAGMENT_SHADER_NAME, defines);

        planeObjectProgram = GLES30.glCreateProgram();
        GLES30.glAttachShader(planeObjectProgram, vertexShader);
        GLES30.glAttachShader(planeObjectProgram, fragmentSahder);
        GLES30.glLinkProgram(planeObjectProgram);
        GLES30.glUseProgram(planeObjectProgram);
        ShaderUtil.checkGLError(TAG, "Program creation");

        planeObjectXZPositionAlphaAttribute = GLES30.glGetAttribLocation(planeObjectProgram, "a_XZPositionAlpha");

        planeObjectModelViewUniform =
                GLES30.glGetUniformLocation(planeObjectProgram, "u_ModelView");
        planeObjectModelViewProjectionUniform =
                GLES30.glGetUniformLocation(planeObjectProgram, "u_ModelViewProjection");
        planeObjectUvMatrixUniform = GLES30.glGetUniformLocation(planeObjectProgram, "u_PlaneUvMatrix");

        textureUniform = GLES30.glGetUniformLocation(planeObjectProgram, "u_Texture");
//        lineColorUniform = GLES30.glGetUniformLocation(planeObjectProgram, "u_lineColor");
//        dotColorUniform = GLES30.glGetUniformLocation(planeObjectProgram, "u_dotColor");
//        gridControlUniform = GLES30.glGetUniformLocation(planeObjectProgram, "u_gridControl");

        lightIntensityUniform = GLES30.glGetUniformLocation(planeObjectProgram, "u_LightIntensity");
        viewInverseUniform = GLES30.glGetUniformLocation(planeObjectProgram, "u_ViewInverse");
        viewLightDirectionUniform = GLES30.glGetUniformLocation(planeObjectProgram, "u_ViewLightDirection");
        sphericalHarmonicsCoefficientsUniform = GLES30.glGetUniformLocation(planeObjectProgram, "u_SphericalHarmonicsCoefficients");
        cubeMapUniform = GLES30.glGetUniformLocation(planeObjectProgram, "u_Cubemap");
        dfgTextureUniform = GLES30.glGetUniformLocation(planeObjectProgram, "u_DfgTexture");
        isLightEstimateUniform = GLES30.glGetUniformLocation(planeObjectProgram, "u_LightEstimateIsValid");
        ShaderUtil.checkGLError(TAG, "Program parameters");

        indexBufferObject = new IndexBuffer(/*entries=*/ null);
        vertexBufferObject = new VertexBuffer(COORDS_PER_VERTEX, /*entries=*/ null);
        VertexBuffer[] vertexBuffers = {vertexBufferObject};
        mesh = new Mesh(GLES30.GL_TRIANGLE_FAN, indexBufferObject, vertexBuffers);

//        int[] buffers = new int[2];
//        GLES30.glGenBuffers(2, buffers, 0);
//        vertexBufferId = buffers[0];
//        indexBufferId = buffers[1];
    }

    /**
     * Sets whether to use depth for occlusion. This reloads the shader code with new {@code
     * #define}s, and must be called on the GL thread.
     */
    public void setUseOcclusion(Context context, boolean useOcclusion) throws IOException {
//        if (shader != null) {
        if (this.useOcclusion == useOcclusion) {
            return;
        }
//            shader.close();
//            shader = null;
//        }
//        HashMap<String, String> defines = new HashMap<>();
//        defines.put("USE_OCCLUSION", useOcclusion ? "1" : "0");
//        // This loads the shader code, and must be called on the GL thread.
//        shader =
//                Shader.createFromAssets(context, VERTEX_SHADER_NAME, FRAGMENT_SHADER_NAME, /*defines=*/ defines)
////                        .setBlend(
////                                GLES30.GL_SRC_ALPHA, // RGBA (src)
////                                GLES30.GL_ONE_MINUS_SRC_ALPHA); // RGBA (dest)
////                        .setBlend(
////                                GLES30.GL_ONE, // RGBA (src)
////                                GLES30.GL_ONE_MINUS_SRC_ALPHA); // RGBA (dest)
//                        .setBlend(
//                                GLES30.GL_ONE, // RGB (src)
//                                GLES30.GL_ONE_MINUS_SRC_ALPHA, // RGB (dest)
//                                GLES30.GL_ONE, // ALPHA (src)
//                                GLES30.GL_ONE_MINUS_SRC_ALPHA); // ALPHA (dest)
////                        .setDepthWrite(false);
        HashMap<String, Integer> defines = new HashMap<>();
        defines.put("USE_OCCLUSION", useOcclusion ? 1 : 0);
        defines.put("NUMBER_OF_MIPMAP_LEVELS", cubemapFilter.getNumberOfMipmapLevels());

        int vertexShader =
                ShaderUtil.loadGLShader(TAG, context, GLES30.GL_VERTEX_SHADER, VERTEX_SHADER_NAME);
        int fragmentSahder =
                ShaderUtil.loadGLShader(TAG, context, GLES30.GL_FRAGMENT_SHADER, FRAGMENT_SHADER_NAME, defines);

        planeObjectProgram = GLES30.glCreateProgram();
        GLES30.glAttachShader(planeObjectProgram, vertexShader);
        GLES30.glAttachShader(planeObjectProgram, fragmentSahder);
        GLES30.glLinkProgram(planeObjectProgram);
        GLES30.glUseProgram(planeObjectProgram);
        ShaderUtil.checkGLError(TAG, "Program creation");

        planeObjectXZPositionAlphaAttribute = GLES30.glGetAttribLocation(planeObjectProgram, "a_XZPositionAlpha");

        planeObjectModelViewUniform =
                GLES30.glGetUniformLocation(planeObjectProgram, "u_ModelView");
        planeObjectModelViewProjectionUniform =
                GLES30.glGetUniformLocation(planeObjectProgram, "u_ModelViewProjection");
        planeObjectUvMatrixUniform = GLES30.glGetUniformLocation(planeObjectProgram, "u_PlaneUvMatrix");

        textureUniform = GLES30.glGetUniformLocation(planeObjectProgram, "u_Texture");

        cubeMapUniform = GLES30.glGetUniformLocation(planeObjectProgram, "u_Cubemap");
        dfgTextureUniform = GLES30.glGetUniformLocation(planeObjectProgram, "u_DfgTexture");
        viewInverseUniform = GLES30.glGetUniformLocation(planeObjectProgram, "u_ViewInverse");
        viewLightDirectionUniform = GLES30.glGetUniformLocation(planeObjectProgram, "u_ViewLightDirection");
        lightIntensityUniform = GLES30.glGetUniformLocation(planeObjectProgram, "u_LightIntensity");
        sphericalHarmonicsCoefficientsUniform = GLES30.glGetUniformLocation(planeObjectProgram, "u_SphericalHarmonicsCoefficients");
        isLightEstimateUniform = GLES30.glGetUniformLocation(planeObjectProgram, "u_LightEstimateIsValid");
        ShaderUtil.checkGLError(TAG, "Program parameters");
        if (useOcclusion) {
//            shader
//                    .setTexture("u_CameraDepthTexture", cameraDepthTexture)
//                    .setFloat("u_DepthAspectRatio", aspectRatio);
            depthTextureUniform = GLES30.glGetUniformLocation(planeObjectProgram, "u_CameraDepthTexture");
            depthUvTransformUniform = GLES30.glGetUniformLocation(planeObjectProgram, "u_DepthUvTransform");
            depthAspectRatioUniform = GLES30.glGetUniformLocation(planeObjectProgram, "u_DepthAspectRatio");
            ShaderUtil.checkGLError(TAG, "Program parameters");
        }
        this.useOcclusion = useOcclusion;
    }

    private void setUvTransformMatrix(float[] transform) {
        this.uvTransform = transform;
    }

    public void setDepthTexture(int textureId, int width, int height) {
        depthTextureId = textureId;
        if (useOcclusion) {
            aspectRatio = (float) width / (float) height;
        }
    }

    /**
     * Returns a transformation matrix that when applied to screen space uvs makes them match
     * correctly with the quad texture coords used to render the camera feed. It takes into account
     * device orientation.
     */
    private float[] getTextureTransformMatrix(Frame frame) {
        float[] frameTransform = new float[6];
        float[] uvTransform = new float[9];
        // XY pairs of coordinates in NDC space that constitute the origin and points along the two
        // principal axes.
        float[] ndcBasis = {0, 0, 1, 0, 0, 1};
//        float[] ndcBasis = {0, 0, 0, 1, 1, 0};

        // Temporarily store the transformed points into outputTransform.
        frame.transformCoordinates2d(
                Coordinates2d.OPENGL_NORMALIZED_DEVICE_COORDINATES,
                ndcBasis,
                Coordinates2d.TEXTURE_NORMALIZED,
                frameTransform);

        // Convert the transformed points into an affine transform and transpose it.
        float ndcOriginX = frameTransform[0];
        float ndcOriginY = frameTransform[1];
        uvTransform[0] = frameTransform[2] - ndcOriginX;
        uvTransform[1] = frameTransform[3] - ndcOriginY;
        uvTransform[2] = 0;
        uvTransform[3] = frameTransform[4] - ndcOriginX;
        uvTransform[4] = frameTransform[5] - ndcOriginY;
        uvTransform[5] = 0;
        uvTransform[6] = ndcOriginX;
        uvTransform[7] = ndcOriginY;
        uvTransform[8] = 1;

        return uvTransform;
    }

    public long getTotalColorArea(int color) {
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

    /**
     * Updates the display geometry. This must be called every frame before calling either of
     * GraffitiRenderer's draw methods.
     *
     * @param frame The current {@code Frame} as returned by {@link Session#update()}.
     */
    public void updateDisplayGeometry(Frame frame) {
        if (frame.hasDisplayGeometryChanged() || calculateUVTransform) {
            // If display rotation changed (also includes view size change), we need to re-query the UV
            // coordinates for the screen rect, as they may have changed as well.
            calculateUVTransform = false;
            setUvTransformMatrix(getTextureTransformMatrix(frame));
        }
    }

    /**
     * Update depth texture with Image contents.
     */
//    public void updateCameraDepthTexture(Image image) {
//        // SampleRender abstraction leaks here
//        GLES30.glActiveTexture(GLES30.GL_TEXTURE1);
//        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, cameraDepthTexture.getTextureId());
//        GLES30.glTexImage2D(
//                GLES30.GL_TEXTURE_2D,
//                0,
//                GLES30.GL_RG8,
//                image.getWidth(),
//                image.getHeight(),
//                0,
//                GLES30.GL_RG,
//                GLES30.GL_UNSIGNED_BYTE,
//                image.getPlanes()[0].getBuffer());
//        if (useOcclusion) {
//            aspectRatio = (float) image.getWidth() / (float) image.getHeight();
//        }
//    }

    /**
     * Updates the plane model transform matrix and extents.
     */
    public void updatePlaneObjectParamaters(
            float[] planeobjectMatrix, float extentX, float extentZ, FloatBuffer boundary) {
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

//        // Load vertex buffer
//        verticesBaseAddress = 0;
//        final int totalBytes = verticesBaseAddress + 4 * vertexBuffer.limit();

//        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, vertexBufferId);
//        GLES30.glBufferData(GLES30.GL_ARRAY_BUFFER, totalBytes, null, GLES30.GL_STATIC_DRAW);
//        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, 0);

        if (indexBuffer.capacity() < numIndices) {
            int size = indexBuffer.capacity();
            while (size < numIndices) {
                size *= 2;
            }
//            indexBuffer =
//                    ByteBuffer.allocateDirect(BYTES_PER_SHORT * size)
//                            .order(ByteOrder.nativeOrder())
//                            .asShortBuffer();
            indexBuffer =
                    ByteBuffer.allocateDirect(BYTES_PER_INT * size)
                            .order(ByteOrder.nativeOrder())
                            .asIntBuffer();
        }
        indexBuffer.rewind();
        indexBuffer.limit(numIndices);

//        // Load index buffer
//        GLES30.glBindBuffer(GLES30.GL_ELEMENT_ARRAY_BUFFER, indexBufferId);
//        indexCount = indexBuffer.limit();
//        GLES30.glBufferData(
//                GLES30.GL_ELEMENT_ARRAY_BUFFER, 2 * indexCount, indexBuffer, GLES30.GL_STATIC_DRAW);
//        GLES30.glBindBuffer(GLES30.GL_ELEMENT_ARRAY_BUFFER, 0);

        // Note: when either dimension of the bounding box is smaller than 2*FADE_RADIUS_M we
        // generate a bunch of 0-area triangles.  These don't get rendered though so it works
        // out ok.
//        float xScale = Math.max((extentX - 2 * FADE_RADIUS_M) / extentX, 0.0f);
//        float zScale = Math.max((extentZ - 2 * FADE_RADIUS_M) / extentZ, 0.0f);
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

    /**
     * Draw the texture.
     *
     * @param x
     * @param y
     * @param r
     * @param trackable
     * @param drawer
     */
    public PointF drawTexture(float x, float y, int r, Trackable trackable, TextureDrawer drawer) {
        if (textureBitmap != null) {
            Integer hitPlaneObjectTextureNo = planeNo.get(trackable);

            Log.d(TAG, "allPlaneNumSize: " + textureBitmaps.size() + ", hitNo. " + hitPlaneObjectTextureNo);
            if (hitPlaneObjectTextureNo == null || textureBitmaps.size() <= hitPlaneObjectTextureNo)
                return null;
            int w = textureBitmaps.get(hitPlaneObjectTextureNo).getWidth();
            int h = textureBitmaps.get(hitPlaneObjectTextureNo).getHeight();
            int pixelX = (int) ((x * DOTS_PER_METER + 0.5) * w);
            int pixelY = (int) ((y * DOTS_PER_METER + 0.5) * h);
            pixelX = Math.floorMod(pixelX, w);
            pixelY = Math.floorMod(pixelY, h);

            Bitmap bitmap = textureBitmaps.get(hitPlaneObjectTextureNo);
            Canvas canvas = new Canvas(bitmap);
            Bitmap miniBitmap;

            drawer.draw(pixelX, pixelY, r, canvas);

//            if(color == Color.TRANSPARENT) {
////                paint.setColor(Color.TRANSPARENT);
//                paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
//                paint.setStyle(Paint.Style.FILL);
//                canvas.drawCircle(pixelX, pixelY, r, paint);
//            } else {
//                paint.setColor(color);
//                paint.setStyle(Paint.Style.FILL);
//                canvas.drawCircle(pixelX, pixelY, r, paint);
//            }
            miniBitmap = Bitmap.createBitmap(bitmap, pixelX - r, pixelY - r, r * 2, r * 2);

            GLES30.glActiveTexture(GLES30.GL_TEXTURE0);
            GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, textures.get(hitPlaneObjectTextureNo));
            GLUtils.texSubImage2D(GLES30.GL_TEXTURE_2D, 0, pixelX - r, pixelY - r, miniBitmap, GLES30.GL_RGBA, GLES30.GL_UNSIGNED_BYTE);
            GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, 0);
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
//        Log.d("pose","camera:" + cameraPose.tx() + "," + cameraPose.ty() + "," + cameraPose.tz() + ","
//                + cameraPose.getXAxis()[0] +","  + cameraPose.getXAxis()[1] +","  + cameraPose.getXAxis()[2] +","
//                + cameraPose.getYAxis()[0] +","  + cameraPose.getYAxis()[1] +","  + cameraPose.getYAxis()[2]);

        for (Plane p : frame.getUpdatedTrackables(Plane.class)) {
            Pose newPose = p.getCenterPose();
            Pose oldPose = planePose.get(p);

//            Log.d("pose","plane:" + p + "," + newPose.tx() + "," + newPose.ty() + "," + newPose.tz() + ","
//                    + newPose.getXAxis()[0] +","  + newPose.getXAxis()[1] +","  + newPose.getXAxis()[2] +","
//                    + newPose.getYAxis()[0] +","  + newPose.getYAxis()[1] +","  + newPose.getYAxis()[2]);

            // 1. テクスチャを平面上で回転＆移動させるための行列の作成
            if (!planeNo.containsKey(p)) return;
            Bitmap bitmap = textureBitmaps.get(planeNo.get(p));
//            textureBitmapToRecycle.eraseColor(backgroundColor);
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

//            Log.d("pose","adjust:" + theta + "," + pixelTransX + "," + pixelTransY);

            // 2. 適用
            canvas.drawBitmap(bitmap, adjustMatrix, new Paint());

            // 3. 転送
            GLES30.glActiveTexture(GLES30.GL_TEXTURE0);
            GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, textures.get(planeNo.get(p)));
            GLUtils.texImage2D(GLES30.GL_TEXTURE_2D, 0, textureBitmapToRecycle, 0);
            GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, 0);

            textureBitmaps.set(planeNo.get(p), textureBitmapToRecycle);
            textureBitmapToRecycle = bitmap;        //  リサイクルに回す
            planePose.put(p, newPose);
        }
    }

    /**
     * Draw the Graffiti.
     *
     * @param cameraView
     * @param cameraPerspective
     * @param planeNormal
     */
    private void draw(float[] cameraView, float[] cameraPerspective, float[] planeNormal) {
        // Build the ModelView and ModelViewProjection matrices
        // for calculating cube position and light.
//        Matrix.multiplyMM(modelViewMatrix, 0, viewMatrix, 0, modelMatrix, 0);
        Matrix.multiplyMM(modelViewMatrix, 0, cameraView, 0, modelMatrix, 0);
        Matrix.multiplyMM(modelViewProjectionMatrix, 0, cameraPerspective, 0, modelViewMatrix, 0);

        // Populate the shader uniforms for this frame.
//        shader.setMat4("u_ModelView", modelViewMatrix);
//        shader.setMat4("u_ModelViewProjection", modelViewProjectionMatrix);
//        shader.setMat2("u_PlaneUvMatrix", planeObjectAngleUvMatrix);
//        shader.setVec3("u_Normal", normalVector);

//        // Set the vertex attributes.
//        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, vertexBufferId);

        // Enable vertex arrays
//        GLES30.glEnableVertexAttribArray(planeObjectXZPositionAlphaAttribute);
//        GLError.maybeThrowGLException("Failed to enable vertex attribute array", "glEnableVertexAttribArray");

        // Set the position of the plane
        vertexBuffer.rewind();

//        GLES30.glVertexAttribPointer(
//                planeObjectXZPositionAlphaAttribute,
//                COORDS_PER_VERTEX,
//                GLES30.GL_FLOAT,
//                false,
//                BYTES_PER_FLOAT * COORDS_PER_VERTEX,
//                vertexBuffer);
//        GLES30.glVertexAttribPointer(
//                planeObjectXZPositionAlphaAttribute,
//                COORDS_PER_VERTEX,
//                GLES30.GL_FLOAT,
//                false,
//                BYTES_PER_FLOAT * COORDS_PER_VERTEX,
//                verticesBaseAddress);
//        GLError.maybeThrowGLException("Failed to glVertexAttribPointer", "glVertexAttribPointer");

//        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, 0);
//        GLError.maybeThrowGLException("Failed to glBindBuffer", "glBindBuffer");

        // Set the Model and ModelViewProjection matrices in the shader.
        GLES30.glUniformMatrix4fv(planeObjectModelViewUniform, 1, false, modelViewMatrix, 0);
        GLError.maybeThrowGLException("Failed to set shader texture uniform", "glUniformMatrix4fv");
        GLES30.glUniformMatrix4fv(
                planeObjectModelViewProjectionUniform, 1, false, modelViewProjectionMatrix, 0);
        GLError.maybeThrowGLException("Failed to set shader texture uniform", "glUniformMatrix4fv");

        // Draw the Model.
        indexBuffer.rewind();
////        GLES30.glBindBuffer(GLES30.GL_ELEMENT_ARRAY_BUFFER, indexBufferId);
////        GLES30.glDrawElements(
////                GLES30.GL_TRIANGLE_FAN, indexCount, GLES30.GL_UNSIGNED_SHORT, 0);
//        GLES30.glDrawElements(
//                GLES30.GL_TRIANGLE_FAN, indexBuffer.limit(), GLES30.GL_UNSIGNED_INT, indexBuffer);
//        GLError.maybeThrowGLException("Failed to glDrawElements", "glDrawElements");
////        GLES30.glBindBuffer(GLES30.GL_ELEMENT_ARRAY_BUFFER, 0);
//        ShaderUtil.checkGLError(TAG, "Drawing plane");

        // Set the position of the plane
        vertexBufferObject.set(vertexBuffer);
        indexBufferObject.set(indexBuffer);

//        shader.lowLevelUse();
        mesh.lowLevelDraw();

//        GLES30.glDisableVertexAttribArray(planeObjectXZPositionAlphaAttribute);
//        GLError.maybeThrowGLException("Failed to disable vertex attribute array", "glDisableVertexAttribArray");

    }

    static class SortablePlane {
        final float distance;
        final Plane plane;

        SortablePlane(float distance, Plane plane) {
            this.distance = distance;
            this.plane = plane;
        }
    }

    /**
     * Draws the collection of tracked planes, with closer planes hiding more distant ones.
     *
     * @param updatePlanes      The collection of planes to draw.
     * @param cameraPose        The pose of the camera, as returned by {@link Camera#getPose()}
     * @param cameraPerspective The projection matrix, as returned by {@link
     *                          Camera#getProjectionMatrix(float[], int, float, float)}
     */
    public void draw(Collection<Plane> updatePlanes, Pose cameraPose, float[] cameraPerspective) {
        // Planes must be sorted by distance from camera so that we draw closer planes first, and
        // they occlude the farther planes.
        List<SortablePlane> sortedPlanes = new ArrayList<>();

        for (Plane plane : updatePlanes) {
            if (plane.getTrackingState() != TrackingState.TRACKING /*|| plane.getSubsumedBy() != null*/) {
                continue;
            }

            float distance = calculateDistanceToPlane(plane.getCenterPose(), cameraPose);
            if (distance < 0) { // PlaneJSON is back-facing.
                continue;
            }
            sortedPlanes.add(new SortablePlane(distance, plane));
        }
        Collections.sort(
                sortedPlanes,
                new Comparator<SortablePlane>() {
                    @Override
                    public int compare(SortablePlane a, SortablePlane b) {
                        return Float.compare(a.distance, b.distance);
                    }
                });

        float[] cameraView = new float[16];
        cameraPose.inverse().toMatrix(cameraView, 0);

        // Planes are drawn with additive blending, masked by the alpha channel for occlusion.

//        if (!sortedPlaneObjects.isEmpty()) {
        // Start by clearing the alpha channel of the color buffer to 1.0.
//        GLES30.glClearColor(1, 1, 1, 1);
//        GLError.maybeThrowGLException("Failed to set clear color", "glClearColor");
        GLES30.glColorMask(false, false, false, true);
        GLError.maybeThrowGLException("Failed to set color mask", "glColorMask");
        GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT);
        GLError.maybeThrowGLException("Failed to clear framebuffer", "glClear");
        GLES30.glColorMask(true, true, true, true);
        GLError.maybeThrowGLException("Failed to set color mask", "glColorMask");

        // Disable depth write.
//        GLES30.glDepthMask(false);
//        GLError.maybeThrowGLException("Failed to set depth write mask", "glDepthMask");

        GLES30.glDepthMask(true);
        GLError.maybeThrowGLException("Failed to set depth write mask", "glDepthMask");

//         Additive blending, masked by alpha channel, clearing alpha channel.
        GLES30.glEnable(GLES30.GL_BLEND);
        GLError.maybeThrowGLException("Failed to enable blending", "glEnable");
//        GLES30.glBlendFunc(GLES30.GL_SRC_ALPHA, GLES30.GL_ONE_MINUS_SRC_ALPHA);
        GLES30.glBlendFunc(GLES30.GL_ONE, GLES30.GL_ONE_MINUS_SRC_ALPHA);
        GLError.maybeThrowGLException("Failed to set blend mode", "glBlendFunc");

//        GLES30.glBlendFuncSeparate(
//                GLES30.GL_DST_ALPHA, GLES30.GL_ONE, // RGB (src, dest)
//                GLES30.GL_ZERO, GLES30.GL_ONE_MINUS_SRC_ALPHA); // ALPHA (src, dest)

        // Set up the shader.
        GLES30.glUseProgram(planeObjectProgram);
        GLError.maybeThrowGLException("Failed to use program", "glUseProgram");

//        drawCircle((int)(Math.random() * textureBitmap.getWidth()), (int)(Math.random() * textureBitmap.getHeight()), Color.BLUE);

        // Shared fragment uniforms.
//            GLES30.glUniform4fv(gridControlUniform, 1, GRID_CONTROL, 0);
//            GLError.maybeThrowGLException("Failed to set shader uniform 4f", "glUniform4fv");

        for (SortablePlane sortedPlaneObject : sortedPlanes) {
            Plane plane = sortedPlaneObject.plane;
            float[] planeMatrix = new float[16];
            plane.getCenterPose().toMatrix(planeMatrix, 0);

            float[] normal = new float[3];
            // Get transformed Y axis of plane's coordinate system.
            plane.getCenterPose().getTransformedAxis(1, 1.0f, normal, 0);

            updatePlaneObjectParamaters(
                    planeMatrix, plane.getExtentX(), plane.getExtentZ(), plane.getPolygon());

            // Get plane index. Keep a map to assign same indices to same planes.
            Integer planeobjectIndex = planeObjectIndexMap.get(plane);
            if (planeobjectIndex == null) {
                planeobjectIndex = planeObjectIndexMap.size();
                planeObjectIndexMap.put(plane, planeobjectIndex);

                planeNo.put(plane, planeobjectIndex);
                planePose.put(plane, plane.getCenterPose());

                GLES30.glActiveTexture(GLES30.GL_TEXTURE0);
                GLError.maybeThrowGLException("Failed to active texture GL_TEXTURE0", "glActiveTexture");

                int textureArray[] = new int[1];

                GLES30.glGenTextures(textureArray.length, textureArray, 0);
                GLError.maybeThrowGLException("Failed to generate textures", "glGenTextures");
                textures.add(textureArray[0]);
                GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, textures.get(planeobjectIndex));
                GLError.maybeThrowGLException("Failed to bind texture", "glBindTexture");
                GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_LINEAR_MIPMAP_LINEAR);
                GLError.maybeThrowGLException("Failed to set texture parameter", "glTexParameteri");
                GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_LINEAR);
                GLError.maybeThrowGLException("Failed to set texture parameter", "glTexParameteri");

                Bitmap tmp = textureBitmap.copy(textureBitmap.getConfig(), true);
//                tmp.eraseColor(backgroundColor);
                textureBitmaps.add(tmp);
                GLUtils.texImage2D(GLES30.GL_TEXTURE_2D, 0, textureBitmaps.get(planeobjectIndex), 0);
                GLError.maybeThrowGLException("Failed to specify color texture format", "glTexImage2D");
                GLES30.glGenerateMipmap(GLES30.GL_TEXTURE_2D);
                GLError.maybeThrowGLException("Failed to generate mipmap", "glGenerateMipmap");
                GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, 0);
                GLError.maybeThrowGLException("Failed to bind texture", "glBindTexture");

                // Occlusion parameters.
                if (useOcclusion) {
                    // Attach the depth texture.
                    GLES30.glActiveTexture(GLES30.GL_TEXTURE1);
                    GLError.maybeThrowGLException("Failed to active texture GL_TEXTURE1", "glActiveTexture");
                    GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, depthTextureId);
                    GLError.maybeThrowGLException("Failed to bind texture", "glBindTexture");

//                    GLES30.glActiveTexture(GLES30.GL_TEXTURE2);
//                    GLError.maybeThrowGLException("Failed to active texture GL_TEXTURE2", "glActiveTexture");
//                    GLES30.glBindTexture(GLES30.GL_TEXTURE_CUBE_MAP, cubemapFilter.getFilteredCubemapTexture().getTextureId());
//                    GLError.maybeThrowGLException("Failed to bind texture", "glBindTexture");
//                    GLES30.glActiveTexture(GLES30.GL_TEXTURE3);
//                    GLError.maybeThrowGLException("Failed to active texture GL_TEXTURE3", "glActiveTexture");
//                    GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, dfgTexture.getTextureId());
//                    GLError.maybeThrowGLException("Failed to bind texture", "glBindTexture");
                }
            }

            // Attach the texture.
            GLES30.glActiveTexture(GLES30.GL_TEXTURE0);
            GLError.maybeThrowGLException("Failed to active texture GL_TEXTURE0", "glActiveTexture");
            GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, textures.get(planeobjectIndex));
            GLError.maybeThrowGLException("Failed to bind texture", "glBindTexture");
            GLES30.glUniform1i(textureUniform, 0);
            GLError.maybeThrowGLException("Failed to set shader texture uniform", "glUniform1i");

            // Occlusion parameters.
            if (useOcclusion) {
                // Attach the depth texture.
                GLES30.glActiveTexture(GLES30.GL_TEXTURE1);
                GLError.maybeThrowGLException("Failed to active texture GL_TEXTURE1", "glActiveTexture");
                GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, depthTextureId);
                GLError.maybeThrowGLException("Failed to bind texture", "glBindTexture");
                GLES30.glUniform1i(depthTextureUniform, 1);
                GLError.maybeThrowGLException("Failed to set shader texture uniform", "glUniform1i");

                // Set the depth texture uv transform.
                GLES30.glUniform1f(depthAspectRatioUniform, aspectRatio);
                GLError.maybeThrowGLException("Failed to set shader texture uniform", "glUniform1f");
                GLES30.glUniformMatrix3fv(depthUvTransformUniform, 1, false, uvTransform, 0);
                GLError.maybeThrowGLException("Failed to set shader texture uniform", "glUniformMatrix3fv");

//                GLES30.glActiveTexture(GLES30.GL_TEXTURE2);
//                GLError.maybeThrowGLException("Failed to active texture GL_TEXTURE2", "glActiveTexture");
//                GLES30.glBindTexture(GLES30.GL_TEXTURE_CUBE_MAP, cubemapFilter.getFilteredCubemapTexture().getTextureId());
//                GLError.maybeThrowGLException("Failed to bind texture", "glBindTexture");
//                GLES30.glUniform1i(cubeMapUniform, 1);
//                GLError.maybeThrowGLException("Failed to set shader texture uniform", "glUniform1i");
//                GLES30.glActiveTexture(GLES30.GL_TEXTURE3);
//                GLError.maybeThrowGLException("Failed to active texture GL_TEXTURE3", "glActiveTexture");
//                GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, dfgTexture.getTextureId());
//                GLError.maybeThrowGLException("Failed to bind texture", "glBindTexture");
//                GLES30.glUniform1i(dfgTextureUniform, 1);
//                GLError.maybeThrowGLException("Failed to set shader texture uniform", "glUniform1i");
            }

            // Set plane color. Computed deterministically from the PlaneJSON index.
            int colorIndex = planeobjectIndex % PLANE_COLORS_RGBA.length;
            colorRgbaToFloat(planeObjectColor, PLANE_COLORS_RGBA[colorIndex]);
//                GLES30.glUniform4fv(lineColorUniform, 1, planeObjectColor, 0);
//                GLES30.glUniform4fv(dotColorUniform, 1, planeObjectColor, 0);

            // Each plane will have its own angle offset from others, to make them easier to
            // distinguish. Compute a 2x2 rotation matrix from the angle.
            float angleRadians = 0.0f;   //planeobjectIndex * 0.1f/*44*/;
            float uScale = DOTS_PER_METER;
            float vScale = DOTS_PER_METER /* EQUILATERAL_TRIANGLE_SCALE*/;
            planeObjectAngleUvMatrix[0] = +(float) Math.cos(angleRadians) * uScale;
            planeObjectAngleUvMatrix[1] = -(float) Math.sin(angleRadians) * vScale;
            planeObjectAngleUvMatrix[2] = +(float) Math.sin(angleRadians) * uScale;
            planeObjectAngleUvMatrix[3] = +(float) Math.cos(angleRadians) * vScale;

            GLES30.glUniformMatrix2fv(planeObjectUvMatrixUniform, 1, false, planeObjectAngleUvMatrix, 0);
            draw(cameraView, cameraPerspective, normal);
        }

        GLES30.glDisable(GLES30.GL_BLEND);
        GLError.maybeThrowGLException("Failed to disable blending", "glDisable");

//        GLES30.glDepthMask(true);
//        GLError.maybeThrowGLException("Failed to set depth write mask", "glDepthMask");

        // Clean up the state we set
//        GLES30.glClearColor(0f, 0f, 0f, 1f);
//        GLError.maybeThrowGLException("Failed to set clear color", "glClearColor");
//        }
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

    /**
     * Update state based on the current frame's light estimation.
     */
    public void updateLightEstimation(LightEstimate lightEstimate, float[] viewMatrix) {
        // Set up the shader.
        GLES30.glUseProgram(planeObjectProgram);
        GLError.maybeThrowGLException("Failed to use program", "glUseProgram");

        if (lightEstimate.getState() != LightEstimate.State.VALID) {
//            shader.setBool("u_LightEstimateIsValid", false);
            GLES30.glUniform1ui(isLightEstimateUniform, /*false*/0);
            GLError.maybeThrowGLException("Failed to set shader texture uniform", "glUniform1ui");
            return;
        }
//        shader.setBool("u_LightEstimateIsValid", true);
        GLES30.glUniform1ui(isLightEstimateUniform, /*true*/1);
        GLError.maybeThrowGLException("Failed to set shader texture uniform", "glUniform1ui");

        android.opengl.Matrix.invertM(viewInverseMatrix, 0, viewMatrix, 0);
//        shader.setMat4("u_ViewInverse", viewInverseMatrix);
        GLES30.glUniformMatrix4fv(viewInverseUniform, 1, false, viewInverseMatrix, 0);
        GLError.maybeThrowGLException("Failed to set shader texture uniform", "glUniformMatrix4fv");

        updateMainLight(
                lightEstimate.getEnvironmentalHdrMainLightDirection(),
                lightEstimate.getEnvironmentalHdrMainLightIntensity(),
                viewMatrix);
        updateSphericalHarmonicsCoefficients(
                lightEstimate.getEnvironmentalHdrAmbientSphericalHarmonics());
        cubemapFilter.update(lightEstimate.acquireEnvironmentalHdrCubeMap());
    }

    private void updateMainLight(float[] direction, float[] intensity, float[] viewMatrix) {
        // We need the direction in a vec4 with 0.0 as the final component to transform it to view space
        worldLightDirection[0] = direction[0];
        worldLightDirection[1] = direction[1];
        worldLightDirection[2] = direction[2];
        android.opengl.Matrix.multiplyMV(viewLightDirection, 0, viewMatrix, 0, worldLightDirection, 0);
//        shader.setVec4("u_ViewLightDirection", viewLightDirection);
        GLES30.glUniform4fv(viewLightDirectionUniform, 1, viewLightDirection, 0);
        GLError.maybeThrowGLException("Failed to set shader texture uniform", "glUniform4fv");
//        shader.setVec3("u_LightIntensity", intensity);
        GLES30.glUniform3fv(lightIntensityUniform, 1, intensity, 0);
        GLError.maybeThrowGLException("Failed to set shader texture uniform", "glUniform3fv");
    }

    private void updateSphericalHarmonicsCoefficients(float[] coefficients) {
        // Pre-multiply the spherical harmonics coefficients before passing them to the shader. The
        // constants in sphericalHarmonicFactors were derived from three terms:
        //
        // 1. The normalized spherical harmonics basis functions (y_lm)
        //
        // 2. The lambertian diffuse BRDF factor (1/pi)
        //
        // 3. A <cos> convolution. This is done to so that the resulting function outputs the irradiance
        // of all incoming light over a hemisphere for a given surface normal, which is what the shader
        // (environmental_hdr.frag) expects.
        //
        // You can read more details about the math here:
        // https://google.github.io/filament/Filament.html#annex/sphericalharmonics

        if (coefficients.length != 9 * 3) {
            throw new IllegalArgumentException(
                    "The given coefficients array must be of length 27 (3 components per 9 coefficients");
        }

        // Apply each factor to every component of each coefficient
        for (int i = 0; i < 9 * 3; ++i) {
            sphericalHarmonicsCoefficients[i] = coefficients[i] * sphericalHarmonicFactors[i / 3];
        }
//        shader.setVec3Array(
//                "u_SphericalHarmonicsCoefficients", sphericalHarmonicsCoefficients);
        GLES30.glUniform3fv(sphericalHarmonicsCoefficientsUniform, 1, sphericalHarmonicsCoefficients, 0);
        GLError.maybeThrowGLException("Failed to set shader texture uniform", "glUniform3fv");

    }
}
