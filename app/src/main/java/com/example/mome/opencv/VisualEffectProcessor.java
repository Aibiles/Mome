package com.example.mome.opencv;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.ImageView;

/**
 * 视觉效果处理器
 * 负责为360度全景视图添加动态颜色变化和视觉增强效果
 */
public class VisualEffectProcessor {
    
    private static final String TAG = "VisualEffectProcessor";
    
    // 动画参数
    private static final long ANIMATION_DURATION = 2000; // 动画持续时间(ms)
    private static final long ANIMATION_INTERVAL = 50;   // 动画更新间隔(ms)
    
    // 效果类型
    public enum EffectType {
        FORWARD_ENHANCEMENT,    // 前进增强效果
        BACKWARD_WARNING,       // 后退警告效果
        TURN_INDICATION,        // 转向指示效果
        NORMAL                  // 正常状态
    }
    
    // 颜色主题
    public enum ColorTheme {
        FORWARD_BLUE(Color.rgb(0, 150, 255), Color.rgb(0, 100, 200)),     // 前进蓝色
        BACKWARD_RED(Color.rgb(255, 100, 100), Color.rgb(200, 0, 0)),     // 后退红色
        LEFT_GREEN(Color.rgb(100, 255, 100), Color.rgb(0, 200, 0)),       // 左转绿色
        RIGHT_ORANGE(Color.rgb(255, 200, 100), Color.rgb(255, 150, 0));   // 右转橙色
        
        public final int primaryColor;
        public final int secondaryColor;
        
        ColorTheme(int primary, int secondary) {
            this.primaryColor = primary;
            this.secondaryColor = secondary;
        }
    }
    
    private Handler animationHandler;
    private boolean isAnimating = false;
    private float animationProgress = 0f;
    private EffectType currentEffect = EffectType.NORMAL;
    private ColorTheme currentTheme = ColorTheme.FORWARD_BLUE;
    
    public VisualEffectProcessor() {
        animationHandler = new Handler(Looper.getMainLooper());
    }
    
    /**
     * 应用前进增强效果
     */
    public void applyForwardEffect(ImageView targetView, Bitmap originalBitmap) {
        currentEffect = EffectType.FORWARD_ENHANCEMENT;
        currentTheme = ColorTheme.FORWARD_BLUE;
        startDynamicEffect(targetView, originalBitmap);
        Log.d(TAG, "应用前进增强效果");
    }
    
    /**
     * 应用后退警告效果
     */
    public void applyBackwardEffect(ImageView targetView, Bitmap originalBitmap) {
        currentEffect = EffectType.BACKWARD_WARNING;
        currentTheme = ColorTheme.BACKWARD_RED;
        startDynamicEffect(targetView, originalBitmap);
        Log.d(TAG, "应用后退警告效果");
    }
    
    /**
     * 应用左转效果
     */
    public void applyLeftTurnEffect(ImageView targetView, Bitmap originalBitmap) {
        currentEffect = EffectType.TURN_INDICATION;
        currentTheme = ColorTheme.LEFT_GREEN;
        startDynamicEffect(targetView, originalBitmap);
        Log.d(TAG, "应用左转效果");
    }
    
    /**
     * 应用右转效果
     */
    public void applyRightTurnEffect(ImageView targetView, Bitmap originalBitmap) {
        currentEffect = EffectType.TURN_INDICATION;
        currentTheme = ColorTheme.RIGHT_ORANGE;
        startDynamicEffect(targetView, originalBitmap);
        Log.d(TAG, "应用右转效果");
    }
    
    /**
     * 停止所有效果，恢复正常
     */
    public void stopAllEffects(ImageView targetView, Bitmap originalBitmap) {
        currentEffect = EffectType.NORMAL;
        stopAnimation();
        if (targetView != null && originalBitmap != null) {
            targetView.setImageBitmap(originalBitmap);
        }
        Log.d(TAG, "停止所有视觉效果");
    }
    
