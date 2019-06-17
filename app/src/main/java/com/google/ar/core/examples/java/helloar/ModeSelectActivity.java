package com.google.ar.core.examples.java.helloar;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;

public class ModeSelectActivity extends AppCompatActivity implements View.OnClickListener {
    private static final String TAG = ModeSelectActivity.class.getSimpleName();

    private Button graffityModeButton;
    private Button coloringbattleModeButton;
    private Button photogalleryButton;

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
