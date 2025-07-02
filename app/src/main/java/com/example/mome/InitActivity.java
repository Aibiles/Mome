package com.example.mome;

import android.annotation.SuppressLint;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.MotionEvent;
import android.widget.Chronometer;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.mome.manager.CameraStatusManager;
import com.example.mome.manager.VehicleControlManager;
import com.example.mome.view.Circle;

import java.text.SimpleDateFormat;
import java.util.Date;

public class InitActivity extends AppCompatActivity implements CameraStatusManager.CameraStatusListener, SensorEventListener {

    private static final String TAG = "InitActivity";
    
    // 传感器相关参数
    private static final float ACCEL_SENSITIVITY = 5.0f;        // 加速度敏感度
    private static final float SENSOR_FILTER_ALPHA = 0.8f;      // 传感器数据滤波系数
    private static final float MAX_SPEED = 200f;                // 最大时速
    private static final float BATTERY_DRAIN_RATE = 0.01f;       // 电量消耗速率
    
    private Chronometer chronometer;
    private SimpleDateFormat dateFormat;
    private Circle left;
    private Circle right;
    private int currentSpeed;      // 当前时速
    private int currentBattery;    // 当前电量
    private TextView tvLeft;
    private TextView tvRight;
    private boolean isClick;
    private Thread t1;
    private TextView tvFake;
    private ImageView ivBat;
    
    // 传感器相关变量
    private SensorManager sensorManager;
    private Sensor accelerometerSensor;
    private boolean isSensorEnabled = false;
    
    // 传感器数据
    private float[] accelValues = new float[3];       // [x, y, z]
    private float[] gravity = new float[3];           // 重力分量
    private float filteredAccelMagnitude = 0f;       // 过滤后的加速度幅值
    private Handler uiHandler;
    
