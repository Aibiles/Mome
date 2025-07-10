package com.example.mome;

import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.transition.TransitionManager;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import com.example.mome.config.Camera360Config;
import com.example.mome.handle.GestureCallback;
import com.example.mome.handle.GestureHandler;
import com.example.mome.manager.CameraConnectionManager;
import com.example.mome.manager.CameraStatusManager;
import com.example.mome.manager.VehicleControlManager;
import com.example.mome.view.AssistLineOverlay;
import com.github.niqdev.mjpeg.MjpegView;

import org.opencv.android.OpenCVLoader;



public class MomeActivity extends AppCompatActivity implements
        VehicleControlManager.VehicleControlListener,
        CameraConnectionManager.ConnectionStatusListener,
        GestureCallback {
    
    private static final String TAG = "MainActivity";
    // UI组件
    private MjpegView frontCameraView, rearCameraView, leftCameraView, rightCameraView;
    private View frontCameraContainer, rearCameraContainer, leftCameraContainer, rightCameraContainer;
    private ImageView topTrapezoidView, leftTrapezoidView, rightTrapezoidView, bottomTrapezoidView;
    private TextView leftStatusText;
    private View frontStatus, rearStatus, leftStatus, rightStatus;
    
    // 控制按钮
    private ImageButton btnForward, btnBackward, btnLeft, btnRight, btnStop;
    
    // 管理器
    private VehicleControlManager vehicleControlManager;
    private CameraConnectionManager cameraConnectionManager;
    
    // 辅助线覆盖层
    private AssistLineOverlay assistLineOverlay;
    
    // 全屏状态
    private boolean isFullscreenMode = false;
    private View controlPanel, connectionStatus;
    private ImageView ivCar;
    private FrameLayout leftPanel;
    
    // 当前全屏显示的摄像头索引
    private View leftDivider;
    private View centerDivider;
    private ConstraintLayout mainLayout;
    private ImageView ivPedal;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        setupFullScreen();
        setContentView(R.layout.activity_mome);
        
        // 初始化组件
        initViews();
        initManagers();
    }
    
    /**
     * 设置全屏显示
     */
    private void setupFullScreen() {
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        WindowInsetsControllerCompat controller = WindowCompat.getInsetsController(
                getWindow(), getWindow().getDecorView());
        controller.hide(WindowInsetsCompat.Type.systemBars());
        controller.setSystemBarsBehavior(
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
    }
    
    /**
     * 初始化视图组件
     */
    private void initViews() {
        // 左侧摄像头视图
        frontCameraView = findViewById(R.id.frontCameraView);
        rearCameraView = findViewById(R.id.rearCameraView);
        leftCameraView = findViewById(R.id.leftCameraView);
        rightCameraView = findViewById(R.id.rightCameraView);


        // 左侧摄像头容器
        frontCameraContainer = findViewById(R.id.frontCameraContainer);
        rearCameraContainer = findViewById(R.id.rearCameraContainer);
        leftCameraContainer = findViewById(R.id.leftCameraContainer);
        rightCameraContainer = findViewById(R.id.rightCameraContainer);
        
        // 右侧显示组件
        topTrapezoidView = findViewById(R.id.topTrapezoidView);
        leftTrapezoidView = findViewById(R.id.leftTrapezoidView);
        rightTrapezoidView = findViewById(R.id.rightTrapezoidView);
        bottomTrapezoidView = findViewById(R.id.bottomTrapezoidView);
        ivCar = findViewById(R.id.iv_car);


        // 叠加层和状态组件
        leftStatusText = findViewById(R.id.leftStatusText);
        
        // 连接状态指示器
        frontStatus = findViewById(R.id.frontStatus);
        rearStatus = findViewById(R.id.rearStatus);
        leftStatus = findViewById(R.id.leftStatus);
        rightStatus = findViewById(R.id.rightStatus);
        
        // 控制按钮
        btnForward = findViewById(R.id.btnForward);
        btnBackward = findViewById(R.id.btnBackward);
        btnLeft = findViewById(R.id.btnLeft);
        btnRight = findViewById(R.id.btnRight);
        ivPedal = findViewById(R.id.iv_pedal);
//        btnStop = findViewById(R.id.btnStop);

        // 控制面板和状态指示器
        controlPanel = findViewById(R.id.controlPanel);
        connectionStatus = findViewById(R.id.connectionStatus);

        // 右侧视频
        leftPanel = findViewById(R.id.leftPanel);

        // 分割线
        leftDivider = findViewById(R.id.leftDivider);
        centerDivider = findViewById(R.id.centerDivider);

        // 约束布局
        mainLayout = findViewById(R.id.main);
        
        // 设置按钮监听器
        setupControlButtons();
    }
    
    /**
     * 设置控制按钮
     */
    @SuppressLint("ClickableViewAccessibility")
    private void setupControlButtons() {
        // 前进按钮（不需要辅助线）
        btnForward.setOnClickListener(v -> {
            vehicleControlManager.moveForward();
            // 前进不显示辅助线，清除现有辅助线
            startForwardTurnAssistLine();
        });

        // 后退按钮（长按显示倒车辅助线）
        btnBackward.setOnClickListener(v -> {
            vehicleControlManager.moveBackward();
            startReverseAssistLine();
        });
        
        // 左转按钮（长按显示左转辅助线）
        btnLeft.setOnClickListener(v -> {
            vehicleControlManager.turnLeft();
            startForwardTurnAssistLine();
        });
        
        // 右转按钮（长按显示右转辅助线）
        btnRight.setOnClickListener(v -> {
            vehicleControlManager.turnRight();
            startForwardTurnAssistLine();
        });
        
        // 停止按钮
//        btnStop.setOnClickListener(v -> {
//            vehicleControlManager.stop();
//            assistLineOverlay.clearLines();
//        });
        
        // 全屏切换按钮
        leftPanel.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                return false;
            }
        });
        GestureHandler gestureHandler = new GestureHandler(this, leftPanel, this);
    }
    
    /**
     * 初始化管理器
     */
    private void initManagers() {
        // 初始化车辆控制管理器
        vehicleControlManager = new VehicleControlManager();
        vehicleControlManager.setControlListener(this);
        
        // 初始化摄像头连接管理器
        cameraConnectionManager = new CameraConnectionManager(this, this);
        cameraConnectionManager.setStatusListener(this);

        // 添加摄像头
//        cameraConnectionManager.addCamera(Camera360Config.CAMERA_TOP, frontCameraView, topTrapezoidView);
//        cameraConnectionManager.addCamera(Camera360Config.CAMERA_BOTTOM, rearCameraView, bottomTrapezoidView);
//        cameraConnectionManager.addCamera(Camera360Config.CAMERA_LEFT, leftCameraView, leftTrapezoidView);
//        cameraConnectionManager.addCamera(Camera360Config.CAMERA_RIGHT, rightCameraView, rightTrapezoidView);
        
        // 初始化辅助线覆盖层
        assistLineOverlay = findViewById(R.id.assistLineOverlay);
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        
        // 连接所有摄像头
        cameraConnectionManager.connectAllCameras();
        
        // 初始显示前方摄像头
        switchLeftCameraView(VehicleControlManager.CameraPosition.FRONT);
        
        // 设置初始摄像头状态
        CameraStatusManager.getInstance().updateCameraStatus(
            VehicleControlManager.Direction.STOP, 
            VehicleControlManager.CameraPosition.FRONT);
        
        Log.d(TAG, "MainActivity 恢复");
    }
    
    @Override
    protected void onPause() {
        super.onPause();
        // 停止所有辅助线
        if (assistLineOverlay != null) {
            assistLineOverlay.clearLines();
        }
        
        // 暂停所有MjpegView播放
        if (frontCameraView != null) frontCameraView.stopPlayback();
        if (rearCameraView != null) rearCameraView.stopPlayback();
        if (leftCameraView != null) leftCameraView.stopPlayback();
        if (rightCameraView != null) rightCameraView.stopPlayback();
        
        Log.d(TAG, "MainActivity 暂停");
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();

        // 清理辅助线覆盖层
        if (assistLineOverlay != null) {
            assistLineOverlay.cleanup();
        }
        
        // 清理资源
        if (cameraConnectionManager != null) {
            cameraConnectionManager.cleanup();
        }
        
        // 停止所有MjpegView播放
        if (frontCameraView != null) frontCameraView.stopPlayback();
        if (rearCameraView != null) rearCameraView.stopPlayback();
        if (leftCameraView != null) leftCameraView.stopPlayback();
        if (rightCameraView != null) rightCameraView.stopPlayback();
        
        Log.d(TAG, "MainActivity 销毁");
    }
    
    // VehicleControlManager.VehicleControlListener 实现
    @Override
    public void onDirectionChanged(VehicleControlManager.Direction direction, 
                                  VehicleControlManager.CameraPosition cameraPosition) {
        runOnUiThread(() -> {
            // 切换左侧显示的摄像头
            switchLeftCameraView(cameraPosition);
            
            // 更新状态文本
            leftStatusText.setText(VehicleControlManager.getCameraName(cameraPosition));
            
            // 更新全局摄像头状态管理器
            CameraStatusManager.getInstance().updateCameraStatus(direction, cameraPosition);
            
            Log.d(TAG, "方向改变: " + direction + ", 摄像头: " + cameraPosition);
        });
    }
    
    @Override
    public void onMovementStateChanged(boolean isMoving) {
    }
    
    @Override
    public void onReverseStateChanged(boolean isReversing) {
        runOnUiThread(() -> {
            Log.d(TAG, "前进状态改变: " + isReversing);
        });
    }
    
    /**
     * 切换左侧摄像头视图
     */
    private void switchLeftCameraView(VehicleControlManager.CameraPosition cameraPosition) {
        // 隐藏所有摄像头容器
        frontCameraContainer.setVisibility(View.INVISIBLE);
        rearCameraContainer.setVisibility(View.INVISIBLE);
        leftCameraContainer.setVisibility(View.INVISIBLE);
        rightCameraContainer.setVisibility(View.INVISIBLE);

        // 显示对应的摄像头视图并连接
        switch (cameraPosition) {
            case FRONT:
                frontCameraContainer.setVisibility(View.VISIBLE);
                break;
            case REAR:
                rearCameraContainer.setVisibility(View.VISIBLE);
                break;
            case LEFT:
                leftCameraContainer.setVisibility(View.VISIBLE);
                break;
            case RIGHT:
                rightCameraContainer.setVisibility(View.VISIBLE);
                break;
        }
    }
    
    // CameraConnectionManager.ConnectionStatusListener 实现
    @Override
    public void onCameraConnected(int cameraIndex) {
        runOnUiThread(() -> {
            updateConnectionStatus(cameraIndex, true);
//            Toast.makeText(this, Camera360Config.getCameraName(cameraIndex) + " 连接成功",
//                          Toast.LENGTH_SHORT).show();
        });
    }
    
    @Override
    public void onCameraDisconnected(int cameraIndex) {
        runOnUiThread(() -> {
            updateConnectionStatus(cameraIndex, false);
//            Toast.makeText(this, Camera360Config.getCameraName(cameraIndex) + " 连接断开",
//                          Toast.LENGTH_SHORT).show();
        });
    }

    @Override
    public void onFrameCaptured(int cameraIndex, Bitmap bitmap) {
        if (cameraIndex == 0 && isFullscreenMode) {
//            ncnn.detectSeg(bitmap);
        }
    }

    @Override
    public void onConnectionError(int cameraIndex, String error) {
        runOnUiThread(() -> {
            updateConnectionStatus(cameraIndex, false);
            Log.e(TAG, "摄像头连接错误: " + Camera360Config.getCameraName(cameraIndex) + " - " + error);
        });
    }
    
    /**
     * 更新连接状态指示器
     */
    private void updateConnectionStatus(int cameraIndex, boolean isConnected) {
        View statusView = getStatusViewForCamera(cameraIndex);
        if (statusView != null) {
            statusView.setBackgroundResource(isConnected ? 
                    R.drawable.status_indicator_on : R.drawable.status_indicator_off);
        }
    }
    
    /**
     * 获取摄像头对应的状态指示器视图
     */
    private View getStatusViewForCamera(int cameraIndex) {
        switch (cameraIndex) {
            case Camera360Config.CAMERA_TOP: return frontStatus;
            case Camera360Config.CAMERA_BOTTOM: return rearStatus;
            case Camera360Config.CAMERA_LEFT: return leftStatus;
            case Camera360Config.CAMERA_RIGHT: return rightStatus;
            default: return null;
        }
    }
    
    // ==================== 辅助线方法 ====================

    
    /**
     * 开始辅助线
     */
    private void startForwardTurnAssistLine() {
        if (assistLineOverlay != null) {
            assistLineOverlay.startDrawing(AssistLineOverlay.LineType.FORWARD);
            Log.d(TAG, "开始辅助线");
        }
    }
    
    /**
     * 开始倒车辅助线
     */
    private void startReverseAssistLine() {
        if (assistLineOverlay != null) {
            assistLineOverlay.startDrawing(AssistLineOverlay.LineType.REVERSE);
            Log.d(TAG, "开始倒车辅助线");
        }
    }
    
    /**
     * 停止辅助线（松开按钮时调用）
     */
    private void stopAssistLine() {
        if (assistLineOverlay != null) {
            assistLineOverlay.stopDrawing();
            Log.d(TAG, "停止辅助线");
        }
    }
    
    /**
     * 切换全屏模式
     */
    private void toggleFullscreenMode() {
        isFullscreenMode = !isFullscreenMode;
        
        if (isFullscreenMode) {
            // 进入全屏模式
            enterFullscreenMode();
            Log.d(TAG, "进入全屏模式");
        } else {
            // 退出全屏模式
            exitFullscreenMode();
            Log.d(TAG, "退出全屏模式");
        }
    }
    
    /**
     * 进入全屏模式 - 只显示当前左侧摄像头
     */
    private void enterFullscreenMode() {
        // 使用 ConstraintSet 实现平滑的约束变化动画
        ConstraintSet constraintSet = new ConstraintSet();
        constraintSet.clone(mainLayout);

        // 移动到底部
        constraintSet.connect(
                R.id.leftDivider,
                ConstraintSet.BOTTOM,
                ConstraintSet.PARENT_ID,
                ConstraintSet.BOTTOM
        );

        constraintSet.connect(
                R.id.centerDivider,
                ConstraintSet.END,
                ConstraintSet.PARENT_ID,
                ConstraintSet.END
        );

        // 添加高度变化动画
        animateDividerHeight(400, 0);

        // 应用动画
        TransitionManager.beginDelayedTransition(mainLayout);
        constraintSet.applyTo(mainLayout);

        // 添加颜色变化动画
        animateDividerColor(200, "#000000");
    }
    
    /**
     * 退出全屏模式 - 恢复正常显示
     */
    private void exitFullscreenMode() {

        ConstraintSet constraintSet = new ConstraintSet();
        constraintSet.clone(mainLayout);

        // 恢复原始约束：app:layout_constraintBottom_toTopOf="@id/guideline12"
        constraintSet.connect(
                R.id.leftDivider,
                ConstraintSet.BOTTOM,
                R.id.guideline12,
                ConstraintSet.BOTTOM
        );

        constraintSet.connect(
                R.id.centerDivider,
                ConstraintSet.END,
                R.id.guideline6,
                ConstraintSet.END
        );

        // 添加高度变化动画
        animateDividerHeight(400, 1);

        // 应用动画
        TransitionManager.beginDelayedTransition(mainLayout);
        constraintSet.applyTo(mainLayout);

        // 添加颜色变化动画
        animateDividerColor(200, "#333333");
    }
    private void animateDividerHeight(int duration, float alpha) {
        ivCar.animate()
                .alpha(alpha)
                .setDuration(duration)
                .start();
        controlPanel.animate()
                .alpha(alpha)
                .setDuration(duration)
                .start();
        connectionStatus.animate()
                .alpha(alpha)
                .setDuration(duration)
                .start();
    }

    private void animateDividerColor(int duration, String toColor) {
        // 创建颜色变化动画
        leftDivider.animate()
                .setDuration(duration)
                .withEndAction(() -> {
                    leftDivider.setBackgroundColor(android.graphics.Color.parseColor(toColor));
                    leftDivider.animate()
                            .setDuration(200)
                            .start();
                })
                .start();

        centerDivider.animate()
                .setDuration(duration)
                .withEndAction(() -> {
                    centerDivider.setBackgroundColor(android.graphics.Color.parseColor(toColor));
                    centerDivider.animate()
                            .setDuration(200)
                            .start();
                })
                .start();
    }

    @Override
    protected void onStop() {
        super.onStop();
        cameraConnectionManager.disconnectAllCameras();
    }

    @Override
    public void onToggleFullscreen() {
        toggleFullscreenMode();
    }

    @Override
    public void onSwipeLeft() {
        vehicleControlManager.turnLeft();
        startForwardTurnAssistLine();
    }

    @Override
    public void onSwipeRight() {
        vehicleControlManager.turnRight();
        startForwardTurnAssistLine();
    }

    @Override
    public void onSwipeUp() {
        vehicleControlManager.moveForward();
        startForwardTurnAssistLine();
    }

    @Override
    public void onSwipeDown() {
        vehicleControlManager.moveBackward();
        startReverseAssistLine();
    }

    @Override
    public void onLongPress() {
        vehicleControlManager.stop();
        assistLineOverlay.clearLines();
    }
}