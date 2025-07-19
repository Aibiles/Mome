package com.example.mome;

import android.Manifest;
import android.content.pm.PackageManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import java.util.Timer;
import java.util.TimerTask;

public class BlazeActivity extends AppCompatActivity {
    private SurfaceView surfaceView;

    private BlazeFaceNcnn ncnn = new BlazeFaceNcnn();
    private Timer timer;
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

        surfaceView = findViewById(R.id.surface);
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

        ncnn.loadModel(getAssets(), 0, 0);

        timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                if (ncnn.isWarn()) {
                    runOnUiThread(()-> {
                        player.setVolume(1, 1);
                        if (ivWarn.getVisibility() != View.VISIBLE) {
                            ivWarn.setVisibility(View.VISIBLE);
                        }
                    });
                } else {
                    runOnUiThread(()-> {
                        player.setVolume(0, 0);
                        if (ivWarn.getVisibility() == View.VISIBLE) {
                            ivWarn.setVisibility(View.GONE);
                        }
                    });


                }
            }
        }, 0, 500);
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
                    if (ActivityCompat.checkSelfPermission(BlazeActivity.this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
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

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (timer != null) {
            timer.cancel();
        }

        if (player != null) {
            player.pause();
            player.release();
        }

        ncnn.release();
    }
}