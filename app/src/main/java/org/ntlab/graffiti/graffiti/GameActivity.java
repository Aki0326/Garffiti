package org.ntlab.graffiti.graffiti;

import org.ntlab.graffiti.graffiti.states.State;
import org.ntlab.graffiti.graffiti.states.StateListener;

/**
 * Created by a-hongo on 04,3月,2021
 * @author a-hongo
 */
public abstract class GameActivity extends ArActivity implements StateListener {

    private State curState;

    @Override
    public abstract void onExitState(State state);

    public void restartCurState() {
        if (curState != null) {
            curState.restart();
        }
    }

    public void pauseCurState() {
        if (curState != null) {
            curState.pause();
        }
    }

    public void changeState(State state) {
        if (curState != null) {
            curState.exit();
        }
        curState = state;
        curState.setStateListener(this);
        curState.enter();
    }

    public State getCurState() {
        return curState;
    }
}
