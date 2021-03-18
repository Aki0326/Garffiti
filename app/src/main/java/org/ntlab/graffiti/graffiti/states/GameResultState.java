package org.ntlab.graffiti.graffiti.states;

import android.content.Context;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.TextView;

import org.ntlab.graffiti.R;
import org.ntlab.graffiti.common.views.CountUpAnimation;

/**
 * State to display my game result.
 *
 * Created by a-hongo on 03,3月,2021
 * @author a-hongo
 */
public class GameResultState extends State {
    private static final int ANIMATION_PERIOD = 2000;

    private TextView myResultText;
    private Animation countUpAnim;
    private Animation bounceoutAnim;
    private long score = 0;

    public GameResultState(TextView myResultText) {
        this.myResultText = myResultText;
        init();
    }

    public GameResultState(Context context, TextView myResultText) {
        this(myResultText);
        initAnimation(context);
    }

    public GameResultState(TextView myResultText, long score) {
        this(myResultText);
        this.score = score;
    }

    private void init() {
        myResultText.setVisibility(View.INVISIBLE);
        myResultText.setText(0 + "p");
        countUpAnim = new CountUpAnimation(myResultText);
        countUpAnim.setDuration(ANIMATION_PERIOD);
        countUpAnim.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {

            }

            @Override
            public void onAnimationEnd(Animation animation) {
                myResultText.startAnimation(bounceoutAnim);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });
    }

    private void initAnimation(Context context) {
        bounceoutAnim = AnimationUtils.loadAnimation(context, R.anim.anim_bounceout);
        // アニメーション後も状態を維持する
        bounceoutAnim.setFillAfter(true);
        bounceoutAnim.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
                finish();
            }

            @Override
            public void onAnimationEnd(Animation animation) {
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
            }
        });

    }

    public void setScore(long score) {
        this.score = score;
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

    private void startAnimation() {
        if (countUpAnim instanceof CountUpAnimation) {
            ((CountUpAnimation) countUpAnim).setCountUpNumber(score);
        }
        myResultText.setVisibility(View.VISIBLE);
        myResultText.startAnimation(countUpAnim);
    }
}
