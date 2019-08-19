package com.google.ar.core.examples.java.common.views;

import android.app.Activity;
import android.content.Context;
import android.support.v7.widget.AppCompatImageView;
import android.util.AttributeSet;
import android.view.animation.Animation;
import android.widget.FrameLayout;

import com.google.ar.core.examples.java.graffiti.R;

/** This view contains the hand motion instructions with animation. */
public class HandMotion  extends AppCompatImageView {
    private HandMotionAnimation animation;
    private static final long ANIMATION_SPEED_MS = 2500;

    public HandMotion(Context context) {
        super(context);
    }

    public HandMotion(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();

        clearAnimation();

        FrameLayout container = (FrameLayout) ((Activity) getContext()).findViewById(R.id.hand_layout);

        animation = new HandMotionAnimation(container, this);
        animation.setRepeatCount(Animation.INFINITE);
        animation.setDuration(ANIMATION_SPEED_MS);
        animation.setStartOffset(1000);

        startAnimation(animation);
    }
}
