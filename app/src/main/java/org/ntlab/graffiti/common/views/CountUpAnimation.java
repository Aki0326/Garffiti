package org.ntlab.graffiti.common.views;

import android.view.animation.Animation;
import android.view.animation.Transformation;
import android.widget.TextView;

/**
 * Animation to count up a number.
 *
 * Created by a-hongo on 03,3月,2021
 * @author a-hongo
 */
public class CountUpAnimation extends Animation {
    private TextView textView;
    private long countUpNum;

    public CountUpAnimation(TextView textView) {
        this.textView = textView;
    }

    public CountUpAnimation(TextView textView, long countUpNum) {
        this(textView);
        this.countUpNum = countUpNum;
    }

    public void setCountUpNumber(long countUpNum) {
        this.countUpNum = countUpNum;
    }

    @Override
    protected void applyTransformation(float interpolatedTime, Transformation transformation) {
        String curText = textView.getText().toString();
        String longStr = curText.replaceAll("[^0-9]", "");
        long curPt = Long.parseLong(longStr);
        long num = (long) (curPt + ((countUpNum - curPt) * interpolatedTime));
        textView.setText(num + "p");
        textView.requestLayout();
    }
}
