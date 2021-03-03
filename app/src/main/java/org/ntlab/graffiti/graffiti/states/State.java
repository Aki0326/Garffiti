package org.ntlab.graffiti.graffiti.states;

/**
 * Created by a-hongo on 01,3月,2021
 */
public abstract class State {

    /**
     * 現在の状態から次の状態に遷移可能か否かを返す
     * @param nextState 次の状態
     * @return true --- 遷移可能, false --- 遷移不可能
     */
    public abstract boolean canChange(State nextState);
}
