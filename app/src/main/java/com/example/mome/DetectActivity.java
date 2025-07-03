package com.example.mome;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import com.example.mome.config.Camera360Config;
import com.github.niqdev.mjpeg.DisplayMode;
import com.github.niqdev.mjpeg.Mjpeg;
import com.github.niqdev.mjpeg.MjpegView;
import com.github.niqdev.mjpeg.OnFrameCapturedListener;

import java.util.Timer;

public class DetectActivity extends AppCompatActivity {

    private SurfaceView surfaceView;
    private BlazeFaceNcnn ncnn = new BlazeFaceNcnn();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detect);
        setupFullScreen();

        surfaceView = findViewById(R.id.sv_face);
        surfaceView.getHolder().addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(@NonNull SurfaceHolder surfaceHolder) {
                ncnn.setOutputWindow(surfaceHolder.getSurface());

            }

            @Override
            public void surfaceChanged(@NonNull SurfaceHolder surfaceHolder, int i, int i1, int i2) {

            }

            @Override
            public void surfaceDestroyed(@NonNull SurfaceHolder surfaceHolder) {

            }
        });

        ncnn.loadSegModel(getAssets());

//        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
//            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
//            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
//            return insets;
//        });


    }

    /**
     * 设置全屏显示
     */
    private void setupFullScreen() {
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        WindowInsetsControllerCompat controller = WindowCompat.getInsetsController(
                getWindow(), getWindow().getDecorView());
        controller.hide(WindowInsetsCompat.Type.systemBars());
        controller.setSystemBarsBehavior(
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (BlazeFaceNcnn.isOpenCVLoaded()) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                ncnn.openCamera();
            } else {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, 10);
            }
        } else {
            // OpenCV未加载，等待一段时间后重试
            surfaceView.postDelayed(new Runnable() {
                @Override
                public void run() {
                    onResume();
                }
            }, 1000);
        }

//        ncnn.loadSegModel(getAssets());
//
//        Mjpeg.newInstance()
//                .open(Camera360Config.getCameraUrl(Camera360Config.CAMERA_TOP), 8)
//                .subscribe(
//                        inputStream -> {
//                            mjpegView.setSource(inputStream);
//                            mjpegView.setDisplayMode(DisplayMode.BEST_FIT);
//                            mjpegView.showFps(true);
//
//                            // 设置帧捕获监听器
//                            mjpegView.setOnFrameCapturedListener(new OnFrameCapturedListener() {
//                                @Override
//                                public void onFrameCaptured(@NonNull Bitmap bitmap) {
//                                    ncnn.detectSeg(bitmap);
//                                }
//
//                                @Override
//                                public void onFrameCapturedWithHeader(@NonNull byte[] bytes, @NonNull byte[] bytes1) {
//                                    // 不使用
//                                }
//                            });
//                        },
//                        throwable -> {
//                            Log.e(getClass().getSimpleName(), "mjpeg error", throwable);
//                            Toast.makeText(this, Camera360Config.getCameraName(Camera360Config.CAMERA_TOP) + "连接错误", Toast.LENGTH_LONG).show();
//                        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (BlazeFaceNcnn.isOpenCVLoaded()) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                ncnn.openCamera();
            } else {
                finish();
            }
        } else {
            // OpenCV未加载完成，延迟打开相机
            surfaceView.postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (ActivityCompat.checkSelfPermission(DetectActivity.this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                        ncnn.openCamera();
                    }
                }
            }, 1000);
        }
    }


    @Override
    protected void onPause() {
        super.onPause();
        ncnn.closeCamera();
    }
}