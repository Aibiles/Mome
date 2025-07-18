package com.example.mome;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceView;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.Toast;

// import androidx.activity.EdgeToEdge; // 在activity 1.5.0中不可用
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import com.example.mome.config.Camera360Config;
import com.example.mome.opencv.DistanceDetector;
import com.example.mome.opencv.TrapezoidTransform;
import com.example.mome.view.AssistLineOverlay;
import com.github.niqdev.mjpeg.DisplayMode;
import com.github.niqdev.mjpeg.Mjpeg;
import com.github.niqdev.mjpeg.MjpegView;
import com.github.niqdev.mjpeg.OnFrameCapturedListener;

import org.opencv.android.OpenCVLoader;

public class TestActivity extends AppCompatActivity {
    private static final int TIMEOUT = Camera360Config.CONNECTION_TIMEOUT;
    private static final String TAG = "MainActivity";

    MjpegView mjpegViewTop;
    MjpegView mjpegViewBottom;
    MjpegView mjpegViewLeft;
    MjpegView mjpegViewRight;

    private ImageView imageViewTop;
    private ImageView imageViewRight;
    private ImageView imageViewBottom;
    private ImageView imageViewLeft;

    // 用于存储最新的帧
    private Bitmap latestTopFrame = null;
    private Bitmap latestBottomFrame = null;
    private Bitmap latestLeftFrame = null;
    private Bitmap latestRightFrame = null;
    private FrameLayout frameLayout;

    private BlazeFaceNcnn ncnn = new BlazeFaceNcnn();
    private ImageView ivTest;

    private AssistLineOverlay assistLineOverlay;
    private DistanceDetector distanceDetector;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // EdgeToEdge.enable(this); // 在activity 1.5.0中不可用
        setupFullScreen();
        setContentView(R.layout.activity_test);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        Log.i(TAG, "onCreate: 初始化中");
        ncnn.loadSegModel(getAssets());
        Log.i(TAG, "onCreate: 初始化成功");


