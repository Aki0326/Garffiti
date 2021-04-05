package org.ntlab.graffiti.common.helpers;

import android.opengl.GLES30;

import org.ntlab.graffiti.common.rendering.GLError;

/**
 * Helper common to renderer.
 * @author a-hongo
 */
public final class RendererHelper {

    public void enableBlend() {
        GLES30.glEnable(GLES30.GL_BLEND);
        GLError.maybeThrowGLException("Failed to enable blending", "glEnable");
    }

    public void clear(float r, float g, float b, float a) {
        GLES30.glClearColor(r, g, b, a);
        GLError.maybeThrowGLException("Failed to set clear color", "glClearColor");
        GLES30.glDepthMask(true);
        GLError.maybeThrowGLException("Failed to set depth write mask", "glDepthMask");
        GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT | GLES30.GL_DEPTH_BUFFER_BIT);
        GLError.maybeThrowGLException("Failed to clear framebuffer", "glClear");
    }
}
