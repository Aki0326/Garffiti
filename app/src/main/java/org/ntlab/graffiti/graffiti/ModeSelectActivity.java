package org.ntlab.graffiti.graffiti;

import android.content.Intent;
import android.os.Bundle;
import android.os.PowerManager;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

import org.ntlab.graffiti.R;
import org.ntlab.graffiti.common.helpers.MusicPlayerHelper;

import java.io.IOException;

/**
 * Select the mode in ModeSelectActivity.
 * @author a-hongo
 */
public class ModeSelectActivity extends AppCompatActivity implements View.OnClickListener, View.OnTouchListener {
    public static final String TAG = ModeSelectActivity.class.getSimpleName();

    private MusicPlayerHelper modeSelectBGM = new MusicPlayerHelper();
    private MusicPlayerHelper modeSelectClickSE = new MusicPlayerHelper();
    private Boolean isLoop; // true:BGMをループする

    private Button graffitiModeButton;
    private Button sharedGraffitiModeButton;
    private Button graffitiTimeAttackModeButton;
    private Button photoGalleryButton;

    private PowerManager.WakeLock wakeLock; // 画面がスリープしないように

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mode_select);

        graffitiModeButton = findViewById(R.id.graffiti_mode_button);
        graffitiModeButton.setOnClickListener(this);
        graffitiModeButton.setOnTouchListener(this);

        sharedGraffitiModeButton = findViewById(R.id.shared_graffiti_mode_button);
        sharedGraffitiModeButton.setOnClickListener(this);
        sharedGraffitiModeButton.setOnTouchListener(this);

        graffitiTimeAttackModeButton = findViewById(R.id.graffiti_time_attack_mode_button);
        graffitiTimeAttackModeButton.setOnClickListener(this);
        graffitiTimeAttackModeButton.setOnTouchListener(this);

        photoGalleryButton = findViewById(R.id.photo_gallery_mode_button);
        photoGalleryButton.setOnClickListener(this);
        photoGalleryButton.setOnTouchListener(this);
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
    public void onClick(View view) {
        modeSelectClickSE.musicStop();
        modeSelectBGM.musicStop();
        try {
            isLoop = false;
            modeSelectClickSE.musicPlay(this, "musics/se/click-sound.mp3", isLoop);
        } catch (IOException e) {
            e.printStackTrace();
        }
        switch (view.getId()) {
            case R.id.graffiti_mode_button:
                ((Graffiti) getApplication()).outputLine("Graffiti mode Clicked.");
                startActivity(new Intent(ModeSelectActivity.this, GraffitiActivity.class));
                break;
            case R.id.shared_graffiti_mode_button:
                ((Graffiti) getApplication()).outputLine("Shared graffiti mode Clicked.");
                startActivity(new Intent(ModeSelectActivity.this, SharedGraffitiActivity.class));
                break;
            case R.id.graffiti_time_attack_mode_button:
                ((Graffiti) getApplication()).outputLine("Graffiti time attack mode Clicked.");
                startActivity(new Intent(ModeSelectActivity.this, GraffitiTimeAttackActivity.class));
                break;
            case R.id.photo_gallery_mode_button:
                ((Graffiti) getApplication()).outputLine("Photo gallery mode Clicked.");
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

    @Override
    public boolean onTouch(View view, MotionEvent motionEvent) {
        Button clickedButton = (Button)findViewById(view.getId());
        if (clickedButton != null) {
            if (motionEvent.getAction() == MotionEvent.ACTION_DOWN) {
                float downScale = 1.2f;
                clickedButton.setScaleX(downScale);
                clickedButton.setScaleY(downScale);
            } else if (motionEvent.getAction() == MotionEvent.ACTION_UP) {
                float upScale = 1.0f;
                clickedButton.setScaleX(upScale);
                clickedButton.setScaleY(upScale);
            }
        }
        return false;
    }
}
