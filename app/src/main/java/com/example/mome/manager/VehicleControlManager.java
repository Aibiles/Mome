package com.example.mome.manager;

import android.util.Log;

/**
 * 车辆控制管理器
 * 负责管理车辆运行方向和摄像头切换逻辑
 */
public class VehicleControlManager {
    
    private static final String TAG = "VehicleControlManager";
    
    // 车辆运行方向
    public enum Direction {
        STOP,     // 停止
        FORWARD,  // 前进
        BACKWARD, // 后退
        LEFT,     // 左转
        RIGHT     // 右转
    }
    
    // 摄像头位置（对应Camera360Config中的定义）
    public enum CameraPosition {
        FRONT(0),   // 前置摄像头
        RIGHT(1),   // 右侧摄像头
        REAR(2),    // 后置摄像头
        LEFT(3);    // 左侧摄像头
        
        private final int value;
        
        CameraPosition(int value) {
            this.value = value;
        }
        
        public int getValue() {
            return value;
        }
    }
    
    private Direction currentDirection = Direction.STOP;
    private CameraPosition currentCamera = CameraPosition.FRONT;
    private VehicleControlListener listener;
    private boolean isMoving = false;
    
    /**
     * 车辆控制监听器
     */
    public interface VehicleControlListener {
        void onDirectionChanged(Direction direction, CameraPosition cameraPosition);
        void onMovementStateChanged(boolean isMoving);
        void onReverseStateChanged(boolean isReversing);
    }
    
    public VehicleControlManager() {
        Log.d(TAG, "VehicleControlManager 初始化完成");
    }
    
    /**
     * 设置控制监听器
     */
    public void setControlListener(VehicleControlListener listener) {
        this.listener = listener;
    }
    
    /**
     * 设置车辆运行方向
     */
    public void setDirection(Direction direction) {
        Direction previousDirection = this.currentDirection;
        this.currentDirection = direction;
        
        // 根据方向切换摄像头
        CameraPosition targetCamera = getCameraForDirection(direction);
        if (targetCamera != currentCamera) {
            currentCamera = targetCamera;
        }
        
        // 更新移动状态
        boolean wasMoving = isMoving;
        isMoving = (direction != Direction.STOP);
        
        Log.d(TAG, "方向改变: " + previousDirection + " -> " + direction + 
                   ", 摄像头: " + currentCamera + ", 移动状态: " + isMoving);
        
        // 通知监听器
        if (listener != null) {
            listener.onDirectionChanged(direction, currentCamera);
            
            if (wasMoving != isMoving) {
                listener.onMovementStateChanged(isMoving);
            }
            
            // 如果是前方状态改变，特别通知
            boolean isReversing = (direction == Direction.FORWARD);
            boolean wasReversing = (previousDirection == Direction.FORWARD);
            if (isReversing != wasReversing) {
                listener.onReverseStateChanged(isReversing);
            }
        }
    }
    
    /**
     * 获取当前运行方向
     */
    public Direction getCurrentDirection() {
        return currentDirection;
    }
    
    /**
     * 获取当前摄像头位置
     */
    public CameraPosition getCurrentCamera() {
        return currentCamera;
    }
    
    /**
     * 是否正在移动
     */
    public boolean isMoving() {
        return isMoving;
    }
    
    /**
     * 是否正在倒车
     */
    public boolean isReversing() {
        return currentDirection == Direction.BACKWARD;
    }
    
    /**
     * 停止车辆
     */
    public void stop() {
        setDirection(Direction.STOP);
    }
    
    /**
     * 前进
     */
    public void moveForward() {
        setDirection(Direction.FORWARD);
    }
    
    /**
     * 后退
     */
    public void moveBackward() {
        setDirection(Direction.BACKWARD);
    }
    
    /**
     * 左转
     */
    public void turnLeft() {
        setDirection(Direction.LEFT);
    }
    
    /**
     * 右转
     */
    public void turnRight() {
        setDirection(Direction.RIGHT);
    }
    
    /**
     * 根据运行方向获取对应的摄像头
     */
    private CameraPosition getCameraForDirection(Direction direction) {
        switch (direction) {
            case FORWARD:
                return CameraPosition.FRONT;
            case BACKWARD:
                return CameraPosition.REAR;
            case LEFT:
                return CameraPosition.LEFT;
            case RIGHT:
                return CameraPosition.RIGHT;
            case STOP:
            default:
                return CameraPosition.FRONT; // 默认显示前方摄像头
        }
    }
    
    /**
     * 获取方向的中文名称
     */
    public static String getDirectionName(Direction direction) {
        switch (direction) {
            case FORWARD:
            case LEFT:
            case RIGHT:
                return "D";
            case BACKWARD: return "R";
            case STOP:
            default: return "P";
        }
    }
    
    /**
     * 获取摄像头位置的中文名称
     */
    public static String getCameraName(CameraPosition position) {
        switch (position) {
            case FRONT: return "前方摄像头";
            case REAR: return "后方摄像头";
            case LEFT: return "左侧摄像头";
            case RIGHT: return "右侧摄像头";
            default: return "未知摄像头";
        }
    }
    
    /**
     * 获取当前状态描述
     */
    public String getCurrentStatusDescription() {
        return getDirectionName(currentDirection) + " - " + getCameraName(currentCamera);
    }
} 