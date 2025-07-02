package com.example.mome;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
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

public class DetectActivity extends AppCompatActivity {

    private MjpegView mjpegView;

    private BlazeFaceNcnn ncnn = new BlazeFaceNcnn();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detect);
        setupFullScreen();

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        mjpegView = findViewById(R.id.mjpegView);

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

        ncnn.loadSegModel(getAssets());

        Mjpeg.newInstance()
                .open(Camera360Config.getCameraUrl(Camera360Config.CAMERA_TOP), 8)
                .subscribe(
                        inputStream -> {
                            mjpegView.setSource(inputStream);
                            mjpegView.setDisplayMode(DisplayMode.BEST_FIT);
                            mjpegView.showFps(true);

                            // 设置帧捕获监听器
                            mjpegView.setOnFrameCapturedListener(new OnFrameCapturedListener() {
                                @Override
                                public void onFrameCaptured(@NonNull Bitmap bitmap) {
                                    ncnn.detectSeg(bitmap);
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


    @Override
    protected void onStop() {
        super.onStop();
        mjpegView.stopPlayback();

    }
}