    /**
     * 开始动态效果
     */
    private void startDynamicEffect(ImageView targetView, Bitmap originalBitmap) {
        if (originalBitmap == null || targetView == null) {
            Log.w(TAG, "无效的参数，无法应用效果");
            return;
        }
        
        stopAnimation(); // 停止之前的动画
        isAnimating = true;
        animationProgress = 0f;
        
        Runnable animationRunnable = new Runnable() {
            @Override
            public void run() {
                if (!isAnimating) return;
                
                // 更新动画进度
                animationProgress += (float) ANIMATION_INTERVAL / ANIMATION_DURATION;
                if (animationProgress > 1f) {
                    animationProgress = 0f; // 循环动画
                }
                
                // 应用效果
                Bitmap effectBitmap = applyVisualEffect(originalBitmap, currentEffect, animationProgress);
                if (effectBitmap != null) {
                    targetView.setImageBitmap(effectBitmap);
                }
                
                // 继续下一帧
                if (isAnimating) {
                    animationHandler.postDelayed(this, ANIMATION_INTERVAL);
                }
            }
        };
        
        animationHandler.post(animationRunnable);
    }
    
    /**
     * 停止动画
     */
    private void stopAnimation() {
        isAnimating = false;
        animationHandler.removeCallbacksAndMessages(null);
    }
    
    /**
     * 应用视觉效果到bitmap
     */
    private Bitmap applyVisualEffect(Bitmap originalBitmap, EffectType effectType, float progress) {
        if (originalBitmap == null) return null;
        
        try {
            Bitmap resultBitmap = originalBitmap.copy(originalBitmap.getConfig(), true);
            Canvas canvas = new Canvas(resultBitmap);
            
            switch (effectType) {
                case FORWARD_ENHANCEMENT:
                    applyForwardEnhancement(canvas, resultBitmap, progress);
                    break;
                case BACKWARD_WARNING:
                    applyBackwardWarning(canvas, resultBitmap, progress);
                    break;
                case TURN_INDICATION:
                    applyTurnIndication(canvas, resultBitmap, progress);
                    break;
                case NORMAL:
                default:
                    // 不应用任何效果
                    break;
            }
            
            return resultBitmap;
            
        } catch (Exception e) {
            Log.e(TAG, "应用视觉效果失败", e);
            return originalBitmap;
        }
    }
    
    /**
     * 应用前进增强效果
     */
    private void applyForwardEnhancement(Canvas canvas, Bitmap bitmap, float progress) {
        // 蓝色调色板增强
        Paint paint = new Paint();
        ColorMatrix colorMatrix = new ColorMatrix();
        
        // 增强蓝色通道
        float blueEnhancement = 0.3f + 0.2f * (float) Math.sin(progress * Math.PI * 2);
        colorMatrix.set(new float[]{
                1.0f, 0.0f, 0.0f, 0.0f, 0,
                0.0f, 1.0f, 0.0f, 0.0f, 0,
                0.0f, 0.0f, 1.0f + blueEnhancement, 0.0f, 0,
                0.0f, 0.0f, 0.0f, 1.0f, 0
        });
        
        paint.setColorFilter(new ColorMatrixColorFilter(colorMatrix));
        canvas.drawBitmap(bitmap, 0, 0, paint);
        
        // 添加向前的动态光效
        addForwardLightEffect(canvas, bitmap.getWidth(), bitmap.getHeight(), progress);
    }
    
