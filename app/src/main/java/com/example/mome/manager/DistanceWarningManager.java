package com.example.mome.manager;

import android.content.Context;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.ToneGenerator;
import android.os.Handler;
import android.os.Looper;
import android.os.Vibrator;
import android.util.Log;

import com.example.mome.R;
import com.example.mome.opencv.DistanceDetector;

/**
 * 距离警告管理器
 * 提供声音、振动和视觉警告功能
 */
public class DistanceWarningManager {
    private static final String TAG = "DistanceWarningManager";
    
    private Context context;
    private MediaPlayer mediaPlayer;
    private ToneGenerator toneGenerator;
    private Vibrator vibrator;
    private Handler warningHandler;
    
    // 警告参数
    private static final int WARNING_TONE_DURATION = 200; // 警告音持续时间（毫秒）
    private static final int CRITICAL_VIBRATION_DURATION = 300; // 紧急振动持续时间（毫秒）
    private static final int DANGER_VIBRATION_DURATION = 200; // 危险振动持续时间（毫秒）
    
    // 警告间隔（防止频繁警告）
    private static final long WARNING_INTERVAL = 1000; // 1秒间隔
    private long lastWarningTime = 0;
    
    // 连续警告相关
    private boolean isContinuousWarning = false;
    private Runnable continuousWarningRunnable;
    private DistanceDetector.DistanceZone currentWarningZone = DistanceDetector.DistanceZone.SAFE;
    
    public DistanceWarningManager(Context context) {
        this.context = context;
        initializeComponents();
    }
    
    /**
     * 初始化组件
     */
    private void initializeComponents() {
        try {
            // 初始化MediaPlayer
            mediaPlayer = MediaPlayer.create(context, R.raw.warring);
            if (mediaPlayer != null) {
                mediaPlayer.setLooping(false);
                Log.d(TAG, "MediaPlayer初始化成功");
            } else {
                Log.w(TAG, "MediaPlayer初始化失败，将使用ToneGenerator");
            }
            
            // 初始化ToneGenerator作为备用
            toneGenerator = new ToneGenerator(AudioManager.STREAM_ALARM, 80);
            
            // 初始化振动器
            vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
            
            // 初始化Handler
            warningHandler = new Handler(Looper.getMainLooper());
            
            Log.d(TAG, "DistanceWarningManager初始化完成");
            
        } catch (Exception e) {
            Log.e(TAG, "初始化警告组件失败", e);
        }
    }
    
    /**
     * 触发距离警告
     */
    public void triggerDistanceWarning(DistanceDetector.DistanceZone zone, double distance) {
        if (zone == DistanceDetector.DistanceZone.SAFE) {
            stopContinuousWarning();
            return;
        }
        
        long currentTime = System.currentTimeMillis();
        
        // 检查警告间隔
        if (currentTime - lastWarningTime < WARNING_INTERVAL) {
            return;
        }
        
        lastWarningTime = currentTime;
        currentWarningZone = zone;
        
        Log.d(TAG, String.format("触发距离警告: %s, 距离: %.1fm", zone.name(), distance));
        
        switch (zone) {
            case CRITICAL:
                triggerCriticalWarning();
                break;
            case DANGER:
                triggerDangerWarning();
                break;
            case CAUTION:
                triggerCautionWarning();
                break;
        }
    }
    
    /**
     * 紧急警告（距离<0.5米）
     */
    private void triggerCriticalWarning() {
        // 播放紧急警告声
        playWarningSound(3); // 播放3次
        
        // 强烈振动
        if (vibrator != null && vibrator.hasVibrator()) {
            // 创建振动模式：短-长-短-长
            long[] pattern = {0, 100, 50, 200, 50, 100};
            vibrator.vibrate(pattern, -1);
        }
        
        // 启动连续警告
        startContinuousWarning(500); // 每500毫秒警告一次
    }
    
    /**
     * 危险警告（距离0.5-1米）
     */
    private void triggerDangerWarning() {
        // 播放危险警告声
        playWarningSound(2); // 播放2次
        
        // 中等振动
        if (vibrator != null && vibrator.hasVibrator()) {
            long[] pattern = {0, 150, 100, 150};
            vibrator.vibrate(pattern, -1);
        }
        
        // 启动连续警告
        startContinuousWarning(1000); // 每1秒警告一次
    }
    
