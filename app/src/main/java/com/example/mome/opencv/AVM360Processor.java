package com.example.mome.opencv;

import android.graphics.Bitmap;
import android.util.Log;

import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.util.HashMap;
import java.util.Map;

/**
 * AVM360全景处理器
 * 负责将四个摄像头的图像拼接成360度全景视图
 */
public class AVM360Processor {
    
    private static final String TAG = "AVM360Processor";
    
    // 输出图像尺寸
    private static final int OUTPUT_WIDTH = 800;
    private static final int OUTPUT_HEIGHT = 600;
    
    // 摄像头位置
    public static final int CAMERA_FRONT = 0;
    public static final int CAMERA_RIGHT = 1;
    public static final int CAMERA_REAR = 2;
    public static final int CAMERA_LEFT = 3;
    
    // 存储最新的摄像头帧
    private Map<Integer, Mat> latestFrames;
    private Mat outputMat;
    private boolean isInitialized = false;
    
    public AVM360Processor() {
        latestFrames = new HashMap<>();
        outputMat = new Mat(OUTPUT_HEIGHT, OUTPUT_WIDTH, CvType.CV_8UC3);
        Log.d(TAG, "AVM360Processor 初始化完成");
    }
    
    /**
     * 更新指定摄像头的帧数据
     */
    public synchronized void updateFrame(int cameraIndex, Bitmap bitmap) {
        if (bitmap == null) return;
        
        try {
            Mat frame = new Mat();
            Utils.bitmapToMat(bitmap, frame);
            
            // 如果是RGBA，转换为RGB
            if (frame.channels() == 4) {
                Imgproc.cvtColor(frame, frame, Imgproc.COLOR_RGBA2RGB);
            }
            
            // 调整大小以适应拼接
            Mat resizedFrame = new Mat();
            Size targetSize = getTargetSizeForCamera(cameraIndex);
            Imgproc.resize(frame, resizedFrame, targetSize);
            
            // 存储帧数据
            latestFrames.put(cameraIndex, resizedFrame);
            
            frame.release();
            
        } catch (Exception e) {
            Log.e(TAG, "更新摄像头帧失败: " + cameraIndex, e);
        }
    }
    
    /**
     * 生成AVM360全景图像
     */
    public synchronized Bitmap generateAVM360View() {
        try {
            // 检查是否有足够的帧数据
            if (latestFrames.size() < 2) {
                return null;
            }
            
            // 创建空白输出图像
            outputMat.setTo(new org.opencv.core.Scalar(30, 30, 30)); // 深灰色背景
            
            // 拼接各个摄像头的图像
            stitchCameraViews();
            
            // 转换为Bitmap
            Bitmap resultBitmap = Bitmap.createBitmap(OUTPUT_WIDTH, OUTPUT_HEIGHT, Bitmap.Config.ARGB_8888);
            Utils.matToBitmap(outputMat, resultBitmap);
            
            return resultBitmap;
            
        } catch (Exception e) {
            Log.e(TAG, "生成AVM360视图失败", e);
            return null;
        }
    }
    
    /**
     * 拼接摄像头视图到输出图像
     */
    private void stitchCameraViews() {
        // 计算各个区域的位置
        int centerX = OUTPUT_WIDTH / 2;
        int centerY = OUTPUT_HEIGHT / 2;
        int quadrantWidth = OUTPUT_WIDTH / 2;
        int quadrantHeight = OUTPUT_HEIGHT / 2;
        
        // 前方摄像头 - 上方区域
        if (latestFrames.containsKey(CAMERA_FRONT)) {
            Mat frontFrame = latestFrames.get(CAMERA_FRONT);
            if (frontFrame != null && !frontFrame.empty()) {
                Rect frontRect = new Rect(centerX - quadrantWidth/2, 0, quadrantWidth, quadrantHeight);
                copyFrameToRegion(frontFrame, frontRect);
            }
        }
        
        // 右侧摄像头 - 右方区域
        if (latestFrames.containsKey(CAMERA_RIGHT)) {
            Mat rightFrame = latestFrames.get(CAMERA_RIGHT);
            if (rightFrame != null && !rightFrame.empty()) {
                Rect rightRect = new Rect(centerX, centerY - quadrantHeight/2, quadrantWidth, quadrantHeight);
                copyFrameToRegion(rightFrame, rightRect);
            }
        }
        
        // 后方摄像头 - 下方区域
        if (latestFrames.containsKey(CAMERA_REAR)) {
            Mat rearFrame = latestFrames.get(CAMERA_REAR);
            if (rearFrame != null && !rearFrame.empty()) {
                Rect rearRect = new Rect(centerX - quadrantWidth/2, centerY, quadrantWidth, quadrantHeight);
                copyFrameToRegion(rearFrame, rearRect);
            }
        }
        
        // 左侧摄像头 - 左方区域
        if (latestFrames.containsKey(CAMERA_LEFT)) {
            Mat leftFrame = latestFrames.get(CAMERA_LEFT);
            if (leftFrame != null && !leftFrame.empty()) {
                Rect leftRect = new Rect(0, centerY - quadrantHeight/2, quadrantWidth, quadrantHeight);
                copyFrameToRegion(leftFrame, leftRect);
            }
        }
        
        // 绘制中心车辆图标
        drawVehicleIcon();
        
        // 绘制方向指示
        drawDirectionIndicators();
    }
    