    /**
     * 应用后退警告效果
     */
    private void applyBackwardWarning(Canvas canvas, Bitmap bitmap, float progress) {
        // 红色警告闪烁
        Paint paint = new Paint();
        
        // 计算闪烁强度
        float flashIntensity = 0.3f + 0.4f * (float) Math.sin(progress * Math.PI * 4); // 更快的闪烁
        
        // 红色叠加
        paint.setColor(Color.argb((int)(flashIntensity * 255), 255, 0, 0));
        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.OVERLAY));
        canvas.drawRect(0, 0, bitmap.getWidth(), bitmap.getHeight(), paint);
        
        // 添加边框警告效果
        addWarningBorder(canvas, bitmap.getWidth(), bitmap.getHeight(), progress);
    }
    
    /**
     * 应用转向指示效果
     */
    private void applyTurnIndication(Canvas canvas, Bitmap bitmap, float progress) {
        // 颜色增强
        Paint paint = new Paint();
        ColorMatrix colorMatrix = new ColorMatrix();
        
        // 根据转向方向调整颜色
        float colorEnhancement = 0.2f + 0.3f * (float) Math.sin(progress * Math.PI * 2);
        
        if (currentTheme == ColorTheme.LEFT_GREEN) {
            // 增强绿色
            colorMatrix.set(new float[]{
                    1.0f, 0.0f, 0.0f, 0.0f, 0,
                    0.0f, 1.0f + colorEnhancement, 0.0f, 0.0f, 0,
                    0.0f, 0.0f, 1.0f, 0.0f, 0,
                    0.0f, 0.0f, 0.0f, 1.0f, 0
            });
        } else if (currentTheme == ColorTheme.RIGHT_ORANGE) {
            // 增强橙色（红+绿）
            colorMatrix.set(new float[]{
                    1.0f + colorEnhancement * 0.5f, 0.0f, 0.0f, 0.0f, 0,
                    0.0f, 1.0f + colorEnhancement * 0.3f, 0.0f, 0.0f, 0,
                    0.0f, 0.0f, 1.0f, 0.0f, 0,
                    0.0f, 0.0f, 0.0f, 1.0f, 0
            });
        }
        
        paint.setColorFilter(new ColorMatrixColorFilter(colorMatrix));
        canvas.drawBitmap(bitmap, 0, 0, paint);
        
        // 添加转向指示箭头
        addTurnIndicator(canvas, bitmap.getWidth(), bitmap.getHeight(), progress);
    }
    
    /**
     * 添加前进光效
     */
    private void addForwardLightEffect(Canvas canvas, int width, int height, float progress) {
        Paint paint = new Paint();
        paint.setAntiAlias(true);
        
        // 创建向前的光束效果
        int centerX = width / 2;
        int centerY = height;
        float radius = height * (0.3f + 0.2f * progress);
        
        RadialGradient gradient = new RadialGradient(
                centerX, centerY, radius,
                new int[]{Color.argb(100, 0, 150, 255), Color.TRANSPARENT},
                new float[]{0f, 1f},
                Shader.TileMode.CLAMP
        );
        
        paint.setShader(gradient);
        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SCREEN));
        canvas.drawCircle(centerX, centerY, radius, paint);
    }
    
    /**
     * 添加警告边框
     */
    private void addWarningBorder(Canvas canvas, int width, int height, float progress) {
        Paint paint = new Paint();
        paint.setAntiAlias(true);
        paint.setStyle(Paint.Style.STROKE);
        
        // 动态边框宽度
        float borderWidth = 5 + 10 * (float) Math.sin(progress * Math.PI * 2);
        paint.setStrokeWidth(borderWidth);
        
        // 红色边框
        int alpha = (int)(150 + 105 * Math.sin(progress * Math.PI * 4));
        paint.setColor(Color.argb(alpha, 255, 0, 0));
        
        canvas.drawRect(borderWidth/2, borderWidth/2, 
                       width - borderWidth/2, height - borderWidth/2, paint);
    }
    
    /**
     * 添加转向指示器
     */
    private void addTurnIndicator(Canvas canvas, int width, int height, float progress) {
        Paint paint = new Paint();
        paint.setAntiAlias(true);
        paint.setColor(currentTheme.primaryColor);
        paint.setAlpha((int)(150 + 105 * Math.sin(progress * Math.PI * 2)));
        
        // 绘制转向箭头（简化版）
        float arrowSize = 50;
        float centerX = width * (currentTheme == ColorTheme.LEFT_GREEN ? 0.2f : 0.8f);
        float centerY = height * 0.2f;
        
        // 简单的三角形箭头
        if (currentTheme == ColorTheme.LEFT_GREEN) {
            // 左箭头
            canvas.drawRect(centerX - arrowSize, centerY - 10, centerX + 10, centerY + 10, paint);
        } else {
            // 右箭头
            canvas.drawRect(centerX - 10, centerY - 10, centerX + arrowSize, centerY + 10, paint);
        }
    }
    
    /**
     * 清理资源
     */
    public void cleanup() {
        stopAnimation();
        Log.d(TAG, "视觉效果处理器已清理");
    }
} 