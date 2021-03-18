package org.ntlab.graffiti.graffiti;

import android.opengl.GLSurfaceView;

import androidx.appcompat.app.AppCompatActivity;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/**
 * Created by a-hongo on 04,3月,2021
 * @author a-hongo
 */
public abstract class ArActivity extends AppCompatActivity implements GLSurfaceView.Renderer {
    private static final String TAG = ArActivity.class.getSimpleName();

    // Rendering. The Renderers are created here, and initialized when the GL surface is created.
    protected GLSurfaceView surfaceView;

    @Override
    public abstract void onSurfaceCreated(GL10 gl, EGLConfig config);

    @Override
    public abstract void onSurfaceChanged(GL10 gl, int width, int height);

    @Override
    public abstract void onDrawFrame(GL10 gl);
}
