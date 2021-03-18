package org.ntlab.graffiti.graffiti.states;

/**
 * State pattern
 *
 * Created by a-hongo on 01,3月,2021
 * @author a-hongo
 */
public abstract class State {
    protected StateListener stateListener;

    public final void setStateListener(StateListener stateListener) {
        this.stateListener = stateListener;
    }

    /**
     * 現在の状態から次の状態に遷移可能か否かを返す
     * @param nextState 次の状態
     * @return true --- 遷移可能, false --- 遷移不可能
     */
    public abstract boolean canChange(State nextState);

    public abstract void enter();

    public abstract void restart();

    public abstract void pause();

    public abstract void exit();

    public final void finish() {
        stateListener.onExitState(this);
    }

}
