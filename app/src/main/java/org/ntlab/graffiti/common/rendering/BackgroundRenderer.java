/*
 * Copyright 2017 Google Inc. All Rights Reserved.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.ntlab.graffiti.common.rendering;

import android.content.Context;
import android.media.Image;
import android.opengl.GLES11Ext;
import android.opengl.GLES30;
import android.opengl.GLSurfaceView;

import com.google.ar.core.Coordinates2d;
import com.google.ar.core.Frame;
import com.google.ar.core.Session;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.HashMap;

/**
 * This class renders the AR background from camera feed. It creates and hosts the texture given to
 * ARCore to be filled with the camera image.
 *
 * @author a-hongo
 */
public class BackgroundRenderer {
    private static final String TAG = BackgroundRenderer.class.getSimpleName();

    // Shader names
    private static final String VERTEX_SHADER_NAME = "shaders/screenquad.vert";
    private static final String FRAGMENT_SHADER_NAME = "shaders/screenquad.frag";

    //  private static final int COORDS_PER_VERTEX = 3;
    private static final int COORDS_PER_VERTEX = 2;
    private static final int TEXCOORDS_PER_VERTEX = 2;
    private static final int FLOAT_SIZE = 4;

    private FloatBuffer quadVertices;
    private FloatBuffer quadTexCoord;
    private FloatBuffer quadTexCoordTransformed;

//  private int quadProgram;

    //  private int quadPositionParam;
//  private int quadTexCoordParam;
    private int textureId = -1;

    private Mesh mesh;
    private VertexBuffer cameraTexCoordsVertexBuffer;
    private Shader backgroundShader;
    private Shader occlusionShader;
    private Texture cameraDepthTexture;
    private Texture cameraColorTexture;

    private boolean useOcclusion;
    private float aspectRatio;

    private static final float[] QUAD_COORDS =
            new float[]{
//                  /*0:*/ -1.0f, -1.0f, 0.0f, /*1:*/ -1.0f, +1.0f, 0.0f, /*2:*/ +1.0f, -1.0f, 0.0f, /*3:*/ +1.0f, +1.0f, 0.0f,
//                  /*0:*/ -1f, -1f, /*1:*/ +1f, -1f, /*2:*/ -1f, +1f, /*3:*/ +1f, +1f,
                    /*0:*/ -1f, -1f, /*1:*/ -1f, +1f, /*2:*/ +1f, -1f, /*3:*/ +1f, +1f,
            };

    private static final float[] QUAD_TEXCOORDS =
            new float[]{
                    /*0:*/ 0.0f, 1.0f,
                    /*1:*/ 0.0f, 0.0f,
                    /*2:*/ 1.0f, 1.0f,
                    /*3:*/ 1.0f, 0.0f,
//                  /*0:*/ 0f, 0f, /*1:*/ 1f, 0f, /*2:*/ 0f, 1f, /*3:*/ 1f, 1f,
            };

    public BackgroundRenderer() {
    }

//    public int getTextureId() {
//        return cameraColorTexture.getTextureId();
////    return textureId;
//    }

