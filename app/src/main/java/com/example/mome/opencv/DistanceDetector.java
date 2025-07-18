package com.example.mome.opencv;

import android.graphics.Bitmap;
import android.util.Log;

import org.opencv.android.Utils;
import org.opencv.core.*;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.List;

/**
 * 距离检测器
 * 用于在倒车摄像头中检测障碍物并计算距离
 */
public class DistanceDetector {
    private static final String TAG = "DistanceDetector";
    
    // 摄像头标定参数（需要根据实际摄像头调整）
    private static final double FOCAL_LENGTH = 500.0; // 焦距（像素）
    private static final double REAL_WORLD_HEIGHT = 1.5; // 参考物体高度（米）
    private static final double CAMERA_HEIGHT = 1.0; // 摄像头离地高度（米）
    private static final double CAMERA_ANGLE = 20.0; // 摄像头俯仰角（度）
    
    // 检测参数
    private static final double MIN_CONTOUR_AREA = 500; // 最小轮廓面积
    private static final double MAX_DISTANCE = 8.0; // 最大检测距离（米）
    private static final double MIN_DISTANCE = 0.5; // 最小检测距离（米）

    // 距离分区（用于不同颜色显示）
    public enum DistanceZone {
        SAFE(2.0, 0xFF00FF00),      // 绿色：安全距离 > 2米
        CAUTION(1.0, 0xFFFFFF00),   // 黄色：注意距离 1-2米
        DANGER(0.5, 0xFFFF0000),    // 红色：危险距离 0.5-1米
        CRITICAL(0.0, 0xFFFF0000);  // 红色闪烁：极危险 < 0.5米
        
        public final double minDistance;
        public final int color;
        
        DistanceZone(double minDistance, int color) {
            this.minDistance = minDistance;
            this.color = color;
        }
    }
    
    // 检测结果
    public static class DetectionResult {
        public List<ObstacleInfo> obstacles;
        public double minDistance;
        public DistanceZone zone;
        public Mat processedImage;
        
        public DetectionResult() {
            obstacles = new ArrayList<>();
            minDistance = Double.MAX_VALUE;
            zone = DistanceZone.SAFE;
        }
    }
    
    // 障碍物信息
    public static class ObstacleInfo {
        public Point position;      // 障碍物中心位置
        public double distance;     // 距离（米）
        public Rect boundingBox;    // 边界框
        public double confidence;   // 置信度
        
        public ObstacleInfo(Point position, double distance, Rect boundingBox, double confidence) {
            this.position = position;
            this.distance = distance;
            this.boundingBox = boundingBox;
            this.confidence = confidence;
        }
    }
    
    private Mat grayMat;
    private Mat blurMat;
    private Mat edgesMat;
    private Mat hierarchyMat;
    
    public DistanceDetector() {
        // 初始化OpenCV矩阵
        grayMat = new Mat();
        blurMat = new Mat();
        edgesMat = new Mat();
        hierarchyMat = new Mat();

        Log.d(TAG, "DistanceDetector 初始化完成");
    }
    
    /**
     * 检测障碍物并计算距离
     */
    public Bitmap detectDistance(Bitmap bitmap, int a) {
        DetectionResult result = new DetectionResult();

        if (bitmap == null || bitmap.isRecycled()) {
            Log.e(TAG, "输入bitmap无效");
            return bitmap;
        }

        try {
            // 转换为OpenCV Mat
            Mat inputMat = new Mat();
            Utils.bitmapToMat(bitmap, inputMat);

            // 预处理图像
            preprocessImage(inputMat);

            // 检测障碍物
            List<MatOfPoint> contours = detectObstacles();

            // 计算距离
            result.obstacles = calculateDistances(contours, inputMat.size());

            // 确定最小距离和危险等级
            updateDistanceZone(result);

            // 创建处理后的图像用于显示
            result.processedImage = createProcessedImage(inputMat, result.obstacles);

            Utils.matToBitmap(result.processedImage, bitmap);

            inputMat.release();

        } catch (Exception e) {
            Log.e(TAG, "距离检测失败", e);
        }

        return bitmap;
    }

