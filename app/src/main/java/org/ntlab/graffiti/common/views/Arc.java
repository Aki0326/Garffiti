package org.ntlab.graffiti.common.views;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;

import java.util.ArrayDeque;
import java.util.Queue;

/**
 * Created by a-hongo on 28,2月,2021
 * @author a-hongo
 * 参考: https://akira-watson.com/android/canvas-animation.html
 */
public class Arc extends View {
    private static final String TAG = Arc.class.getSimpleName();

    // Animation 開始地点をセット
    private static final int ANGLE_TARGET = 270;

    ArcAnimation animation;
    private int animPeriod = 3500;

    private Paint paint;
    private RectF rect;

    // アニメーション終了後angle=curAngle
    private float angle = 0;
    private float curAngle = 0;

    // アニメーション予定の新しいangleを保持
    private Queue<Float> angleQueue = new ArrayDeque<>();

    public Arc(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        paint = new Paint();
        paint.setAntiAlias(true);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(70); // Arcの幅
        paint.setColor(Color.BLACK); // Arcの色(default:BLACK)

        // Arcの範囲
        rect = new RectF();

        animation = new ArcAnimation(this);
        // アニメーションの起動期間を設定
        animation.setDuration(animPeriod);
        animation.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                Log.d(TAG, "end");
                if (!angleQueue.isEmpty()) {
                    Log.d(TAG, "poll");
                    setAnimationPeriod(1);
                    startAnimation(getAngle() - angleQueue.poll());
                }
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
            }
        });
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        // 背景、透明
        canvas.drawColor(Color.argb(0, 0, 0, 0));
        // Canvas 中心点
        float x = getWidth() / 2;
        float y = getHeight() / 2;
        // 半径
        float radius = getWidth() / 3;

        // 円弧の領域設定
        rect.set(x - radius, y - radius, x + radius, y + radius);

        // 円弧の描画
        canvas.drawArc(rect, ANGLE_TARGET, curAngle, false, paint);
    }

    public void startAnimation(float angle) {
        this.angle = angle;
        animation.setAngle(angle);
        startAnimation(animation);
    }

    /*
     * ArcAnimationへ現在のangleを返す
     */
    public float getAngle() {
        return angle;
    }

    /*
     * ArcAnimationへ現在のangleを返す
     */
    public float getCurAngle() {
        return curAngle;
    }

    /*
     * Animationから新しいangleが設定される
     */
    public void setCurAngle(float angle) {
        this.curAngle = angle;
    }

    public void setArcColor(int color) {
        paint.setColor(color); // Arcの色(default:BLACK)
    }

    public void setAnimationPeriod(int period) {
        this.animPeriod = period;
        animation.setDuration(animPeriod);
    }

    public void addAngleQueue(float newAngle) {
        Log.d(TAG, "hasStarted " + animation.hasStarted() + ", hasEnded " + animation.hasEnded());
        if (animation.hasStarted() && animation.hasEnded()) {
            setAnimationPeriod(100);
            startAnimation(getAngle() - newAngle);
        } else {
            Log.d(TAG, "add " + newAngle);
            angleQueue.add(newAngle);
        }
    }
}