    /**
     * Allocates and initializes OpenGL resources needed by the background renderer. Must be called on
     * the OpenGL thread, typically in {@link GLSurfaceView.Renderer#onSurfaceCreated(GL10,
     * EGLConfig)}.
     *
     * @param context Needed to access shader source.
     */
    public void createOnGlThread(Context context) throws IOException {
        // Generate the background texture.
        cameraColorTexture =
                new Texture(
                        GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
                        GLES30.GL_CLAMP_TO_EDGE,
                        /*useMipmaps=*/ false);

        cameraDepthTexture =
                new Texture(
                        GLES30.GL_TEXTURE_2D,
                        GLES30.GL_CLAMP_TO_EDGE,
                        /*useMipmaps=*/ false);

//    int[] textures = new int[1];
//    GLES30.glGenTextures(1, textures, 0);
//    textureId = textures[0];
//    int textureTarget = GLES11Ext.GL_TEXTURE_EXTERNAL_OES;
//    GLES30.glBindTexture(textureTarget, textureId);
//    GLES30.glTexParameteri(textureTarget, GLES30.GL_TEXTURE_WRAP_S, GLES30.GL_CLAMP_TO_EDGE);
//    GLES30.glTexParameteri(textureTarget, GLES30.GL_TEXTURE_WRAP_T, GLES30.GL_CLAMP_TO_EDGE);
//    GLES30.glTexParameteri(textureTarget, GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_NEAREST);
//    GLES30.glTexParameteri(textureTarget, GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_NEAREST);

        int numVertices = 4;
        if (numVertices != QUAD_COORDS.length / COORDS_PER_VERTEX) {
            throw new RuntimeException("Unexpected number of vertices in BackgroundRenderer.");
        }

        ByteBuffer bbVertices = ByteBuffer.allocateDirect(QUAD_COORDS.length * FLOAT_SIZE);
        bbVertices.order(ByteOrder.nativeOrder());
        quadVertices = bbVertices.asFloatBuffer();
        quadVertices.put(QUAD_COORDS);
        quadVertices.position(0);

        ByteBuffer bbTexCoords =
                ByteBuffer.allocateDirect(numVertices * TEXCOORDS_PER_VERTEX * FLOAT_SIZE);
        bbTexCoords.order(ByteOrder.nativeOrder());
        quadTexCoord = bbTexCoords.asFloatBuffer();
        quadTexCoord.put(QUAD_TEXCOORDS);
        quadTexCoord.position(0);

        ByteBuffer bbTexCoordsTransformed =
                ByteBuffer.allocateDirect(numVertices * TEXCOORDS_PER_VERTEX * FLOAT_SIZE);
        bbTexCoordsTransformed.order(ByteOrder.nativeOrder());
        quadTexCoordTransformed = bbTexCoordsTransformed.asFloatBuffer();

        // Create a Mesh with three vertex buffers: one for the screen coordinates (normalized device
        // coordinates), one for the camera texture coordinates (to be populated with proper data later
        // before drawing), and one for the virtual scene texture coordinates (unit texture quad)
        VertexBuffer screenCoordsVertexBuffer =
                new VertexBuffer(/* numberOfEntriesPerVertex=*/ 2, quadVertices);
        cameraTexCoordsVertexBuffer =
                new VertexBuffer(/*numberOfEntriesPerVertex=*/ 2, /*entries=*/ null);
        VertexBuffer virtualSceneTexCoordsVertexBuffer =
                new VertexBuffer(/* numberOfEntriesPerVertex=*/ 2, quadTexCoord);
        VertexBuffer[] vertexBuffers = {
                screenCoordsVertexBuffer, cameraTexCoordsVertexBuffer, virtualSceneTexCoordsVertexBuffer,
        };
        mesh =
                new Mesh(GLES30.GL_TRIANGLE_STRIP, /*indexBuffer=*/ null, vertexBuffers);

        // This loads the shader code, and must be called on the GL thread.
        backgroundShader =
                Shader.createFromAssets(
                        context,
                        "shaders/background_show_camera.vert",
                        "shaders/background_show_camera.frag",
                        /*defines=*/ null)
                        .setTexture("u_CameraColorTexture", cameraColorTexture)
                        .setDepthTest(false)
                        .setDepthWrite(false);

//    int vertexShader =
//        ShaderUtil.loadGLShader(TAG, context, GLES30.GL_VERTEX_SHADER, VERTEX_SHADER_NAME);
//    int fragmentShader =
//        ShaderUtil.loadGLShader(TAG, context, GLES30.GL_FRAGMENT_SHADER, FRAGMENT_SHADER_NAME);

//    quadProgram = GLES30.glCreateProgram();
//    GLES30.glAttachShader(quadProgram, vertexShader);
//    GLES30.glAttachShader(quadProgram, fragmentShader);
//    GLES30.glLinkProgram(quadProgram);
//    GLES30.glUseProgram(quadProgram);
//    ShaderUtil.checkGLError(TAG, "Program creation");

//    quadPositionParam = GLES30.glGetAttribLocation(quadProgram, "a_Position");
//    quadTexCoordParam = GLES30.glGetAttribLocation(quadProgram, "a_TexCoord");
//    ShaderUtil.checkGLError(TAG, "Program parameters");
    }

    /**
     * Sets whether to use depth for occlusion. This reloads the shader code with new {@code
     * #define}s, and must be called on the GL thread.
     */
    public void setUseOcclusion(Context context, boolean useOcclusion) throws IOException {
        if (occlusionShader != null) {
            if (this.useOcclusion == useOcclusion) {
                return;
            }
            occlusionShader.close();
            occlusionShader = null;
        }
        HashMap<String, String> defines = new HashMap<>();
        defines.put("USE_OCCLUSION", useOcclusion ? "1" : "0");
        occlusionShader =
                Shader.createFromAssets(context,
                        "shaders/occlusion.vert", "shaders/occlusion.frag", defines)
                        .setDepthTest(false)
                        .setDepthWrite(false)
                        .setBlend(GLES30.GL_SRC_ALPHA, GLES30.GL_ONE_MINUS_SRC_ALPHA);
        if (useOcclusion) {
            occlusionShader
                    .setTexture("u_CameraDepthTexture", cameraDepthTexture)
                    .setFloat("u_DepthAspectRatio", aspectRatio);
        }
        this.useOcclusion = useOcclusion;
    }

    /**
     * Updates the display geometry. This must be called every frame before calling either of
     * BackgroundRenderer's draw methods.
     *
     * @param frame The current {@code Frame} as returned by {@link Session#update()}.
     */
    public void updateDisplayGeometry(Frame frame) {
        if (frame.hasDisplayGeometryChanged()) {
            // If display rotation changed (also includes view size change), we need to re-query the UV
            // coordinates for the screen rect, as they may have changed as well.
            frame.transformCoordinates2d(
                    Coordinates2d.OPENGL_NORMALIZED_DEVICE_COORDINATES,
                    quadVertices,
                    Coordinates2d.TEXTURE_NORMALIZED,
                    quadTexCoordTransformed);
            cameraTexCoordsVertexBuffer.set(quadTexCoordTransformed);
        }
    }

