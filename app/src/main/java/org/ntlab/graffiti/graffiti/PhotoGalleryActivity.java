package org.ntlab.graffiti.graffiti;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.Toast;

import org.ntlab.graffiti.R;
import org.ntlab.graffiti.common.helpers.TimeoutHelper;
import org.ntlab.graffiti.common.view.GridAdapter;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;

public class PhotoGalleryActivity extends AppCompatActivity {
    private static final String TAG = PhotoGalleryActivity.class.getSimpleName();
    private static final int PERMISSON_REQUEST_CODE = 2;
    private GridView gridViewPhotos;
    private ArrayList<String> photoPathList;
//    private View surface;

    @SuppressLint("ClickableViewAccessibility")
    @RequiresApi(api = Build.VERSION_CODES.M)
    private void requestReadStorage() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            if (shouldShowRequestPermissionRationale(Manifest.permission.READ_EXTERNAL_STORAGE)) {
            }
            requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, PERMISSON_REQUEST_CODE);
        } else {
            // ここに許可済みの時の動作を書く
            // 許可された時の動作
            photoPathList = getLocalPhotos(Environment.DIRECTORY_PICTURES + File.separator + "AR/");
            gridViewPhotos = findViewById(R.id.gridView_photos);
            Collections.reverse(photoPathList);

            final GridAdapter adapter = new GridAdapter(this, photoPathList);
            gridViewPhotos.setAdapter(adapter);
            gridViewPhotos.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
//                    TimeoutHelper.resetTimer();
                    adapter.isSelected(view);
                }
            });

            gridViewPhotos.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View view, MotionEvent motionEvent) {
                    if (motionEvent.getAction() != MotionEvent.ACTION_UP) {
//                        TimeoutHelper.resetTimer();
                        return false;
                    } else {
//                        TimeoutHelper.startTimer(PhotoGalleryActivity.this);
                        return false;
                    }
                }
            });
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_photo_gallery);
        requestReadStorage();
    }

    @Override
    protected void onResume() {
        super.onResume();
//        TimeoutHelper.startTimer(PhotoGalleryActivity.this);
    }

    @Override
    protected void onPause() {
        super.onPause();
//        TimeoutHelper.resetTimer();
    }

    private ArrayList<String> getLocalPhotos(String directoryName)  {
        String fullpath = "";
        String filename = "";
        String fileExtension = "";
        int lastDotPosition = 0;
        ArrayList<String> photoFilePaths = new ArrayList<String>() ;

        File dir = Environment.getExternalStoragePublicDirectory(directoryName);

        if(!dir.exists()){
            Toast.makeText(this, directoryName+ "は存在しません", Toast.LENGTH_LONG).show();
            return null;
        }

        File[] files = dir.listFiles();

        for(int fileCount = 0; fileCount < files.length; fileCount++){
            fullpath = String.valueOf(files[fileCount]);
            filename = String.valueOf(files[fileCount].getName());

            if(files[fileCount].isDirectory()){
                // ディレクトリのため対象外
                continue;
            }

            // 拡張子取得
            fileExtension = "";
            lastDotPosition = filename.lastIndexOf(".");
            if (lastDotPosition != -1) {
                fileExtension = filename.substring(lastDotPosition + 1);
            }
            if (fullpath.contains(fileExtension)) {
                photoFilePaths.add(fullpath);
            }

        }
        return photoFilePaths;

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults){
        if(requestCode == PERMISSON_REQUEST_CODE){
            if(grantResults.length != 1 || grantResults[0] != PackageManager.PERMISSION_GRANTED){
                //拒否された時の動作
                Toast.makeText(this, "Camera permission is needed to run this application", Toast.LENGTH_LONG).show();
            }else{
                // 許可された時の動作
                photoPathList = getLocalPhotos(Environment.DIRECTORY_DOWNLOADS);
                Collections.reverse(photoPathList);
                gridViewPhotos = findViewById(R.id.gridView_photos);

                final GridAdapter adapter = new GridAdapter(this, photoPathList);
                gridViewPhotos.setAdapter(adapter);
                gridViewPhotos.setOnItemClickListener(new AdapterView.OnItemClickListener(){
                    @Override
                    public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
                        adapter.isSelected(view);
                    }
                });
            }
        }
    }
}