package com.google.ar.core.examples.java.common.view;

import android.content.Context;
import android.graphics.Color;
import android.util.AttributeSet;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;

import com.google.ar.core.examples.java.helloar.R;

public class ColorSelector extends RelativeLayout implements View.OnClickListener{
    private static final String TAG = ColorSelector.class.getSimpleName();

    private int selectedLineColor = Color.BLUE;

    private FrameLayout greenButton;
    private FrameLayout blackButton;
    private FrameLayout blueButton;
    private FrameLayout yellowButton;
    private FrameLayout redButton;

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

        blueButton.setScaleX(1.5f);
        blueButton.setScaleY(1.5f);
    }

    @Override
    public void onClick(View view) {

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
                break;
        }
    }

    public int getSelectedLineColor() {
        return selectedLineColor;
    }

}

