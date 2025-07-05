package com.example.mome;

import android.app.ActivityOptions;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.hardware.display.DisplayManager;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.Display;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatSeekBar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mome.adapter.OtherAdapter;
import com.example.mome.utils.Util;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

public class FuActivity extends AppCompatActivity {

    public static FuActivity fuActivity;
    private ArrayList<String> list;
    private boolean isFirst;
    private SurfaceView surfaceView;
    private Broad broad;
    private Intent act;
    private DisplayManager manager;
    private Display[] displays;
    private ActivityOptions options;
    private RelativeLayout rlBar;
    private boolean isClick;
    private AppCompatSeekBar seekBar;
    private ImageView ivPlay;
    private TextView tvTotal;
    private TextView tvPlayed;
    private SimpleDateFormat dateFormat;
    private SimpleDateFormat dateFormat1;
    private boolean isPre;
    private int index;
    private SharedPreferences.Editor video;
    private MediaPlayer player;
    private Timer timer;
    private RelativeLayout rlOther;
    private boolean isOpen;
    private RecyclerView rvOther;
    private LinearLayoutManager linearLayoutManager;
    private OtherAdapter otherAdapter;

    public class Broad extends BroadcastReceiver{
        @Override
        public void onReceive(Context context, Intent intent) {
            switch (intent.getIntExtra("act", -1)) {
                case 0:
                    finish();
                    break;
                case 1:
                    isClick = !isClick;
                    rlBar.setVisibility(isClick ? View.VISIBLE : View.GONE);
                    break;
                case 2:
                    ivPlay.setSelected(!ivPlay.isSelected());
                    if (ivPlay.isSelected()) {
                        player.start();
                    } else {
                        player.pause();
                    }
                    break;
                case 3:
                    seekBar.setProgress(player.getCurrentPosition()-30000);
                    player.seekTo(player.getCurrentPosition()-30000);
                    break;
                case 4:
                    seekBar.setProgress(player.getCurrentPosition()+30000);
                    player.seekTo(player.getCurrentPosition()+30000);
                    break;
                case 5:
                    player.seekTo(intent.getIntExtra("data", 0));
                    break;
                case 6:
                    isOpen = !isOpen;
                    rlOther.setVisibility(isOpen ? View.VISIBLE : View.GONE);
                    break;
                case 7:
                    initPlayer(intent.getIntExtra("data", 0));
                    break;
                case 8:
                    if (player != null) {
                        player.start();
                        player.seekTo(0);
                        ivPlay.setSelected(true);
                    }
                    break;
                default:
            }
            rlBar.postInvalidate();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_fu);
        getWindow().getDecorView().setSystemUiVisibility(5894);
        fuActivity = this;

        list = Util.getVideo(this);
        isFirst = getIntent().getBooleanExtra("isFirst", true);

        broad = new Broad();
        registerReceiver(broad, new IntentFilter("act"));
        act = new Intent("act");

        manager = (DisplayManager) getSystemService(DISPLAY_SERVICE);
        displays = manager.getDisplays();
        options = ActivityOptions.makeBasic();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            options.setLaunchDisplayId(displays[1].getDisplayId());
        }
        findViewById(R.id.back).setOnClickListener(v->{
            act.putExtra("act", 0);
            sendBroadcast(act);
            if (isFirst) {
                video.putString(list.get(index), dateFormat1.format(player.getCurrentPosition())).apply();
                Intent intent = new Intent(this, VideoActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent, options.toBundle());
            }
        });

        surfaceView = findViewById(R.id.surface);
        rlBar = findViewById(R.id.rl_bar);
        isClick = false;
        surfaceView.setOnClickListener(v->{
            act.putExtra("act", 1);
            sendBroadcast(act);
        });
        seekBar = findViewById(R.id.seek_bar);
        ivPlay = findViewById(R.id.iv_play);
        tvTotal = findViewById(R.id.tv_total);
        tvPlayed = findViewById(R.id.tv_played);
        dateFormat = new SimpleDateFormat("mm:ss");
        dateFormat1 = new SimpleDateFormat("mm分ss秒");
        video = getSharedPreferences("video", 0).edit();

        ivPlay.setOnClickListener(v->{
            act.putExtra("act", 2);
            sendBroadcast(act);
        });
        findViewById(R.id.tv_left).setOnClickListener(v->{
            act.putExtra("act", 3);
            sendBroadcast(act);
        });
        findViewById(R.id.tv_right).setOnClickListener(v->{
            act.putExtra("act", 4);
            sendBroadcast(act);
        });
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                if (b) {
                    act.putExtra("act", 5);
                    act.putExtra("data", i);
                    sendBroadcast(act);
                }
                tvPlayed.setText(dateFormat.format(i));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        rlOther = findViewById(R.id.rl_other);
        isOpen = false;
        findViewById(R.id.tv_other).setOnClickListener(v->{
            act.putExtra("act", 6);
            sendBroadcast(act);
        });
        rvOther = findViewById(R.id.rv_other);
        linearLayoutManager = new LinearLayoutManager(this);
        linearLayoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        rvOther.setLayoutManager(linearLayoutManager);
        otherAdapter = new OtherAdapter(this, list);
        rvOther.setAdapter(otherAdapter);
        otherAdapter.setClick(position -> {
            act.putExtra("act", 7);
            act.putExtra("data", position);
            sendBroadcast(act);
        });

        isPre = false;
        surfaceView.getHolder().addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(@NonNull SurfaceHolder surfaceHolder) {
                Log.i("Mimsy", "surfaceCreated: surfaceCreated");
                isPre = true;
                player.setDisplay(surfaceView.getHolder());
            }

            @Override
            public void surfaceChanged(@NonNull SurfaceHolder surfaceHolder, int i, int i1, int i2) {
                Log.i("Mimsy", "surfaceCreated: surfaceChanged");
            }

            @Override
            public void surfaceDestroyed(@NonNull SurfaceHolder surfaceHolder) {
                Log.i("Mimsy", "surfaceCreated: surfaceDestroyed");
                if (timer != null) {
                    timer.cancel();
                }
                if (player != null) {
                    player.pause();
                    player.release();
                    player = null;
                }
            }
        });
        index = getIntent().getIntExtra("index", 0);
        initPlayer(index);
    }

    private void initPlayer(int i) {
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
        if (player != null) {
            video.putString(list.get(index), dateFormat1.format(player.getCurrentPosition())).apply();
            player.pause();
            player.release();
            player = null;
        }
        index = i;
        player = new MediaPlayer();
        player.setLooping(true);
        if (!isFirst) {
            player.setVolume(0,0);
        }
        try {
            player.setDataSource(getAssets().openFd(list.get(i)));
            player.prepare();
            if (isPre) {
                player.setDisplay(surfaceView.getHolder());
            }
            int duration = player.getDuration();
            seekBar.setMax(duration);
            tvTotal.setText(dateFormat.format(duration));


            timer = new Timer();
            timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    if (player != null && player.isPlaying()) {
                        seekBar.setProgress(player.getCurrentPosition());
                    }
                }
            }, 0, 1000);

            act.putExtra("act", 8);
            sendBroadcast(act);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(broad);
    }
}