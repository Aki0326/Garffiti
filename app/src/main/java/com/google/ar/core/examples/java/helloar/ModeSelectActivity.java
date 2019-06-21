package com.google.ar.core.examples.java.helloar;

import android.content.Intent;
import android.os.Bundle;
import android.os.PowerManager;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;

public class ModeSelectActivity extends AppCompatActivity implements View.OnClickListener {
    public static final String TAG = ModeSelectActivity.class.getSimpleName();

    private Button graffityModeButton;
    private Button coloringbattleModeButton;
    private Button photogalleryButton;

    private PowerManager.WakeLock wakeLock;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mode_select);

        graffityModeButton = findViewById(R.id.graffity_mode_button);
        graffityModeButton.setOnClickListener(this);

        coloringbattleModeButton = findViewById(R.id.coloringbattle_mode_button);
        coloringbattleModeButton.setOnClickListener(this);

        photogalleryButton = findViewById(R.id.photogallery_button);
        photogalleryButton.setOnClickListener(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if(wakeLock!=null){
            wakeLock.acquire();
        }else{
            PowerManager pm = (PowerManager)getSystemService(POWER_SERVICE);
            wakeLock = pm.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK, ":tag");
            wakeLock.acquire();
        }
}

    @Override
    protected void onPause() {
        super.onPause();

        wakeLock.release();
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.graffity_mode_button:
                startActivity(new Intent(ModeSelectActivity.this, HelloArActivity.class));
                break;
            case R.id.coloringbattle_mode_button:
                break;
            case R.id.photogallery_button:
                startActivity(new Intent(ModeSelectActivity.this, PhotoGalleryActivity.class));
                break;

        }
    }
}
