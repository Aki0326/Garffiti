package org.ntlab.graffiti.graffiti;

import android.content.Intent;
import android.os.Bundle;
import android.os.PowerManager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import org.ntlab.graffiti.R;
import org.ntlab.graffiti.common.helpers.MusicPlayerHelper;

import java.io.IOException;

public class ModeSelectActivity extends AppCompatActivity implements View.OnClickListener, View.OnTouchListener {
    public static final String TAG = ModeSelectActivity.class.getSimpleName();

    private MusicPlayerHelper modeSelectBGM = new MusicPlayerHelper();
    private MusicPlayerHelper modeSelectClickSE = new MusicPlayerHelper();
    private Boolean isLoop;

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
        graffityModeButton.setOnTouchListener(this);

        coloringbattleModeButton = findViewById(R.id.coloringbattle_mode_button);
        coloringbattleModeButton.setOnClickListener(this);
        coloringbattleModeButton.setOnTouchListener(this);

        photogalleryButton = findViewById(R.id.photogallery_button);
        photogalleryButton.setOnClickListener(this);
        photogalleryButton.setOnTouchListener(this);
    }

    @Override
    protected void onRestart() {
        super.onRestart();

        if(modeSelectClickSE != null) {
            modeSelectClickSE.musicStop();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (wakeLock != null) {
            wakeLock.acquire();
        } else {
            PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
            wakeLock = pm.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK, ":tag");
            wakeLock.acquire();
        }

        try {
            isLoop = true;
            modeSelectBGM.musicPlay(this, "musics/bgm/title.ogg", isLoop);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        wakeLock.release();
        modeSelectBGM.musicStop();
    }

    @Override
    protected void onDestroy() {
        modeSelectBGM.musicStop();

        super.onDestroy();
    }

    @Override
    public void onClick(View view) {
        modeSelectClickSE.musicStop();
        switch (view.getId()) {
            case R.id.graffity_mode_button:
                modeSelectBGM.musicStop();
                try {
                    isLoop = false;
                    modeSelectClickSE.musicPlay(this, "musics/se/click-sound.mp3", isLoop);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                ((Graffiti) getApplication()).outputLine("Graffiti mode Clicked.");
                startActivity(new Intent(ModeSelectActivity.this, HelloArActivity.class));
                break;
            case R.id.coloringbattle_mode_button:
                Toast.makeText(this, "すみません。ただいま工事中です。", Toast.LENGTH_SHORT).show();
                ((Graffiti) getApplication()).outputLine("Cloring battle mode Clicked.");
                break;
            case R.id.photogallery_button:
                modeSelectBGM.musicStop();
                try {
                    isLoop = false;
                    modeSelectClickSE.musicPlay(this, "musics/se/click-sound.mp3", isLoop);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                ((Graffiti) getApplication()).outputLine("Photo gallery mode Clicked.");
                startActivity(new Intent(ModeSelectActivity.this, PhotoGalleryActivity.class));
                break;
        }
    }

//    @Override
//    public boolean dispatchKeyEvent(KeyEvent event) {
//        if(event.getAction() == KeyEvent.ACTION_DOWN) {
//            switch(event.getKeyCode()) {
//                case KeyEvent.KEYCODE_BACK:
//                    return true;
//            }
//        }
//        return super.dispatchKeyEvent(event);
//    }

    @Override
    public boolean onTouch(View view, MotionEvent motionEvent) {
        switch (view.getId()) {
            case R.id.graffity_mode_button:
                if(motionEvent.getAction() == MotionEvent.ACTION_DOWN) {
                    graffityModeButton.setScaleX(1.2f);
                    graffityModeButton.setScaleY(1.2f);
                } else if(motionEvent.getAction() == MotionEvent.ACTION_UP) {
                    graffityModeButton.setScaleX(1.0f);
                    graffityModeButton.setScaleY(1.0f);
                }
                break;
            case R.id.coloringbattle_mode_button:
                break;
            case R.id.photogallery_button:
                if(motionEvent.getAction() == MotionEvent.ACTION_DOWN) {
                    photogalleryButton.setScaleX(1.2f);
                    photogalleryButton.setScaleY(1.2f);
                } else if(motionEvent.getAction() == MotionEvent.ACTION_UP) {
                    photogalleryButton.setScaleX(1.0f);
                    photogalleryButton.setScaleY(1.0f);
                }
                break;
        }
        return false;
    }
}