    /**
     * 将帧复制到指定区域
     */
    private void copyFrameToRegion(Mat frame, Rect targetRect) {
        try {
            // 确保目标区域在输出图像范围内
            if (targetRect.x < 0 || targetRect.y < 0 || 
                targetRect.x + targetRect.width > OUTPUT_WIDTH ||
                targetRect.y + targetRect.height > OUTPUT_HEIGHT) {
                return;
            }
            
            // 调整帧大小以适应目标区域
            Mat resizedFrame = new Mat();
            Size targetSize = new Size(targetRect.width, targetRect.height);
            Imgproc.resize(frame, resizedFrame, targetSize);
            
            // 复制到输出图像的指定区域
            Mat roi = new Mat(outputMat, targetRect);
            resizedFrame.copyTo(roi);
            
            resizedFrame.release();
            roi.release();
            
        } catch (Exception e) {
            Log.e(TAG, "复制帧到区域失败", e);
        }
    }
    
    /**
     * 绘制车辆图标
     */
    private void drawVehicleIcon() {
        int centerX = OUTPUT_WIDTH / 2;
        int centerY = OUTPUT_HEIGHT / 2;
        
        // 绘制车辆轮廓（简单矩形）
        Point topLeft = new Point(centerX - 30, centerY - 15);
        Point bottomRight = new Point(centerX + 30, centerY + 15);
        
        // 车辆主体
        Imgproc.rectangle(outputMat, topLeft, bottomRight, 
                         new org.opencv.core.Scalar(255, 255, 255), 2);
        
        // 车头指示
        Point frontTop = new Point(centerX - 20, centerY - 15);
        Point frontBottom = new Point(centerX + 20, centerY - 25);
        Imgproc.rectangle(outputMat, frontTop, frontBottom, 
                         new org.opencv.core.Scalar(100, 255, 100), -1);
    }
    
    /**
     * 绘制方向指示器
     */
    private void drawDirectionIndicators() {
        int centerX = OUTPUT_WIDTH / 2;
        int centerY = OUTPUT_HEIGHT / 2;
        
        // 绘制十字分割线
        // 垂直线
        Imgproc.line(outputMat, 
                    new Point(centerX, 0), 
                    new Point(centerX, OUTPUT_HEIGHT), 
                    new org.opencv.core.Scalar(80, 80, 80), 2);
        
        // 水平线
        Imgproc.line(outputMat, 
                    new Point(0, centerY), 
                    new Point(OUTPUT_WIDTH, centerY), 
                    new org.opencv.core.Scalar(80, 80, 80), 2);
        
        // 绘制方向标签
        drawTextLabel("前", centerX, 20);
        drawTextLabel("后", centerX, OUTPUT_HEIGHT - 10);
        drawTextLabel("左", 20, centerY);
        drawTextLabel("右", OUTPUT_WIDTH - 30, centerY);
    }
    
    /**
     * 绘制文本标签
     */
    private void drawTextLabel(String text, int x, int y) {
        Imgproc.putText(outputMat, text, new Point(x, y), 
                       Imgproc.FONT_HERSHEY_SIMPLEX, 0.8, 
                       new org.opencv.core.Scalar(255, 255, 255), 2);
    }
    
    /**
     * 获取摄像头的目标尺寸
     */
    private Size getTargetSizeForCamera(int cameraIndex) {
        // 所有摄像头使用相同的基础尺寸
        return new Size(OUTPUT_WIDTH / 2, OUTPUT_HEIGHT / 2);
    }
    
    /**
     * 清理资源
     */
    public void release() {
        try {
            if (outputMat != null) {
                outputMat.release();
            }
            
            for (Mat frame : latestFrames.values()) {
                if (frame != null) {
                    frame.release();
                }
            }
            latestFrames.clear();
            
            Log.d(TAG, "AVM360Processor 资源已释放");
        } catch (Exception e) {
            Log.e(TAG, "释放资源失败", e);
        }
    }
    
    /**
     * 检查是否有有效的摄像头数据
     */
    public boolean hasValidData() {
        return latestFrames.size() > 0;
    }
    
    /**
     * 获取当前连接的摄像头数量
     */
    public int getConnectedCameraCount() {
        return latestFrames.size();
    }
} 