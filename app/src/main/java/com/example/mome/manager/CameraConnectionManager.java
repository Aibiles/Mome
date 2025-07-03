package com.example.mome.manager;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.example.mome.config.Camera360Config;
import com.example.mome.opencv.TrapezoidTransform;
import com.github.niqdev.mjpeg.DisplayMode;
import com.github.niqdev.mjpeg.Mjpeg;
import com.github.niqdev.mjpeg.MjpegView;
import com.github.niqdev.mjpeg.OnFrameCapturedListener;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 摄像头连接管理器
 * 负责管理所有摄像头的连接状态和自动重连
 */
public class CameraConnectionManager {
    
    private static final String TAG = "CameraConnectionManager";
    private static final int RECONNECT_INTERVAL = 5000; // 5秒重连间隔
    private static final int CONNECTION_TIMEOUT = Camera360Config.CONNECTION_TIMEOUT;
    private final Activity activity;

    private final Context context;
    private final Handler reconnectHandler;
    private final Map<Integer, CameraConnection> cameraConnections;
    private final Map<Integer, AtomicBoolean> connectionStates;
    private ConnectionStatusListener statusListener;
    
    /**
     * 连接状态监听器
     */
    public interface ConnectionStatusListener {
        void onCameraConnected(int cameraIndex);
        void onCameraDisconnected(int cameraIndex);
        void onFrameCaptured(int cameraIndex, Bitmap bitmap);
        void onConnectionError(int cameraIndex, String error);
    }
    
    /**
     * 摄像头连接信息
     */
    private static class CameraConnection {
        MjpegView mjpegView;
        ImageView imageView;
        String url;
        AtomicBoolean isConnecting;
        long lastFrameTime;
        Runnable reconnectTask;

        CameraConnection(MjpegView view, String url, ImageView imageView) {
            this.mjpegView = view;
            this.url = url;
            this.isConnecting = new AtomicBoolean(false);
            this.lastFrameTime = System.currentTimeMillis();
            this.imageView = imageView;
        }
    }
    
    public CameraConnectionManager(Context context, Activity activity) {
        this.context = context;
        this.reconnectHandler = new Handler(Looper.getMainLooper());
        this.cameraConnections = new HashMap<>();
        this.connectionStates = new HashMap<>();
        this.activity = activity;

        // 初始化连接状态
        for (int i = 0; i < 4; i++) {
            connectionStates.put(i, new AtomicBoolean(false));
        }
    }
    
    /**
     * 设置连接状态监听器
     */
    public void setStatusListener(ConnectionStatusListener listener) {
        this.statusListener = listener;
    }
    
    /**
     * 添加摄像头连接
     */
    public void addCamera(int cameraIndex, MjpegView mjpegView, ImageView imageView) {
        String url = Camera360Config.getCameraUrl(cameraIndex);
        CameraConnection connection = new CameraConnection(mjpegView, url, imageView);
        cameraConnections.put(cameraIndex, connection);
        
        // 配置MjpegView
//        mjpegView.setRotate(Camera360Config.getCameraRotation(cameraIndex));
        
        Log.d(TAG, "添加摄像头: " + Camera360Config.getCameraName(cameraIndex));
    }
    
    /**
     * 连接指定摄像头
     */
    public void connectCamera(int cameraIndex) {
        CameraConnection connection = cameraConnections.get(cameraIndex);
        if (connection == null) {
            Log.e(TAG, "未找到摄像头连接: " + cameraIndex);
            return;
        }
        
        if (connection.isConnecting.get()) {
            Log.d(TAG, "摄像头正在连接中: " + Camera360Config.getCameraName(cameraIndex));
            return;
        }
        
        connection.isConnecting.set(true);
        Log.d(TAG, "开始连接摄像头: " + Camera360Config.getCameraName(cameraIndex));
        
        Mjpeg.newInstance().open(connection.url, CONNECTION_TIMEOUT).subscribe(
            inputStream -> {
                Log.d(TAG, "摄像头连接成功: " + Camera360Config.getCameraName(cameraIndex));
                connection.mjpegView.setSource(inputStream);
                connection.mjpegView.setDisplayMode(DisplayMode.FULLSCREEN);
                connection.mjpegView.showFps(false);

                // 设置帧捕获监听器
                connection.mjpegView.setOnFrameCapturedListener(new OnFrameCapturedListener() {

                    @Override
                    public void onFrameCaptured(@NonNull Bitmap bitmap) {
                        connection.lastFrameTime = System.currentTimeMillis();

                        // 更新连接状态
                        if (!Objects.requireNonNull(connectionStates.get(cameraIndex)).get()) {
                            Objects.requireNonNull(connectionStates.get(cameraIndex)).set(true);
                            if (statusListener != null) {
                                statusListener.onCameraConnected(cameraIndex);
                            }
                        }

                        try {
                            // 使用TrapezoidTransform工具类进行变换
                            TrapezoidTransform.TrapezoidDirection direction =
                                    TrapezoidTransform.getCameraTransformDirection(cameraIndex);
                            double shrinkRatio = TrapezoidTransform.getDefaultShrinkRatio(cameraIndex);

                            if (statusListener != null) {
                                statusListener.onFrameCaptured(cameraIndex, bitmap);
                            }


                            Bitmap transformedBitmap = TrapezoidTransform.applyTrapezoidTransform(
                                    bitmap, direction, shrinkRatio, cameraIndex);

                            // 在主线程更新UI
                            activity.runOnUiThread(() -> {
                                if (transformedBitmap != null) {
                                    connection.imageView.setImageBitmap(transformedBitmap);
                                }
                            });

                        } catch (Exception e) {
                            Log.e(TAG, "梯形变换失败: " + Camera360Config.getCameraName(cameraIndex), e);
                        }
                    }

                    @Override
                    public void onFrameCapturedWithHeader(@NonNull byte[] bytes, @NonNull byte[] bytes1) {
                        // 不使用
                    }
                });

                connection.isConnecting.set(false);
                startHeartbeatCheck(cameraIndex);
            },
            throwable -> {
                Log.e(TAG, "摄像头连接失败: " + Camera360Config.getCameraName(cameraIndex), throwable);
                connection.isConnecting.set(false);

                // 更新连接状态
                Objects.requireNonNull(connectionStates.get(cameraIndex)).set(false);
                if (statusListener != null) {
                    statusListener.onCameraDisconnected(cameraIndex);
                    statusListener.onConnectionError(cameraIndex, throwable.getMessage());
                }

                // 显示错误提示
                if (context instanceof android.app.Activity) {
                    ((android.app.Activity) context).runOnUiThread(() -> {
                        Toast.makeText(context,
                            Camera360Config.getCameraName(cameraIndex) + " 连接失败",
                            Toast.LENGTH_SHORT).show();
                    });
                }

                // 启动重连
                scheduleReconnect(cameraIndex);
            });
    }
    
