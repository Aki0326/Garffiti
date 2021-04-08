package org.ntlab.graffiti.graffiti.states;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;

import org.ntlab.graffiti.R;
import org.ntlab.graffiti.common.views.GameResultView;
import org.ntlab.graffiti.db.AppDatabase;
import org.ntlab.graffiti.db.entity.GameResult;

import java.util.ArrayList;
import java.util.List;

/**
 * State to display game ranking.
 *
 * Created by a-hongo on 03,3月,2021
 * @author a-hongo
 */
public class GameRankingState extends State {
    private static final String TAG = GameRankingState.class.getSimpleName();

    private AppDatabase db;

    private GameResult myGameResult = new GameResult();

    private int top = 3; //　上位何位まで
    private List<GameResult> topGameResults = new ArrayList<>();
    private List<GameResultView> gameResultViews = new ArrayList<>();
    private ConstraintLayout gameRankingLayout;

    private int textSize = 35;
    private int textColor = Color.BLACK;

    private Animation animation;

    public GameRankingState(Context context, ViewGroup layout) {
        init(context, layout);
        initAnimation(context);
    }

    public GameRankingState(Context context, ViewGroup layout, long score) {
        this(context, layout);
        setScore(score);
    }

    private void init(Context context, ViewGroup layout) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                db = AppDatabase.getInstance(context);
                topGameResults = db.gameResultDao().getTopGameResults(top);
            }
        }).start();

        // Create ConstraintLayout
        gameRankingLayout = new ConstraintLayout(context);
        gameRankingLayout.setVisibility(View.INVISIBLE);

        for (int i = 0; i < top; i++) {
            // Create GameResultView
            GameResultView gameResultView = new GameResultView(context);
            gameResultView.setVisibility(View.INVISIBLE);
            gameResultView.setTextSize(textSize);
            gameResultView.setTextColor(textColor);
            gameResultView.setRankText(i + 1);

            gameRankingLayout.addView(gameResultView); // Add gameResultView to created ConstraintLayout
            gameResultViews.add(gameResultView);
        }

        // Generate viewId
        for (int i = 0; i < gameRankingLayout.getChildCount(); i++) {
            int childId = gameRankingLayout.getChildAt(i).getId();
            if (childId == -1) { // not have Id
                gameRankingLayout.getChildAt(i).setId(View.generateViewId());
            }
        }

        ConstraintSet constraintSet = new ConstraintSet();
        constraintSet.clone(gameRankingLayout);

        for (int i = 0; i < top; i++) {
            GameResultView gameResultView = gameResultViews.get(i);

            // android:layout_width="WRAP_CONTENT"
            constraintSet.constrainHeight(gameResultView.getId(),
                    ConstraintSet.WRAP_CONTENT);

            //android:layout_height="match_constraint"
            constraintSet.constrainWidth(gameResultView.getId(),
                    ConstraintSet.MATCH_CONSTRAINT);

            // TODO customize margin of gameResultView
            // app:layout_constraintLeft_toLeftOf="parent"
            constraintSet.connect(
                    gameResultView.getId(),
                    ConstraintSet.LEFT,
                    ConstraintSet.PARENT_ID,
                    ConstraintSet.LEFT,
                    0);

            // app:layout_constraintRight_toRightOf="parent"
            constraintSet.connect(
                    gameResultView.getId(),
                    ConstraintSet.RIGHT,
                    ConstraintSet.PARENT_ID,
                    ConstraintSet.RIGHT,
                    0);

            if (i == 0) {
                // app:layout_constraintTop_toTopOf="parent"
                constraintSet.connect(
                        gameResultView.getId(),
                        ConstraintSet.TOP,
                        ConstraintSet.PARENT_ID,
                        ConstraintSet.TOP,
                        80);
            } else {
                GameResultView aboveGameResultView = gameResultViews.get(i - 1);
                // app:layout_constraintTop_toTopOf="parent"
                constraintSet.connect(
                        gameResultView.getId(),
                        ConstraintSet.TOP,
                        aboveGameResultView.getId(),
                        ConstraintSet.BOTTOM,
                        50);

                if (i == top - 1) {
                    // app:layout_constraintBottom_ofBottom="parent"
                    constraintSet.connect(
                            gameResultView.getId(),
                            ConstraintSet.BOTTOM,
                            ConstraintSet.PARENT_ID,
                            ConstraintSet.BOTTOM,
                            80);
                }

            }
        }
        constraintSet.applyTo(gameRankingLayout);
        layout.addView(gameRankingLayout);

        if (layout instanceof ConstraintLayout) {
            for (int i = 0; i < layout.getChildCount(); i++) {
                int childId = layout.getChildAt(i).getId();
                if (childId == -1) { // not have Id
                    layout.getChildAt(i).setId(View.generateViewId());
                }
            }
            constraintSet.clone((ConstraintLayout)layout);

            // android:layout_height="wrap_content"
            constraintSet.constrainHeight(gameRankingLayout.getId(),
                    ConstraintSet.WRAP_CONTENT);

            //android:layout_width="match_constraint"
            constraintSet.constrainWidth(gameRankingLayout.getId(),
                    ConstraintSet.MATCH_CONSTRAINT);

            // TODO customize margin of layout
            // app:layout_constraintLeft_toLeftOf="parent"
            constraintSet.connect(
                    gameRankingLayout.getId(),
                    ConstraintSet.LEFT,
                    ConstraintSet.PARENT_ID,
                    ConstraintSet.LEFT,
                    0);

            // app:layout_constraintRight_toRightOf="parent"
            constraintSet.connect(
                    gameRankingLayout.getId(),
                    ConstraintSet.RIGHT,
                    ConstraintSet.PARENT_ID,
                    ConstraintSet.RIGHT,
                    0);

            // app:layout_constraintTop_toTopOf="parent"
            constraintSet.connect(
                    gameRankingLayout.getId(),
                    ConstraintSet.TOP,
                    ConstraintSet.PARENT_ID,
                    ConstraintSet.TOP,
                    350);

            constraintSet.applyTo((ConstraintLayout)layout);
        }
    }

    private void initAnimation(Context context) {
        animation = AnimationUtils.loadAnimation(context, R.anim.anim_bouncein);
        animation.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                finish();
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
            }
        });
    }

    @Override
    public boolean canChange(State nextState) {
        return false;
    }

    @Override
    public void enter() {
        show();
    }

    @Override
    public void restart() {
    }

    @Override
    public void pause() {
    }

    @Override
    public void exit() {
        gameRankingLayout.setVisibility(View.INVISIBLE);
        for (int i = 0; i < top; i++) {
            GameResultView gameResultView = gameResultViews.get(i);
            gameResultView.setVisibility(View.INVISIBLE);
        }
    }

    private int getRankByScore(long score) {
        int rank = -1;
        if (topGameResults.isEmpty()) {
            rank = 0;
            return rank;
        }

        for (int i = top - 1; i >= 0; i--) {
            if (i < topGameResults.size()) {
                GameResult gameResult = topGameResults.get(i);
                if (gameResult.getScore() <= score) {
                    rank = i;
                    if (gameResult.getScore() == score) break;
                } else {
                    break;
                }
            } else {
                rank = i;
                break;
            }
        }
        return rank;
    }

    public void show() {
        int myRank = getRankByScore(myGameResult.getScore());
        replaceTopGameResults(myRank, myGameResult);
        for (int i = 0; i < top; i++) {
            GameResultView gameResultView = gameResultViews.get(i);
            if (i < topGameResults.size()) {
                GameResult gameResult = topGameResults.get(i);
//                gameResultView.setNameText(gameResult.getName());
                gameResultView.setScoreText(gameResult.getScore());
            } else { // 記録なし
//                gameResultView.setNameText(gameResult.getName());
                gameResultView.setScoreText(-1);
            }
            gameResultView.setVisibility(View.VISIBLE);
        }
        gameRankingLayout.setVisibility(View.VISIBLE);
        gameRankingLayout.startAnimation(animation);
        if (myRank <= top) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    db.gameResultDao().insert(myGameResult);
                }
            }).start();
        }
    }

    private void storeDB(GameResult gameResult) {
        db.gameResultDao().insert(gameResult);
    }

    private void replaceTopGameResults(int topNumber, GameResult gameResult) {
        if (topNumber == -1) return;
        if (!topGameResults.isEmpty() && topGameResults.size() >= top) topGameResults.remove(topGameResults.size()-1);
        topGameResults.add(topNumber, gameResult);
    }

    public void insertGameResultDb() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                db.gameResultDao().insert(myGameResult);
            }
        }).start();
    }

    public void setScore(long score) {
        myGameResult.setScore(score);
    }

    public void setGameRankingBg(Drawable gameRankingBgs) {
        gameRankingLayout.setBackground(gameRankingBgs);
    }

    public void setGameResultBgs(List<Drawable> gameResultBgs) {
        for (int i = 0; i < gameResultViews.size(); i++) {
            GameResultView gameResultView = gameResultViews.get(i);
            gameResultView.setBackground(gameResultBgs.get(i % gameResultBgs.size()));
        }
    }

    public void setRankBgs(Resources resources, List<Drawable> rankBgs, boolean isVisibleText) {
        for (int i = 0; i < gameResultViews.size(); i++) {
            TextView rankTextView = gameResultViews.get(i).getRankTextView();
            ViewTreeObserver vto = rankTextView.getViewTreeObserver();
            BitmapDrawable rankBg = (BitmapDrawable) rankBgs.get(i % rankBgs.size());
            vto.addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
                public boolean onPreDraw() {
                    rankTextView.getViewTreeObserver().removeOnPreDrawListener(this);
                    int viewHeight = rankTextView.getMeasuredHeight();
                    int bgHeight = rankBg.getIntrinsicHeight();
                    int bgWidth = rankBg.getIntrinsicWidth();
                    float scale = (float) viewHeight / bgHeight;
                    Log.d(TAG, "Height: " + viewHeight + ": (" + bgHeight + ", " + bgWidth + "), " + scale);
                    if (!isVisibleText) rankTextView.setText(null);
                    Bitmap resizedBitmap = Bitmap.createScaledBitmap(rankBg.getBitmap(), (int) (bgWidth * scale), viewHeight, true);
                    Drawable resizedRankBg = new BitmapDrawable(resources, resizedBitmap);
                    Log.d(TAG, "ResizedHeight: " + resizedRankBg.getIntrinsicHeight());
                    rankTextView.setBackground(resizedRankBg);
                    return true;
                }
            });
        }
    }

    public void setTextFont(@Nullable Typeface tf) {
        for (int i = 0; i < gameResultViews.size(); i++) {
            GameResultView gameResultView = gameResultViews.get(i);
            gameResultView.setTypeface(tf);
        }
    }
}
