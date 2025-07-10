package com.example.mome.handle;

import android.content.Context;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Toast;

public class GestureHandler implements
        GestureDetector.OnGestureListener,
        GestureDetector.OnDoubleTapListener {

    private final GestureDetector gestureDetector;
    private final GestureCallback callback;

    // 手势识别阈值
    private static final int SWIPE_THRESHOLD = 100;
    private static final int SWIPE_VELOCITY_THRESHOLD = 100;

    public GestureHandler(Context context, View targetView, GestureCallback callback) {
        this.callback = callback;
        this.gestureDetector = new GestureDetector(context, this);

        // 设置双击监听器
        gestureDetector.setOnDoubleTapListener(this);

        // 设置触摸监听
        targetView.setOnTouchListener((v, event) -> gestureDetector.onTouchEvent(event));
    }

    // ========== 双击事件 ==========
    @Override
    public boolean onDoubleTap(MotionEvent e) {
        callback.onToggleFullscreen();
        return true;
    }

    // ========== 滑动手势 ==========
    @Override
    public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
        if (e1 == null || e2 == null) return false;

        float diffX = e2.getX() - e1.getX();
        float diffY = e2.getY() - e1.getY();

        // 计算滑动距离和速度
        float distanceX = Math.abs(diffX);
        float distanceY = Math.abs(diffY);
        float velocityAbsX = Math.abs(velocityX);
        float velocityAbsY = Math.abs(velocityY);

        // 判断滑动方向（水平优先还是垂直优先）
        boolean isHorizontal = distanceX > distanceY;
        boolean isVertical = distanceY > distanceX;

        // 检查是否达到滑动阈值
        if (isHorizontal && distanceX > SWIPE_THRESHOLD && velocityAbsX > SWIPE_VELOCITY_THRESHOLD) {
            // 水平滑动
            if (diffX > 0) {
                callback.onSwipeRight(); // 右滑
            } else {
                callback.onSwipeLeft(); // 左滑
            }
            return true;
        } else if (isVertical && distanceY > SWIPE_THRESHOLD && velocityAbsY > SWIPE_VELOCITY_THRESHOLD) {
            // 垂直滑动
            if (diffY > 0) {
                callback.onSwipeDown(); // 下滑
            } else {
                callback.onSwipeUp(); // 上滑
            }
            return true;
        }

        // 对角线滑动处理
        if (distanceX > SWIPE_THRESHOLD && distanceY > SWIPE_THRESHOLD &&
                velocityAbsX > SWIPE_VELOCITY_THRESHOLD && velocityAbsY > SWIPE_VELOCITY_THRESHOLD) {

            // 计算对角线角度（0-90度）
            double angle = Math.toDegrees(Math.atan2(distanceY, distanceX));

            if (angle < 45) {
                // 接近水平方向
                if (diffX > 0) {
                    callback.onSwipeRight();
                } else {
                    callback.onSwipeLeft();
                }
            } else {
                // 接近垂直方向
                if (diffY > 0) {
                    callback.onSwipeDown();
                } else {
                    callback.onSwipeUp();
                }
            }
            return true;
        }

        return false;
    }

    // ========== 长按事件 ==========
    @Override
    public void onLongPress(MotionEvent e) {
        callback.onLongPress();
    }

    // ========== 其他必须实现的接口方法 ==========
    @Override public boolean onDoubleTapEvent(MotionEvent e) { return false; }
    @Override public boolean onSingleTapConfirmed(MotionEvent e) { return false; }
    @Override public boolean onDown(MotionEvent e) { return true; }
    @Override public void onShowPress(MotionEvent e) {}
    @Override public boolean onSingleTapUp(MotionEvent e) { return false; }
    @Override public boolean onScroll(MotionEvent e1, MotionEvent e2, float dX, float dY) { return false; }
}