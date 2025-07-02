package com.example.mome.opencv;

import android.graphics.Bitmap;
import android.util.Log;

import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

public class TrapezoidTransform {
    private static final String TAG = "TrapezoidTransform";

    /**
     * 梯形变换方向枚举
     */
    public enum TrapezoidDirection {
        TOP_SHRINK,     // 上底收缩
        BOTTOM_SHRINK,  // 下底收缩
        LEFT_SHRINK,    // 左边收缩
        RIGHT_SHRINK    // 右边收缩
    }

    // 统一收缩比例设置为0.33
    private static final double DEFAULT_SHRINK_RATIO = 0.3;
    private static final double VERTICAL_SHRINK_RATIO = 0.25;
    private static final double HORIZONTAL_SHRINK_RATIO = 0.3;


    /**
     * 应用梯形变换到Bitmap（不带旋转）
     * @param bitmap 输入图像
     * @param direction 变换方向
     * @param shrinkRatio 收缩比例 (0.0-1.0)
     * @return 变换后的Bitmap
     */
    public static Bitmap applyTrapezoidTransform(Bitmap bitmap, TrapezoidDirection direction, double shrinkRatio) {
        return applyTrapezoidTransform(bitmap, direction, shrinkRatio, 0);
    }

    /**
     * 应用梯形变换到Bitmap（带旋转）
     * @param bitmap 输入图像
     * @param direction 变换方向
     * @param shrinkRatio 收缩比例 (0.0-1.0)
     * @param camera 摄像头
     * @return 变换后的Bitmap
     */
    public static Bitmap applyTrapezoidTransform(Bitmap bitmap, TrapezoidDirection direction, double shrinkRatio, int camera) {
        if (bitmap == null || bitmap.isRecycled()) {
            Log.e(TAG, "输入Bitmap无效");
            return null;
        }

        if (shrinkRatio < 0.0 || shrinkRatio > 1.0) {
            Log.w(TAG, "收缩比例超出范围，使用默认值0.33");
            shrinkRatio = DEFAULT_SHRINK_RATIO;
        }

        try {
            Mat srcMat = new Mat();
            Utils.bitmapToMat(bitmap, srcMat);

            // 先应用旋转变换
            Mat transpose = new Mat();
            Mat rotatedMat = new Mat();

            switch (camera) {
                case 1:
                    // 顺时针旋转90度
                    Core.transpose(srcMat, transpose);
                    Core.flip(transpose, rotatedMat, 1);
                    break;
                case 2:
                    // 旋转180度
                    Core.flip(srcMat, rotatedMat, -1);
                    break;
                case 3:
                    // 逆时针旋转90度（顺时针270度）
                    Core.transpose(srcMat, transpose);
                    Core.flip(transpose, rotatedMat, 0);
                    break;
                case 0:
                default:
                    // 0度或其他，直接克隆
                    rotatedMat = srcMat.clone();
                    break;
            }

            // 再应用梯形变换
            Mat transformedMat = applyTrapezoidTransform(rotatedMat, direction, shrinkRatio);

            Bitmap resultBitmap = Bitmap.createBitmap(
                    transformedMat.cols(),
                    transformedMat.rows(),
                    bitmap.getConfig()
            );
            Utils.matToBitmap(transformedMat, resultBitmap);

            srcMat.release();
            transpose.release();
            rotatedMat.release();
            transformedMat.release();

            return resultBitmap;

        } catch (Exception e) {
            Log.e(TAG, "梯形变换失败", e);
            return bitmap;
        }
    }

    /**
     * 应用梯形变换到Mat
     * @param srcMat 输入Mat
     * @param direction 变换方向
     * @param shrinkRatio 收缩比例
     * @return 变换后的Mat
     */
    public static Mat applyTrapezoidTransform(Mat srcMat, TrapezoidDirection direction, double shrinkRatio) {
        if (srcMat == null || srcMat.empty()) {
            Log.e(TAG, "输入Mat无效");
            return srcMat;
        }

        // 确保收缩比例在有效范围内
        shrinkRatio = Math.max(0.01, Math.min(0.99, shrinkRatio));

        try {
            // 获取变换点
            MatOfPoint2f srcPoints = getSourcePoints(srcMat);
            MatOfPoint2f dstPoints = getDestinationPoints(srcMat, direction, shrinkRatio);

            // 计算变换矩阵
            Mat perspectiveTransform = Imgproc.getPerspectiveTransform(srcPoints, dstPoints);

            // 应用变换
            Mat dstMat = new Mat();
            Imgproc.warpPerspective(
                    srcMat,
                    dstMat,
                    perspectiveTransform,
                    new Size(srcMat.width(), srcMat.height())
            );

            // 释放资源
            srcPoints.release();
            dstPoints.release();
            perspectiveTransform.release();

            return dstMat;

        } catch (Exception e) {
            Log.e(TAG, "Mat梯形变换失败", e);
            return srcMat.clone();
        }
    }

