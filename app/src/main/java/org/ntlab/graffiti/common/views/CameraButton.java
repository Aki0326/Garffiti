package org.ntlab.graffiti.common.views;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import org.ntlab.graffiti.R;

/**
 * This class controll the camera button.
 */
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
