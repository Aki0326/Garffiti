//// Copyright 2018 Google LLC
////
//// Licensed under the Apache License, Version 2.0 (the "License");
//// you may not use this file except in compliance with the License.
//// You may obtain a copy of the License at
////
////      http://www.apache.org/licenses/LICENSE-2.0
////
//// Unless required by applicable law or agreed to in writing, software
//// distributed under the License is distributed on an "AS IS" BASIS,
//// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//// See the License for the specific language governing permissions and
//// limitations under the License.
//
//package com.google.ar.core.examples.java.common.rendering;
//
//
//import android.content.Context;
//import android.graphics.Bitmap;
//import android.graphics.BitmapFactory;
//import android.opengl.GLES20;
//import android.opengl.GLSurfaceView;
//import android.opengl.GLUtils;
//import android.opengl.Matrix;
//import android.util.Log;
//
//import com.google.ar.core.Anchor;
//import com.google.ar.sceneform.math.Vector3;
//
//import java.io.IOException;
//import java.nio.ByteBuffer;
//import java.nio.ByteOrder;
//import java.nio.FloatBuffer;
//import java.util.ArrayList;
//import java.util.List;
//import java.util.Map;
//import java.util.concurrent.atomic.AtomicBoolean;
//
//import javax.microedition.khronos.egl.EGLConfig;
//import javax.microedition.khronos.opengles.GL10;
//
///**
// * Renders a point cloud.
// */
//public class LineShaderRenderer {
//    private static final String TAG = LineShaderRenderer.class.getSimpleName();
//
//    // Shader names.
//    private static final String VERTEX_SHADER_NAME = "shaders/line.vert";
//    private static final String FRAGMENT_SHADER_NAME = "shaders/line.frag";
//
//    private static final int FLOATS_PER_POINT = 3;  // X,Y,Z.
//    private static final int BYTES_PER_FLOAT = 4;
//    private static final int BYTES_PER_POINT = BYTES_PER_FLOAT * FLOATS_PER_POINT;
//
//    private static final float[] DEFAULT_COLOR = new float[] {1f, 1f, 1f};
//
//    // Object vertex buffer variables.
//    private int positionAddress;
//    private int previousAddress;
//    private int nextAddress;
//    private int sideAddress;
//    private int widthAddress;
//    private int lengthAddress;
//    private int endCapsAddress;
//
//    private int program;
//    private final int[] textures = new int[1];
//
//    // Shader location: model view projection matrix.
//    private int modelViewUniform;
//    private int modelViewProjectionUniform;
//    private int resolutionUniform;
//    private int endCapTextureUniform;
//
//    // Shader location: object attributes.
//    private int positionAttribute;
//    private int previousAttribute;
//    private int nextAttribute;
//    private int sideAttribute;
//    private int widthAttribute;
//    private int lengthsAttribute;
//    private int endCapsAttribute;
//    private int normalAttribute;
//
//    // Shader location: texture sampler.
//    private int textureUniform;
//
//    // Shader location: object color property (to change the primary color of the object).
//    private int colorUniform;
//
//    public float[] modelMatrix = new float[16];
//    private float[] modelViewMatrix = new float[16];
//    private float[] modelViewProjectionMatrix = new float[16];
//
//    private float[] positions;
//    private float[] next;
//    private float[] side;
//    private float[] width;
//    private float[] previous;
//    private float[] lengths;
//    private float[] endCaps;
//
//    private int numBytes = 0;
//
//    private int vbo = 0;
//    private int vboSize = 0;
//
//
//    private float lineWidth = 0;
//
//
//    public AtomicBoolean bNeedsUpdate = new AtomicBoolean();
//
//    public int numPoints;
//
//
//    public LineShaderRenderer() {
//    }
//
//    /**
//     * Allocates and initializes OpenGL resources needed by the Line renderer.  Must be
//     * called on the OpenGL thread, typically in
//     * {@link GLSurfaceView.Renderer#onSurfaceCreated(GL10, EGLConfig)}.
//     *
//     * @param context Needed to access shader source.
//     */
//    public void createOnGlThread(Context context) throws IOException {
//        ShaderUtil.checkGLError(TAG, "before create");
//
//        int buffers[] = new int[1];
//        GLES20.glGenBuffers(1, buffers, 0);
//        vbo = buffers[0];
//        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, vbo);
//        vboSize = 0;
//        GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, vboSize, null, GLES20.GL_DYNAMIC_DRAW);
//        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);
//
//
//        ShaderUtil.checkGLError(TAG, "buffer alloc");
//
//
//        /*
//         *
//         * The LineShaderRenderer uses an ES2 pipeline.  It uses the line_vert.glsl and
//         * line_frag.glsl shader to render a volumetric line.  It uses several techniques detailed in
//         * the following resources:
//         *
//         *      Drawing Lines is Hard by Matt DesLauriers
//         *          https://mattdesl.svbtle.com/drawing-lines-is-hard
//         *
//         *      InkSpace an Android Experiment by Zach Lieberman
//         *          https://experiments.withgoogle.com/android/ink-space
//         *          https://github.com/ofZach/inkSpace
//         *
//         *      THREEJS.MeshLine by Jaume Sanchez
//         *          https://github.com/spite/THREE.MeshLine/blob/master/src/THREE.MeshLine.js
//         *
//         *
//         * The Renderer batches all of the geometry into a single VBO.  This allows us to have a single
//         * draw call to render the geometry.  We also optimize the application to only re-upload the
//         * geometry data when a new stroke or new points are added to the drawing. The renderer uses
//         * a technique detailed in the following link to create degenerate faces between the strokes
//         * to disconnect them from one another.
//         *      https://developer.apple.com/library/content/documentation/3DDrawing/Conceptual/OpenGLES_ProgrammingGuide/TechniquesforWorkingwithVertexData/TechniquesforWorkingwithVertexData.html
//         *
//         */
//
//        int vertexShader = ShaderUtil.loadGLShader(TAG, context,
//                GLES20.GL_VERTEX_SHADER, VERTEX_SHADER_NAME);
//        int fragmentShader = ShaderUtil.loadGLShader(TAG, context,
//                GLES20.GL_FRAGMENT_SHADER, FRAGMENT_SHADER_NAME);
//
//
//        program = GLES20.glCreateProgram();
//        GLES20.glAttachShader(program, vertexShader);
//        GLES20.glAttachShader(program, fragmentShader);
//        GLES20.glLinkProgram(program);
//        GLES20.glUseProgram(program);
//
//        ShaderUtil.checkGLError(TAG, "program");
//
//        modelViewUniform = GLES20.glGetUniformLocation(program, "u_ModelView");
//        modelViewProjectionUniform = GLES20.glGetUniformLocation(program, "u_ModelViewProjection");
//
//        positionAttribute = GLES20.glGetAttribLocation(program, "position");
//        previousAttribute = GLES20.glGetAttribLocation(program, "previous");
//        nextAttribute = GLES20.glGetAttribLocation(program, "next");
//        sideAttribute = GLES20.glGetAttribLocation(program, "side");
//        widthAttribute = GLES20.glGetAttribLocation(program, "width");
//        lengthsAttribute = GLES20.glGetAttribLocation(program, "length");
//        endCapsAttribute = GLES20.glGetAttribLocation(program, "endCaps");
//        normalAttribute = GLES20.glGetAttribLocation(program, "normal");
//
//        textureUniform = GLES20.glGetUniformLocation(program, "u_Texture");
//        endCapTextureUniform = GLES20.glGetUniformLocation(program, "u_EndCapTexture");
//
//        resolutionUniform = GLES20.glGetUniformLocation(program, "resolution");
//        colorUniform = GLES20.glGetUniformLocation(program, "color");
//
//        ShaderUtil.checkGLError(TAG, "program  params");
//
//
//        // Read the texture.
////        Bitmap textureBitmap =
////                BitmapFactory.decodeStream(context.getAssets().open("texture.png"));
////
////        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
////        GLES20.glGenTextures(textures.length, textures, 0);
////        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textures[0]);
////
////        GLES20.glTexParameteri(
////                GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR_MIPMAP_LINEAR);
////        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
////        GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, textureBitmap, 0);
////        GLES20.glGenerateMipmap(GLES20.GL_TEXTURE_2D);
//////        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
////
////        ShaderUtil.checkGLError(TAG, "Texture loading");
//
//        // Read the line texture.
//        Bitmap endCapTextureBitmap =
//                BitmapFactory.decodeStream(context.getAssets().open("linecap.png"));
//
////        GLES20.glActiveTexture(GLES20.GL_TEXTURE1);
////        GLES20.glGenTextures(textures.length, textures, 0);
//        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textures[0]);
//
//        GLES20.glTexParameteri(
//                GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR_MIPMAP_LINEAR);
//        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
//        GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, endCapTextureBitmap, 0);
//        GLES20.glGenerateMipmap(GLES20.GL_TEXTURE_2D);
//        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
//        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
//
//        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
//
//        ShaderUtil.checkGLError(TAG, "Line texture loading");
//
//
//        Matrix.setIdentityM(modelMatrix, 0);
//    }
//
//    public void clearGL() {
//        GLES20.glDeleteShader(program);
//        GLES20.glDeleteBuffers(1, new int[]{vbo}, 0);
//    }
//
//
//    /**
//     * Sets the LineWidth of the Line.
//     * Requires bNeedsUpdate.set(true) to take effect
//     *
//     * @param width of line in decimal format
//     */
//    public void setLineWidth(float width) {
//        lineWidth = width;
//    }
//
//    /**
//     * This updates the geometry data to be rendered. It ensures the capacity of the float arrays
//     * and then calls updateLine to generate the geometry.
//     *
//     * @param strokes a ArrayList of ArrayLists of Vector3fs in world space.  The outer ArrayList
//     *                contains the strokes, while the inner ArrayList contains the Vertex of each Line
//     */
//    public void updateStrokes(List<Stroke> strokes, Map<String,Stroke> sharedStrokes) {
//        numPoints = 0;
//
//        for (Stroke l : strokes) {
//            numPoints += l.size() * 2 + 2;
//        }
//
//        for (Stroke l : sharedStrokes.values()) {
//            numPoints += l.size() * 2 + 2;
//        }
//
//        ensureCapacity(numPoints);
//
//        int offset = 0;
//
//        for (Stroke l : strokes) {
//            offset = addLine(l, offset);
//        }
//
//        for (Stroke l : sharedStrokes.values()) {
//            offset = addLine(l, offset);
//        }
//        numBytes = offset;
//    }
//
//    /**
//     * This ensures the capacity of the float arrays that hold the information bound to the Vertex
//     * Attributes needed to render the line with the Vertex and Fragment shader.
//     *
//     * @param numPoints int denoting number of points
//     */
//    private void ensureCapacity(int numPoints) {
//        int count = 1024;
//        if (side != null) {
//            count = side.length;
//        }
//
//        while (count < numPoints) {
//            count += 1024;
//        }
//
//        if (side == null || side.length < count) {
//            Log.i(TAG, "alloc " + count);
//            positions = new float[count * 3];
//            next = new float[count * 3];
//            previous = new float[count * 3];
//
//            side = new float[count];
//            width = new float[count];
//            lengths = new float[count];
//            endCaps = new float[count];
//        }
//    }
//
//
//    /**
//     * AddLine takes in the 3D positions adds to the buffers to create the stroke and the degenerate
//     * faces needed so the lines render properly.
//     */
//    private int addLine(Stroke line, int offset) {
//        if (line == null || line.size() < 2)
//            return offset;
//
//
//        int lineSize = line.size();
//
//        float mLineWidthMax = lineWidth = line.getLineWidth();
//
//        float length = 0;
//        float totalLength;
//        int ii = offset;
//
//        if (line.localLine) {
//            totalLength = line.totalLength;
//        } else {
//            totalLength = line.animatedLength;
//        }
//
//        for (int i = 0; i < lineSize; i++) {
//
//            int iGood = i;
//            if (iGood >= lineSize) iGood = lineSize - 1;
//
//            int i_m_1 = (iGood - 1) < 0 ? iGood : iGood - 1;
//            int i_p_1 = (iGood + 1) > (lineSize - 1) ? iGood : iGood + 1;
//
//            Vector3 current = line.get(iGood);
//            Vector3 previous = line.get(i_m_1);
//            Vector3 next = line.get(i_p_1);
//
//            Vector3 dist = new Vector3(current);
//            dist.subtract(dist, previous);
//            length += dist.length();
//
//
////            if (i < line.mTapperPoints) {
////                lineWidth = mLineWidthMax * line.mTaperLookup[i];
////            } else if (i > lineSize - line.mTapperPoints) {
////                lineWidth = mLineWidthMax * line.mTaperLookup[lineSize - i];
////            } else {
//            lineWidth = line.getLineWidth();
////            }
//
//
//            lineWidth = Math.max(0, Math.min(mLineWidthMax, lineWidth));
//
//
//            if (i == 0) {
//                setMemory(ii++, current, previous, next, lineWidth, 1f, length, totalLength);
//            }
//
//            setMemory(ii++, current, previous, next, lineWidth, 1f, length, totalLength);
//            setMemory(ii++, current, previous, next, lineWidth, -1f, length, totalLength);
//
//            if (i == lineSize - 1) {
//                setMemory(ii++, current, previous, next, lineWidth, -1f, length, totalLength);
//            }
//
//
//        }
//        return ii;
//    }
//
//    /**
//     * setMemory is a helper method used to add the stroke data to the float[] buffers
//     */
//    private void setMemory(int index, Vector3 pos, Vector3 prev, Vector3 next, float width, float side, float length, float endCapPosition) {
//        positions[index * 3] = pos.x;
//        positions[index * 3 + 1] = pos.y;
//        positions[index * 3 + 2] = pos.z;
//
//        this.next[index * 3] = next.x;
//        this.next[index * 3 + 1] = next.y;
//        this.next[index * 3 + 2] = next.z;
//
//        previous[index * 3] = prev.x;
//        previous[index * 3 + 1] = prev.y;
//        previous[index * 3 + 2] = prev.z;
//
//        this.side[index] = side;
//        this.width[index] = width;
//        lengths[index] = length;
//        endCaps[index] = endCapPosition;
//    }
//
//    /**
//     * Sets the bNeedsUpdate to true.
//     */
//    public void clear() {
//        bNeedsUpdate.set(true);
//    }
//
//
//    /**
//     * This takes the float[] and creates FloatBuffers, Binds the VBO, and upload the Attributes to
//     * correct locations with the correct offsets so the Vertex and Fragment shader can render the lines
//     */
//    public void upload() {
//        bNeedsUpdate.set(false);
//
//        FloatBuffer current = toFloatBuffer(positions);
//        FloatBuffer next = toFloatBuffer(this.next);
//        FloatBuffer previous = toFloatBuffer(this.previous);
//
//        FloatBuffer side = toFloatBuffer(this.side);
//        FloatBuffer width = toFloatBuffer(this.width);
//        FloatBuffer lengths = toFloatBuffer(this.lengths);
//        FloatBuffer endCaps = toFloatBuffer(this.endCaps);
//
//
////        numPoints = positions.length;
//
//        positionAddress = 0;
//        nextAddress = positionAddress + numBytes * 3 * BYTES_PER_FLOAT;
//        previousAddress = nextAddress + numBytes * 3 * BYTES_PER_FLOAT;
//        sideAddress = previousAddress + numBytes * 3 * BYTES_PER_FLOAT;
//        widthAddress = sideAddress + numBytes * BYTES_PER_FLOAT;
//        lengthAddress = widthAddress + numBytes * BYTES_PER_FLOAT;
//        endCapsAddress = lengthAddress + numBytes * BYTES_PER_FLOAT;
//        vboSize = endCapsAddress + numBytes * BYTES_PER_FLOAT;
//
//        ShaderUtil.checkGLError(TAG, "before update");
//
//        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, vbo);
//
//        GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, vboSize, null, GLES20.GL_DYNAMIC_DRAW);
//
//        GLES20.glBufferSubData(GLES20.GL_ARRAY_BUFFER, positionAddress, numBytes * 3 * BYTES_PER_FLOAT,
//                current);
//        GLES20.glBufferSubData(GLES20.GL_ARRAY_BUFFER, nextAddress, numBytes * 3 * BYTES_PER_FLOAT,
//                next);
//        GLES20.glBufferSubData(GLES20.GL_ARRAY_BUFFER, previousAddress, numBytes * 3 * BYTES_PER_FLOAT,
//                previous);
//        GLES20.glBufferSubData(GLES20.GL_ARRAY_BUFFER, sideAddress, numBytes * BYTES_PER_FLOAT,
//                side);
//        GLES20.glBufferSubData(GLES20.GL_ARRAY_BUFFER, widthAddress, numBytes * BYTES_PER_FLOAT,
//                width);
//        GLES20.glBufferSubData(GLES20.GL_ARRAY_BUFFER, lengthAddress, numBytes * BYTES_PER_FLOAT,
//                lengths);
//        GLES20.glBufferSubData(GLES20.GL_ARRAY_BUFFER, endCapsAddress, numBytes * BYTES_PER_FLOAT,
//                endCaps);
//
//
//        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);
//
//        ShaderUtil.checkGLError(TAG, "after update");
//    }
//
//
//    /**
//     * This method takes in the current CameraView Matrix and the Camera's Projection Matrix, the
//     * current position and pose of the device, uses those to calculate the ModelViewMatrix and
//     * ModelViewProjectionMatrix.  It binds the VBO, enables the custom attribute locations,
//     * binds and uploads the shader uniforms, calls our single DrawArray call, and finally disables
//     * and unbinds the shader attributes and VBO.
//     */
//    public void draw(float[] cameraView, float[] cameraPerspective, float[] colorCorrectionRgba) {
//        draw(cameraView, cameraPerspective, colorCorrectionRgba, DEFAULT_COLOR);
//    }
//    public void draw(
//        float[] cameraView,
//        float[] cameraPerspective,
//        float[] colorCorrectionRgba,
//        float[] objColor) {
//
//        ShaderUtil.checkGLError(TAG, "Before draw");
//
//        // Build the ModelView and ModelViewProjection matrices
//        // for calculating object position and light.
//        Matrix.multiplyMM(modelViewMatrix, 0, cameraView, 0, modelMatrix, 0);
//        Matrix.multiplyMM(modelViewProjectionMatrix, 0, cameraPerspective, 0, modelViewMatrix, 0);
//
//        GLES20.glUseProgram(program);
//
//        // Blending setup
//        GLES20.glEnable(GLES20.GL_BLEND);
////        GLES20.glBlendFuncSeparate(
////                GLES20.GL_SRC_ALPHA, GLES20.GL_DST_ALPHA, // RGB (src, dest)
////                GLES20.GL_ZERO, GLES20.GL_ONE); // ALPHA (src, dest)
//        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);
//
//        // Attach the texture.
//        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
//        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textures[0]);
//
////        GLES20.glActiveTexture(GLES20.GL_TEXTURE1);
////        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textures[1]);
//
////        GLES20.glUniform1i(lineshaderTextureUniform, 0);
//        GLES20.glUniform1i(endCapTextureUniform, 0);
////
//        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, vbo);
//        GLES20.glVertexAttribPointer(
//                positionAttribute, FLOATS_PER_POINT, GLES20.GL_FLOAT, false, BYTES_PER_POINT, positionAddress);
//        GLES20.glVertexAttribPointer(
//                previousAttribute, FLOATS_PER_POINT, GLES20.GL_FLOAT, false, BYTES_PER_POINT, previousAddress);
//        GLES20.glVertexAttribPointer(
//                nextAttribute, FLOATS_PER_POINT, GLES20.GL_FLOAT, false, BYTES_PER_POINT, nextAddress);
//        GLES20.glVertexAttribPointer(
//                sideAttribute, 1, GLES20.GL_FLOAT, false, BYTES_PER_FLOAT, sideAddress);
//        GLES20.glVertexAttribPointer(
//                widthAttribute, 1, GLES20.GL_FLOAT, false, BYTES_PER_FLOAT, widthAddress);
//        GLES20.glVertexAttribPointer(
//                lengthsAttribute, 1, GLES20.GL_FLOAT, false, BYTES_PER_FLOAT, lengthAddress);
//        GLES20.glVertexAttribPointer(
//                endCapsAttribute, 1, GLES20.GL_FLOAT, false, BYTES_PER_FLOAT, endCapsAddress);
////
//
//        GLES20.glUniformMatrix4fv(
//                modelViewUniform, 1, false, modelViewMatrix, 0);
//        GLES20.glUniformMatrix4fv(
//                modelViewProjectionUniform, 1, false, cameraPerspective, 0);
//
//        GLES20.glUniform4fv(colorUniform, 1, DEFAULT_COLOR, 0);
//
//        GLES20.glEnableVertexAttribArray(positionAttribute);
//        GLES20.glEnableVertexAttribArray(previousAttribute);
//        GLES20.glEnableVertexAttribArray(nextAttribute);
//        GLES20.glEnableVertexAttribArray(sideAttribute);
//        GLES20.glEnableVertexAttribArray(widthAttribute);
//        GLES20.glEnableVertexAttribArray(lengthsAttribute);
//        GLES20.glEnableVertexAttribArray(endCapsAttribute);
//
//        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, numBytes);
//
//        GLES20.glDisableVertexAttribArray(endCapsAttribute);
//        GLES20.glDisableVertexAttribArray(lengthsAttribute);
//        GLES20.glDisableVertexAttribArray(widthAttribute);
//        GLES20.glDisableVertexAttribArray(sideAttribute);
//        GLES20.glDisableVertexAttribArray(nextAttribute);
//        GLES20.glDisableVertexAttribArray(previousAttribute);
//        GLES20.glDisableVertexAttribArray(positionAttribute);
//
//
//        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);
//
//        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
//        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
////        GLES20.glActiveTexture(GLES20.GL_TEXTURE1);
////        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
////        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
//
//        GLES20.glDisable(GLES20.GL_BLEND);
//        GLES20.glEnable(GLES20.GL_DEPTH_TEST);
//
//    }
//
//    static class Stroke {
//        private ArrayList<Vector3> points = new ArrayList<>();
//        private float lineWidth = 0.020f;
//        public boolean localLine = true;
//        public float totalLength = 0;
//        public float animatedLength = 0;
//
//        public int size() {
//            return points.size();
//        }
//
//        public float getLineWidth() {
//            return lineWidth;
//        }
//
//        public Vector3 get(int index) {
//            return points.get(index);
//        }
//    }
//
//        /**
//         * A helper function to allocate a FloatBuffer the size of our float[] and copy the float[] into
//         * the newly created FloatBuffer.
//         */
//        private FloatBuffer toFloatBuffer(float[] data) {
//            FloatBuffer buff;
//            ByteBuffer bb = ByteBuffer.allocateDirect(data.length * BYTES_PER_FLOAT);
//            bb.order(ByteOrder.nativeOrder());
//            buff = bb.asFloatBuffer();
//            buff.put(data);
//            buff.position(0);
//            return buff;
//        }
//
//        private static void normalizeVec3(float[] v) {
//            float reciprocalLength = 1.0f / (float) Math.sqrt(v[0] * v[0] + v[1] * v[1] + v[2] * v[2]);
//            v[0] *= reciprocalLength;
//            v[1] *= reciprocalLength;
//            v[2] *= reciprocalLength;
//        }
//
//
//    }
/*
Copyright 2017 Google Inc.
Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at
    https://www.apache.org/licenses/LICENSE-2.0
Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
 */
