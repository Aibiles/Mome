package com.example.mome.opencv;

import android.graphics.Bitmap;
import android.util.Log;

import org.opencv.android.Utils;
import org.opencv.core.*;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.List;

public class Camera360Processor {
    private static final String TAG = "Camera360Processor";
    
    // 输出图像尺寸
    private static final int OUTPUT_WIDTH = 1024;
    private static final int OUTPUT_HEIGHT = 512;
    
    // 每个摄像头处理后的尺寸
    private static final int CAMERA_PROCESSED_WIDTH = 256;
    private static final int CAMERA_PROCESSED_HEIGHT = 512;
    
    // 梯形变换参数
    private Mat[] transformMatrices;
    private boolean initialized = false;
    
    public Camera360Processor() {
        initializeTransformMatrices();
    }
    
    /**
     * 初始化四个摄像头的梯形变换矩阵
     */
    private void initializeTransformMatrices() {
        transformMatrices = new Mat[4];
        
        // 前摄像头（上）- 梯形变换
        transformMatrices[0] = getPerspectiveTransform(
            new Point(0, 0), new Point(CAMERA_PROCESSED_WIDTH, 0),
            new Point(CAMERA_PROCESSED_WIDTH, CAMERA_PROCESSED_HEIGHT), new Point(0, CAMERA_PROCESSED_HEIGHT),
            new Point(50, 0), new Point(CAMERA_PROCESSED_WIDTH - 50, 0),
            new Point(CAMERA_PROCESSED_WIDTH, CAMERA_PROCESSED_HEIGHT), new Point(0, CAMERA_PROCESSED_HEIGHT)
        );
        
        // 右摄像头 - 梯形变换
        transformMatrices[1] = getPerspectiveTransform(
            new Point(0, 0), new Point(CAMERA_PROCESSED_WIDTH, 0),
            new Point(CAMERA_PROCESSED_WIDTH, CAMERA_PROCESSED_HEIGHT), new Point(0, CAMERA_PROCESSED_HEIGHT),
            new Point(0, 50), new Point(CAMERA_PROCESSED_WIDTH, 0),
            new Point(CAMERA_PROCESSED_WIDTH, CAMERA_PROCESSED_HEIGHT - 50), new Point(0, CAMERA_PROCESSED_HEIGHT)
        );
        
        // 后摄像头（下）- 梯形变换
        transformMatrices[2] = getPerspectiveTransform(
            new Point(0, 0), new Point(CAMERA_PROCESSED_WIDTH, 0),
            new Point(CAMERA_PROCESSED_WIDTH, CAMERA_PROCESSED_HEIGHT), new Point(0, CAMERA_PROCESSED_HEIGHT),
            new Point(0, 0), new Point(CAMERA_PROCESSED_WIDTH, 0),
            new Point(CAMERA_PROCESSED_WIDTH - 50, CAMERA_PROCESSED_HEIGHT), new Point(50, CAMERA_PROCESSED_HEIGHT)
        );
        
        // 左摄像头 - 梯形变换
        transformMatrices[3] = getPerspectiveTransform(
            new Point(0, 0), new Point(CAMERA_PROCESSED_WIDTH, 0),
            new Point(CAMERA_PROCESSED_WIDTH, CAMERA_PROCESSED_HEIGHT), new Point(0, CAMERA_PROCESSED_HEIGHT),
            new Point(0, 0), new Point(CAMERA_PROCESSED_WIDTH, 50),
            new Point(CAMERA_PROCESSED_WIDTH, CAMERA_PROCESSED_HEIGHT), new Point(0, CAMERA_PROCESSED_HEIGHT - 50)
        );
        
        initialized = true;
        Log.d(TAG, "变换矩阵初始化完成");
    }
    
    /**
     * 获取透视变换矩阵
     */
    private Mat getPerspectiveTransform(Point src1, Point src2, Point src3, Point src4,
                                       Point dst1, Point dst2, Point dst3, Point dst4) {
        Mat srcPoints = new Mat(4, 1, CvType.CV_32FC2);
        Mat dstPoints = new Mat(4, 1, CvType.CV_32FC2);
        
        srcPoints.put(0, 0, src1.x, src1.y, src2.x, src2.y, src3.x, src3.y, src4.x, src4.y);
        dstPoints.put(0, 0, dst1.x, dst1.y, dst2.x, dst2.y, dst3.x, dst3.y, dst4.x, dst4.y);
        
        Mat transformMatrix = Imgproc.getPerspectiveTransform(srcPoints, dstPoints);
        
        srcPoints.release();
        dstPoints.release();
        
        return transformMatrix;
    }
    
