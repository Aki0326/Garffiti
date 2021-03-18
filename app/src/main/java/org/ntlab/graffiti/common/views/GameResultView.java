package org.ntlab.graffiti.common.views;

import android.content.Context;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.Gravity;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Space;
import android.widget.TextView;

import androidx.annotation.ColorInt;
import androidx.annotation.Nullable;

/**
 * View displaying GameResult for a person
 *
 * Created by a-hongo on 03,3月,2021
 * @author a-hongo
 */
public class GameResultView extends LinearLayout {

    private TextView rankTextView;
    private EditText nameTextView;
    private TextView scoreTextView;

    public GameResultView(Context context) {
        super(context);
        init();
    }

    public GameResultView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        this.setOrientation(LinearLayout.HORIZONTAL); // 水平方向

        // TextView, EditText インスタンス生成
        rankTextView = new TextView(getContext());
        nameTextView = new EditText(getContext());
        scoreTextView = new TextView(getContext());

        scoreTextView.setGravity(Gravity.RIGHT);// テキスト右寄せ

        // Space インスタンス生成
        Space spaceL = new Space(getContext()); // Margin between left and rankTextView
        Space spaceM = new Space(getContext()); // Margin between rankTextView and scoreTextView
        Space spaceR = new Space(getContext()); // Margin between scoreTextView and right

        LayoutParams viewLp = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        LayoutParams scoreLp = new LayoutParams(0, LayoutParams.WRAP_CONTENT, 1.5f);
        LayoutParams spaceLRLp = new LayoutParams(0, LayoutParams.WRAP_CONTENT, 1.0f);
        LayoutParams spaceMLp = new LayoutParams(0, LayoutParams.WRAP_CONTENT, 0.1f);
        // レイアウト中央寄せ
        viewLp.gravity = Gravity.CENTER;
        scoreLp.gravity = Gravity.CENTER;
        this.addView(spaceL, spaceLRLp);
        this.addView(rankTextView, viewLp);
//        this.addView(nameTextView, viewLp);
        this.addView(spaceM, spaceMLp);
        this.addView(scoreTextView, scoreLp);
        this.addView(spaceR, spaceLRLp);

        LayoutParams linearLayoutLp = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        this.setLayoutParams(linearLayoutLp);
        this.setHorizontalGravity(Gravity.CENTER);
    }

    public TextView getRankTextView() {
        return rankTextView;
    }

    public TextView getScoreTextView() {
        return scoreTextView;
    }

    public void setRankText(int rank) {
        rankTextView.setText(rank + "位");
    }

    public void setNameText(String name) {
        nameTextView.setText(name);
    }

    public void setScoreText(long score) {
        if (score >= 0) {
            scoreTextView.setText(score + "p");
        } else { // 記録なし
            scoreTextView.setText("- p");
        }
    }

    // テキストカラー
    public void setTextColor(@ColorInt int color) {
        setRankTextColor(color);
        setNameTextColor(color);
        setScoreTextColor(color);
    }

    public void setRankTextColor(@ColorInt int color) {
        rankTextView.setTextColor(color);
    }

    public void setNameTextColor(@ColorInt int color) {
        nameTextView.setTextColor(color);
    }

    public void setScoreTextColor(@ColorInt int color) {
        scoreTextView.setTextColor(color);
    }

    // テキストフォント, テキストスタイル
    public void setTypeface(@Nullable Typeface tf) {
        setRankTypeface(tf);
        setNameTypeface(tf);
        setScoreTypeface(tf);
    }

    public void setRankTypeface(@Nullable Typeface tf) {
        rankTextView.setTypeface(tf);
    }

    public void setNameTypeface(@Nullable Typeface tf) {
        nameTextView.setTypeface(tf);
    }

    public void setScoreTypeface(@Nullable Typeface tf) {
        scoreTextView.setTypeface(tf);
    }

    // テキストサイズ
    public void setTextSize(float size) {
        setRankTextSize(size);
        setNameTextSize(size);
        setScoreTextSize(size);
    }

    public void setRankTextSize(float size) {
        rankTextView.setTextSize(size);
    }

    public void setNameTextSize(float size) {
        nameTextView.setTextSize(size);
    }

    public void setScoreTextSize(float size) {
        scoreTextView.setTextSize(size);
    }
}
