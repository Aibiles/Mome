package com.example.mome.opencv;

import android.graphics.Bitmap;
import android.util.Log;

import org.opencv.android.Utils;
import org.opencv.core.*;
import org.opencv.imgproc.Imgproc;

public class SphericalProjector {
    private static final String TAG = "SphericalProjector";
    
    // 球面投影参数
    private static final int SPHERE_RADIUS = 200;
    private static final double FOV_HORIZONTAL = 90.0; // 每个摄像头的水平视角
    private static final double FOV_VERTICAL = 60.0;   // 每个摄像头的垂直视角
    
    // 输出全景图尺寸
    private static final int PANORAMA_WIDTH = 1024;
    private static final int PANORAMA_HEIGHT = 512;
    
    private Mat[] lookupMaps;
    private boolean initialized = false;
    
    public SphericalProjector() {
        initializeLookupMaps();
    }
    
    /**
     * 初始化每个摄像头的查找表
     */
    private void initializeLookupMaps() {
        lookupMaps = new Mat[8]; // 4个摄像头，每个2个映射表(x,y)
        
        for (int camera = 0; camera < 4; camera++) {
            Mat mapX = new Mat(PANORAMA_HEIGHT, PANORAMA_WIDTH, CvType.CV_32FC1);
            Mat mapY = new Mat(PANORAMA_HEIGHT, PANORAMA_WIDTH, CvType.CV_32FC1);
            
            generateCameraLookupMap(camera, mapX, mapY);
            
            lookupMaps[camera * 2] = mapX;
            lookupMaps[camera * 2 + 1] = mapY;
        }
        
        initialized = true;
        Log.d(TAG, "球面投影查找表初始化完成");
    }
    
    /**
     * 为特定摄像头生成查找表
     */
    private void generateCameraLookupMap(int cameraIndex, Mat mapX, Mat mapY) {
        double cameraAngle = cameraIndex * 90.0; // 每个摄像头相差90度
        
        for (int v = 0; v < PANORAMA_HEIGHT; v++) {
            for (int u = 0; u < PANORAMA_WIDTH; u++) {
                // 将全景图坐标转换为球面坐标
                double longitude = (u * 360.0 / PANORAMA_WIDTH) - 180.0;
                double latitude = (v * 180.0 / PANORAMA_HEIGHT) - 90.0;
                
                // 转换为笛卡尔坐标
                double[] cartesian = sphericalToCartesian(longitude, latitude);
                
                // 应用摄像头旋转
                double[] rotated = rotateCameraCoordinates(cartesian, cameraAngle);
                
                // 检查是否在当前摄像头的视野内
                if (isInCameraFOV(rotated, cameraIndex)) {
                    // 投影到摄像头平面
                    Point imagePoint = projectToCamera(rotated);
                    
                    mapX.put(v, u, imagePoint.x);
                    mapY.put(v, u, imagePoint.y);
                } else {
                    // 不在视野内，设置为无效值
                    mapX.put(v, u, -1);
                    mapY.put(v, u, -1);
                }
            }
        }
    }
    
    /**
     * 球面坐标转笛卡尔坐标
     */
    private double[] sphericalToCartesian(double longitude, double latitude) {
        double lonRad = Math.toRadians(longitude);
        double latRad = Math.toRadians(latitude);
        
        double x = SPHERE_RADIUS * Math.cos(latRad) * Math.cos(lonRad);
        double y = SPHERE_RADIUS * Math.cos(latRad) * Math.sin(lonRad);
        double z = SPHERE_RADIUS * Math.sin(latRad);
        
        return new double[]{x, y, z};
    }
    
    /**
     * 旋转摄像头坐标系
     */
    private double[] rotateCameraCoordinates(double[] coords, double angle) {
        double angleRad = Math.toRadians(angle);
        double cos = Math.cos(angleRad);
        double sin = Math.sin(angleRad);
        
        double x = coords[0] * cos - coords[1] * sin;
        double y = coords[0] * sin + coords[1] * cos;
        double z = coords[2];
        
        return new double[]{x, y, z};
    }
    
    /**
     * 检查点是否在摄像头视野内
     */
    private boolean isInCameraFOV(double[] coords, int cameraIndex) {
        // 检查z坐标（深度）
        if (coords[2] < 0) return false;
        
        // 计算视角
        double horizontalAngle = Math.toDegrees(Math.atan2(coords[0], coords[2]));
        double verticalAngle = Math.toDegrees(Math.atan2(coords[1], coords[2]));
        
        return Math.abs(horizontalAngle) <= FOV_HORIZONTAL / 2 && 
               Math.abs(verticalAngle) <= FOV_VERTICAL / 2;
    }
    
    /**
     * 投影到摄像头平面
     */
    private Point projectToCamera(double[] coords) {
        if (coords[2] == 0) return new Point(-1, -1);
        
        double x = coords[0] / coords[2];
        double y = coords[1] / coords[2];
        
        // 标准化到图像坐标
        double imageX = (x + 1.0) * 0.5;
        double imageY = (y + 1.0) * 0.5;
        
        return new Point(imageX, imageY);
    }
    
