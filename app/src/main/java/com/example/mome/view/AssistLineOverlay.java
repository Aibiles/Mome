package com.example.mome.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

import com.example.mome.opencv.DistanceDetector;

import java.util.List;

/**
 * 辅助线覆盖层
 * 用于在摄像头画面上方绘制辅助线，支持根据陀螺仪Z轴和加速度传感器动态调整
 */
public class AssistLineOverlay extends View {
    
    private static final String TAG = "AssistLineOverlay";
    private MySensorEventListener listener;

    // 辅助线类型
    public enum LineType {
        FORWARD,      // 前进辅助线（包含直走、左转、右转）
        REVERSE,      // 倒车辅助线
        NONE          // 无辅助线
    }
    
    // 颜色变化参数
    private static final int COLOR_GREEN = Color.rgb(0, 255, 0);
    private static final int COLOR_YELLOW = Color.rgb(255, 255, 0);
    private static final int COLOR_RED = Color.rgb(255, 0, 0);
    
    // 辅助线绘制参数
    private static final int LINE_WIDTH = 8;
    private static final int LINE_ALPHA = 200;
    
    // 传感器相关参数
    private static final float GYRO_SENSITIVITY = 2.0f;         // 陀螺仪敏感度
    private static final float MAX_TURN_ANGLE = 30f;            // 最大转向角度（度）
    private static final float ACCEL_SENSITIVITY = 2.0f;        // 加速度敏感度
    private static final float SENSOR_FILTER_ALPHA = 0.8f;      // 传感器数据滤波系数
    private static final float GRAVITY = 9.8f;                  // 重力加速度
    
    private Handler colorHandler;
    private boolean isPressed = false;
    private LineType currentLineType = LineType.NONE;
    
    // 传感器相关变量
    private SensorManager sensorManager;
    private Sensor gyroscopeSensor;
    private Sensor accelerometerSensor;
    private boolean isSensorEnabled = false;
    private boolean isAutoSensorMode = false; // 是否自动传感器模式
    
    // 传感器数据
    private float[] gyroValues = new float[3];        // [x, y, z]
    private float[] accelValues = new float[3];       // [x, y, z]
    private float[] gravity = new float[3];           // 重力分量
    private float filteredRotationZ = 0f;            // 过滤后的Z轴旋转值（左右转向）
    private float filteredAccelMagnitude = 0f;       // 过滤后的加速度幅值
    private float currentTurnAngle = 0f;             // 当前转向角度（度）
    private float manualTurnAngle = 0f;              // 手动设置的转向角度
    private int currentColor = COLOR_GREEN;          // 当前颜色
    
    // 距离检测相关变量
    private boolean isDistanceDetectionEnabled = false;    // 是否启用距离检测
    private DistanceDetector.DetectionResult lastDetectionResult; // 最新检测结果
    private Handler distanceHandler;                      // 距离检测处理器
    private final long DISTANCE_UPDATE_INTERVAL = 100;   // 距离更新间隔（毫秒）
    
    public AssistLineOverlay(Context context) {
        super(context);
        init(context);
    }
    
    public AssistLineOverlay(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }
    
    public AssistLineOverlay(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }
    
    private void init(Context context) {
        colorHandler = new Handler(Looper.getMainLooper());
        distanceHandler = new Handler(Looper.getMainLooper());
        setWillNotDraw(false); // 允许绘制
        
        // 初始化传感器
        initSensors(context);
    }

    private class MySensorEventListener implements SensorEventListener {
        private long lastGyroLogTime = 0;
        private long lastAccelLogTime = 0;
        private static final long LOG_INTERVAL = 1000; // 每秒记录一次日志