    // 摄像头状态显示组件
    private ImageView ivCameraDirection;
    private TextView tvDirection;
    private TextView tvCameraPosition;

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_init);
        getWindow().getDecorView().setSystemUiVisibility(6);

        chronometer = findViewById(R.id.date);
        dateFormat = new SimpleDateFormat("hh:mm");
        chronometer.setOnChronometerTickListener(c->{
            chronometer.setText(dateFormat.format(new Date()));
        });
        chronometer.start();

        left = findViewById(R.id.left);
        right = findViewById(R.id.right);
        currentSpeed = 0;
        currentBattery = 100;
        tvLeft = findViewById(R.id.tv_left);
        tvRight = findViewById(R.id.tv_right);

        ivBat = findViewById(R.id.iv_bat);

        // 初始化UI Handler
        uiHandler = new Handler(Looper.getMainLooper());

        // 初始化摄像头状态显示组件
        initCameraStatusViews();
        
        // 注册摄像头状态监听器
        CameraStatusManager.getInstance().addListener(this);

        new Thread(()->{
            while (currentBattery < 200) {
                currentBattery++;
                runOnUiThread(()->{
                    tvRight.setText(String.valueOf(currentBattery));
                    right.refreshUi(currentBattery);
                });
                try {
                    Thread.sleep(3);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            while (currentBattery > 100) {
                currentBattery--;
                runOnUiThread(()->{
                    tvRight.setText(String.valueOf(currentBattery));
                    right.refreshUi(currentBattery);
                });
                try {
                    Thread.sleep(5);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();

        // 初始化传感器
        initSensors();
        
        // 启用传感器监听
        enableSensors();

        // 初始化仪表盘显示
        updateSpeedDisplay();
        updateBatteryDisplay();
    }
    
    /**
     * 初始化传感器
     */
    private void initSensors() {
        try {
            sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
            
            if (sensorManager != null) {
                accelerometerSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
                
                Log.d(TAG, "=== 传感器初始化检测 ===");
                Log.d(TAG, "SensorManager: " + (sensorManager != null ? "可用" : "不可用"));
                
                if (accelerometerSensor != null) {
                    Log.d(TAG, "加速度传感器: 可用");
                    Log.d(TAG, "加速度名称: " + accelerometerSensor.getName());
                    Log.d(TAG, "加速度供应商: " + accelerometerSensor.getVendor());
                    Log.d(TAG, "加速度最大范围: " + accelerometerSensor.getMaximumRange());
                    Log.d(TAG, "加速度分辨率: " + accelerometerSensor.getResolution());
                } else {
                    Log.w(TAG, "加速度传感器: 不可用");
                }
                
            } else {
                Log.e(TAG, "SensorManager 获取失败!");
            }
        } catch (Exception e) {
            Log.e(TAG, "传感器初始化失败", e);
        }
    }
    
    /**
     * 启用传感器监听
     */
    public void enableSensors() {
        if (sensorManager != null && !isSensorEnabled) {
            Log.d(TAG, "=== 开始启用传感器监听 ===");
            
            boolean accelSuccess = false;
            
            if (accelerometerSensor != null) {
                accelSuccess = sensorManager.registerListener(this, accelerometerSensor, SensorManager.SENSOR_DELAY_UI);
                Log.d(TAG, "加速度传感器注册结果: " + (accelSuccess ? "成功" : "失败"));
            } else {
                Log.w(TAG, "加速度传感器为null，跳过注册");
            }
            
            isSensorEnabled = accelSuccess;
            
            Log.d(TAG, "传感器监听状态: " + (isSensorEnabled ? "已启用" : "启用失败"));
            Log.d(TAG, "==========================");
        } else {
            Log.w(TAG, "传感器启用失败 - SensorManager: " + (sensorManager != null) + 
                      ", 已启用: " + isSensorEnabled);
        }
    }
    
    /**
     * 禁用传感器监听
     */
    public void disableSensors() {
        if (sensorManager != null && isSensorEnabled) {
            sensorManager.unregisterListener(this);
            isSensorEnabled = false;
            
            // 重置到默认状态
            currentSpeed = 0;
            currentBattery = 100;
            
            Log.d(TAG, "传感器监听已禁用");
            updateSpeedDisplay();
            updateBatteryDisplay();
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        try {
            switch (event.sensor.getType()) {
                case Sensor.TYPE_ACCELEROMETER:
                    handleAccelerometerData(event);
                    break;
                default:
                    break;
            }
        } catch (Exception e) {
            Log.e(TAG, "处理传感器数据失败", e);
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        String sensorName = "未知";
        switch (sensor.getType()) {
            case Sensor.TYPE_ACCELEROMETER:
                sensorName = "加速度传感器";
                break;
        }
        Log.d(TAG, String.format("%s精度变化 - 精度: %d", sensorName, accuracy));
    }
    
    /**
     * 处理加速度传感器数据
     */
    private void handleAccelerometerData(SensorEvent event) {
        // 获取加速度数据
        accelValues[0] = event.values[0]; // X轴
        accelValues[1] = event.values[1]; // Y轴  
        accelValues[2] = event.values[2]; // Z轴
        
        // 提取重力分量
        gravity[0] = SENSOR_FILTER_ALPHA * gravity[0] + (1 - SENSOR_FILTER_ALPHA) * accelValues[0];
        gravity[1] = SENSOR_FILTER_ALPHA * gravity[1] + (1 - SENSOR_FILTER_ALPHA) * accelValues[1];
        gravity[2] = SENSOR_FILTER_ALPHA * gravity[2] + (1 - SENSOR_FILTER_ALPHA) * accelValues[2];
        
        // 去除重力，得到线性加速度
        float linearAccelX = accelValues[0] - gravity[0];
        float linearAccelY = accelValues[1] - gravity[1];
        float linearAccelZ = accelValues[2] - gravity[2];
        
        // 计算加速度幅值
        float accelMagnitude = (float) Math.sqrt(linearAccelX * linearAccelX + 
                                               linearAccelY * linearAccelY + 
                                               linearAccelZ * linearAccelZ);
        
        // 过滤加速度数据
        filteredAccelMagnitude = SENSOR_FILTER_ALPHA * filteredAccelMagnitude + 
                                (1 - SENSOR_FILTER_ALPHA) * accelMagnitude;
        
        // 根据加速度更新时速和电量
        updateSpeedFromAccel();
        updateBatteryFromAccel();
    }
    
    /**
     * 根据加速度更新时速
     */
    private void updateSpeedFromAccel() {
        try {
            // 将加速度幅值转换为时速 (0-200)
            float normalizedAccel = filteredAccelMagnitude * ACCEL_SENSITIVITY;
            int newSpeed = (int) Math.min(normalizedAccel * 15, MAX_SPEED); // 乘以20进行放大
            
            // 检查速度变化是否显著
            if (Math.abs(currentSpeed - newSpeed) > 2) { // 变化超过2才更新
                int oldSpeed = currentSpeed;
                currentSpeed = newSpeed;
                
                Log.d(TAG, String.format("时速更新: %d -> %d (加速度: %.4f)", 
                        oldSpeed, newSpeed, filteredAccelMagnitude));
                
                uiHandler.post(this::updateSpeedDisplay);
            }
        } catch (Exception e) {
            Log.e(TAG, "更新时速失败", e);
        }
    }
    
    /**
     * 根据加速度更新电量
     */
    private void updateBatteryFromAccel() {
        try {
            // 根据加速度计算电量消耗
            // 加速度越大，电量消耗越快
            float drainRate = filteredAccelMagnitude * BATTERY_DRAIN_RATE;
            
            // 模拟电量缓慢下降（每次最多下降0.1）
            if (drainRate > 0.05f && currentBattery > 0) {
                int oldBattery = currentBattery;
                currentBattery = Math.max(0, currentBattery - 1);
                
                if (oldBattery != currentBattery) {
                    Log.d(TAG, String.format("电量更新: %d -> %d (消耗率: %.4f)", 
                            oldBattery, currentBattery, drainRate));
                    
                    uiHandler.post(this::updateBatteryDisplay);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "更新电量失败", e);
        }
    }
    
    /**
     * 更新时速显示
     */
    private void updateSpeedDisplay() {
        tvLeft.setText(String.valueOf(currentSpeed));
        left.refreshUi(currentSpeed);
    }
    
    /**
     * 更新电量显示
     */
    private void updateBatteryDisplay() {
        tvRight.setText(String.valueOf(currentBattery));
        right.refreshUi(currentBattery);
    }
    
    /**
     * 初始化摄像头状态显示组件
     */
    private void initCameraStatusViews() {
        ivCameraDirection = findViewById(R.id.ivCameraDirection);
        tvDirection = findViewById(R.id.tv_direction);
        tvCameraPosition = findViewById(R.id.tvCameraPosition);
    }
    
    /**
     * 摄像头状态改变回调
     */
    @Override
    public void onCameraStatusChanged(VehicleControlManager.Direction direction, 
                                      VehicleControlManager.CameraPosition cameraPosition) {
        runOnUiThread(() -> {
            updateCameraStatusDisplay(direction, cameraPosition);
        });
    }
    
    /**
     * 更新摄像头状态显示
     */
    private void updateCameraStatusDisplay(VehicleControlManager.Direction direction, 
                                           VehicleControlManager.CameraPosition cameraPosition) {
        // 更新方向文本
        tvDirection.setText(VehicleControlManager.getDirectionName(direction));
        
        // 更新摄像头位置文本
        tvCameraPosition.setText(VehicleControlManager.getCameraName(cameraPosition));
        
        // 更新摄像头方向图标
        int iconRes = getCameraDirectionIcon(cameraPosition);
        ivCameraDirection.setImageResource(iconRes);
    }
    
    /**
     * 根据摄像头位置获取对应的图标资源
     */
    private int getCameraDirectionIcon(VehicleControlManager.CameraPosition cameraPosition) {
        switch (cameraPosition) {
            case FRONT:
                return R.drawable.ic_camera_front;
            case REAR:
                return R.drawable.ic_camera_rear;
            case LEFT:
                return R.drawable.ic_camera_left;
            case RIGHT:
                return R.drawable.ic_camera_right;
            default:
                return R.drawable.ic_camera_front;
        }
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        // 禁用传感器监听
        disableSensors();
        
        // 清理UI Handler
        if (uiHandler != null) {
            uiHandler.removeCallbacksAndMessages(null);
        }
        
        // 移除摄像头状态监听器
        CameraStatusManager.getInstance().removeListener(this);
        
        Log.d(TAG, "InitActivity已清理");
    }
    
    @Override
    protected void onPause() {
        super.onPause();
        // 暂停时禁用传感器以节省电量
        disableSensors();
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        // 恢复时重新启用传感器
        enableSensors();
    }
}