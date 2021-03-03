package org.ntlab.graffiti.common.helpers;

import android.app.Activity;
import android.content.Intent;
import android.os.CountDownTimer;

import org.ntlab.graffiti.graffiti.ModeSelectActivity;

/**
 * Helper to back to ModeSelectActivity after START_TIME.
 * @author a-hongo
 */
public class TimeoutHelper {
    private static long START_TIME = 600000; // 10m
    private static long timeLeftInMillis = START_TIME;
    private static CountDownTimer countDownTimer = null;

    /**
     * Start the CountDownTimer.
     *
     * @param activity the application's current activity.
     */
    public static void startTimer(Activity activity){
        if (countDownTimer == null) {
            countDownTimer = new CountDownTimer(timeLeftInMillis,START_TIME) {
                @Override
                public void onTick(long millisUntilFinished) {
                    timeLeftInMillis = millisUntilFinished;
                }

                @Override
                public void onFinish() {
                    Intent intent = new Intent(activity, ModeSelectActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    activity.startActivity(intent);
                }
            };
        }
        countDownTimer.start();
    }

    /**
     * Reset the CountDownTimer.
     */
    public static void resetTimer(){
        if(countDownTimer != null) {
            countDownTimer.cancel();
            timeLeftInMillis = START_TIME;
        }
    }

}