        initViews();
    }

    private void setupFullScreen() {
        // 使用兼容库方法
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);

        // 设置沉浸式控制器
        WindowInsetsControllerCompat controller = WindowCompat.getInsetsController(
                getWindow(),
                getWindow().getDecorView()
        );

        if (controller != null) {
            controller.hide(WindowInsetsCompat.Type.systemBars());
            controller.setSystemBarsBehavior(
                    WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            );
        }
    }

    private void initViews() {
//        mjpegViewTop = findViewById(R.id.mjpegViewTop);
//        mjpegViewBottom = findViewById(R.id.mjpegViewBottom);
//        mjpegViewLeft = findViewById(R.id.mjpegViewLeft);
//        mjpegViewRight = findViewById(R.id.mjpegViewRight);
//
//        frameLayout = findViewById(R.id.camera_frame);
//
//        imageViewTop = findViewById(R.id.imageViewTop);
//        imageViewBottom = findViewById(R.id.imageViewBottom);
//        imageViewLeft = findViewById(R.id.imageViewLeft);
//        imageViewRight = findViewById(R.id.imageViewRight);

        ivTest = findViewById(R.id.iv_test);

//        Bitmap bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.test);

//        // 初始化辅助线覆盖层
//        assistLineOverlay = findViewById(R.id.assistLineOverlay);
//        assistLineOverlay.startDrawing(AssistLineOverlay.LineType.REVERSE);
//        assistLineOverlay.enableDistanceDetection();
//
//        // 初始化距离检测器
//        distanceDetector = distanceDetector = new DistanceDetector();
//
////        DistanceDetector.DetectionResult result = distanceDetector.detectDistance(bitmap);
//
//        result.minDistance = 3.4;
//
//        assistLineOverlay.updateDistanceDetectionResult(result);

//        Bitmap bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.test);
//
//        applyTrapezoidTransformAndDisplay(bitmap, Camera360Config.CAMERA_TOP, ivTest);



//                                    runOnUiThread(() -> {
//                                        if (imageViewTop != null) {
//                                            imageViewTop.setImageBitmap(latestTopFrame);
//                                        }
//                                    });

    }

    /**
     * 应用梯形变换到指定摄像头的图像
     * @param bitmap 原始图像
     * @param cameraPosition 摄像头位置（0=顶部, 1=右侧, 2=底部, 3=左侧）
     * @param targetImageView 目标显示ImageView
     */
    private void applyTrapezoidTransformAndDisplay(Bitmap bitmap, int cameraPosition, ImageView targetImageView) {
        if (bitmap == null || targetImageView == null) return;

        new Thread(() -> {
            try {
                // 使用TrapezoidTransform工具类进行变换
                TrapezoidTransform.TrapezoidDirection direction =
                        TrapezoidTransform.getCameraTransformDirection(cameraPosition);
                double shrinkRatio = TrapezoidTransform.getDefaultShrinkRatio(cameraPosition);

                Bitmap transformedBitmap = TrapezoidTransform.applyTrapezoidTransform(
                        bitmap, direction, shrinkRatio, cameraPosition);

                // 在主线程更新UI
                runOnUiThread(() -> {
                    if (transformedBitmap != null) {
                        targetImageView.setImageBitmap(transformedBitmap);
                    }
                });

            } catch (Exception e) {
                Log.e(TAG, "梯形变换失败: " + Camera360Config.getCameraName(cameraPosition), e);
            }
        }).start();
    }

    private void loadIpCamTop() {
        Mjpeg.newInstance()
                .open(Camera360Config.getCameraUrl(Camera360Config.CAMERA_TOP), TIMEOUT)
                .subscribe(
                        inputStream -> {
                            mjpegViewTop.setSource(inputStream);
                            mjpegViewTop.setDisplayMode(DisplayMode.BEST_FIT);
                            mjpegViewTop.showFps(true);

                            // 设置帧捕获监听器
                            mjpegViewTop.setOnFrameCapturedListener(new OnFrameCapturedListener() {
                                @Override
                                public void onFrameCaptured(@NonNull Bitmap bitmap) {
                                    latestTopFrame = bitmap.copy(bitmap.getConfig(), false);
                                    applyTrapezoidTransformAndDisplay(bitmap, Camera360Config.CAMERA_TOP, imageViewTop);

//                                    ncnn.detectSeg(bitmap, );
//                                    // 应用梯形变换并显示
//                                    runOnUiThread(() -> {
//                                        if (imageViewTop != null) {
//                                            imageViewTop.setImageBitmap(latestTopFrame);
//                                        }
//                                    });
//                                    applyTrapezoidTransformAndDisplay(ncnn.detectYolov8Seg(latestTopFrame), Camera360Config.CAMERA_TOP, imageViewTop);
                                }

                                @Override
                                public void onFrameCapturedWithHeader(@NonNull byte[] bytes, @NonNull byte[] bytes1) {
                                    // 不使用
                                }
                            });
                        },
                        throwable -> {
                            Log.e(getClass().getSimpleName(), "mjpeg error", throwable);
                            Toast.makeText(this, Camera360Config.getCameraName(Camera360Config.CAMERA_TOP) + "连接错误", Toast.LENGTH_LONG).show();
                        });
    }

    private void loadIpCamBottom() {
        Mjpeg.newInstance()
                .open(Camera360Config.getCameraUrl(Camera360Config.CAMERA_BOTTOM), TIMEOUT)
                .subscribe(
                        inputStream -> {
                            mjpegViewBottom.setSource(inputStream);
                            mjpegViewBottom.setDisplayMode(DisplayMode.SCALE_FIT);
                            mjpegViewBottom.showFps(true);

                            // 设置帧捕获监听器
                            mjpegViewBottom.setOnFrameCapturedListener(new OnFrameCapturedListener() {
                                @Override
                                public void onFrameCaptured(@NonNull Bitmap bitmap) {
                                    latestBottomFrame = bitmap.copy(bitmap.getConfig(), false);
                                    // 应用梯形变换并显示
                                    applyTrapezoidTransformAndDisplay(bitmap, Camera360Config.CAMERA_BOTTOM, imageViewBottom);
                                }

                                @Override
                                public void onFrameCapturedWithHeader(@NonNull byte[] bytes, @NonNull byte[] bytes1) {
                                    // 不使用
                                }
                            });
                        },
                        throwable -> {
                            Log.e(getClass().getSimpleName(), "mjpeg error", throwable);
                            Toast.makeText(this, Camera360Config.getCameraName(Camera360Config.CAMERA_BOTTOM) + "连接错误", Toast.LENGTH_LONG).show();
                        });
    }

    private void loadIpCamLeft() {
        Mjpeg.newInstance()
                .open(Camera360Config.getCameraUrl(Camera360Config.CAMERA_LEFT), TIMEOUT)
                .subscribe(
                        inputStream -> {
                            mjpegViewLeft.setSource(inputStream);
                            mjpegViewLeft.setDisplayMode(DisplayMode.BEST_FIT);
                            mjpegViewLeft.showFps(false);

                            // 设置帧捕获监听器
                            mjpegViewLeft.setOnFrameCapturedListener(new OnFrameCapturedListener() {
                                @Override
                                public void onFrameCaptured(@NonNull Bitmap bitmap) {
                                    latestLeftFrame = bitmap.copy(bitmap.getConfig(), false);
                                    // 应用梯形变换并显示
                                    applyTrapezoidTransformAndDisplay(bitmap, Camera360Config.CAMERA_LEFT, imageViewLeft);

//                                    latestLeftFrame = bitmap.copy(bitmap.getConfig(), false);
//                                    ncnn.detectSeg(bitmap);
//                                     //应用梯形变换并显示
//                                    runOnUiThread(() -> {
//                                        if (imageViewLeft != null) {
//                                            imageViewLeft.setImageBitmap(latestLeftFrame);
//                                        }
//                                    });
//                                    applyTrapezoidTransformAndDisplay(ncnn.detectYolov8Seg(latestTopFrame), Camera360Config.CAMERA_LEFT, imageViewLeft);
                                }

                                @Override
                                public void onFrameCapturedWithHeader(@NonNull byte[] bytes, @NonNull byte[] bytes1) {
                                    // 不使用
                                }
                            });
                        },
                        throwable -> {
                            Log.e(getClass().getSimpleName(), "mjpeg error", throwable);
                            Toast.makeText(this, Camera360Config.getCameraName(Camera360Config.CAMERA_LEFT) + "连接错误", Toast.LENGTH_LONG).show();
                        });
    }

    private void loadIpCamRight() {
        Mjpeg.newInstance()
                .open(Camera360Config.getCameraUrl(Camera360Config.CAMERA_RIGHT), TIMEOUT)
                .subscribe(
                        inputStream -> {
                            mjpegViewRight.setSource(inputStream);
                            mjpegViewRight.setDisplayMode(DisplayMode.SCALE_FIT);
                            mjpegViewRight.showFps(true);

                            // 设置帧捕获监听器
                            mjpegViewRight.setOnFrameCapturedListener(new OnFrameCapturedListener() {
                                @Override
                                public void onFrameCaptured(@NonNull Bitmap bitmap) {
                                    latestRightFrame = bitmap.copy(bitmap.getConfig(), false);
                                    // 应用梯形变换并显示
                                    applyTrapezoidTransformAndDisplay(bitmap, Camera360Config.CAMERA_RIGHT, imageViewRight);
                                }

                                @Override
                                public void onFrameCapturedWithHeader(@NonNull byte[] bytes, @NonNull byte[] bytes1) {
                                    // 不使用
                                }
                            });
                        },
                        throwable -> {
                            Log.e(getClass().getSimpleName(), "mjpeg error", throwable);
                            Toast.makeText(this, Camera360Config.getCameraName(Camera360Config.CAMERA_RIGHT) + "连接错误", Toast.LENGTH_LONG).show();
                        });
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (OpenCVLoader.initLocal()) {
            Log.i(TAG, "OpenCV loaded successfully");
        } else {
            Log.e(TAG, "OpenCV initialization failed!");
            Toast.makeText(this, "OpenCV initialization failed!", Toast.LENGTH_LONG).show();
            return;
        }

        // 加载摄像头流
//        loadIpCamTop();
//        loadIpCamBottom();
//        loadIpCamLeft();
//        loadIpCamRight();
    }

    @Override
    protected void onPause() {
        super.onPause();

        // 停止MJPEG流
        if (mjpegViewTop != null) mjpegViewTop.stopPlayback();
        if (mjpegViewBottom != null) mjpegViewBottom.stopPlayback();
        if (mjpegViewLeft != null) mjpegViewLeft.stopPlayback();
        if (mjpegViewRight != null) mjpegViewRight.stopPlayback();

        if (assistLineOverlay != null) {
            assistLineOverlay.clearLines();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        // 清理bitmap资源
        if (latestTopFrame != null && !latestTopFrame.isRecycled()) {
            latestTopFrame.recycle();
        }
        if (latestBottomFrame != null && !latestBottomFrame.isRecycled()) {
            latestBottomFrame.recycle();
        }
        if (latestLeftFrame != null && !latestLeftFrame.isRecycled()) {
            latestLeftFrame.recycle();
        }
        if (latestRightFrame != null && !latestRightFrame.isRecycled()) {
            latestRightFrame.recycle();
        }
        if (distanceDetector != null) {
            distanceDetector.release();
        }
        if (assistLineOverlay != null) {
            assistLineOverlay.cleanup();
        }

    }
}