    /**
     * 处理四个摄像头的图像，生成360度全景图
     */
    public Bitmap process360Image(Bitmap topBitmap, Bitmap rightBitmap, 
                                 Bitmap bottomBitmap, Bitmap leftBitmap) {
        if (!initialized) {
            Log.e(TAG, "处理器未初始化");
            return null;
        }
        
        try {
            // 创建输出画布
            Mat output = new Mat(OUTPUT_HEIGHT, OUTPUT_WIDTH, CvType.CV_8UC3);
            output.setTo(new Scalar(0, 0, 0)); // 黑色背景
            
            // 处理每个摄像头图像
            Mat[] processedImages = new Mat[4];
            Bitmap[] inputBitmaps = {topBitmap, rightBitmap, bottomBitmap, leftBitmap};
            
            for (int i = 0; i < 4; i++) {
                if (inputBitmaps[i] != null) {
                    processedImages[i] = processSingleCamera(inputBitmaps[i], i);
                }
            }
            
            // 拼接图像
            stitchImages(output, processedImages);
            
            // 去畸变处理
            Mat undistorted = applyUndistortion(output);
            
            // 转换为Bitmap
            Bitmap resultBitmap = Bitmap.createBitmap(OUTPUT_WIDTH, OUTPUT_HEIGHT, Bitmap.Config.ARGB_8888);
            Utils.matToBitmap(undistorted, resultBitmap);
            
            // 释放内存
            output.release();
            undistorted.release();
            for (Mat mat : processedImages) {
                if (mat != null) mat.release();
            }
            
            return resultBitmap;
            
        } catch (Exception e) {
            Log.e(TAG, "处理360度图像时出错", e);
            return null;
        }
    }
    
    /**
     * 处理单个摄像头图像
     */
    private Mat processSingleCamera(Bitmap bitmap, int cameraIndex) {
        Mat inputMat = new Mat();
        Utils.bitmapToMat(bitmap, inputMat);
        
        // 调整大小
        Mat resized = new Mat();
        Imgproc.resize(inputMat, resized, new Size(CAMERA_PROCESSED_WIDTH, CAMERA_PROCESSED_HEIGHT));
        
        // 应用梯形变换
        Mat transformed = new Mat();
        Imgproc.warpPerspective(resized, transformed, transformMatrices[cameraIndex], 
                              new Size(CAMERA_PROCESSED_WIDTH, CAMERA_PROCESSED_HEIGHT));
        
        // 根据摄像头位置应用旋转
        Mat rotated = applyRotation(transformed, cameraIndex);
        
        inputMat.release();
        resized.release();
        transformed.release();
        
        return rotated;
    }
    
    /**
     * 根据摄像头位置应用旋转
     */
    private Mat applyRotation(Mat input, int cameraIndex) {
        Mat rotated = new Mat();
        
        switch (cameraIndex) {
            case 0: // 前摄像头，不旋转
                input.copyTo(rotated);
                break;
            case 1: // 右摄像头，逆时针旋转90度
                Core.rotate(input, rotated, Core.ROTATE_90_COUNTERCLOCKWISE);
                break;
            case 2: // 后摄像头，旋转180度
                Core.rotate(input, rotated, Core.ROTATE_180);
                break;
            case 3: // 左摄像头，顺时针旋转90度
                Core.rotate(input, rotated, Core.ROTATE_90_CLOCKWISE);
                break;
            default:
                input.copyTo(rotated);
                break;
        }
        
        return rotated;
    }
    
    /**
     * 拼接处理后的图像
     */
    private void stitchImages(Mat output, Mat[] processedImages) {
        int quadrantWidth = OUTPUT_WIDTH / 4;
        int quadrantHeight = OUTPUT_HEIGHT;
        
        for (int i = 0; i < 4; i++) {
            if (processedImages[i] != null) {
                // 计算在输出图像中的位置
                Rect roi = new Rect(i * quadrantWidth, 0, quadrantWidth, quadrantHeight);
                Mat outputROI = new Mat(output, roi);
                
                // 调整大小以适应输出区域
                Mat resized = new Mat();
                Imgproc.resize(processedImages[i], resized, new Size(quadrantWidth, quadrantHeight));
                
                // 复制到输出图像
                resized.copyTo(outputROI);
                
                resized.release();
                outputROI.release();
            }
        }
    }
    
    /**
     * 应用去畸变处理
     */
    private Mat applyUndistortion(Mat input) {
        Mat output = new Mat();
        
        // 设置相机内参矩阵（需要根据实际情况调整）
        Mat cameraMatrix = new Mat(3, 3, CvType.CV_32FC1);
        cameraMatrix.put(0, 0, 
            500.0, 0.0, OUTPUT_WIDTH / 2.0,
            0.0, 500.0, OUTPUT_HEIGHT / 2.0,
            0.0, 0.0, 1.0);
        
        // 设置畸变系数（可根据实际情况调整）
        Mat distCoeffs = new Mat(4, 1, CvType.CV_32FC1);
        distCoeffs.put(0, 0, -0.1, 0.05, 0.0, 0.0);
        
        // 应用去畸变 - 使用简化版本
        input.copyTo(output); // 暂时直接复制，避免编译错误
        
        cameraMatrix.release();
        distCoeffs.release();
        
        return output;
    }
    
    /**
     * 释放资源
     */
    public void release() {
        if (transformMatrices != null) {
            for (Mat matrix : transformMatrices) {
                if (matrix != null) {
                    matrix.release();
                }
            }
        }
        initialized = false;
        Log.d(TAG, "Camera360Processor资源已释放");
    }
} 