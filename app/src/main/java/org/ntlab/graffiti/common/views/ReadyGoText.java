package org.ntlab.graffiti.common.views;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.FrameLayout;
import android.widget.TextView;

import org.ntlab.graffiti.R;

/**
 * Created by a-hongo on 01,3月,2021
 */
public class ReadyGoText extends FrameLayout {
    private TextView readyText;
    private TextView goText;

    private Animation slideinAnim;
    private Animation fadeoutAnim;
    private Animation slowinAnim;
    private Animation fastoutAnim;

    public ReadyGoText(Context context) {
        super(context);
        init();
    }

    public ReadyGoText(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        inflate(getContext(), R.layout.view_ready_go_text, this);

        readyText = findViewById(R.id.ready_text);
        readyText.setVisibility(View.INVISIBLE);

        goText = findViewById(R.id.go_text);
        goText.setVisibility(View.INVISIBLE);

        slideinAnim = AnimationUtils.loadAnimation(getContext(), R.anim.anim_slidein);
        fadeoutAnim = AnimationUtils.loadAnimation(getContext(), R.anim.anim_fadeout);
        fadeoutAnim.setStartOffset(1000);
        slowinAnim = AnimationUtils.loadAnimation(getContext(), R.anim.anim_slowin);
        fastoutAnim = AnimationUtils.loadAnimation(getContext(), R.anim.anim_fastout);
        fastoutAnim.setStartOffset(1000);
    }

    public void startAnimation(Animation.AnimationListener animationListener) {
        slideinAnim.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
                animationListener.onAnimationStart(animation);
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                readyText.startAnimation(fadeoutAnim);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
            }
        });
        fadeoutAnim.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                readyText.setVisibility(View.INVISIBLE);
                goText.startAnimation(slowinAnim);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
            }
        });
        slowinAnim.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
                goText.setVisibility(View.VISIBLE);
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                goText.startAnimation(fastoutAnim);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
            }
        });
        fastoutAnim.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                goText.setVisibility(View.INVISIBLE);
                animationListener.onAnimationEnd(animation);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
            }
        });

        readyText.setVisibility(View.VISIBLE);
        readyText.startAnimation(slideinAnim);
    }
}