        @Override
        public void onSensorChanged(SensorEvent event) {
            if (!isAutoSensorMode) {
                Log.d(TAG, "传感器数据被忽略 - 自动模式未启用");
                return;
            }

            try {
                long currentTime = System.currentTimeMillis();
                
                switch (event.sensor.getType()) {
                    case Sensor.TYPE_GYROSCOPE:
                        if (currentTime - lastGyroLogTime > LOG_INTERVAL) {
                            Log.d(TAG, String.format("收到陀螺仪数据: X=%.4f, Y=%.4f, Z=%.4f", 
                                    event.values[0], event.values[1], event.values[2]));
                            lastGyroLogTime = currentTime;
                        }
                        handleGyroscopeData(event);
                        break;
                    case Sensor.TYPE_ACCELEROMETER:
                        if (currentTime - lastAccelLogTime > LOG_INTERVAL) {
                            Log.d(TAG, String.format("收到加速度数据: X=%.4f, Y=%.4f, Z=%.4f", 
                                    event.values[0], event.values[1], event.values[2]));
                            lastAccelLogTime = currentTime;
                        }
                        handleAccelerometerData(event);
                        break;
                    default:
                        Log.d(TAG, "收到未知传感器数据，类型: " + event.sensor.getType());
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
                case Sensor.TYPE_GYROSCOPE:
                    sensorName = "陀螺仪";
                    break;
                case Sensor.TYPE_ACCELEROMETER:
                    sensorName = "加速度传感器";
                    break;
            }
            Log.d(TAG, String.format("%s精度变化 - 精度: %d", sensorName, accuracy));
        }
    }


    /**
     * 初始化传感器
     */
    private void initSensors(Context context) {
        try {
            sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
            listener = new MySensorEventListener();
            
            if (sensorManager != null) {
                gyroscopeSensor = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
                accelerometerSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
                
                // 详细的传感器检测日志
                Log.d(TAG, "=== 传感器初始化检测 ===");
                Log.d(TAG, "SensorManager: " + (sensorManager != null ? "可用" : "不可用"));
                
                if (gyroscopeSensor != null) {
                    Log.d(TAG, "陀螺仪传感器: 可用");
                    Log.d(TAG, "陀螺仪名称: " + gyroscopeSensor.getName());
                    Log.d(TAG, "陀螺仪供应商: " + gyroscopeSensor.getVendor());
                    Log.d(TAG, "陀螺仪最大范围: " + gyroscopeSensor.getMaximumRange());
                    Log.d(TAG, "陀螺仪分辨率: " + gyroscopeSensor.getResolution());
                } else {
                    Log.w(TAG, "陀螺仪传感器: 不可用");
                }
                
                if (accelerometerSensor != null) {
                    Log.d(TAG, "加速度传感器: 可用");
                    Log.d(TAG, "加速度名称: " + accelerometerSensor.getName());
                    Log.d(TAG, "加速度供应商: " + accelerometerSensor.getVendor());
                    Log.d(TAG, "加速度最大范围: " + accelerometerSensor.getMaximumRange());
                    Log.d(TAG, "加速度分辨率: " + accelerometerSensor.getResolution());
                } else {
                    Log.w(TAG, "加速度传感器: 不可用");
                }
                
                // 列出所有可用传感器
                List<Sensor> allSensors = sensorManager.getSensorList(Sensor.TYPE_ALL);
                Log.d(TAG, "设备总共有 " + allSensors.size() + " 个传感器:");
                for (Sensor sensor : allSensors) {
                    Log.d(TAG, "  - " + sensor.getName() + " (类型: " + sensor.getType() + ")");
                }
                Log.d(TAG, "========================");
                
            } else {
                Log.e(TAG, "SensorManager 获取失败!");
            }

            setAutoSensorMode(true);
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
            
            boolean gyroSuccess = false;
            boolean accelSuccess = false;
            
            if (gyroscopeSensor != null) {
                gyroSuccess = sensorManager.registerListener(listener, gyroscopeSensor, SensorManager.SENSOR_DELAY_UI);
                Log.d(TAG, "陀螺仪注册结果: " + (gyroSuccess ? "成功" : "失败"));
            } else {
                Log.w(TAG, "陀螺仪传感器为null，跳过注册");
            }
            
            if (accelerometerSensor != null) {
                accelSuccess = sensorManager.registerListener(listener, accelerometerSensor, SensorManager.SENSOR_DELAY_UI);
                Log.d(TAG, "加速度传感器注册结果: " + (accelSuccess ? "成功" : "失败"));
            } else {
                Log.w(TAG, "加速度传感器为null，跳过注册");
            }
            
            isSensorEnabled = gyroSuccess || accelSuccess;
            isAutoSensorMode = true;
            
            Log.d(TAG, "传感器监听状态: " + (isSensorEnabled ? "已启用" : "启用失败"));
            Log.d(TAG, "自动传感器模式: " + isAutoSensorMode);
            Log.d(TAG, "==========================");
            
            // 测试数据初始化
            if (isSensorEnabled) {
                // 开始一个定时器来检查是否有数据流入
                startSensorDataCheck();
            }
        } else {
            Log.w(TAG, "传感器启用失败 - SensorManager: " + (sensorManager != null) + 
                      ", 已启用: " + isSensorEnabled);
        }
    }
    
