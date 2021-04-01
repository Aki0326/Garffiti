package org.ntlab.graffiti.common.views;

import android.view.animation.Animation;
import android.view.animation.Transformation;

/**
 * Created by a-hongo on 28,2月,2021
 * 参考: https://akira-watson.com/android/canvas-animation.html
 */
public class ArcAnimation extends Animation {
    private Arc arcView;

    // アニメーション角度
    private float newAngle;

    public ArcAnimation(Arc arcView) {
        this.arcView = arcView;
    }

    public void setAngle(float newAngle) {
        this.newAngle = newAngle;
    }

    @Override
    protected void applyTransformation(float interpolatedTime, Transformation transformation) {
        float oldAngle = arcView.getCurAngle();
        float angle = oldAngle + ((newAngle - oldAngle) * interpolatedTime);
        arcView.setCurAngle(angle);
        arcView.requestLayout();
    }
}
