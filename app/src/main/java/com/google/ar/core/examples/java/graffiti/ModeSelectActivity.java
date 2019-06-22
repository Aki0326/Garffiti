package com.google.ar.core.examples.java.graffiti;

import android.content.Intent;
import android.os.Bundle;
import android.os.PowerManager;
import android.support.v7.app.AppCompatActivity;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;

import com.google.ar.core.examples.java.common.helpers.MusicPlayerHelper;

import java.io.IOException;

public class ModeSelectActivity extends AppCompatActivity implements View.OnClickListener {
    public static final String TAG = ModeSelectActivity.class.getSimpleName();

    private MusicPlayerHelper modeSelectBGM = new MusicPlayerHelper();
    private MusicPlayerHelper modeSelectClickSE = new MusicPlayerHelper();
    private Boolean isLoop = true;

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

        try {
            modeSelectBGM.musicPlay(this, "musics/bgm/title.ogg", isLoop);
        } catch (IOException e) {
            e.printStackTrace();
        }
}

    @Override
    protected void onPause() {
        super.onPause();

        wakeLock.release();
    }

    @Override
    public void onClick(View view) {
        modeSelectBGM.musicStop();
        try {
            isLoop = false;
            modeSelectClickSE.musicPlay(this, "musics/se/click-sound.mp3", isLoop);
        } catch (IOException e) {
            e.printStackTrace();
        }
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

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if(event.getAction() == KeyEvent.ACTION_DOWN) {
            switch(event.getKeyCode()) {
                case KeyEvent.KEYCODE_BACK:
                    return true;
            }
        }
        return super.dispatchKeyEvent(event);
    }
}