package com.google.ar.core.examples.java.common.rendering;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.opengl.GLES20;
import android.opengl.GLUtils;
import android.opengl.Matrix;
import android.util.Log;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

/**
 * This class renders the line shader like just a line. Not currently using.
 */
public class LineShaderRenderer {
    private static final String TAG = LineShaderRenderer.class.getSimpleName();

    // Shader names.
//    private static final String VERTEX_SHADER_NAME = "shaders/plane.vert";
//    private static final String FRAGMENT_SHADER_NAME = "shaders/plane.frag";

    // Shader source code, since they are small, just include them inline.
    private static final String VERTEX_SHADER = "uniform mat4 u_ModelViewProjection;\n"
            + "attribute vec4 a_Position;\n"
            + "attribute vec2 a_TexCoord;\n"
            + "\n"
            + "varying vec2 v_TexCoord;\n"
            + "\n"
            + "void main() {\n"
            + "gl_Position = u_ModelViewProjection * a_Position;\n"
            + "   v_TexCoord = a_TexCoord;\n"
            + "}";

    private static final String FRAGMENT_SHADER = "uniform sampler2D u_Texture;\n"
            + "varying vec2 v_TexCoord;\n"
            + "\n"
            + "void main() {\n"
            + "   gl_FragColor = texture2D(u_Texture, v_TexCoord);\n"
            + "}\n";