    /**
     * Update depth texture with Image contents.
     */
    public void updateCameraDepthTexture(Image image) {
        // SampleRender abstraction leaks here
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, cameraDepthTexture.getTextureId());
        GLES30.glTexImage2D(
                GLES30.GL_TEXTURE_2D,
                0,
                GLES30.GL_RG8,
                image.getWidth(),
                image.getHeight(),
                0,
                GLES30.GL_RG,
                GLES30.GL_UNSIGNED_BYTE,
                image.getPlanes()[0].getBuffer());
        if (useOcclusion) {
            aspectRatio = (float) image.getWidth() / (float) image.getHeight();
            occlusionShader.setFloat("u_DepthAspectRatio", aspectRatio);
        }
    }

    /**
     * Draws the AR background image. The image will be drawn such that virtual content rendered with
     * the matrices provided by {@link com.google.ar.core.Camera#getViewMatrix(float[], int)} and
     * {@link com.google.ar.core.Camera#getProjectionMatrix(float[], int, float, float)} will
     * accurately follow static physical objects. This must be called <b>before</b> drawing virtual
     * content.
     *
     * @param frame The last {@code Frame} returned by {@link Session#update()} or null when ARCore is
     *              paused. See shared_camera_java sample details.
     */
    public void draw(Frame frame) {
        if (frame != null) {
            // If display rotation changed (also includes view size change), we need to re-query the uv
            // coordinates for the screen rect, as they may have changed as well.
//            updateDisplayGeometry(frame);
//        if (frame.hasDisplayGeometryChanged()) {
//        frame.transformDisplayUvCoords(quadTexCoord, quadTexCoordTransformed);
//      }

            if (frame.getTimestamp() == 0) {
                // Suppress rendering if the camera did not produce the first frame yet. This is to avoid
                // drawing possible leftover data from previous sessions if the texture is reused.
                return;
            }
        }

//    draw(mesh, shader, /*framebuffer=*/ null);
//    useFramebuffer(framebuffer);
        backgroundShader.lowLevelUse();
        mesh.lowLevelDraw();

        // No need to test or write depth, the screen quad has arbitrary depth, and is expected
        // to be drawn first.
//    GLES30.glDisable(GLES30.GL_DEPTH_TEST);
//    GLES30.glDepthMask(false);

//    GLES30.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, textureId);

//    GLES30.glUseProgram(quadProgram);

        // Set the vertex positions.
//    GLES30.glVertexAttribPointer(
//        quadPositionParam, COORDS_PER_VERTEX, GLES30.GL_FLOAT, false, 0, quadVertices);

        // Set the texture coordinates.
//    GLES30.glVertexAttribPointer(
//        quadTexCoordParam,
//        TEXCOORDS_PER_VERTEX,
//        GLES30.GL_FLOAT,
//        false,
//        0,
//        quadTexCoordTransformed);

        // Enable vertex arrays
//    GLES30.glEnableVertexAttribArray(quadPositionParam);
//    GLES30.glEnableVertexAttribArray(quadTexCoordParam);

//    GLES30.glDrawArrays(GLES30.GL_TRIANGLE_STRIP, 0, 4);

        // Disable vertex arrays
//    GLES30.glDisableVertexAttribArray(quadPositionParam);
//    GLES30.glDisableVertexAttribArray(quadTexCoordParam);

        // Restore the depth state for further drawing.
//    GLES30.glDepthMask(true);
//    GLES30.glEnable(GLES30.GL_DEPTH_TEST);

//    ShaderUtil.checkGLError(TAG, "Draw");
    }

    /**
     * Draws the virtual scene. Any objects rendered in the given {@link Framebuffer} will be drawn
     * given the previously specified {@link OcclusionMode}.
     *
     * <p>Virtual content should be rendered using the matrices provided by {@link
     * com.google.ar.core.Camera#getViewMatrix(float[], int)} and {@link
     * com.google.ar.core.Camera#getProjectionMatrix(float[], int, float, float)}.
     */
    public void drawVirtualScene(Framebuffer virtualSceneFramebuffer, float zNear, float zFar) {
        occlusionShader.setTexture(
                "u_VirtualSceneColorTexture", virtualSceneFramebuffer.getColorTexture());
        if (useOcclusion) {
            occlusionShader
                    .setTexture("u_VirtualSceneDepthTexture", virtualSceneFramebuffer.getDepthTexture())
                    .setFloat("u_ZNear", zNear)
                    .setFloat("u_ZFar", zFar);
        }

//    draw(mesh, shader, /*framebuffer=*/ null);
//    useFramebuffer(framebuffer);
        occlusionShader.lowLevelUse();
        mesh.lowLevelDraw();
    }

    /** Return the camera color texture generated by this object. */
    public Texture getCameraColorTexture() {
        return cameraColorTexture;
    }

    /** Return the camera depth texture generated by this object. */
    public Texture getCameraDepthTexture() {
        return cameraDepthTexture;
    }

}
