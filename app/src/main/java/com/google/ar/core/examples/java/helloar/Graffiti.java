package com.google.ar.core.examples.java.helloar;

import android.app.Activity;
import android.app.Application;
import android.graphics.Bitmap;
import android.os.Bundle;

public class Graffiti extends Application {
    private final String TAG = Graffiti.class.getSimpleName();
    private Bitmap bitmap;

    public Graffiti() {

        registerActivityLifecycleCallbacks(new ActivityLifecycleCallbacks() {
            @Override
            public void onActivityCreated(Activity activity, Bundle bundle) {
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

}