// Copyright 2018 Google LLC
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.


/**
 * Created by Kat on 11/13/17.
 * Custom view for selecting brush size
 */
package org.ntlab.graffiti.common.views;

import android.animation.Animator;
import android.content.Context;
import android.util.AttributeSet;
import android.util.Pair;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.TextView;

import org.ntlab.graffiti.common.helpers.MusicPlayerHelper;
import org.ntlab.graffiti.R;

import java.io.IOException;

/**
 * This class selects brush size.
 */
public class BrushSizeSelector extends RelativeLayout implements View.OnClickListener {

    private static final String TAG = BrushSizeSelector.class.getSimpleName();

    public enum LineWidth {
        SMALL(1),
        MEDIUM(4),
        LARGE(8);

        private final int width;

        LineWidth(int width) {
            this.width = width;
        }

        public int getWidth() {
            return width;
        }
    }


    private static final int SMALL_BRUSH = 0;
    private static final int MEDIUM_BRUSH = 1;
    private static final int LARGE_BRUSH = 2;

    private static final Pair<Integer, LineWidth> defaultBrush = new Pair<>(MEDIUM_BRUSH, LineWidth.MEDIUM);

    private View brushButton;
    private View smallButton, mediumButton, largeButton;

    private View selectedSizeIndicator;
    private TextView selectedSizeIndicatorText;

    private int selectedBrush = defaultBrush.first;
    private LineWidth selectedLineWidth = defaultBrush.second;

    private boolean isOpen = true;

    //the locations of the buttons
    private int smallButtonLoc[] = new int[2];
    private int mediumButtonLoc[] = new int[2];
    private int largeButtonLoc[] = new int[2];

    private MusicPlayerHelper brushSizeSelectClickSE = new MusicPlayerHelper();
    private Boolean isLoop = false;

    public BrushSizeSelector(Context context) {
        super(context);
        init();
    }

    public BrushSizeSelector(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public BrushSizeSelector(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        inflate(getContext(), R.layout.view_brush_size_selector, this);

        brushButton = findViewById(R.id.brush_button);
        brushButton.setOnClickListener(this);

        selectedSizeIndicator = findViewById(R.id.selected_size_indicator);
        selectedSizeIndicatorText = findViewById(R.id.selected_size_indicator_text);

        smallButton = findViewById(R.id.brush_selection_small);
        mediumButton = findViewById(R.id.brush_selection_medium);
        largeButton = findViewById(R.id.brush_selection_large);

        smallButton.setOnClickListener(this);
        mediumButton.setOnClickListener(this);
        largeButton.setOnClickListener(this);

        brushButton.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                if (motionEvent.getAction() == MotionEvent.ACTION_DOWN) {
                    performClick();
                    brushSizeSelectClickSE.musicStop();
                    try {
                        brushSizeSelectClickSE.musicPlay(getContext(), "musics/se/color-click-sound.mp3", isLoop);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                } else if (motionEvent.getAction() == MotionEvent.ACTION_MOVE && isOpen) {
                    //get the point where we let go
                    float yloc = motionEvent.getRawY();

                    LineWidth lineWidth;

                    //determine which button was released over
                    if (smallButtonLoc[1] < yloc && yloc < (smallButtonLoc[1] + smallButton.getHeight())) {
                        //prevent calling an update when not needed
                        if (selectedBrush != SMALL_BRUSH) {
                            lineWidth = LineWidth.SMALL;
                            onBrushSizeSelected(lineWidth);
                        }
                    } else if (mediumButtonLoc[1] < yloc && yloc < (mediumButtonLoc[1] + mediumButton.getHeight())) {
                        if (selectedBrush != MEDIUM_BRUSH) {
                            lineWidth = LineWidth.MEDIUM;
                            onBrushSizeSelected(lineWidth);
                        }
                    } else if (largeButtonLoc[1] < yloc && yloc < (largeButtonLoc[1] + largeButton.getHeight())) {
                        if (selectedBrush != LARGE_BRUSH) {
                            lineWidth = LineWidth.LARGE;
                            onBrushSizeSelected(lineWidth);
                        }
                    }
                } else if (motionEvent.getAction() == MotionEvent.ACTION_UP) {
                    //toggle if over a button
                    float yloc = motionEvent.getRawY();
                    if (smallButtonLoc[1] < yloc && yloc < (smallButtonLoc[1] + smallButton.getHeight())) {
                        toggleBrushSelectorVisibility();
                    } else if (mediumButtonLoc[1] < yloc && yloc < (mediumButtonLoc[1] + mediumButton.getHeight())) {
                        toggleBrushSelectorVisibility();
                    } else if (largeButtonLoc[1] < yloc && yloc < (largeButtonLoc[1] + largeButton.getHeight())) {
                        toggleBrushSelectorVisibility();
                    }
                }
                return true;
            }
        });

