package com.example.mome;

import static android.view.View.VISIBLE;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.core.app.ActivityCompat;

import com.example.mome.config.Camera360Config;
import com.github.niqdev.mjpeg.DisplayMode;
import com.github.niqdev.mjpeg.Mjpeg;
import com.github.niqdev.mjpeg.MjpegView;
import com.github.niqdev.mjpeg.OnFrameCapturedListener;

import java.util.Timer;
import java.util.TimerTask;

public class BlazeActivity extends AppCompatActivity {
    private MjpegView mjpegView;
    private BlazeFaceNcnn ncnn = new BlazeFaceNcnn();
    private MediaPlayer player;
    private ImageView ivWarn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_blaze);

        getWindow().getDecorView().setSystemUiVisibility(5894);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        player = MediaPlayer.create(this, R.raw.warring);
        player.setLooping(true);
        player.setVolume(0,0);
        player.start();

        ivWarn = findViewById(R.id.iv_warn);
        mjpegView = findViewById(R.id.mjpegView);

        ncnn.loadModel(getAssets(), 0, 0);
    }

    @Override
    protected void onResume() {
        super.onResume();
        Mjpeg.newInstance()
            .open(Camera360Config.getCameraUrl(Camera360Config.CAMERA_TOP), 8)
                .subscribe(
                    inputStream -> {
                        mjpegView.setSource(inputStream);
                        mjpegView.setDisplayMode(DisplayMode.FULLSCREEN);
                        mjpegView.showFps(false);

                        // 设置帧捕获监听器
                        mjpegView.setOnFrameCapturedListener(new OnFrameCapturedListener() {
                            @Override
                            public void onFrameCaptured(@NonNull Bitmap bitmap) {
                                ncnn.detect(bitmap);
                                runOnUiThread(()-> {
                                    if (ncnn.isWarn()) {
                                        player.setVolume(1, 1);
                                        if (ivWarn.getVisibility() != View.VISIBLE) {
                                            ivWarn.setVisibility(View.VISIBLE);
                                        }
                                    } else {
                                        player.setVolume(0, 0);
                                        if (ivWarn.getVisibility() == View.VISIBLE) {
                                            ivWarn.setVisibility(View.GONE);
                                        }
                                    }
                                });
                            }

                            @Override
                            public void onFrameCapturedWithHeader(@NonNull byte[] bytes, @NonNull byte[] bytes1) {
                                // 不使用
                            }
                        });
                    },
                    throwable -> {
                        Log.e(getClass().getSimpleName(), "mjpeg error", throwable);
                        Toast.makeText(this, Camera360Config.CAMERA_FACE + "连接错误", Toast.LENGTH_LONG).show();
                    });
    }


    @Override
    protected void onStop() {
        super.onStop();
//        mjpegView.stopPlayback();

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (player != null) {
            player.pause();
            player.release();
        }
    }
}