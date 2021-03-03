package org.ntlab.graffiti.graffiti.states;

import android.app.Activity;
import android.view.animation.Animation;

import org.ntlab.graffiti.R;
import org.ntlab.graffiti.common.views.ReadyGoText;

/**
 * Created by a-hongo on 01,3月,2021
 */
public class ReadyGoState extends State {
    private ReadyGoText readyGoText;

    private Animation.AnimationListener animationListener;

    public ReadyGoState(Activity activity, Animation.AnimationListener animationListener) {
        this.animationListener = animationListener;
        init(activity);
    }

    private void init(Activity activity) {
        readyGoText = activity.findViewById(R.id.ready_go_text_view);
    }

    @Override
    public boolean canChange(State nextState) {
        return false;
    }

    public void showReadyGo() {
        readyGoText.startAnimation(animationListener);
    }
}
