package com.google.ar.core.examples.java.common.view;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;

import com.google.ar.core.examples.java.graffiti.Graffiti;
import com.google.ar.core.examples.java.graffiti.ShowPhotoActivity;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;

public class GridAdapter extends BaseAdapter {
    private Context context;
    private ArrayList<String> photoPaths;       // 画像のファイルパス

    public GridAdapter(Context context, ArrayList<String> photos) {
        super();
        this.context = context;
        this.photoPaths = photos;
    }

    @Override
    public int getCount() {
        return photoPaths.size();
    }

    @Override
    public Object getItem(int i) {
        return i;
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {

        /* 画像表示用のImageView */
        ImageView imagePhoto;

        if(view == null){
            // imageViewの新規生成
            imagePhoto = new ImageView(context);
            imagePhoto.setLayoutParams(new GridView.LayoutParams(320, 500));
            imagePhoto.setScaleType(ImageView.ScaleType.FIT_XY);
            imagePhoto.setPadding(0,10,0,10);
        }else{
            imagePhoto = (ImageView) view;
        }

        // ImageViewに画像ファイルを設定(Picassoを使って画像を表示)
        Picasso.with(context).load("file://" + photoPaths.get(i)).into(imagePhoto);
        return imagePhoto;
    }

    public void isSelected(View view){
//        GradientDrawable gradientDrawable = new GradientDrawable();
//        gradientDrawable.setStroke(5, Color.BLUE);
//        gradientDrawable.setCornerRadius(2);
//        gradientDrawable.setColor(Color.WHITE);
//        view.setBackground(gradientDrawable);

        Bitmap bmp = ((BitmapDrawable) ((ImageView) view).getDrawable()).getBitmap();
        if(bmp!= null) {
            Graffiti app = (Graffiti) context.getApplicationContext();
            app.setBitmap(bmp);
            context.startActivity(new Intent(context, ShowPhotoActivity.class));
        } else {
            return;
        }
    }
}