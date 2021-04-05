/*
 * Copyright 2020 Google LLC
 *
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
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.opengl.GLES30;
import android.util.Log;

import java.io.Closeable;
import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * A GPU-side texture.
 * @author a-hongo
 */
public class Texture implements Closeable {
  private static final String TAG = Texture.class.getSimpleName();

  private final int[] textureId = {0};
  private int textureTarget;

  /**
   * Construct an empty {@link Texture}.
   *
   * <p>Since {@link Texture}s created in this way are not populated with data, this method is
   * mostly only useful for creating {@link android.opengl.GLES11Ext#GL_TEXTURE_EXTERNAL_OES} textures. See {@link
   * #createFromAsset} if you want a texture with data.
   */
  public Texture(int textureTarget, int wrapMode) {
    this(textureTarget, wrapMode, /*useMipmaps=*/ true);
  }

  public Texture(int textureTarget, int wrapMode, boolean useMipmaps) {
    this.textureTarget = textureTarget;

    GLES30.glGenTextures(1, textureId, 0);
    GLError.maybeThrowGLException("Texture creation failed", "glGenTextures");

    int minFilter = useMipmaps ? GLES30.GL_LINEAR_MIPMAP_LINEAR : GLES30.GL_LINEAR;

    try {
      GLES30.glBindTexture(textureTarget, textureId[0]);
      GLError.maybeThrowGLException("Failed to bind texture", "glBindTexture");
      GLES30.glTexParameteri(textureTarget, GLES30.GL_TEXTURE_MIN_FILTER, minFilter);
      GLError.maybeThrowGLException("Failed to set texture parameter", "glTexParameteri");
      GLES30.glTexParameteri(textureTarget, GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_LINEAR);
      GLError.maybeThrowGLException("Failed to set texture parameter", "glTexParameteri");

      GLES30.glTexParameteri(textureTarget, GLES30.GL_TEXTURE_WRAP_S, wrapMode);
      GLError.maybeThrowGLException("Failed to set texture parameter", "glTexParameteri");
      GLES30.glTexParameteri(textureTarget, GLES30.GL_TEXTURE_WRAP_T, wrapMode);
      GLError.maybeThrowGLException("Failed to set texture parameter", "glTexParameteri");
    } catch (Throwable t) {
      close();
      throw t;
    }
  }

  /** Create a texture from the given asset file name. */
  public static Texture createFromAsset(Context context, String assetFileName, int wrapMode, int colorFormat) throws IOException {
    Texture texture = new Texture(GLES30.GL_TEXTURE_2D, wrapMode);
    Bitmap bitmap = null;
    try {
      // The following lines up to glTexImage2D could technically be replaced with
      // GLUtils.texImage2d, but this method does not allow for loading sRGB images.

      // Load and convert the bitmap and copy its contents to a direct ByteBuffer. Despite its name,
      // the ARGB_8888 config is actually stored in RGBA order.
      bitmap =
          convertBitmapToConfig(
              BitmapFactory.decodeStream(context.getAssets().open(assetFileName)),
              Bitmap.Config.ARGB_8888);
      ByteBuffer buffer = ByteBuffer.allocateDirect(bitmap.getByteCount());
      bitmap.copyPixelsToBuffer(buffer);
      buffer.rewind();

      GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, texture.getTextureId());
      GLError.maybeThrowGLException("Failed to bind texture", "glBindTexture");
      GLES30.glTexImage2D(
          GLES30.GL_TEXTURE_2D,
          /*level=*/ 0,
          colorFormat,
          bitmap.getWidth(),
          bitmap.getHeight(),
          /*border=*/ 0,
          GLES30.GL_RGBA,
          GLES30.GL_UNSIGNED_BYTE,
          buffer);
      GLError.maybeThrowGLException("Failed to populate texture data", "glTexImage2D");
      GLES30.glGenerateMipmap(GLES30.GL_TEXTURE_2D);
      GLError.maybeThrowGLException("Failed to generate mipmaps", "glGenerateMipmap");
    } catch (Throwable t) {
      texture.close();
      throw t;
    } finally {
      if (bitmap != null) {
        bitmap.recycle();
      }
    }
    return texture;
  }

  @Override
  public void close() {
    if (textureId[0] != 0) {
      GLES30.glDeleteTextures(1, textureId, 0);
      GLError.maybeLogGLError(Log.WARN, TAG, "Failed to free texture", "glDeleteTextures");
      textureId[0] = 0;
    }
  }

  /** Retrieve the native texture ID. */
  public int getTextureId() {
    return textureId[0];
  }

  /* package-private */
  int getTarget() {
    return textureTarget;
  }

  private static Bitmap convertBitmapToConfig(Bitmap bitmap, Bitmap.Config config) {
    // We use this method instead of BitmapFactory.Options.outConfig to support a minimum of Android
    // API level 24.
    if (bitmap.getConfig() == config) {
      return bitmap;
    }
    Bitmap result = bitmap.copy(config, /*isMutable=*/ false);
    bitmap.recycle();
    return result;
  }
}
