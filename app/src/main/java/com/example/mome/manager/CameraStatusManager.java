package com.example.mome.manager;

import android.util.Log;

import java.util.ArrayList;
import java.util.List;

/**
 * 摄像头状态管理器
 * 用于在不同Activity之间共享摄像头状态信息
 */
public class CameraStatusManager {
    
    private static final String TAG = "CameraStatusManager";
    private static CameraStatusManager instance;
    
    private VehicleControlManager.Direction currentDirection = VehicleControlManager.Direction.STOP;
    private VehicleControlManager.CameraPosition currentCameraPosition = VehicleControlManager.CameraPosition.FRONT;
    private List<CameraStatusListener> listeners = new ArrayList<>();
    
    /**
     * 摄像头状态监听器
     */
    public interface CameraStatusListener {
        void onCameraStatusChanged(VehicleControlManager.Direction direction, 
                                   VehicleControlManager.CameraPosition cameraPosition);
    }
    
    private CameraStatusManager() {
        Log.d(TAG, "CameraStatusManager 初始化");
    }
    
    public static synchronized CameraStatusManager getInstance() {
        if (instance == null) {
            instance = new CameraStatusManager();
        }
        return instance;
    }
    
    /**
     * 添加状态监听器
     */
    public void addListener(CameraStatusListener listener) {
        if (!listeners.contains(listener)) {
            listeners.add(listener);
            // 立即通知当前状态
            listener.onCameraStatusChanged(currentDirection, currentCameraPosition);
            Log.d(TAG, "添加监听器，当前监听器数量: " + listeners.size());
        }
    }
    
    /**
     * 移除状态监听器
     */
    public void removeListener(CameraStatusListener listener) {
        listeners.remove(listener);
        Log.d(TAG, "移除监听器，当前监听器数量: " + listeners.size());
    }
    
    /**
     * 更新摄像头状态
     */
    public void updateCameraStatus(VehicleControlManager.Direction direction, 
                                   VehicleControlManager.CameraPosition cameraPosition) {
        this.currentDirection = direction;
        this.currentCameraPosition = cameraPosition;
        
        Log.d(TAG, "摄像头状态更新: " + VehicleControlManager.getDirectionName(direction) + 
                   " - " + VehicleControlManager.getCameraName(cameraPosition));
        
        // 通知所有监听器
        for (CameraStatusListener listener : listeners) {
            try {
                listener.onCameraStatusChanged(direction, cameraPosition);
            } catch (Exception e) {
                Log.e(TAG, "通知监听器失败", e);
            }
        }
    }
    
    /**
     * 获取当前方向
     */
    public VehicleControlManager.Direction getCurrentDirection() {
        return currentDirection;
    }
    
    /**
     * 获取当前摄像头位置
     */
    public VehicleControlManager.CameraPosition getCurrentCameraPosition() {
        return currentCameraPosition;
    }
    
    /**
     * 获取当前状态描述
     */
    public String getCurrentStatusDescription() {
        return VehicleControlManager.getDirectionName(currentDirection) + " - " + 
               VehicleControlManager.getCameraName(currentCameraPosition);
    }
} 