    /**
     * 注意警告（距离1-2米）
     */
    private void triggerCautionWarning() {
        // 播放注意警告声
        playWarningSound(1); // 播放1次
        
        // 轻微振动
        if (vibrator != null && vibrator.hasVibrator()) {
            vibrator.vibrate(150);
        }
        
        // 启动连续警告
        startContinuousWarning(2000); // 每2秒警告一次
    }
    
    /**
     * 播放警告声音
     */
    private void playWarningSound(int repeatCount) {
        try {
            if (mediaPlayer != null) {
                // 使用预设的警告音文件
                if (!mediaPlayer.isPlaying()) {
                    mediaPlayer.seekTo(0);
                    mediaPlayer.start();
                }
            } else if (toneGenerator != null) {
                // 使用系统音调作为备用
                for (int i = 0; i < repeatCount; i++) {
                    toneGenerator.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, WARNING_TONE_DURATION);
                    try {
                        Thread.sleep(WARNING_TONE_DURATION + 100); // 间隔100毫秒
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "播放警告声音失败", e);
        }
    }
    
    /**
     * 启动连续警告
     */
    private void startContinuousWarning(long interval) {
        stopContinuousWarning(); // 先停止之前的警告
        
        isContinuousWarning = true;
        continuousWarningRunnable = new Runnable() {
            @Override
            public void run() {
                if (isContinuousWarning) {
                    // 播放简短的警告音
                    playWarningSound(1);
                    
                    // 调度下一次警告
                    warningHandler.postDelayed(this, interval);
                }
            }
        };
        
        warningHandler.postDelayed(continuousWarningRunnable, interval);
        Log.d(TAG, "启动连续警告，间隔: " + interval + "毫秒");
    }
    
    /**
     * 停止连续警告
     */
    private void stopContinuousWarning() {
        isContinuousWarning = false;
        
        if (continuousWarningRunnable != null && warningHandler != null) {
            warningHandler.removeCallbacks(continuousWarningRunnable);
            continuousWarningRunnable = null;
        }
        
        // 停止振动
        if (vibrator != null) {
            vibrator.cancel();
        }
        
        Log.d(TAG, "停止连续警告");
    }
    
    /**
     * 是否正在警告
     */
    public boolean isWarning() {
        return isContinuousWarning;
    }
    
    /**
     * 获取当前警告等级
     */
    public DistanceDetector.DistanceZone getCurrentWarningZone() {
        return currentWarningZone;
    }
    
    /**
     * 设置警告音量
     */
    public void setWarningVolume(float volume) {
        if (mediaPlayer != null) {
            mediaPlayer.setVolume(volume, volume);
        }
    }
    
    /**
     * 静音警告
     */
    public void muteWarning() {
        if (mediaPlayer != null) {
            mediaPlayer.setVolume(0, 0);
        }
    }
    
    /**
     * 取消静音
     */
    public void unmuteWarning() {
        if (mediaPlayer != null) {
            mediaPlayer.setVolume(1.0f, 1.0f);
        }
    }
    
    /**
     * 释放资源
     */
    public void release() {
        stopContinuousWarning();
        
        if (mediaPlayer != null) {
            try {
                if (mediaPlayer.isPlaying()) {
                    mediaPlayer.stop();
                }
                mediaPlayer.release();
                mediaPlayer = null;
            } catch (Exception e) {
                Log.e(TAG, "释放MediaPlayer失败", e);
            }
        }
        
        if (toneGenerator != null) {
            try {
                toneGenerator.release();
                toneGenerator = null;
            } catch (Exception e) {
                Log.e(TAG, "释放ToneGenerator失败", e);
            }
        }
        
        if (vibrator != null) {
            vibrator.cancel();
            vibrator = null;
        }
        
        if (warningHandler != null) {
            warningHandler.removeCallbacksAndMessages(null);
            warningHandler = null;
        }
        
        Log.d(TAG, "DistanceWarningManager资源已释放");
    }
} 