    /**
     * 开始传感器数据检查
     */
    private void startSensorDataCheck() {
        if (colorHandler != null) {
            colorHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    Log.d(TAG, "=== 传感器数据检查 ===");
                    Log.d(TAG, "当前转向角度: " + currentTurnAngle);
                    Log.d(TAG, "过滤后Z轴旋转: " + filteredRotationZ);
                    Log.d(TAG, "过滤后加速度幅值: " + filteredAccelMagnitude);
                    Log.d(TAG, "当前颜色: " + String.format("#%06X", (0xFFFFFF & currentColor)));
                    Log.d(TAG, "自动传感器模式: " + isAutoSensorMode);
                    Log.d(TAG, "==================");
                }
            }, 3000); // 3秒后检查
        }
    }
    
    /**
     * 禁用传感器监听
     */
    public void disableSensors() {
        if (sensorManager != null && isSensorEnabled) {
            sensorManager.unregisterListener(listener);
            isSensorEnabled = false;
            isAutoSensorMode = false;
            
            // 重置到默认状态
            currentTurnAngle = manualTurnAngle;
            currentColor = COLOR_GREEN;
            
            Log.d(TAG, "传感器监听已禁用");
            invalidate();
        }
    }
    
    /**
     * 手动设置转向角度（不使用传感器时）
     */
    public void setTurnAngle(float angle) {
        manualTurnAngle = Math.max(-MAX_TURN_ANGLE, Math.min(MAX_TURN_ANGLE, angle));
        if (!isAutoSensorMode) {
            currentTurnAngle = manualTurnAngle;
            invalidate();
        }
        Log.d(TAG, "手动设置转向角度: " + manualTurnAngle);
    }
    
    /**
     * 设置是否启用自动传感器模式
     */
    public void setAutoSensorMode(boolean enabled) {
        if (enabled) {
            enableSensors();
        } else {
            disableSensors();
        }
    }
    
    /**
     * 处理陀螺仪数据（使用Z轴）
     */
    private void handleGyroscopeData(SensorEvent event) {
        // 获取陀螺仪数据 (弧度/秒)
        gyroValues[0] = event.values[0]; // X轴（俯仰）
        gyroValues[1] = event.values[1]; // Y轴（偏航）
        gyroValues[2] = event.values[2]; // Z轴（翻滚，左右转向）
        
        // 对Z轴数据进行低通滤波
        filteredRotationZ = SENSOR_FILTER_ALPHA * filteredRotationZ + 
                           (1 - SENSOR_FILTER_ALPHA) * gyroValues[2];
        
        // 根据Z轴旋转计算转向角度
        updateTurnAngle();
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
        
        // 根据加速度更新颜色
        updateColor();
    }
    
    /**
     * 根据陀螺仪Z轴数据更新转向角度
     */
    private void updateTurnAngle() {
        try {
            // 将陀螺仪角速度转换为转向角度
            float rotationRate = Math.abs(filteredRotationZ) * GYRO_SENSITIVITY;
            
            // 限制在合理范围内
            float newTurnAngle = Math.min(rotationRate * 10, MAX_TURN_ANGLE); // 乘以10进行放大
            
            // 保持方向（正负）
            if (filteredRotationZ < 0) {
                newTurnAngle = -newTurnAngle; // 左转为负
            }
            
            // 如果变化较大，更新并重绘
            if (Math.abs(currentTurnAngle - newTurnAngle) > 0.5f) { // 降低阈值，提高敏感度
                float oldAngle = currentTurnAngle;
                currentTurnAngle = newTurnAngle;
                
                Log.d(TAG, String.format("转向角度更新: %.2f° -> %.2f° (陀螺仪Z: %.4f)", 
                        oldAngle, newTurnAngle, filteredRotationZ));
                
                post(this::invalidate); // 在主线程中重绘
            }
        } catch (Exception e) {
            Log.e(TAG, "更新转向角度失败", e);
        }
    }
    
    /**
     * 根据加速度数据更新颜色（渐变从绿色到红色）
     */
    private void updateColor() {
        try {
            // 根据加速度幅值计算颜色渐变
            float normalizedAccel = filteredAccelMagnitude * ACCEL_SENSITIVITY;
            
            // 限制范围在0-6之间，超过6就是最大红色
            float colorProgress = Math.min(normalizedAccel / 6.0f, 1.0f);
            
            // 从绿色(0,255,0)渐变到红色(255,0,0)
            int newColor = interpolateColor(COLOR_GREEN, COLOR_RED, colorProgress);
            
            // 检查颜色变化是否显著（避免过度重绘）
            if (isColorChangeSignificant(currentColor, newColor)) {
                String oldColorHex = String.format("#%06X", (0xFFFFFF & currentColor));
                String newColorHex = String.format("#%06X", (0xFFFFFF & newColor));
                currentColor = newColor;
                
                Log.d(TAG, String.format("颜色渐变更新: %s -> %s (加速度: %.4f, 进度: %.2f)", 
                        oldColorHex, newColorHex, filteredAccelMagnitude, colorProgress));
                
                post(this::invalidate);
            }
        } catch (Exception e) {
            Log.e(TAG, "更新颜色失败", e);
        }
    }
    
    /**
     * 颜色线性插值（从startColor渐变到endColor）
     */
    private int interpolateColor(int startColor, int endColor, float fraction) {
        // 限制fraction在0-1之间
        fraction = Math.max(0f, Math.min(1f, fraction));
        
        int startR = Color.red(startColor);
        int startG = Color.green(startColor);
        int startB = Color.blue(startColor);
        
        int endR = Color.red(endColor);
        int endG = Color.green(endColor);
        int endB = Color.blue(endColor);
        
        int currentR = (int) (startR + (endR - startR) * fraction);
        int currentG = (int) (startG + (endG - startG) * fraction);
        int currentB = (int) (startB + (endB - startB) * fraction);
        
        return Color.rgb(currentR, currentG, currentB);
    }
    
    /**
     * 检查颜色变化是否显著
     */
    private boolean isColorChangeSignificant(int oldColor, int newColor) {
        if (oldColor == newColor) return false;
        
        // 计算RGB各分量的差异
        int rDiff = Math.abs(Color.red(oldColor) - Color.red(newColor));
        int gDiff = Math.abs(Color.green(oldColor) - Color.green(newColor));
        int bDiff = Math.abs(Color.blue(oldColor) - Color.blue(newColor));
        
        // 如果任一分量差异超过10，认为是显著变化
        return rDiff > 10 || gDiff > 10 || bDiff > 10;
    }
    
    /**
     * 获取颜色名称（用于调试）
     */
    private String getColorName(int color) {
        if (color == COLOR_GREEN) return "绿色";
        if (color == COLOR_YELLOW) return "黄色";
        if (color == COLOR_RED) return "红色";
        return "未知颜色";
    }

