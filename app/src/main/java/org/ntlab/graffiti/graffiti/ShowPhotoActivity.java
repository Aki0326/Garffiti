package org.ntlab.graffiti.graffiti;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.MotionEvent;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;

import org.ntlab.graffiti.R;
import org.ntlab.graffiti.common.helpers.TimeoutHelper;

/**
 * This ShowPhotoActivity shows the selected picture.
 * @author a-hongo
 */
public class ShowPhotoActivity  extends AppCompatActivity {
    private static final String TAG = ShowPhotoActivity.class.getSimpleName();
    private ImageView imagePhoto;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_show_photo);

        Graffiti app = (Graffiti)this.getApplication();
        Bitmap bmp = app.getBitmap();

        imagePhoto = findViewById(R.id.image_photo);
        imagePhoto.setImageBitmap(bmp);

        app.clearBitmap();
    }

    @Override
    protected void onResume() {
        super.onResume();
        TimeoutHelper.startTimer(ShowPhotoActivity.this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        TimeoutHelper.resetTimer();
    }

    @Override
    public boolean onTouchEvent(MotionEvent motionEvent) {
        if (motionEvent.getAction() != MotionEvent.ACTION_UP) {
            TimeoutHelper.resetTimer();
            return true;
        } else {
            TimeoutHelper.startTimer(ShowPhotoActivity.this);
            return false;
        }
    }
}
