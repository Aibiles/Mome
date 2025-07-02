package com.example.mome.config;

public class ExternalCameraConfig {
    
    // 摄像头设置
    public static final int TARGET_FPS = 20; // 目标帧率
    public static final int PREVIEW_WIDTH = 1280; // 预览宽度
    public static final int PREVIEW_HEIGHT = 720; // 预览高度
    
    // 支持的分辨率配置
    public static class Resolution {
        public final int width;
        public final int height;
        public final String name;
        
        public Resolution(int width, int height, String name) {
            this.width = width;
            this.height = height;
            this.name = name;
        }
    }
    
    public static final Resolution[] SUPPORTED_RESOLUTIONS = {
        new Resolution(1920, 1080, "1080p"),
        new Resolution(1280, 720, "720p"),
        new Resolution(640, 480, "480p"),
        new Resolution(320, 240, "240p")
    };
    
    // 摄像头类型
    public static class CameraType {
        public static final int FRONT = 0;
        public static final int BACK = 1;
        public static final int EXTERNAL = 2;
        public static final int UNKNOWN = 3;
        
        public static String getTypeName(int type) {
            switch (type) {
                case FRONT: return "前置摄像头";
                case BACK: return "后置摄像头";
                case EXTERNAL: return "外接摄像头";
                default: return "未知摄像头";
            }
        }
    }
    
    // 性能设置
    public static final int MAX_CONCURRENT_CAMERAS = 4; // 最大同时使用的摄像头数量
    public static final boolean AUTO_FOCUS_ENABLED = true; // 自动对焦
    public static final boolean STABILIZATION_ENABLED = true; // 防抖
    public static final boolean LOW_LIGHT_BOOST = false; // 低光增强
    
    // UI设置
    public static final boolean SHOW_FPS = true; // 显示FPS
    public static final boolean SHOW_RESOLUTION = true; // 显示分辨率
    public static final boolean SHOW_CAMERA_INFO = true; // 显示摄像头信息
    public static final int DEBUG_UPDATE_INTERVAL = 1000; // 调试信息更新间隔（毫秒）
    
    // 录制设置
    public static final int VIDEO_QUALITY_HIGH = 0;
    public static final int VIDEO_QUALITY_MEDIUM = 1;
    public static final int VIDEO_QUALITY_LOW = 2;
    
    public static final int DEFAULT_VIDEO_QUALITY = VIDEO_QUALITY_HIGH;
    public static final String VIDEO_CODEC = "H.264";
    public static final int VIDEO_BITRATE = 8000000; // 8Mbps
    
    // 外接摄像头检测
    public static final boolean ENABLE_USB_CAMERA_DETECTION = true;
    public static final int CAMERA_SCAN_INTERVAL = 5000; // 摄像头扫描间隔（毫秒）
    public static final boolean AUTO_SWITCH_TO_EXTERNAL = true; // 自动切换到外接摄像头
    
    // 错误处理
    public static final int MAX_CAMERA_RESTART_ATTEMPTS = 3;
    public static final int CAMERA_RESTART_DELAY = 2000; // 重启延迟（毫秒）
    public static final boolean ENABLE_CAMERA_FALLBACK = true; // 启用摄像头回退
} 