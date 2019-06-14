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

    private LineColor selectedLineColor = LineColor.WHITE;

    private FrameLayout whiteButton;
    private FrameLayout blackButton;
    private FrameLayout blueButton;
    private FrameLayout yellowButton;
    private FrameLayout redButton;

    public enum LineColor {
        WHITE(Color.WHITE),
        BLACK(Color.BLACK),
        BLUE(Color.BLUE),
        YELLOW(Color.YELLOW),
        RED(Color.RED);

        private final int color;

        LineColor(int color) {
            this.color = color;
        }

        public int getColor() {
            return color;
        }
    }

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

        whiteButton = findViewById(R.id.white_color_button);
        whiteButton.setOnClickListener(this);
        blackButton = findViewById(R.id.black_color_button);
        blackButton.setOnClickListener(this);
        blueButton = findViewById(R.id.blue_color_button);
        blueButton.setOnClickListener(this);
        yellowButton = findViewById(R.id.yellow_color_button);
        yellowButton.setOnClickListener(this);
        redButton = findViewById(R.id.red_color_button);
        redButton.setOnClickListener(this);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.white_color_button:
                selectedLineColor = LineColor.WHITE;
                break;
            case R.id.black_color_button:
                selectedLineColor = LineColor.BLACK;
                break;
            case R.id.blue_color_button:
                selectedLineColor = LineColor.BLUE;
                break;
            case R.id.yellow_color_button:
                selectedLineColor = LineColor.YELLOW;
                break;
            case R.id.red_color_button:
                selectedLineColor = LineColor.RED;
                break;
        }
    }

    public LineColor getSelectedLineColor() {
        return selectedLineColor;
    }


}

