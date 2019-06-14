package com.google.ar.core.examples.java.common.view;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import com.google.ar.core.examples.java.helloar.R;

class CameraButton extends RelativeLayout implements View.OnClickListener {
    private static final String TAG = CameraButton.class.getSimpleName();

    private ImageView cameraButton;
    public CameraButton(Context context) {
        super(context);
        init();
    }

    public CameraButton(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }
    private void init() {
        inflate(getContext(), R.layout.view_camera_button, this);

        cameraButton = findViewById(R.id.camera_image);
        cameraButton.setOnClickListener(this);
    }

    @Override
    public void onClick(View view) {

    }
}