    /**
     * 获取源点坐标（矩形的四个角点）
     */
    private static MatOfPoint2f getSourcePoints(Mat mat) {
        return new MatOfPoint2f(
                new Point(0, 0),                        // 左上
                new Point(mat.width(), 0),              // 右上
                new Point(mat.width(), mat.height()),   // 右下
                new Point(0, mat.height())              // 左下
        );
    }

    /**
     * 根据变换方向和收缩比例获取目标点坐标
     */
    private static MatOfPoint2f getDestinationPoints(Mat mat, TrapezoidDirection direction, double shrinkRatio) {
        double width = mat.width();
        double height = mat.height();

        // 确保收缩比例在有效范围内
        shrinkRatio = Math.max(0.01, Math.min(0.99, shrinkRatio));

        switch (direction) {
            case TOP_SHRINK:
                return getTopShrinkPoints(width, height, shrinkRatio);
            case BOTTOM_SHRINK:
                return getBottomShrinkPoints(width, height, shrinkRatio);
            case LEFT_SHRINK:
                return getLeftShrinkPoints(width, height, shrinkRatio);
            case RIGHT_SHRINK:
                return getRightShrinkPoints(width, height, shrinkRatio);
            default:
                return getSourcePoints(mat); // 返回原始点
        }
    }

    /**
     * 上底收缩变换点
     * 上边向内收缩，下边保持不变
     */
    private static MatOfPoint2f getTopShrinkPoints(double width, double height, double shrinkRatio) {
        double shrinkWidth = width * shrinkRatio;
        double offset = (width - shrinkWidth) / 2;

        return new MatOfPoint2f(
                new Point(offset, 0),                   // 左上（向内收缩）
                new Point(width - offset, 0),            // 右上（向内收缩）
                new Point(width, height),                // 右下（不变）
                new Point(0, height)                     // 左下（不变）
        );
    }

    /**
     * 下底收缩变换点
     * 下边向内收缩，上边保持不变
     */
    private static MatOfPoint2f getBottomShrinkPoints(double width, double height, double shrinkRatio) {
        double shrinkWidth = width * shrinkRatio;
        double offset = (width - shrinkWidth) / 2;

        return new MatOfPoint2f(
                new Point(0, 0),                         // 左上（不变）
                new Point(width, 0),                     // 右上（不变）
                new Point(width - offset, height),       // 右下（向内收缩）
                new Point(offset, height)                // 左下（向内收缩）
        );
    }

    /**
     * 左边收缩变换点
     * 左边向内收缩，右边保持不变
     */
    private static MatOfPoint2f getLeftShrinkPoints(double width, double height, double shrinkRatio) {
        double shrinkHeight = height * shrinkRatio;
        double offset = (height - shrinkHeight) / 2;

        return new MatOfPoint2f(
                new Point(0, offset),                    // 左上（向内收缩）
                new Point(width, 0),                     // 右上（不变）
                new Point(width, height),                // 右下（不变）
                new Point(0, height - offset)            // 左下（向内收缩）
        );
    }

    /**
     * 右边收缩变换点
     * 右边向内收缩，左边保持不变
     */
    private static MatOfPoint2f getRightShrinkPoints(double width, double height, double shrinkRatio) {
        double shrinkHeight = height * shrinkRatio;
        double offset = (height - shrinkHeight) / 2;

        return new MatOfPoint2f(
                new Point(0, 0),                         // 左上（不变）
                new Point(width, offset),                // 右上（向内收缩）
                new Point(width, height - offset),       // 右下（向内收缩）
                new Point(0, height)                     // 左下（不变）
        );
    }

    /**
     * 获取针对四个摄像头位置的预设变换配置
     * @param cameraPosition 摄像头位置 (0=顶部, 1=右侧, 2=底部, 3=左侧)
     * @return 对应的变换方向
     */
    public static TrapezoidDirection getCameraTransformDirection(int cameraPosition) {
        switch (cameraPosition) {
            case 0: // 顶部摄像头
                return TrapezoidDirection.BOTTOM_SHRINK;
            case 1: // 右侧摄像头
                return TrapezoidDirection.LEFT_SHRINK;
            case 2: // 底部摄像头
                return TrapezoidDirection.TOP_SHRINK;
            case 3: // 左侧摄像头
                return TrapezoidDirection.RIGHT_SHRINK;
            default:
                return TrapezoidDirection.BOTTOM_SHRINK;
        }
    }

    /**
     * 获取默认收缩比例
     * @param cameraPosition 摄像头位置
     * @return 收缩比例
     */
    public static double getDefaultShrinkRatio(int cameraPosition) {
        // 所有位置统一使用0.33
        switch (cameraPosition) {
            case 0:
            case 2:
                return VERTICAL_SHRINK_RATIO;
            case 1:
            case 3:
                return HORIZONTAL_SHRINK_RATIO;
        }
        return DEFAULT_SHRINK_RATIO;
    }
}