        this.post(new Runnable() {
            @Override
            public void run() {
                //the navigation bar is visible here at this point and throws off my location capture....
                //have to get the height to fix this
                smallButton.getLocationInWindow(smallButtonLoc);
                mediumButton.getLocationInWindow(mediumButtonLoc);
                largeButton.getLocationInWindow(largeButtonLoc);
            }
        });

        onBrushSizeSelected(defaultBrush.second);
        toggleBrushSelectorVisibility();
    }

    @Override
    public void onClick(View view) {

        LineWidth lineWidth = null;

        brushSizeSelectClickSE.musicStop();
        try {
            brushSizeSelectClickSE.musicPlay(getContext(), "musics/se/color-click-sound.mp3", isLoop);
        } catch (IOException e) {
            e.printStackTrace();
        }
        switch (view.getId()) {
            case R.id.brush_button:
                toggleBrushSelectorVisibility();
                return;
            case R.id.brush_selection_small:
                lineWidth = LineWidth.SMALL;
                break;
            case R.id.brush_selection_medium:
                lineWidth = LineWidth.MEDIUM;
                break;
            case R.id.brush_selection_large:
                lineWidth = LineWidth.LARGE;
                break;
        }

        onBrushSizeSelected(lineWidth);

        toggleBrushSelectorVisibility();
    }

    @Override
    public boolean performClick() {
        toggleBrushSelectorVisibility();
        return super.performClick();
    }

    private void onBrushSizeSelected(LineWidth lineWidth) {
        selectedLineWidth = lineWidth;

        TypedValue outValue = new TypedValue();

        switch (lineWidth) {
            case SMALL:
                getResources().getValue(R.dimen.brush_size_small, outValue, true);
                selectedSizeIndicatorText.setText("小");
                selectedSizeIndicatorText.setTextSize(10);
                selectedBrush = SMALL_BRUSH;
                break;
            case MEDIUM:
                getResources().getValue(R.dimen.brush_size_medium, outValue, true);
                selectedSizeIndicatorText.setText("中");
                selectedSizeIndicatorText.setTextSize(14);
                selectedBrush = MEDIUM_BRUSH;
                break;
            default:
            case LARGE:
                getResources().getValue(R.dimen.brush_size_large, outValue, true);
                selectedSizeIndicatorText.setText("大");
                selectedSizeIndicatorText.setTextSize(18);
                selectedBrush = LARGE_BRUSH;
                break;
        }

        float scale = outValue.getFloat();

        selectedSizeIndicator.animate().scaleX(scale).scaleY(scale);
    }

    private void toggleBrushSelectorVisibility() {
        if (isOpen) {
            float y = selectedSizeIndicator.getY();
            Animator.AnimatorListener hideListener = new Animator.AnimatorListener() {
                @Override
                public void onAnimationStart(Animator animation) {

                }

                @Override
                public void onAnimationEnd(Animator animation) {
                    smallButton.setVisibility(GONE);
                    mediumButton.setVisibility(GONE);
                    largeButton.setVisibility(GONE);
                }

                @Override
                public void onAnimationCancel(Animator animation) {

                }

                @Override
                public void onAnimationRepeat(Animator animation) {

                }
            };
            smallButton.animate().alpha(0).setListener(hideListener).translationY(y);
            mediumButton.animate().alpha(0).translationY(y);
            largeButton.animate().alpha(0).translationY(y);
            smallButton.setEnabled(false);
            mediumButton.setEnabled(false);
            largeButton.setEnabled(false);

        } else {
            Animator.AnimatorListener showListener = new Animator.AnimatorListener() {
                @Override
                public void onAnimationStart(Animator animation) {

                }

                @Override
                public void onAnimationEnd(Animator animation) {
                    smallButton.setVisibility(VISIBLE);
                    mediumButton.setVisibility(VISIBLE);
                    largeButton.setVisibility(VISIBLE);
                }

                @Override
                public void onAnimationCancel(Animator animation) {

                }

                @Override
                public void onAnimationRepeat(Animator animation) {

                }
            };
            smallButton.animate().alpha(1).setListener(showListener).translationY(0);
            mediumButton.animate().alpha(1).translationY(0);
            largeButton.animate().alpha(1).translationY(0);
            smallButton.setEnabled(true);
            mediumButton.setEnabled(true);
            largeButton.setEnabled(true);

            brushButton.setAccessibilityTraversalBefore(R.id.brush_selection_small);
            smallButton.setAccessibilityTraversalBefore(R.id.brush_selection_medium);
            mediumButton.setAccessibilityTraversalBefore(R.id.brush_selection_large);
        }
        isOpen = !isOpen;
    }

    public int getSelectedLineWidth() {
        return selectedLineWidth.getWidth();
    }

//    public boolean isOpen() {
//        return isOpen;
//    }
//
//    public void close() {
//        if (isOpen) {
//            toggleBrushSelectorVisibility();
//        }
//    }

}