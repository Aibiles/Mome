package com.example.mome.config;

public class Camera360Config {
    
    // 摄像头连接配置
    public static final String[] CAMERA_URLS = {
        "http://192.168.43.175:7777/video.mjpg", // 顶部摄像头
        "http://10.18.156.50:7775/video.mjpg", // 右侧摄像头  
        "http://10.18.156.50:7778/video.mjpg", // 底部摄像头
        "http://10.18.156.50:7776/video.mjpg"  // 左侧摄像头
    };

    public static String CAMERA_FACE = "http://192.168.43.175:7779/video.mjpg";
    
    // 摄像头位置标识
    public static final int CAMERA_TOP = 0;
    public static final int CAMERA_RIGHT = 1;
    public static final int CAMERA_BOTTOM = 2;
    public static final int CAMERA_LEFT = 3;
    
    // 摄像头旋转角度
    public static final int[] CAMERA_ROTATIONS = {0, 90, 180, 270};

    
    // 输出图像尺寸
    public static final int OUTPUT_WIDTH = 1024;
    public static final int OUTPUT_HEIGHT = 512;
    
    // 去畸变参数
    public static final double[] CAMERA_MATRIX = {
        500.0, 0.0, OUTPUT_WIDTH / 2.0,
        0.0, 500.0, OUTPUT_HEIGHT / 2.0,
        0.0, 0.0, 1.0
    };
    
    // 连接超时
    public static final int CONNECTION_TIMEOUT = 8; // 连接超时秒数
    
    /**
     * 获取摄像头URL
     */
    public static String getCameraUrl(int cameraIndex) {
        if (cameraIndex >= 0 && cameraIndex < CAMERA_URLS.length) {
            return CAMERA_URLS[cameraIndex];
        }
        return CAMERA_URLS[0];
    }
    
    /**
     * 获取摄像头旋转角度
     */
    public static int getCameraRotation(int cameraIndex) {
        if (cameraIndex >= 0 && cameraIndex < CAMERA_ROTATIONS.length) {
            return CAMERA_ROTATIONS[cameraIndex];
        }
        return 0;
    }
    
    /**
     * 获取摄像头名称
     */
    public static String getCameraName(int cameraIndex) {
        switch (cameraIndex) {
            case CAMERA_TOP: return "顶部摄像头";
            case CAMERA_RIGHT: return "右侧摄像头";
            case CAMERA_BOTTOM: return "底部摄像头";
            case CAMERA_LEFT: return "左侧摄像头";
            default: return "未知摄像头";
        }
    }

    
    /**
     * 是否启用该摄像头
     */
    public static boolean isCameraEnabled(int cameraIndex) {
        return cameraIndex >= 0 && cameraIndex < CAMERA_URLS.length;
    }
} 