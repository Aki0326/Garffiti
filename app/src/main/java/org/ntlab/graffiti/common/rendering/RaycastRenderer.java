package org.ntlab.graffiti.common.rendering;

import android.content.Context;
import android.opengl.GLES30;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

/**
 * This class renders the line raycast. Not currently using.
 * @author a-hongo
 */
public class RaycastRenderer {
    private static final String TAG = RaycastRenderer.class.getSimpleName();

    private final String VERTEX_SHADER_CODE =
            "uniform mat4 uMVPMatrix;" +
                    "attribute vec4 vPosition;" +
                    "void main() {" +
                    "  gl_Position = uMVPMatrix * vPosition;" +
                    "}";

    private final String FRAGMENT_SHADER_CODE =
            "precision mediump float;" +
                    "uniform vec4 vColor;" +
                    "void main() {" +
                    "  gl_FragColor = vColor;" +
                    "}";

    private static final int COORDS_PER_VERTEX = 3;
    private static float[] DEFAULT_COLOR = new float[] {1.0f, 0.0f, 0.0f, 1.0f};

    private FloatBuffer vertexBuffer;
    private ShortBuffer drawListBuffer;
    private int program;
    private int mPositionHandle;
    private int mColorHandle;
    private int mMVPMatrixHandle;


    private final int vertexStride = COORDS_PER_VERTEX * 4;

    private float[] pathCords =
            {
                    0.00f, 0.0f, 0.0f,

                    0.5f, 0.3f, 0.0f
            };
    private short[] pathDrawOrder = {0,1};

    public RaycastRenderer() {}

    /**
     * Allocates and initializes OpenGL resources needed by the plane renderer. Must be called on the
     * OpenGL thread, typically in {@link GLSurfaceView.Renderer#onSurfaceCreated(GL10, EGLConfig)}.
     *
     * @param context Needed to access shader source.
     */
    public void createOnGlThread(Context context) throws IOException {
        ShaderUtil.checkGLError(TAG, "before create");

        final int vertexShader = ShaderUtil.loadGLShader(TAG, context, GLES30.GL_VERTEX_SHADER, VERTEX_SHADER_CODE);
        final int fragmentShader = ShaderUtil.loadGLShader(TAG, context, GLES30.GL_FRAGMENT_SHADER, FRAGMENT_SHADER_CODE);

        program = GLES30.glCreateProgram();
        GLES30.glAttachShader(program, vertexShader);
        GLES30.glAttachShader(program, fragmentShader);
        GLES30.glLinkProgram(program);

        ShaderUtil.checkGLError(TAG, "Program creation");

        ByteBuffer bb = ByteBuffer.allocateDirect(pathCords.length * 4);
        bb.order(ByteOrder.nativeOrder());
        vertexBuffer = bb.asFloatBuffer();
        vertexBuffer.put(pathCords);
        vertexBuffer.position(0);

        ByteBuffer dlb = ByteBuffer.allocateDirect(pathDrawOrder.length * 2);
        dlb.order(ByteOrder.nativeOrder());
        drawListBuffer = dlb.asShortBuffer();
        drawListBuffer.put(pathDrawOrder);
        drawListBuffer.position(0);



    }

    public void draw(float[] cameraView, float[] cameraPerspective) {
        float[] modelViewProjection = new float[16];
        Matrix.multiplyMM(modelViewProjection, 0, cameraPerspective, 0, cameraView, 0);

        ShaderUtil.checkGLError(TAG, "Before draw");


        GLES30.glUseProgram(program);
        mPositionHandle = GLES30.glGetAttribLocation(program, "vPosition");
        GLES30.glEnableVertexAttribArray(mPositionHandle);
        GLES30.glVertexAttribPointer(mPositionHandle, COORDS_PER_VERTEX, GLES30.GL_FLOAT, false,
                vertexStride, vertexBuffer);

        mColorHandle = GLES30.glGetUniformLocation(program, "vColor");
        GLES30.glUniform4fv(mColorHandle, 1, DEFAULT_COLOR, 0);
        mMVPMatrixHandle = GLES30.glGetUniformLocation(program, "uMVPMatrix");
        GLES30.glUniformMatrix4fv(mMVPMatrixHandle, 1, false, modelViewProjection, 0);

        GLES30.glDrawElements(GLES30.GL_LINES, DEFAULT_COLOR.length,
                GLES30.GL_UNSIGNED_SHORT, drawListBuffer);

        GLES30.glDisableVertexAttribArray(mPositionHandle);
        GLES30.glDisable(mColorHandle);
    }
}