    private static final int COORDS_PER_VERTEX = 3;

    private static final int TEXCOORDS_PER_VERTEX = 2;

    private static final float[] LINE_COORDS = new float[] {
            // x, y, z
            -.1f, 0.0f, +.1f,//左下
            +.1f, 0.0f, +.1f,//左上
            -.1f, 0.0f, -.1f,//右下
            +.1f, 0.0f, -.1f,//右上
    };

    private static final float[] LINE_TEXCOORDS = new float[] {
            // x, y
            0.0f, 1.0f,
            0.0f, 0.0f,
            1.0f, 1.0f,
            1.0f, 0.0f,
    };

    private int lineshaderProgram;
    private int[] textures = new int[1];

    private FloatBuffer lineshaderVertices;
    private FloatBuffer lineshaderTexCoord;

    private int lineshaderPositionParam;
    private int lineshaderTexCoordParam;
    private int lineshaderTextureUniform;
    private int lineshaderModelViewProjectionUniform;

    // Temporary matrices allocated here to reduce number of allocations for each frame.
    private float[] modelMatrix = new float[16];
    private float[] modelViewMatrix = new float[16];
    private float[] modelViewProjectionMatrix = new float[16];

    public void createOnGlThread(Context context, String gridDistanceTextureName) throws IOException {
        // Read the texture.
        Bitmap textureBitmap = null;
        try {
            textureBitmap = BitmapFactory.decodeStream(context.getAssets().open(gridDistanceTextureName));
        } catch (IOException e) {
            Log.e(TAG, "Exception reading texture", e);
            return;
        }

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glGenTextures(textures.length, textures, 0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textures[0]);

        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_NEAREST);
        GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, textureBitmap, 0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);

        textureBitmap.recycle();

        ShaderUtil.checkGLError(TAG, "Texture loading");

        // Build the geometry of a simple quad.
        int numVertices = 4;
        if (numVertices != LINE_COORDS.length / COORDS_PER_VERTEX) {
            throw new RuntimeException("Unexpected number of vertices in BackgroundRenderer.");
        }

        ByteBuffer bbVertices = ByteBuffer.allocateDirect(LINE_COORDS.length * Float.BYTES);
        bbVertices.order(ByteOrder.nativeOrder());
        lineshaderVertices = bbVertices.asFloatBuffer();
        lineshaderVertices.put(LINE_COORDS);
        lineshaderVertices.position(0);

        ByteBuffer bbTexCoords =
                ByteBuffer.allocateDirect(numVertices * TEXCOORDS_PER_VERTEX * Float.BYTES);
        bbTexCoords.order(ByteOrder.nativeOrder());
        lineshaderTexCoord = bbTexCoords.asFloatBuffer();
        lineshaderTexCoord.put(LINE_TEXCOORDS);
        lineshaderTexCoord.position(0);

        ByteBuffer bbTexCoordsTransformed =
                ByteBuffer.allocateDirect(numVertices * TEXCOORDS_PER_VERTEX * Float.BYTES);
        bbTexCoordsTransformed.order(ByteOrder.nativeOrder());

        int vertexShader = loadGLShader(TAG, GLES20.GL_VERTEX_SHADER, VERTEX_SHADER);
        int fragmentShader = loadGLShader(TAG, GLES20.GL_FRAGMENT_SHADER, FRAGMENT_SHADER);