//    @Override
//    public void onAccuracyChanged(Sensor sensor, int accuracy) {
//        Log.d(TAG, String.format("传感器精度变化 - 类型: %d, 精度: %d", sensor.getType(), accuracy));
//    }
    
    /**
     * 开始绘制辅助线（按下时调用）
     */
    public void startDrawing(LineType lineType) {
        this.currentLineType = lineType;
        this.isPressed = true;
        
        Log.d(TAG, "开始绘制辅助线: " + lineType);
        setVisibility(VISIBLE);
        invalidate();
    }
    
    /**
     * 停止绘制辅助线（松开时调用）
     */
    public void stopDrawing() {
        if (!isPressed) return;
        
        this.isPressed = false;
        Log.d(TAG, "停止绘制辅助线");
    }
    
    /**
     * 完全清除辅助线
     */
    public void clearLines() {
        this.currentLineType = LineType.NONE;
        this.isPressed = false;
        setVisibility(GONE);
        Log.d(TAG, "清除所有辅助线");
    }
    
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        
        if (currentLineType == LineType.NONE) return;
        
        try {
            // 设置画笔
            Paint paint = new Paint();
            paint.setColor(currentColor);
            paint.setAlpha(LINE_ALPHA);
            paint.setStrokeWidth(LINE_WIDTH);
            paint.setStyle(Paint.Style.STROKE);
            paint.setAntiAlias(true);
            
            // 根据辅助线类型绘制
            switch (currentLineType) {
                case FORWARD:
                    drawForwardLines(canvas, paint);
                    break;
                case REVERSE:
                    drawReverseLines(canvas, paint);
                    break;
            }
            
        } catch (Exception e) {
            Log.e(TAG, "绘制辅助线失败", e);
        }
    }
    
    /**
     * 绘制前进辅助线（合并的直走、左转、右转）
     */
    private void drawForwardLines(Canvas canvas, Paint paint) {
        int width = getWidth();
        int height = getHeight();
        float centerX = width * 0.5f;
        float bottomY = height;
        
        // 根据转向角度调整弧线
        float turnFactor = currentTurnAngle / MAX_TURN_ANGLE * -1; // -1.0 到 1.0
        
        // 绘制左侧轨迹线
        Path leftPath = new Path();
        float leftStartX = centerX - width * 0.4f;
        float leftControlX = centerX - width * (0.2f + Math.abs(turnFactor) * 0.1f);
        float leftControlY = height * 0.5f;
        float leftEndX = centerX + width * turnFactor * 0.3f - width * 0.1f;
        float leftEndY = height * 0.1f;
        
        leftPath.moveTo(leftStartX, bottomY);
        leftPath.quadTo(leftControlX, leftControlY, leftEndX, leftEndY);
        canvas.drawPath(leftPath, paint);
        
        // 绘制右侧轨迹线
        Path rightPath = new Path();
        float rightStartX = centerX + width * 0.4f;
        float rightControlX = centerX + width * (0.2f + Math.abs(turnFactor) * 0.1f);
        float rightControlY = height * 0.5f;
        float rightEndX = centerX + width * turnFactor * 0.3f + width * 0.1f;
        float rightEndY = height * 0.1f;
        
        rightPath.moveTo(rightStartX, bottomY);
        rightPath.quadTo(rightControlX, rightControlY, rightEndX, rightEndY);
        canvas.drawPath(rightPath, paint);
        
        // 绘制中心参考线
        Path centerPath = new Path();
        float centerControlX = centerX + width * turnFactor * 0.15f;
        float centerEndX = centerX + width * turnFactor * 0.3f;
        
        centerPath.moveTo(centerX, bottomY);
        centerPath.quadTo(centerControlX, height * 0.5f, centerEndX, height * 0.1f);
        canvas.drawPath(centerPath, paint);
        
        // 绘制转向箭头
        if (Math.abs(turnFactor) > 0.1f) {
            float arrowX = centerX + width * turnFactor * 0.26f;
            float arrowY = height * 0.2f;
            float arrowAngle = turnFactor > 0 ? -45 : -135; // 右转或左转
            drawArrow(canvas, paint, arrowX, arrowY, arrowAngle);
        } else {
            // 直行箭头
            drawArrow(canvas, paint, centerX, height * 0.2f, -90);
        }
        
        // 绘制车辆轮廓参考线
        drawVehicleOutline(canvas, paint, centerX, bottomY);
    }
    
    /**
     * 绘制倒车辅助线（透视梯形，根据转向调整）
     */
    private void drawReverseLines(Canvas canvas, Paint paint) {
        int width = getWidth();
        int height = getHeight();
        float centerX = width * 0.5f;
        
        // 根据转向角度调整梯形倾斜度
        float turnFactor = currentTurnAngle / MAX_TURN_ANGLE; // -1.0 到 1.0
        float tiltFactor = turnFactor * 0.3f; // 倾斜因子
        
        // 根据距离动态调整辅助线范围
        float distanceFactor = calculateDistanceFactor();
        
        // 梯形的底边（画面底部，较宽）- 宽度保持不变
        float bottomY = height;
        float baseBottomWidth = width * 0.4f; // 宽度不受距离影响
        float bottomLeftX = centerX - baseBottomWidth;
        float bottomRightX = centerX + baseBottomWidth;
        
        // 梯形的顶边（画面中上部，较窄）- 只调整高度
        float topY = height * (0.3f + (1 - distanceFactor) * 0.3f); // 距离近时，顶边更靠下
        float baseTopWidth = width * 0.1f; // 顶部宽度也保持不变
        float topLeftX = centerX - baseTopWidth + (width * tiltFactor);
        float topRightX = centerX + baseTopWidth + (width * tiltFactor);
        
        // 绘制梯形边线
        canvas.drawLine(bottomLeftX, bottomY, topLeftX, topY, paint); // 左边
        canvas.drawLine(bottomRightX, bottomY, topRightX, topY, paint); // 右边
        canvas.drawLine(topLeftX, topY, topRightX, topY, paint); // 顶边
        
        // 距离标记线已移除 - 只保留基本辅助线框架
        
        // 绘制中心线（根据倾斜调整）
        float centerTopX = centerX + (width * tiltFactor);
        canvas.drawLine(centerX, bottomY, centerTopX, topY, paint);
        
        // 绘制倒车方向箭头（位置只根据高度调整）
        float arrowY = height * (0.4f + (1 - distanceFactor) * 0.2f); // 距离近时，箭头更靠下
        float arrowX = centerX + (width * tiltFactor * 0.83f); // 箭头位置跟随倾斜
        float arrowAngle = -90 + (turnFactor * 30); // 箭头角度跟随倾斜
        drawArrow(canvas, paint, arrowX, arrowY, arrowAngle);
        
        // 绘制距离信息（如果启用了距离检测）
        if (isDistanceDetectionEnabled && lastDetectionResult != null) {
            drawDistanceInfo(canvas, paint, width, height, centerX, tiltFactor);
        }
        
        // 绘制车辆轮廓参考线
        drawVehicleOutline(canvas, paint, centerX, bottomY);
    }
    
    /**
     * 绘制箭头
     */
    private void drawArrow(Canvas canvas, Paint paint, float x, float y, float angle) {
        float arrowSize = 30;
        
        // 计算箭头的三个点
        double radians = Math.toRadians(angle);
        
        // 箭头顶点
        float tipX = x;
        float tipY = y;
        
        // 箭头左翼
        float leftX = (float) (tipX - arrowSize * Math.cos(radians - Math.PI / 6));
        float leftY = (float) (tipY - arrowSize * Math.sin(radians - Math.PI / 6));
        
        // 箭头右翼
        float rightX = (float) (tipX - arrowSize * Math.cos(radians + Math.PI / 6));
        float rightY = (float) (tipY - arrowSize * Math.sin(radians + Math.PI / 6));
        
        // 绘制箭头
        canvas.drawLine(tipX, tipY, leftX, leftY, paint);
        canvas.drawLine(tipX, tipY, rightX, rightY, paint);
    }
    
    /**
     * 根据距离计算辅助线高度调整因子
     * @return 高度因子 (0.3f - 1.0f)，距离近时值小（辅助线高度较短），距离远时值大（辅助线高度较长）
     */
    private float calculateDistanceFactor() {
        if (!isDistanceDetectionEnabled || lastDetectionResult == null) {
            return 1.0f; // 默认最大范围
        }
        
        double minDistance = lastDetectionResult.minDistance;
        if (minDistance == Double.MAX_VALUE) {
            return 1.0f; // 无障碍物，使用最大范围
        }
        
        // 距离范围映射：0.5m - 3.0m
        float minRange = 0.5f;  // 最小距离
        float maxRange = 3.0f;  // 最大距离
        float minFactor = 0.3f; // 最小范围因子
        float maxFactor = 1.0f; // 最大范围因子
        
        // 限制距离范围
        float distance = Math.max(minRange, Math.min(maxRange, (float) minDistance));
        
        // 计算范围因子（距离越小，因子越小）
        float factor = minFactor + (maxFactor - minFactor) * (distance - minRange) / (maxRange - minRange);
        
        Log.d(TAG, String.format("距离: %.2fm, 高度因子: %.2f", minDistance, factor));
        
        return factor;
    }

    /**
     * 绘制距离信息
     */
    private void drawDistanceInfo(Canvas canvas, Paint paint, int width, int height, float centerX, float tiltFactor) {
        if (lastDetectionResult == null || lastDetectionResult.obstacles.isEmpty()) {
            return;
        }
        
        // 创建距离信息专用画笔
        Paint distancePaint = new Paint(paint);
        distancePaint.setTextSize(36);
        distancePaint.setAntiAlias(true);
        distancePaint.setStyle(Paint.Style.FILL);
        
        // 创建背景画笔
        Paint backgroundPaint = new Paint();
        backgroundPaint.setColor(Color.argb(180, 0, 0, 0));
        backgroundPaint.setStyle(Paint.Style.FILL);
        
        // 显示最近障碍物距离
        if (lastDetectionResult.minDistance != Double.MAX_VALUE) {
            String distanceText = String.format("距离: %.1fm", lastDetectionResult.minDistance);
            
            // 根据距离设置颜色
            int textColor = getDistanceColor(lastDetectionResult.minDistance);
            distancePaint.setColor(textColor);
            
            // 计算文本位置
            float textWidth = distancePaint.measureText(distanceText);
            float textX = centerX - textWidth / 2;
            float textY = height * 0.15f;
            
            // 绘制背景
            canvas.drawRect(textX - 20, textY - 40, textX + textWidth + 20, textY + 10, backgroundPaint);
            
            // 绘制文本
            canvas.drawText(distanceText, textX, textY, distancePaint);
        }
        
        // 障碍物标记已移除 - 只显示距离数值
    }
    
    /**
     * 绘制危险等级指示器
     */
    private void drawDangerIndicator(Canvas canvas, int width, int height, DistanceDetector.DistanceZone zone) {
        Paint indicatorPaint = new Paint();
        indicatorPaint.setStyle(Paint.Style.FILL);
        indicatorPaint.setAlpha(150);
        
        // 根据危险等级设置颜色
        switch (zone) {
            case CRITICAL:
                indicatorPaint.setColor(Color.RED);
                // 闪烁效果
                long currentTime = System.currentTimeMillis();
                if ((currentTime / 200) % 2 == 0) {
                    indicatorPaint.setAlpha(255);
                }
                break;
            case DANGER:
                indicatorPaint.setColor(Color.RED);
                break;
            case CAUTION:
                indicatorPaint.setColor(Color.YELLOW);
                break;
            case SAFE:
            default:
                indicatorPaint.setColor(Color.GREEN);
                break;
        }
        
        // 绘制指示器条
        float indicatorHeight = height * 0.02f;
        float indicatorY = height * 0.05f;
        canvas.drawRect(0, indicatorY, width, indicatorY + indicatorHeight, indicatorPaint);
    }
    
    // 绘制障碍物标记的方法已移除 - 简化显示，只保留距离数值
    
    /**
     * 根据距离获取颜色
     */
    private int getDistanceColor(double distance) {
        if (distance < 0.5) {
            return Color.RED;
        } else if (distance < 1.0) {
            return Color.rgb(255, 165, 0); // 橙色
        } else if (distance < 2.0) {
            return Color.YELLOW;
        } else {
            return Color.GREEN;
        }
    }
    
    /**
     * 绘制车辆轮廓参考线
     */
    private void drawVehicleOutline(Canvas canvas, Paint paint, float centerX, float bottomY) {
        int width = getWidth();
        
        // 创建半透明画笔
        Paint outlinePaint = new Paint(paint);
        outlinePaint.setAlpha(LINE_ALPHA / 3);
        outlinePaint.setStrokeWidth(LINE_WIDTH / 2);
        
        // 车辆轮廓（简化的矩形）
        float vehicleWidth = width * 0.16f; // 车辆宽度
        float vehicleLength = width * 0.08f; // 车辆长度（在画面中的显示）
        
        float leftX = centerX - vehicleWidth / 2;
        float rightX = centerX + vehicleWidth / 2;
        float frontY = bottomY - vehicleLength;
        
        // 绘制车辆轮廓
        canvas.drawLine(leftX, bottomY, rightX, bottomY, outlinePaint); // 后部
        canvas.drawLine(leftX, frontY, rightX, frontY, outlinePaint); // 前部
        canvas.drawLine(leftX, bottomY, leftX, frontY, outlinePaint); // 左侧
        canvas.drawLine(rightX, bottomY, rightX, frontY, outlinePaint); // 右侧
    }
    
    /**
     * 启用距离检测
     */
    public void enableDistanceDetection() {
        isDistanceDetectionEnabled = true;
        Log.d(TAG, "距离检测已启用");
    }
    
    /**
     * 禁用距离检测
     */
    public void disableDistanceDetection() {
        isDistanceDetectionEnabled = false;
        lastDetectionResult = null;
        Log.d(TAG, "距离检测已禁用");
    }
    
    /**
     * 更新距离检测结果
     */
    public void updateDistanceDetectionResult(DistanceDetector.DetectionResult result) {
        if (isDistanceDetectionEnabled) {
            lastDetectionResult = result;
            // 在主线程中重绘
            post(this::invalidate);
        }
    }
    
    /**
     * 获取当前距离检测状态
     */
    public boolean isDistanceDetectionEnabled() {
        return isDistanceDetectionEnabled;
    }
    
    /**
     * 获取最后的检测结果
     */
    public DistanceDetector.DetectionResult getLastDetectionResult() {
        return lastDetectionResult;
    }
    
    /**
     * 清理资源
     */
    public void cleanup() {
        disableSensors();
        disableDistanceDetection();
        if (colorHandler != null) {
            colorHandler.removeCallbacksAndMessages(null);
        }
        if (distanceHandler != null) {
            distanceHandler.removeCallbacksAndMessages(null);
        }
        Log.d(TAG, "辅助线覆盖层已清理");
    }
    
    /**
     * 获取当前转向角度
     */
    public float getCurrentTurnAngle() {
        return currentTurnAngle;
    }
    
    /**
     * 获取当前颜色
     */
    public int getCurrentColor() {
        return currentColor;
    }
    
    /**
     * 获取传感器状态
     */
    public boolean isSensorEnabled() {
        return isSensorEnabled;
    }
    
    /**
     * 获取自动传感器模式状态
     */
    public boolean isAutoSensorMode() {
        return isAutoSensorMode;
    }
    
    /**
     * 手动设置辅助线颜色（用于外部控制）
     */
    public void setAssistLineColor(int color) {
        if (currentColor != color) {
            currentColor = color;
            Log.d(TAG, String.format("手动设置辅助线颜色: #%06X", (0xFFFFFF & color)));
            post(this::invalidate); // 在主线程中重绘
        }
    }
    
    /**
     * 设置辅助线颜色进度（0-1之间，0=绿色，1=红色）
     */
    public void setColorProgress(float progress) {
        progress = Math.max(0f, Math.min(1f, progress));
        int newColor = interpolateColor(COLOR_GREEN, COLOR_RED, progress);
        setAssistLineColor(newColor);
    }
    
    /**
     * 重置辅助线颜色为绿色
     */
    public void resetToGreenColor() {
        setAssistLineColor(COLOR_GREEN);
    }
} 