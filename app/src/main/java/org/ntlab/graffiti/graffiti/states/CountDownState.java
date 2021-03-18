package org.ntlab.graffiti.graffiti.states;

import android.os.CountDownTimer;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

/**
 * State to control CountDownTimer.
 *
 * Created by a-hongo on 05,3月,2021
 * @author a-hongo
 */
public class CountDownState extends State {
    //    private static final long START_TIME = 61000; //61s
    private static final long START_TIME = 6100; //61s
    private static final long COUNT_DOWN_INTERVAL = 1000;
    private long timeLeftInMillis = START_TIME;

    private CountDownTimer countDownTimer;
    private TextView timerText;
    private ImageView timerBgImage;

    public CountDownState(TextView timerText) {
        this.timerText = timerText;
    }

    public CountDownState(TextView timerText, ImageView timerBgImage) {
        this(timerText);
        this.timerBgImage = timerBgImage;
        init();
    }

    private void init() {
        timerBgImage.setVisibility(View.INVISIBLE);
    }

    @Override
    public boolean canChange(State nextState) {
        return false;
    }

    @Override
    public void enter() {
        resetTimer();
        startTimer();
    }

    @Override
    public void restart() {
        restartTimer();
    }

    @Override
    public void pause() {
        pauseTimer();
    }

    @Override
    public void exit() {

    }

    private void startTimer() {
        if (timerBgImage != null) {
            timerBgImage.setVisibility(View.VISIBLE);
        }
        // Set up the countDownTimer.
        countDownTimer = new GameCountDownTimer(START_TIME, COUNT_DOWN_INTERVAL);
        countDownTimer.start();
    }

    private void restartTimer() {
        // Set up the countDownTimer by timeLeftIn.
        countDownTimer = new GameCountDownTimer(timeLeftInMillis + COUNT_DOWN_INTERVAL, COUNT_DOWN_INTERVAL);
        countDownTimer.start();
    }

    private void pauseTimer() {
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
    }

    private void resetTimer() {
        if (countDownTimer != null) {
            if (timerBgImage != null) {
                timerBgImage.setVisibility(View.INVISIBLE);
            }
            countDownTimer.cancel();
            timeLeftInMillis = START_TIME;
        }
    }

    // CountDownTimerクラスを継承して、GameCountDownTimer
    class GameCountDownTimer extends CountDownTimer {

        public GameCountDownTimer(long millisInFuture, long countDownInterval) {
            super(millisInFuture, countDownInterval);
        }

        // カウントダウン処理
        @Override
        public void onTick(long millisUntilFinished) {
            timeLeftInMillis = millisUntilFinished;
            long mm = timeLeftInMillis / 1000 / 60;
            long ss = timeLeftInMillis / 1000 % 60;
            timerText.setText(String.format("%1$01d:%2$02d", mm, ss));
        }

        // カウントダウン終了後の処理
        @Override
        public void onFinish() {
            finish();
        }
    }
}
