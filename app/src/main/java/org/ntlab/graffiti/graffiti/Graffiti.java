package org.ntlab.graffiti.graffiti;

import android.app.Activity;
import android.app.Application;
import android.graphics.Bitmap;
import android.os.Bundle;

import org.ntlab.graffiti.common.helpers.WriteLogThreadHelper;

/**
 * This class is main application of graffiti.
 */
public class Graffiti extends Application {
    private final String TAG = Graffiti.class.getSimpleName();
    private Bitmap bitmap;
    private WriteLogThreadHelper writeLogThread = null;

    public Graffiti() {

        registerActivityLifecycleCallbacks(new ActivityLifecycleCallbacks() {
            @Override
            public void onActivityCreated(Activity activity, Bundle bundle) {
                if (writeLogThread == null) {
                    writeLogThread = new WriteLogThreadHelper(activity.getApplicationContext());
                    writeLogThread.start();
                }
            }

            @Override
            public void onActivityStarted(Activity activity) {
            }

            @Override
            public void onActivityResumed(Activity activity) {
            }

            @Override
            public void onActivityPaused(Activity activity) {
            }

            @Override
            public void onActivityStopped(Activity activity) {
            }

            @Override
            public void onActivitySaveInstanceState(Activity activity, Bundle bundle) {
            }

            @Override
            public void onActivityDestroyed(Activity activity) {
            }
        });
    }

    public void setBitmap(Bitmap bmp){
        bitmap = bmp;
    }

    public Bitmap getBitmap(){
        return bitmap;
    }

    public void clearBitmap(){
        bitmap = null;
    }

    public void outputLine(String line) {
        writeLogThread.outputLine(line);
    }

}
