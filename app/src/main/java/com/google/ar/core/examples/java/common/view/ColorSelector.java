package com.google.ar.core.examples.java.common.view;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;

import com.google.ar.core.examples.java.helloar.R;

public class ColorSelector extends RelativeLayout implements View.OnClickListener{
    private static final String TAG = ColorSelector.class.getSimpleName();

    private FrameLayout whiteButton;

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
    }

    @Override
    public void onClick(View view) {

    }
}

