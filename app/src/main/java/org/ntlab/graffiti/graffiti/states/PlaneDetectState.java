package org.ntlab.graffiti.graffiti.states;

/**
 * State to detect plane.
 *
 * Created by a-hongo on 05,3月,2021
 * @author a-hogno
 */
// TODO Move here the process to detect Planes in each activity
public class PlaneDetectState extends State {
    @Override
    public boolean canChange(State nextState) {
        return false;
    }

    @Override
    public void enter() {

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
}
