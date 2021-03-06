package org.ntlab.graffiti.common.views;

import android.app.Activity;
import android.content.Context;
import android.util.AttributeSet;
import android.view.animation.Animation;
import android.widget.FrameLayout;

import androidx.appcompat.widget.AppCompatImageView;

import org.ntlab.graffiti.R;

/**
 * This view contains the hand motion instructions with animation.
 *  @author a-hongo
 */
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