//        int vertexShader = ShaderUtil.loadGLShader(TAG, context, GLES20.GL_VERTEX_SHADER, VERTEX_SHADER_NAME);
//        int fragmentShader = ShaderUtil.loadGLShader(TAG, context, GLES20.GL_FRAGMENT_SHADER, FRAGMENT_SHADER_NAME);

        lineshaderProgram = GLES20.glCreateProgram();
        GLES20.glAttachShader(lineshaderProgram, vertexShader);
        GLES20.glAttachShader(lineshaderProgram, fragmentShader);
        GLES20.glLinkProgram(lineshaderProgram);
        GLES20.glUseProgram(lineshaderProgram);

        ShaderUtil.checkGLError(TAG, "Program creation");

        lineshaderPositionParam = GLES20.glGetAttribLocation(lineshaderProgram, "a_Position");
        lineshaderTexCoordParam = GLES20.glGetAttribLocation(lineshaderProgram, "a_TexCoord");
        lineshaderTextureUniform = GLES20.glGetUniformLocation(lineshaderProgram, "u_Texture");
        lineshaderModelViewProjectionUniform =
                GLES20.glGetUniformLocation(lineshaderProgram, "u_ModelViewProjection");

        ShaderUtil.checkGLError(TAG, "Program parameters");

        Matrix.setIdentityM(modelMatrix, 0);
    }

    private int loadGLShader(String tag, int type, String source) {
        int shader = GLES20.glCreateShader(type);
        GLES20.glShaderSource(shader, source);
        GLES20.glCompileShader(shader);

        // Get the compilation status.
        final int[] compileStatus = new int[1];
        GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compileStatus, 0);

        // If the compilation failed, delete the shader.
        if (compileStatus[0] == 0) {
            Log.e(tag, "Error compiling shader: " + GLES20.glGetShaderInfoLog(shader));
            GLES20.glDeleteShader(shader);
            shader = 0;
        }

        if (shader == 0) {
            throw new RuntimeException("Error creating shader.");
        }

        return shader;
    }

    public void updateModelMatrix(float[] modelMatrix, float scaleFactor) {
        float[] scaleMatrix = new float[16];
        Matrix.setIdentityM(scaleMatrix, 0);
        scaleMatrix[0] = scaleFactor/3;
        scaleMatrix[5] = scaleFactor/3;
        scaleMatrix[10] = scaleFactor/3;
        Matrix.multiplyMM(this.modelMatrix, 0, modelMatrix, 0, scaleMatrix, 0);
    }

    public void draw(float[] cameraView, float[] cameraPerspective) {
        ShaderUtil.checkGLError(TAG, "Before draw");
        Matrix.multiplyMM(modelViewMatrix, 0, cameraView, 0, modelMatrix, 0);
        Matrix.multiplyMM(modelViewProjectionMatrix, 0, cameraPerspective, 0, modelViewMatrix, 0);

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

        GLES20.glUseProgram(lineshaderProgram);

        // Attach the object texture.
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textures[0]);
        GLES20.glUniform1i(lineshaderTextureUniform, 0);
        GLES20.glUniformMatrix4fv(lineshaderModelViewProjectionUniform, 1, false, modelViewProjectionMatrix, 0);
        // Set the vertex positions.
        GLES20.glVertexAttribPointer(
                lineshaderPositionParam, COORDS_PER_VERTEX, GLES20.GL_FLOAT, false, 0, lineshaderVertices);

        // Set the texture coordinates.
        GLES20.glVertexAttribPointer(
                lineshaderTexCoordParam, TEXCOORDS_PER_VERTEX, GLES20.GL_FLOAT, false, 0, lineshaderTexCoord);

        // Enable vertex arrays
        GLES20.glEnableVertexAttribArray(lineshaderPositionParam);
        GLES20.glEnableVertexAttribArray(lineshaderTexCoordParam);

        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);

        // Disable vertex arrays
        GLES20.glDisableVertexAttribArray(lineshaderPositionParam);
        GLES20.glDisableVertexAttribArray(lineshaderTexCoordParam);

        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);

        // Clean up the state we set
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
        GLES20.glDisable(GLES20.GL_BLEND);
        GLES20.glDepthMask(true);
        GLES20.glClearColor(0.1f, 0.1f, 0.1f, 1.0f);

        ShaderUtil.checkGLError(TAG, "After draw");
    }
}