    /**
     * 创建360度全景图
     */
    public Bitmap createSphericalPanorama(Bitmap[] cameraImages) {
        if (!initialized || cameraImages.length != 4) {
            Log.e(TAG, "球面投影器未初始化或摄像头数量不正确");
            return null;
        }
        
        try {
            // 创建输出图像
            Mat panorama = new Mat(PANORAMA_HEIGHT, PANORAMA_WIDTH, CvType.CV_8UC3);
            panorama.setTo(new Scalar(0, 0, 0));
            
            // 权重图，用于混合重叠区域
            Mat weights = new Mat(PANORAMA_HEIGHT, PANORAMA_WIDTH, CvType.CV_32FC1);
            weights.setTo(new Scalar(0));
            
            // 将每个摄像头的图像投影到全景图
            for (int camera = 0; camera < 4; camera++) {
                if (cameraImages[camera] != null) {
                    projectCameraToSphere(cameraImages[camera], camera, panorama, weights);
                }
            }
            
            // 标准化权重
            normalizeWeights(panorama, weights);
            
            // 应用后处理
            Mat processed = postProcessPanorama(panorama);
            
            // 转换为Bitmap
            Bitmap resultBitmap = Bitmap.createBitmap(PANORAMA_WIDTH, PANORAMA_HEIGHT, Bitmap.Config.ARGB_8888);
            Utils.matToBitmap(processed, resultBitmap);
            
            // 清理资源
            panorama.release();
            weights.release();
            processed.release();
            
            return resultBitmap;
            
        } catch (Exception e) {
            Log.e(TAG, "创建球面全景图时出错", e);
            return null;
        }
    }
    
    /**
     * 将单个摄像头图像投影到球面
     */
    private void projectCameraToSphere(Bitmap cameraImage, int cameraIndex, Mat panorama, Mat weights) {
        Mat cameraMat = new Mat();
        Utils.bitmapToMat(cameraImage, cameraMat);
        
        // 调整大小
        Mat resized = new Mat();
        Size targetSize = new Size(512, 384); // 4:3比例
        Imgproc.resize(cameraMat, resized, targetSize);
        
        Mat mapX = lookupMaps[cameraIndex * 2];
        Mat mapY = lookupMaps[cameraIndex * 2 + 1];
        
        // 使用重映射投影图像
        Mat projected = new Mat();
        Imgproc.remap(resized, projected, mapX, mapY, Imgproc.INTER_LINEAR);
        
        // 创建遮罩，只处理有效像素
        Mat mask = new Mat();
        Imgproc.threshold(mapX, mask, -0.5, 1.0, Imgproc.THRESH_BINARY);
        
        // 混合到全景图中
        blendImages(projected, panorama, weights, mask, getBlendWeight(cameraIndex));
        
        cameraMat.release();
        resized.release();
        projected.release();
        mask.release();
    }
    
    /**
     * 混合图像
     */
    private void blendImages(Mat source, Mat destination, Mat weights, Mat mask, double weight) {
        for (int y = 0; y < destination.rows(); y++) {
            for (int x = 0; x < destination.cols(); x++) {
                if (mask.get(y, x)[0] > 0) {
                    double currentWeight = weights.get(y, x)[0];
                    double newWeight = currentWeight + weight;
                    
                    if (newWeight > 0) {
                        double[] srcPixel = source.get(y, x);
                        double[] dstPixel = destination.get(y, x);
                        
                        for (int c = 0; c < 3; c++) {
                            double blended = (dstPixel[c] * currentWeight + srcPixel[c] * weight) / newWeight;
                            dstPixel[c] = blended;
                        }
                        
                        destination.put(y, x, dstPixel);
                        weights.put(y, x, newWeight);
                    }
                }
            }
        }
    }
    
    /**
     * 获取混合权重
     */
    private double getBlendWeight(int cameraIndex) {
        return 1.0; // 可以根据需要调整权重
    }
    
    /**
     * 标准化权重
     */
    private void normalizeWeights(Mat panorama, Mat weights) {
        for (int y = 0; y < panorama.rows(); y++) {
            for (int x = 0; x < panorama.cols(); x++) {
                double weight = weights.get(y, x)[0];
                if (weight > 0) {
                    double[] pixel = panorama.get(y, x);
                    for (int c = 0; c < 3; c++) {
                        pixel[c] /= weight;
                    }
                    panorama.put(y, x, pixel);
                }
            }
        }
    }
    
    /**
     * 后处理全景图
     */
    private Mat postProcessPanorama(Mat input) {
        Mat output = new Mat();
        
        // 高斯模糊去噪
        Imgproc.GaussianBlur(input, output, new Size(3, 3), 0);
        
        // 对比度增强
        output.convertTo(output, -1, 1.1, 10);
        
        return output;
    }
    
    /**
     * 释放资源
     */
    public void release() {
        if (lookupMaps != null) {
            for (Mat map : lookupMaps) {
                if (map != null) {
                    map.release();
                }
            }
        }
        initialized = false;
        Log.d(TAG, "SphericalProjector资源已释放");
    }
} 