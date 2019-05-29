package com.google.ar.core.examples.java.common.rendering;

import android.content.Context;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

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

        final int vertexShader = ShaderUtil.loadGLShader(TAG, context, GLES20.GL_VERTEX_SHADER, VERTEX_SHADER_CODE);
        final int fragmentShader = ShaderUtil.loadGLShader(TAG, context, GLES20.GL_FRAGMENT_SHADER, FRAGMENT_SHADER_CODE);

        program = GLES20.glCreateProgram();
        GLES20.glAttachShader(program, vertexShader);
        GLES20.glAttachShader(program, fragmentShader);
        GLES20.glLinkProgram(program);

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


        GLES20.glUseProgram(program);
        mPositionHandle = GLES20.glGetAttribLocation(program, "vPosition");
        GLES20.glEnableVertexAttribArray(mPositionHandle);
        GLES20.glVertexAttribPointer(mPositionHandle, COORDS_PER_VERTEX, GLES20.GL_FLOAT, false,
                vertexStride, vertexBuffer);

        mColorHandle = GLES20.glGetUniformLocation(program, "vColor");
        GLES20.glUniform4fv(mColorHandle, 1, DEFAULT_COLOR, 0);
        mMVPMatrixHandle = GLES20.glGetUniformLocation(program, "uMVPMatrix");
        GLES20.glUniformMatrix4fv(mMVPMatrixHandle, 1, false, modelViewProjection, 0);

        GLES20.glDrawElements(GLES20.GL_LINES, DEFAULT_COLOR.length,
                GLES20.GL_UNSIGNED_SHORT, drawListBuffer);

        GLES20.glDisableVertexAttribArray(mPositionHandle);
        GLES20.glDisable(mColorHandle);
    }
}
