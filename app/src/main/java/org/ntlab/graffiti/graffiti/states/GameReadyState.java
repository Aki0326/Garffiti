package org.ntlab.graffiti.graffiti.states;

import android.content.Context;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.TextView;

import org.ntlab.graffiti.R;
import org.ntlab.graffiti.common.views.Arc;

/**
 * State to display Ready? GO!.
 *
 * Created by a-hongo on 01,3月,2021
 * @author a-hongo
 */
public class GameReadyState extends State {
    private TextView readyText;
    private TextView goText;
    private View arcView;

    private Animation slideinAnim;
    private Animation fadeoutAnim;
    private Animation slowinAnim;
    private Animation fastoutAnim;

    public GameReadyState(Context context, TextView readyText, TextView goText) {
        initAnimation(context);
        this.readyText = readyText;
        this.goText = goText;
        init();
    }

    public GameReadyState(Context context, TextView readyText, TextView goText, View arcView) {
        this(context, readyText, goText);
        this.arcView = arcView;
    }

    private void init() {
        readyText.setVisibility(View.INVISIBLE);
        goText.setVisibility(View.INVISIBLE);
    }

    private void initAnimation(Context context) {
        slideinAnim = AnimationUtils.loadAnimation(context, R.anim.anim_slidein);
        fadeoutAnim = AnimationUtils.loadAnimation(context, R.anim.anim_fadeout);
        fadeoutAnim.setStartOffset(1000);
        slowinAnim = AnimationUtils.loadAnimation(context, R.anim.anim_slowin);
        fastoutAnim = AnimationUtils.loadAnimation(context, R.anim.anim_fastout);
        fastoutAnim.setStartOffset(1000);
        slideinAnim.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
                if (arcView instanceof Arc) {
                    ((Arc) arcView).startAnimation(360);
                }
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
                finish();
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
            }
        });
    }

    @Override
    public boolean canChange(State nextState) {
        return false;
    }

    @Override
    public void enter() {
        startAnimation();
    }

    @Override
    public void restart() {

    }

    @Override
    public void pause() {

    }

    @Override
    public void exit() {

    }

    public void startAnimation() {
        readyText.setVisibility(View.VISIBLE);
        readyText.startAnimation(slideinAnim);
    }
}