    public DetectionResult detectDistance(Bitmap bitmap) {
        DetectionResult result = new DetectionResult();
        
        if (bitmap == null || bitmap.isRecycled()) {
            Log.e(TAG, "输入bitmap无效");
            return result;
        }
        
        try {
            // 转换为OpenCV Mat
            Mat inputMat = new Mat();
            Utils.bitmapToMat(bitmap, inputMat);
            
            // 预处理图像
            preprocessImage(inputMat);
            
            // 检测障碍物
            List<MatOfPoint> contours = detectObstacles();
            
            // 计算距离
            result.obstacles = calculateDistances(contours, inputMat.size());

            // 确定最小距离和危险等级
            updateDistanceZone(result);

            // 创建处理后的图像用于显示
            result.processedImage = createProcessedImage(inputMat, result.obstacles);
            
            inputMat.release();
            
        } catch (Exception e) {
            Log.e(TAG, "距离检测失败", e);
        }
        
        return result;
    }
    
    /**
     * 预处理图像
     */
    private void preprocessImage(Mat inputMat) {
        // 转换为灰度图
        Imgproc.cvtColor(inputMat, grayMat, Imgproc.COLOR_BGR2GRAY);
        
        // 边缘检测
        Imgproc.Canny(grayMat, edgesMat, 30, 50);
        
        // 形态学操作连接断开的边缘
        Mat kernel = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(3, 3));
        Imgproc.morphologyEx(edgesMat, edgesMat, Imgproc.MORPH_CLOSE, kernel);
        kernel.release();
    }
    
    /**
     * 检测障碍物轮廓
     */
    private List<MatOfPoint> detectObstacles() {
        List<MatOfPoint> contours = new ArrayList<>();
        Imgproc.findContours(edgesMat, contours, hierarchyMat, 
                           Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);
    
        int imgHeight = edgesMat.rows();
        int halfHeight = imgHeight / 2;
    
        List<MatOfPoint> filteredContours = new ArrayList<>();
        for (MatOfPoint contour : contours) {
            // 判断该轮廓是否有点在下半部分
            boolean hasPointInLowerHalf = false;
            for (Point p : contour.toArray()) {
                if (p.y >= halfHeight) {
                    hasPointInLowerHalf = true;
                    break;
                }
            }
            if (hasPointInLowerHalf) {
                double area = Imgproc.contourArea(contour);
                if (area > MIN_CONTOUR_AREA) {
                    filteredContours.add(contour);
                }
            }
        }
        return filteredContours;
    }
    
    /**
     * 计算距离
     */
    private List<ObstacleInfo> calculateDistances(List<MatOfPoint> contours, Size imageSize) {
        List<ObstacleInfo> obstacles = new ArrayList<>();
        
        for (MatOfPoint contour : contours) {
            // 获取边界框
            Rect boundingBox = Imgproc.boundingRect(contour);
            
            // 计算中心点
            Point center = new Point(
                boundingBox.x + boundingBox.width / 2.0,
                boundingBox.y + boundingBox.height / 2.0
            );
            
            // 基于透视几何计算距离
            double distance = calculateDistanceFromPosition(center, boundingBox, imageSize);
            
            // 计算置信度（基于轮廓面积和形状）
            double confidence = calculateConfidence(contour, boundingBox);
            
            if (distance >= MIN_DISTANCE && distance <= MAX_DISTANCE) {
                obstacles.add(new ObstacleInfo(center, distance, boundingBox, confidence));
            }
        }
        
        return obstacles;
    }
    
    /**
     * 基于位置计算距离
     */
    private double calculateDistanceFromPosition(Point center, Rect boundingBox, Size imageSize) {
        // 图像中心
        double imageCenterX = imageSize.width / 2.0;
        double imageCenterY = imageSize.height / 2.0;
        
        // 计算相对于图像中心的位置
        double relativeY = (center.y - imageCenterY) / imageCenterY;
        
        // 基于透视几何的简化距离计算
        // 距离与图像中垂直位置相关：越靠近图像底部，距离越近
        double normalizedY = center.y / imageSize.height;
        
        if (normalizedY < 0.3) {
            // 图像上半部分，距离较远
            return MAX_DISTANCE;
        } else if (normalizedY > 0.9) {
            // 图像下半部分，距离很近
            return MIN_DISTANCE;
        } else {
            // 线性插值计算距离
            double distanceRatio = (normalizedY - 0.3) / 0.6; // 0.3到0.9映射到0到1
            return MAX_DISTANCE - (distanceRatio * (MAX_DISTANCE - MIN_DISTANCE));
        }
    }
    
    /**
     * 计算检测置信度
     */
    private double calculateConfidence(MatOfPoint contour, Rect boundingBox) {
        // 基于轮廓面积和边界框的比例
        double contourArea = Imgproc.contourArea(contour);
        double boundingBoxArea = boundingBox.area();
        
        if (boundingBoxArea == 0) return 0.0;
        
        // 填充比例：轮廓面积与边界框面积的比例
        double fillRatio = contourArea / boundingBoxArea;
        
        // 长宽比检查
        double aspectRatio = (double) boundingBox.width / boundingBox.height;
        double aspectScore = Math.min(1.0, 1.0 / Math.max(aspectRatio, 1.0 / aspectRatio));
        
        // 综合置信度
        return (fillRatio * 0.7 + aspectScore * 0.3);
    }
    
    /**
     * 更新距离区域
     */
    private void updateDistanceZone(DetectionResult result) {
        for (ObstacleInfo obstacle : result.obstacles) {
            if (obstacle.distance < result.minDistance) {
                result.minDistance = obstacle.distance;
            }
        }
        
        // 确定危险等级
        if (result.minDistance < DistanceZone.CRITICAL.minDistance) {
            result.zone = DistanceZone.CRITICAL;
        } else if (result.minDistance < DistanceZone.DANGER.minDistance) {
            result.zone = DistanceZone.DANGER;
        } else if (result.minDistance < DistanceZone.CAUTION.minDistance) {
            result.zone = DistanceZone.CAUTION;
        } else {
            result.zone = DistanceZone.SAFE;
        }
    }
    
    /**
     * 创建处理后的图像用于显示
     */
    private Mat createProcessedImage(Mat inputMat, List<ObstacleInfo> obstacles) {
        Mat outputMat = inputMat.clone();
        
        // 绘制检测结果
        for (ObstacleInfo obstacle : obstacles) {
            // 选择颜色
            Scalar color = getColorForDistance(obstacle.distance);
            
            // 绘制边界框
            Imgproc.rectangle(outputMat, obstacle.boundingBox.tl(), 
                            obstacle.boundingBox.br(), color, 20);
            
            // 绘制距离文本
            String distanceText = String.format("%.1fm", obstacle.distance);
            Point textPoint = new Point(obstacle.boundingBox.x, obstacle.boundingBox.y - 10);
            Imgproc.putText(outputMat, distanceText, textPoint, 
                          Imgproc.FONT_HERSHEY_SIMPLEX, 5, color, 10);
        }
        
        return outputMat;
    }
    
    /**
     * 根据距离获取颜色
     */
    private Scalar getColorForDistance(double distance) {
        if (distance < 1.0) {
            return new Scalar(0, 0, 255); // 红色
        } else if (distance < 2.0) {
            return new Scalar(0, 255, 255); // 黄色
        } else {
            return new Scalar(0, 255, 0); // 绿色
        }
    }
    
    /**
     * 释放资源
     */
    public void release() {
        if (grayMat != null) grayMat.release();
        if (blurMat != null) blurMat.release();
        if (edgesMat != null) edgesMat.release();
        if (hierarchyMat != null) hierarchyMat.release();
        
        Log.d(TAG, "DistanceDetector 资源已释放");
    }
} 