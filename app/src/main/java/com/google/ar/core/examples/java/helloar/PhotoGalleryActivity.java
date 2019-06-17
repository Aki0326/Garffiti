package com.google.ar.core.examples.java.helloar;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.support.v7.app.AppCompatActivity;
import android.widget.ImageView;

import java.io.FileDescriptor;
import java.io.IOException;

public class PhotoGalleryActivity extends AppCompatActivity {
    private static final String TAG = PhotoGalleryActivity.class.getSimpleName();

    private static final int RESULT_PICK_IMAGEFILE = 1000;
    private ImageView imagePhoto;
    private boolean setImage = false; // false():Imageが選択されずfinish()、true:Imageが選択されて続行

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_photo_gallery);

        imagePhoto = findViewById(R.id.image_photo);
        setImage = true;

        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("image/*");
        startActivityForResult(intent, RESULT_PICK_IMAGEFILE);
        if(imagePhoto == null) finish();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if(setImage) { //OnCreate()された直後、または、Imageが選択された場合
            setImage = false;
        } else { //Imageが選択されなかった場合
            finish();
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent resultData) {
        if (requestCode == RESULT_PICK_IMAGEFILE && resultCode == RESULT_OK) {
            Uri uri = null;
            if (resultData != null) {
                uri = resultData.getData();
                try {
                    Bitmap bmp = getBitmapFromUri(uri);
                    imagePhoto.setImageBitmap(bmp);
                    setImage = true; //Imageが選択された
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private Bitmap getBitmapFromUri(Uri uri) throws IOException {
        ParcelFileDescriptor parcelFileDescriptor = getContentResolver().openFileDescriptor(uri, "r");
        FileDescriptor fileDescriptor = parcelFileDescriptor.getFileDescriptor();
        Bitmap image = BitmapFactory.decodeFileDescriptor(fileDescriptor);
        parcelFileDescriptor.close();
        return image;
    }

}