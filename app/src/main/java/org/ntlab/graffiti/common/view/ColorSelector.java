package org.ntlab.graffiti.common.view;

import android.content.Context;
import android.graphics.Color;
import android.util.AttributeSet;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;

import org.ntlab.graffiti.common.helpers.MusicPlayerHelper;
import org.ntlab.graffiti.R;

import java.io.IOException;

public class ColorSelector extends RelativeLayout implements View.OnClickListener{
    private static final String TAG = ColorSelector.class.getSimpleName();

    private MusicPlayerHelper colorSelectClickSE = new MusicPlayerHelper();
    private Boolean isLoop = false;

    private int selectedLineColor = Color.BLUE;

    private FrameLayout greenButton;
    private FrameLayout blackButton;
    private FrameLayout blueButton;
    private FrameLayout yellowButton;
    private FrameLayout redButton;
    private FrameLayout eraserButton;

    public ColorSelector(Context context) {
        super(context);
        init();
    }

    public ColorSelector(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }
    private void init() {
        inflate(getContext(), R.layout.view_color_selector, this);

        blueButton = findViewById(R.id.blue_color_button);
        blueButton.setOnClickListener(this);
        yellowButton = findViewById(R.id.yellow_color_button);
        yellowButton.setOnClickListener(this);
        redButton = findViewById(R.id.red_color_button);
        redButton.setOnClickListener(this);
        greenButton = findViewById(R.id.green_color_button);
        greenButton.setOnClickListener(this);
        blackButton = findViewById(R.id.black_color_button);
        blackButton.setOnClickListener(this);
        eraserButton = findViewById(R.id.eraser_button);
        eraserButton.setOnClickListener(this);

        blueButton.setScaleX(1.5f);
        blueButton.setScaleY(1.5f);
    }

    @Override
    public void onClick(View view) {
        colorSelectClickSE.musicStop();
        try {
            colorSelectClickSE.musicPlay(getContext(), "musics/se/color-click-sound.mp3", isLoop);
        } catch (IOException e) {
            e.printStackTrace();
        }

        switch (view.getId()) {
            case R.id.blue_color_button:
                selectedLineColor = Color.BLUE;
                blueButton.setScaleX(1.5f);
                blueButton.setScaleY(1.5f);
                yellowButton.setScaleX(1.0f);
                yellowButton.setScaleY(1.0f);
                redButton.setScaleX(1.0f);
                redButton.setScaleY(1.0f);
                greenButton.setScaleX(1.0f);
                greenButton.setScaleY(1.0f);
                blackButton.setScaleX(1.0f);
                blackButton.setScaleY(1.0f);
                eraserButton.setScaleX(1.0f);
                eraserButton.setScaleY(1.0f);
                break;

            case R.id.yellow_color_button:
                selectedLineColor = Color.YELLOW;
                blueButton.setScaleX(1.0f);
                blueButton.setScaleY(1.0f);
                yellowButton.setScaleX(1.5f);
                yellowButton.setScaleY(1.5f);
                redButton.setScaleX(1.0f);
                redButton.setScaleY(1.0f);
                greenButton.setScaleX(1.0f);
                greenButton.setScaleY(1.0f);
                blackButton.setScaleX(1.0f);
                blackButton.setScaleY(1.0f);
                eraserButton.setScaleX(1.0f);
                eraserButton.setScaleY(1.0f);
                break;

            case R.id.red_color_button:
                selectedLineColor = Color.RED;
                blueButton.setScaleX(1.0f);
                blueButton.setScaleY(1.0f);
                yellowButton.setScaleX(1.0f);
                yellowButton.setScaleY(1.0f);
                redButton.setScaleX(1.5f);
                redButton.setScaleY(1.5f);
                greenButton.setScaleX(1.0f);
                greenButton.setScaleY(1.0f);
                blackButton.setScaleX(1.0f);
                blackButton.setScaleY(1.0f);
                eraserButton.setScaleX(1.0f);
                eraserButton.setScaleY(1.0f);
                break;

            case R.id.green_color_button:
                selectedLineColor = Color.GREEN;
                blueButton.setScaleX(1.0f);
                blueButton.setScaleY(1.0f);
                yellowButton.setScaleX(1.0f);
                yellowButton.setScaleY(1.0f);
                redButton.setScaleX(1.0f);
                redButton.setScaleY(1.0f);
                greenButton.setScaleX(1.5f);
                greenButton.setScaleY(1.5f);
                blackButton.setScaleX(1.0f);
                blackButton.setScaleY(1.0f);
                eraserButton.setScaleX(1.0f);
                eraserButton.setScaleY(1.0f);
                break;

            case R.id.black_color_button:
                selectedLineColor = Color.BLACK;
                blueButton.setScaleX(1.0f);
                blueButton.setScaleY(1.0f);
                yellowButton.setScaleX(1.0f);
                yellowButton.setScaleY(1.0f);
                redButton.setScaleX(1.0f);
                redButton.setScaleY(1.0f);
                greenButton.setScaleX(1.0f);
                greenButton.setScaleY(1.0f);
                blackButton.setScaleX(1.5f);
                blackButton.setScaleY(1.5f);
                eraserButton.setScaleX(1.0f);
                eraserButton.setScaleY(1.0f);
                break;

            case R.id.eraser_button:
                selectedLineColor = Color.TRANSPARENT;
                blueButton.setScaleX(1.0f);
                blueButton.setScaleY(1.0f);
                yellowButton.setScaleX(1.0f);
                yellowButton.setScaleY(1.0f);
                redButton.setScaleX(1.0f);
                redButton.setScaleY(1.0f);
                greenButton.setScaleX(1.0f);
                greenButton.setScaleY(1.0f);
                blackButton.setScaleX(1.0f);
                blackButton.setScaleY(1.0f);
                eraserButton.setScaleX(1.5f);
                eraserButton.setScaleY(1.5f);
                break;

        }
    }

    public int getSelectedLineColor() {
        return selectedLineColor;
    }

}

