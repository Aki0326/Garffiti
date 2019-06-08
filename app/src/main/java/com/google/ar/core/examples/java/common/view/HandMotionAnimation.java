package com.google.ar.core.examples.java.common.view;

import android.view.View;
import android.view.animation.Animation;
import android.view.animation.Transformation;

/**
 * This drives the AR hand motion animation.
 */
public class HandMotionAnimation extends Animation{
    private final View handImageView;
    private final View containerView;
    private static final float TWO_PI = (float) Math.PI * 2.0f;
    private static final float HALF_PI = (float) Math.PI / 2.0f;

    public HandMotionAnimation(View containerView, View handImageView) {
        this.handImageView = handImageView;
        this.containerView = containerView;
    }

    @Override
    protected void applyTransformation(float interpolatedTime, Transformation transformation) {
        float startAngle = HALF_PI;
        float progressAngle = TWO_PI * interpolatedTime;
        float currentAngle = startAngle + progressAngle;

        float handWidth = handImageView.getWidth();
        float radius = handImageView.getResources().getDisplayMetrics().density * 25.0f;

        float xPos = radius * 2.0f * (float) Math.cos(currentAngle);
        float yPos = radius * (float) Math.sin(currentAngle);

        xPos += containerView.getWidth() / 2.0f;
        yPos += containerView.getHeight() / 2.0f;

        xPos -= handWidth / 2.0f;
        yPos -= handImageView.getHeight() / 2.0f;

        // Position the hand.
        handImageView.setX(xPos);
        handImageView.setY(yPos);

        handImageView.invalidate();
    }
}
