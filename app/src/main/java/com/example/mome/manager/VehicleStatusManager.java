package com.example.mome.manager;

import android.util.Log;

import java.util.ArrayList;
import java.util.List;

/**
 * 车辆状态管理器
 * 用于在不同Activity之间共享踏板状态信息（速度、电量等）
 */
public class VehicleStatusManager {
    
    private static final String TAG = "VehicleStatusManager";
    private static VehicleStatusManager instance;
    
    private int currentSpeed = 0;           // 当前速度 (0-200)
    private int currentBattery = 100;       // 当前电量 (0-100)
    private float pedalProgress = 0f;       // 踏板进度 (0.0-1.0)
    private boolean isPedalPressed = false; // 踏板是否被按下
    
    private List<VehicleStatusListener> listeners = new ArrayList<>();
    
    /**
     * 车辆状态监听器
     */
    public interface VehicleStatusListener {
        /**
         * 踏板状态改变回调
         * @param speed 当前速度 (0-200)
         * @param battery 当前电量 (0-100)
         * @param pedalProgress 踏板进度 (0.0-1.0)
         * @param isPedalPressed 踏板是否被按下
         */
        void onVehicleStatusChanged(int speed, int battery, float pedalProgress, boolean isPedalPressed);
    }
    
    private VehicleStatusManager() {
        Log.d(TAG, "VehicleStatusManager 初始化");
    }
    
    public static synchronized VehicleStatusManager getInstance() {
        if (instance == null) {
            instance = new VehicleStatusManager();
        }
        return instance;
    }
    
    /**
     * 添加状态监听器
     */
    public void addListener(VehicleStatusListener listener) {
        if (!listeners.contains(listener)) {
            listeners.add(listener);
            // 立即通知当前状态
            listener.onVehicleStatusChanged(currentSpeed, currentBattery, pedalProgress, isPedalPressed);
            Log.d(TAG, "添加踏板状态监听器，当前监听器数量: " + listeners.size());
        }
    }
    
    /**
     * 移除状态监听器
     */
    public void removeListener(VehicleStatusListener listener) {
        listeners.remove(listener);
        Log.d(TAG, "移除踏板状态监听器，当前监听器数量: " + listeners.size());
    }
    
    /**
     * 更新车辆状态
     */
    public void updateVehicleStatus(int speed, int battery, float pedalProgress, boolean isPedalPressed) {
        boolean changed = false;
        
        if (this.currentSpeed != speed) {
            this.currentSpeed = speed;
            changed = true;
        }
        
        if (this.currentBattery != battery) {
            this.currentBattery = battery;
            changed = true;
        }
        
        if (Math.abs(this.pedalProgress - pedalProgress) > 0.01f) {
            this.pedalProgress = pedalProgress;
            changed = true;
        }
        
        if (this.isPedalPressed != isPedalPressed) {
            this.isPedalPressed = isPedalPressed;
            changed = true;
        }
        
        // 只在状态有变化时通知监听器
        if (changed) {
            Log.d(TAG, String.format("车辆状态更新: 速度=%d, 电量=%d, 踏板进度=%.2f, 是否按下=%s", 
                    speed, battery, pedalProgress, isPedalPressed));
            
            // 通知所有监听器
            for (VehicleStatusListener listener : listeners) {
                try {
                    listener.onVehicleStatusChanged(speed, battery, pedalProgress, isPedalPressed);
                } catch (Exception e) {
                    Log.e(TAG, "通知踏板状态监听器失败", e);
                }
            }
        }
    }
    
    /**
     * 更新速度
     */
    public void updateSpeed(int speed) {
        updateVehicleStatus(speed, currentBattery, pedalProgress, isPedalPressed);
    }
    
    /**
     * 更新电量
     */
    public void updateBattery(int battery) {
        updateVehicleStatus(currentSpeed, battery, pedalProgress, isPedalPressed);
    }
    
    /**
     * 更新踏板进度
     */
    public void updatePedalProgress(float progress) {
        updateVehicleStatus(currentSpeed, currentBattery, progress, isPedalPressed);
    }
    
    /**
     * 设置踏板按下状态
     */
    public void setPedalPressed(boolean pressed) {
        updateVehicleStatus(currentSpeed, currentBattery, pedalProgress, pressed);
    }
    
    /**
     * 获取当前速度
     */
    public int getCurrentSpeed() {
        return currentSpeed;
    }
    
    /**
     * 获取当前电量
     */
    public int getCurrentBattery() {
        return currentBattery;
    }
    
    /**
     * 获取当前踏板进度
     */
    public float getPedalProgress() {
        return pedalProgress;
    }
    
    /**
     * 获取踏板是否被按下
     */
    public boolean isPedalPressed() {
        return isPedalPressed;
    }
    
    /**
     * 重置到默认状态
     */
    public void reset() {
        updateVehicleStatus(0, 100, 0f, false);
        Log.d(TAG, "车辆状态已重置");
    }
    
    /**
     * 获取当前状态描述
     */
    public String getCurrentStatusDescription() {
        return String.format("速度: %d km/h, 电量: %d%%, 踏板进度: %.1f%%, 按下状态: %s",
                currentSpeed, currentBattery, pedalProgress * 100, isPedalPressed ? "是" : "否");
    }
} 