package org.ntlab.graffiti.common.views;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

/**
 * Created by a-hongo on 28,2月,2021
 * 参考: https://akira-watson.com/android/canvas-animation.html
 */
public class Arc extends View {
    // Animation 開始地点をセット
    private static final int ANGLE_TARGET = 270;
    private static final int ANIMATION_PERIOD = 2000;

    ArcAnimation animation;

    private Paint paint;
    private RectF rect;

    private float curAngle = 0;

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
        animation.setDuration(ANIMATION_PERIOD);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        // 背景、透明
        canvas.drawColor(Color.argb(0, 0, 0, 0));
//        canvas.drawColor(Color.BLACK);
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
        animation.setAngle(angle);
        startAnimation(animation);
    }

    /*
     * ArcAnimationへ現在のangleを返す
     */
    public float getAngle() {
        return curAngle;
    }

    /*
     * Animationから新しいangleが設定される
     */
    public void setAngle(float angle) {
        this.curAngle = angle;
    }

    public void setArcColor(int color) {
        paint.setColor(color); // Arcの色(default:BLACK)
    }
}