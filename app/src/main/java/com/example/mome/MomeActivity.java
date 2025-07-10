package com.example.mome;

import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
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
import com.example.mome.manager.VehicleStatusManager;
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
    private VehicleStatusManager vehicleStatusManager;
    
    // 辅助线覆盖层
    private AssistLineOverlay assistLineOverlay;
    
    // 全屏状态
    private boolean isFullscreenMode = false;
    private View controlPanel, connectionStatus;
    private ImageView ivCar;
    private FrameLayout leftPanel;
    
    // 当前全屏显示的摄像头索引
    private ImageView ivPedal;
    private ImageView ivSwipeArrow;

    // 踏板长按相关
    private Handler pedalHandler = new Handler(Looper.getMainLooper());
    private Runnable pedalPressRunnable;
    private boolean isPedalPressed = false;
    private float currentSpeed = 0f;
    private float currentBattery = 100f;
    private float pedalPressProgress = 0f;
    private float baseProgress = 0f; // 基础进度，用于记忆功能
    private ValueAnimator recoveryAnimator; // 恢复动画器
    private static final float MAX_SPEED = 100f;
    private static final float MIN_BATTERY = 0f;
    private static final int PEDAL_UPDATE_INTERVAL = 50; // 50ms更新间隔
    
    // 电量消耗相关 - 独立于踏板进度
    private int batteryConsumeCounter = 0; // 电量消耗计数器
    private static final int BATTERY_CONSUME_INTERVAL = 40; // 每40次更新消耗1点电量 (40 * 50ms = 2秒消耗1点)
    private FrameLayout flSwipeArrow;
    private int currentFullscreenCameraIndex;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 初始化OpenCV
        initOpenCV();

        setupFullScreen();
        setContentView(R.layout.activity_mome);
        
        // 初始化组件
        initViews();
        initManagers();
    }

    /**
     * 初始化OpenCV
     */
    private void initOpenCV() {
        if (!OpenCVLoader.initLocal()) {
            Log.e(TAG, "OpenCV初始化失败");
            Toast.makeText(this, "OpenCV初始化失败", Toast.LENGTH_LONG).show();
        } else {
            Log.d(TAG, "OpenCV初始化成功");
        }
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
        ivSwipeArrow = findViewById(R.id.iv_swipe_arrow);
        flSwipeArrow = findViewById(R.id.fl_swipe_arrow);

        // 控制面板和状态指示器
        controlPanel = findViewById(R.id.controlPanel);
        connectionStatus = findViewById(R.id.connectionStatus);

        // 右侧视频
        leftPanel = findViewById(R.id.leftPanel);
        
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
        
        // 踏板长按监听器
        ivPedal.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        ivPedal.animate().rotationX(-8).setDuration(100).start();
                        startPedalPress();
                        assistLineOverlay.setAutoSensorMode(false);
                        return true;
                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_CANCEL:
                        ivPedal.animate().rotationX(8).setDuration(100).start();
                        assistLineOverlay.setAutoSensorMode(true);
                        stopPedalPress();
                        return true;
                }
                return false;
            }
        });

        GestureHandler gestureHandler = new GestureHandler(this, leftPanel, this);
    }

    /**
     * 开始踏板长按
     */
    private void startPedalPress() {
        if (isPedalPressed) return;
        
        // 停止恢复动画（如果正在进行）
        if (recoveryAnimator != null && recoveryAnimator.isRunning()) {
            recoveryAnimator.cancel();
        }
        
        isPedalPressed = true;
        pedalPressProgress = baseProgress; // 从基础进度开始，而不是从0开始
        
        // 重置电量消耗计数器
        batteryConsumeCounter = 0;
        
        // 开始显示辅助线
        if (vehicleControlManager.getCurrentCamera() == VehicleControlManager.CameraPosition.REAR) {
            startReverseAssistLine();
        } else {
            startForwardTurnAssistLine();
        }
        
        // 立即更新一次UI以显示当前状态
        updatePedalUI();
        
        // 通知车辆状态管理器踏板被按下
        if (vehicleStatusManager != null) {
            vehicleStatusManager.setPedalPressed(true);
        }
        
        // 开始踏板长按更新循环
        pedalPressRunnable = new Runnable() {
            @Override
            public void run() {
                if (isPedalPressed) {
                    updatePedalProgress();
                    pedalHandler.postDelayed(this, PEDAL_UPDATE_INTERVAL);
                }
            }
        };
        pedalHandler.post(pedalPressRunnable);
        
        Log.d(TAG, String.format("开始踏板长按，从进度 %.2f 开始，当前电量 %.1f", baseProgress, currentBattery));
    }
    
    /**
     * 停止踏板长按
     */
    private void stopPedalPress() {
        if (!isPedalPressed) return;
        
        isPedalPressed = false;
        
        // 停止更新循环
        if (pedalPressRunnable != null) {
            pedalHandler.removeCallbacks(pedalPressRunnable);
        }
        
        // 保存当前进度作为基础进度
        baseProgress = pedalPressProgress;
        
        // 通知车辆状态管理器踏板被松开
        if (vehicleStatusManager != null) {
            vehicleStatusManager.setPedalPressed(false);
        }
        
        // 开始恢复动画
        startPedalRelease();
        
        Log.d(TAG, "停止踏板长按");
    }
    
    /**
     * 更新踏板长按进度
     */
    private void updatePedalProgress() {
        // 增加进度（0-1之间）
        pedalPressProgress = Math.min(1f, pedalPressProgress + 0.02f);
        
        // 更新UI
        updatePedalUI();
        
        Log.d(TAG, String.format("踏板进度: %.2f, 速度: %.1f, 电量: %.1f%%", 
                pedalPressProgress, currentSpeed, currentBattery));
    }
    
    /**
     * 踏板松开后的恢复动画
     */
    private void startPedalRelease() {
        // 如果基础进度已经是0，则不需要恢复动画
        if (baseProgress <= 0f) {
            if (assistLineOverlay != null) {
                assistLineOverlay.clearLines();
            }
            return;
        }
        
        recoveryAnimator = ValueAnimator.ofFloat(baseProgress, 0f);
        recoveryAnimator.setDuration(500);
        recoveryAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
        
        recoveryAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                float progress = (float) animation.getAnimatedValue();
                
                // 实时更新基础进度和显示进度
                baseProgress = progress;
                pedalPressProgress = progress;
                
                // 只更新速度和辅助线，不影响电量
                currentSpeed = pedalPressProgress * MAX_SPEED;
                updateAssistLineColor(pedalPressProgress);
                
                // 同步更新车辆状态管理器（保持电量不变）
                if (vehicleStatusManager != null) {
                    vehicleStatusManager.updateVehicleStatus(
                        (int) currentSpeed,     // 速度随恢复动画变化
                        (int) currentBattery,   // 电量保持不变
                        pedalPressProgress,     // 踏板进度
                        false                   // 踏板未按下
                    );
                }
                
                Log.d(TAG, String.format("恢复进度: %.2f, 速度: %.1f, 电量: %.1f%% (保持)", 
                        progress, currentSpeed, currentBattery));
            }
        });

        
        recoveryAnimator.start();
        Log.d(TAG, String.format("开始恢复动画，从进度 %.2f 恢复到 0", baseProgress));
    }
    
    /**
     * 更新辅助线颜色
     */
    private void updateAssistLineColor(float progress) {
        if (assistLineOverlay != null) {
            // 使用AssistLineOverlay的新API来设置颜色进度
            assistLineOverlay.setColorProgress(progress);
        }
    }
    
    /**
     * 更新踏板相关UI
     */
    private void updatePedalUI() {
        // 更新速度（基于踏板进度）
        currentSpeed = pedalPressProgress * MAX_SPEED;
        
        // 更新电量（基于计数器消耗，只在踏板按下时消耗）
        if (isPedalPressed) {
            batteryConsumeCounter++;
            if (batteryConsumeCounter >= BATTERY_CONSUME_INTERVAL) {
                // 消耗1点电量
                currentBattery = Math.max(MIN_BATTERY, currentBattery - 1);
                batteryConsumeCounter = 0; // 重置计数器
                Log.d(TAG, String.format("电量消耗：当前电量 %.1f%%", currentBattery));
            }
        }
        
        // 更新辅助线颜色
        updateAssistLineColor(pedalPressProgress);
        
        // 同步更新车辆状态管理器
        if (vehicleStatusManager != null) {
            vehicleStatusManager.updateVehicleStatus(
                (int) currentSpeed,     // 速度 (转换为int)
                (int) currentBattery,   // 电量 (转换为int)
                pedalPressProgress,     // 踏板进度
                isPedalPressed          // 踏板按下状态
            );
        }
    }
    
    /**
     * 获取当前速度
     */
    public float getCurrentSpeed() {
        return currentSpeed;
    }
    
    /**
     * 获取当前电量
     */
    public float getCurrentBattery() {
        return currentBattery;
    }
    
    /**
     * 获取踏板按压进度
     */
    public float getPedalPressProgress() {
        return pedalPressProgress;
    }
    
    /**
     * 是否正在按压踏板
     */
    public boolean isPedalPressed() {
        return isPedalPressed;
    }
    
    /**
     * 获取基础进度（记忆功能）
     */
    public float getBaseProgress() {
        return baseProgress;
    }
    
    /**
     * 重置电量到满电状态
     */
    public void resetBattery() {
        currentBattery = 100f;
        batteryConsumeCounter = 0;
        Log.d(TAG, "电量已重置到100%");
        
        // 同步更新状态管理器
        if (vehicleStatusManager != null) {
            vehicleStatusManager.updateBattery(100);
        }
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

        // 初始化车辆状态管理器
        vehicleStatusManager = VehicleStatusManager.getInstance();

        // 添加摄像头
        cameraConnectionManager.addCamera(Camera360Config.CAMERA_TOP, frontCameraView, topTrapezoidView);
        cameraConnectionManager.addCamera(Camera360Config.CAMERA_BOTTOM, rearCameraView, bottomTrapezoidView);
        cameraConnectionManager.addCamera(Camera360Config.CAMERA_LEFT, leftCameraView, leftTrapezoidView);
        cameraConnectionManager.addCamera(Camera360Config.CAMERA_RIGHT, rightCameraView, rightTrapezoidView);
        
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
        
        // 隐藏滑动箭头
        if (flSwipeArrow != null) {
            flSwipeArrow.setVisibility(View.GONE);
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

        // 清理踏板相关资源
        if (pedalHandler != null) {
            pedalHandler.removeCallbacksAndMessages(null);
        }
        
        // 清理恢复动画
        if (recoveryAnimator != null && recoveryAnimator.isRunning()) {
            recoveryAnimator.cancel();
        }
        
        // 清理滑动箭头动画
        if (flSwipeArrow != null) {
            flSwipeArrow.clearAnimation();
            flSwipeArrow.animate().cancel();
            flSwipeArrow.setVisibility(View.GONE);
        }
        
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
                leftStatusText.setText("前方摄像头");
                break;
            case REAR:
                rearCameraContainer.setVisibility(View.VISIBLE);
                leftStatusText.setText("后方摄像头");
                break;
            case LEFT:
                leftCameraContainer.setVisibility(View.VISIBLE);
                leftStatusText.setText("左侧摄像头");
                break;
            case RIGHT:
                rightCameraContainer.setVisibility(View.VISIBLE);
                leftStatusText.setText("右侧摄像头");
                break;
        }
        
        // 更新全局摄像头状态管理器
        CameraStatusManager.getInstance().updateCameraStatus(
            VehicleControlManager.Direction.STOP, 
            cameraPosition);
        
        Log.d(TAG, "切换摄像头: " + cameraPosition);
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

        // 获取当前左侧显示的摄像头位置
        VehicleControlManager.CameraPosition currentCameraPosition = vehicleControlManager.getCurrentCamera();
        currentFullscreenCameraIndex = currentCameraPosition.getValue();

        // 隐藏所有摄像头容器
        frontCameraContainer.setVisibility(View.GONE);
        rearCameraContainer.setVisibility(View.GONE);
        leftCameraContainer.setVisibility(View.GONE);
        rightCameraContainer.setVisibility(View.GONE);
        
        // 显示当前摄像头对应的容器
        switch (currentCameraPosition) {
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
        
        // 隐藏梯形拼接视图
        topTrapezoidView.setVisibility(View.GONE);
        leftTrapezoidView.setVisibility(View.GONE);
        rightTrapezoidView.setVisibility(View.GONE);
        bottomTrapezoidView.setVisibility(View.GONE);
        ivCar.setVisibility(View.GONE);
        
        // 隐藏控制面板和连接状态指示器
        controlPanel.setVisibility(View.INVISIBLE);

        // 隐藏分隔线
        findViewById(R.id.leftDivider).setVisibility(View.GONE);
        findViewById(R.id.centerDivider).setVisibility(View.GONE);


        // 修改左侧面板布局参数以全屏显示
        ViewGroup.LayoutParams layoutParams = leftPanel.getLayoutParams();
        layoutParams.height = ViewGroup.LayoutParams.MATCH_PARENT;
        layoutParams.width = ViewGroup.LayoutParams.MATCH_PARENT;
        leftPanel.setLayoutParams(layoutParams);
        
        Log.d(TAG, "进入全屏模式 - 显示摄像头: " + VehicleControlManager.getCameraName(currentCameraPosition));
    }
    
    /**
     * 退出全屏模式 - 恢复正常显示
     */
    private void exitFullscreenMode() {

        // 恢复梯形拼接视图
        topTrapezoidView.setVisibility(View.VISIBLE);
        leftTrapezoidView.setVisibility(View.VISIBLE);
        rightTrapezoidView.setVisibility(View.VISIBLE);
        bottomTrapezoidView.setVisibility(View.VISIBLE);
        ivCar.setVisibility(View.VISIBLE);

        
        // 恢复控制面板和连接状态指示器
        controlPanel.setVisibility(View.VISIBLE);

        // 恢复分隔线
        findViewById(R.id.leftDivider).setVisibility(View.VISIBLE);
        findViewById(R.id.centerDivider).setVisibility(View.VISIBLE);

        // 恢复左侧面板的原始布局参数
        ViewGroup.LayoutParams layoutParams = leftPanel.getLayoutParams();
        layoutParams.height = 0;
        layoutParams.width = 0;
        leftPanel.setLayoutParams(layoutParams);
        
        // 恢复显示当前左侧摄像头
        switchLeftCameraView(vehicleControlManager.getCurrentCamera());


        Log.d(TAG, "退出全屏模式 - 恢复到摄像头: " + VehicleControlManager.getCameraName(vehicleControlManager.getCurrentCamera()));
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
        // 抬起时切换到左侧摄像头
        vehicleControlManager.turnLeft();
        startForwardTurnAssistLine();
        hideSwipeArrow();
        Log.d(TAG, "滑动结束：切换到左侧摄像头");
    }

    @Override
    public void onSwipeRight() {
        // 抬起时切换到右侧摄像头
        vehicleControlManager.turnRight();
        startForwardTurnAssistLine();
        hideSwipeArrow();
        Log.d(TAG, "滑动结束：切换到右侧摄像头");
    }

    @Override
    public void onSwipeUp() {
        // 抬起时切换到前方摄像头
        vehicleControlManager.moveForward();
        startForwardTurnAssistLine();
        hideSwipeArrow();
        Log.d(TAG, "滑动结束：切换到前方摄像头");
    }

    @Override
    public void onSwipeDown() {
        // 抬起时切换到后方摄像头
        vehicleControlManager.moveBackward();
        startReverseAssistLine();
        hideSwipeArrow();
        Log.d(TAG, "滑动结束：切换到后方摄像头");
    }

    @Override
    public void onLongPress() {
        vehicleControlManager.stop();
        assistLineOverlay.clearLines();
    }

    @Override
    public void onIconLeft() {
//        ivSwipeArrow.setRotation(-45);
        ivSwipeArrow.animate().rotation(-45).setDuration(200).start();
        showSwipeArrow(R.drawable.wheel);
        Log.d(TAG, "显示左箭头");
    }

    @Override
    public void onIconRight() {
        showSwipeArrow(R.drawable.wheel);
//        ivSwipeArrow.setRotation(45);
        ivSwipeArrow.animate().rotation(45).setDuration(200).start();

        Log.d(TAG, "显示右箭头");
    }

    @Override
    public void onIconUp() {
//        ivSwipeArrow.setRotation(0);
        ivSwipeArrow.animate().rotation(0).setDuration(200).start();
        showSwipeArrow(R.drawable.wheel);
        Log.d(TAG, "显示上箭头");
    }

    @Override
    public void onIconDown() {
        ivSwipeArrow.setRotation(0);
        showSwipeArrow(R.drawable.reverse_car);
        Log.d(TAG, "显示下箭头");
    }
    
    /**
     * 显示滑动方向箭头
     */
    private void showSwipeArrow(int arrowResourceId) {
        if (flSwipeArrow == null) return;
        
        ivSwipeArrow.setImageResource(arrowResourceId);
        flSwipeArrow.setVisibility(View.VISIBLE);
        flSwipeArrow.setAlpha(0.8f);
    }
    
    /**
     * 隐藏滑动箭头
     */
    private void hideSwipeArrow() {
        if (flSwipeArrow == null) return;

        flSwipeArrow.animate()
                .alpha(0f)
                .setDuration(200)
                .withEndAction(() -> {
                    if (flSwipeArrow != null) {
                        flSwipeArrow.setVisibility(View.GONE);
                    }
                })
                .start();
    }
}