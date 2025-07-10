package com.example.mome.handle;

public interface GestureCallback {
    void onToggleFullscreen();
    void onSwipeLeft();
    void onSwipeRight();
    void onSwipeUp();
    void onSwipeDown();
    void onLongPress();

    void onIconLeft();
    void onIconRight();
    void onIconUp();
    void onIconDown();
}