    /**
     * 启动心跳检测
     */
    private void startHeartbeatCheck(int cameraIndex) {
        CameraConnection connection = cameraConnections.get(cameraIndex);
        if (connection == null) return;
        
        // 清除之前的重连任务
        if (connection.reconnectTask != null) {
            reconnectHandler.removeCallbacks(connection.reconnectTask);
        }
        
        // 创建心跳检测任务
        connection.reconnectTask = new Runnable() {
            @Override
            public void run() {
                long currentTime = System.currentTimeMillis();
                long timeSinceLastFrame = currentTime - connection.lastFrameTime;
                
                // 如果超过5秒没有收到帧，认为连接断开
                if (timeSinceLastFrame > 5000) {
                    Log.w(TAG, "摄像头心跳检测失败: " + Camera360Config.getCameraName(cameraIndex));
                    
                    // 更新连接状态
                    Objects.requireNonNull(connectionStates.get(cameraIndex)).set(false);
                    if (statusListener != null) {
                        statusListener.onCameraDisconnected(cameraIndex);
                    }
                    
                    // 断开并重连
                    disconnectCamera(cameraIndex);
                    scheduleReconnect(cameraIndex);
                } else {
                    // 继续下一次心跳检测
                    reconnectHandler.postDelayed(this, 5000);
                }
            }
        };
        
        // 启动心跳检测
        reconnectHandler.postDelayed(connection.reconnectTask, 5000);
    }
    
    /**
     * 安排重连任务
     */
    private void scheduleReconnect(int cameraIndex) {
        CameraConnection connection = cameraConnections.get(cameraIndex);
        if (connection == null) return;
        
        Log.d(TAG, "安排重连任务: " + Camera360Config.getCameraName(cameraIndex));
        
        connection.reconnectTask = () -> {
            Log.d(TAG, "执行重连任务: " + Camera360Config.getCameraName(cameraIndex));
            connectCamera(cameraIndex);
        };
        
        reconnectHandler.postDelayed(connection.reconnectTask, RECONNECT_INTERVAL);
    }
    
    /**
     * 断开指定摄像头
     */
    public void disconnectCamera(int cameraIndex) {
        CameraConnection connection = cameraConnections.get(cameraIndex);
        if (connection == null) return;
        
        Log.d(TAG, "断开摄像头连接: " + Camera360Config.getCameraName(cameraIndex));
        
        // 停止视频流
        try {
            connection.mjpegView.stopPlayback();
        } catch (Exception e) {
            Log.e(TAG, "停止视频流失败", e);
        }
        
        // 清除重连任务
        if (connection.reconnectTask != null) {
            reconnectHandler.removeCallbacks(connection.reconnectTask);
            connection.reconnectTask = null;
        }
        
        // 更新连接状态
        Objects.requireNonNull(connectionStates.get(cameraIndex)).set(false);
        connection.isConnecting.set(false);
    }
    
    /**
     * 连接所有摄像头
     */
    public void connectAllCameras() {
        Log.d(TAG, "连接所有摄像头");
        for (int i = 0; i < 4; i++) {
            if (cameraConnections.containsKey(i)) {
                connectCamera(i);
            }
        }
    }
    
    /**
     * 断开所有摄像头
     */
    public void disconnectAllCameras() {
        Log.d(TAG, "断开所有摄像头");
        for (int i = 0; i < 4; i++) {
            disconnectCamera(i);
        }
    }

    /**
     * 获取摄像头连接状态
     */
    public boolean isCameraConnected(int cameraIndex) {
        AtomicBoolean state = connectionStates.get(cameraIndex);
        return state != null && state.get();
    }

    /**
     * 获取连接状态统计
     */
    @SuppressLint("DefaultLocale")
    public String getConnectionStatus() {
        int connectedCount = 0;
        int totalCount = cameraConnections.size();

        for (AtomicBoolean state : connectionStates.values()) {
            if (state.get()) {
                connectedCount++;
            }
        }

        return String.format("已连接: %d/%d", connectedCount, totalCount);
    }
    
    /**
     * 清理资源
     */
    public void cleanup() {
        Log.d(TAG, "清理连接管理器资源");
        disconnectAllCameras();
        
        // 清理Handler
        if (reconnectHandler != null) {
            reconnectHandler.removeCallbacksAndMessages(null);
        }
        
        cameraConnections.clear();
        connectionStates.clear();
    }
} 