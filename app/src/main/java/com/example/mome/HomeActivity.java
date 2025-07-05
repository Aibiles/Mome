package com.example.mome;

import android.app.Activity;
import android.app.ActivityOptions;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.hardware.display.DisplayManager;
import android.icu.util.ChineseCalendar;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Bundle;
import android.view.Display;
import android.view.View;
import android.widget.Chronometer;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

import com.example.mome.utils.LunarCalender;
import com.example.mome.view.*;
import com.example.mome.utils.Util;

public class HomeActivity extends AppCompatActivity {

    private int index;
    private int[] ints;
    private TextView tvMusic;
    private ImageView ivMucis;
    private Chronometer chronometer;
    private SimpleDateFormat dateFormat;
    private ImageView[] imageViews;
    private boolean[] booleans;
    private int id;
    private HashMap<Integer, String> appMap;
    private SharedPreferences sharedPreferences;
    private SharedPreferences.Editor edit;
    private AllView allView;
    private RepView repView;
    private DisplayManager systemService;
    private Display[] displays;
    private ImageView ivWea;
    private RelativeLayout rlApp;
    private TextView tvApp;
    private ArrayList<String> list;
    private MediaPlayer player;
    private ImageView ivPlay;

    private void initPlayer(String s) {
        if (player != null) {
            player.pause();
            player.release();
        }

        player = new MediaPlayer();
        player.setLooping(true);
        try {
            player.setDataSource(getAssets().openFd(s+".ogg"));
            player.prepare();
        } catch (IOException e) {
            e.printStackTrace();
        }
        tvMusic.setText(s);
        ivMucis.setBackground(getDrawable(ints[index]));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);
        getWindow().getDecorView().setSystemUiVisibility(5894);

        systemService = (DisplayManager) getSystemService(Context.DISPLAY_SERVICE);
        displays = systemService.getDisplays();

        Intent intent = new Intent(this, InitActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        ActivityOptions options = ActivityOptions.makeBasic();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            options.setLaunchDisplayId(displays[2].getDisplayId());
        }
        startActivity(intent, options.toBundle());
        

//        ivWea = findViewById(R.id.iv_wea);
//        findViewById(R.id.cv_wea).setOnClickListener(v->{
//            v.setSelected(!v.isSelected());
//            ivWea.setBackground(getDrawable(v.isSelected() ? R.drawable.b : R.drawable.img_1));
//        });
//
//        rlApp = findViewById(R.id.rl_app);
//        tvApp = findViewById(R.id.tv_app);
//        findViewById(R.id.cv_music).setOnClickListener(v->{
//            Toast.makeText(this, "音视频App已打开", Toast.LENGTH_SHORT).show();
//            tvApp.setText("音视频App");
//            rlApp.setVisibility(View.VISIBLE);
//        });
//        findViewById(R.id.cv_car).setOnClickListener(v->{
//            Toast.makeText(this, "车辆信息App已打开", Toast.LENGTH_SHORT).show();
//            tvApp.setText("车辆信息App");
//            rlApp.setVisibility(View.VISIBLE);
//        });
//        findViewById(R.id.back).setOnClickListener(v->rlApp.setVisibility(View.GONE));
//
//
        list = new ArrayList<>();
        try {
            for (String s : getAssets().list("")) {
                if (s.endsWith(".ogg")) {
                    list.add(s.substring(0,s.lastIndexOf(".")));
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        index = 0;
        try {
            player.setDataSource(getAssets().openFd(list.get(index)));
            player.prepare();
        } catch (IOException e) {
            e.printStackTrace();
        }
        ints = new int[]{R.drawable.i_believe, R.drawable.yonqi,R.drawable.lixiangsanxun, R.drawable.feidegenggao, };
        tvMusic = findViewById(R.id.tv_music);
        ivMucis = findViewById(R.id.iv_music);

        findViewById(R.id.iv_left).setOnClickListener(v->{
            index++;
            if (index == 4)
                index = 0;
            initPlayer(list.get(index));
            player.start();
            ivPlay.setSelected(true);
        });
        findViewById(R.id.iv_right).setOnClickListener(v->{
            index--;
            if (index == -1)
                index = 3;
            initPlayer(list.get(index));
            player.start();
            ivPlay.setSelected(true);
        });
        ivPlay = findViewById(R.id.iv_play);
        findViewById(R.id.iv_play).setOnClickListener(v->{
            v.setSelected(!v.isSelected());
            if (v.isSelected()) {
                player.start();
            } else {
                player.pause();
            }
        });
        initPlayer(list.get(index));

        TextView tvDate = findViewById(R.id.date);
        tvDate.setText(LunarCalender.getDayLunar());

        chronometer = findViewById(R.id.chronometer);
        dateFormat = new SimpleDateFormat("hh:mm");
        chronometer.setOnChronometerTickListener(v->{
            chronometer.setText(dateFormat.format(new Date()));
        });
        chronometer.start();

        imageViews = new ImageView[] {
                findViewById(R.id.one),
                findViewById(R.id.two),
                findViewById(R.id.three),
                findViewById(R.id.four),
                findViewById(R.id.five),
        };
        booleans = new boolean[]{false, false, false, false, false};
        id = 0;
        appMap = new HashMap<>();
        sharedPreferences = getSharedPreferences("app", MODE_PRIVATE);
        edit = sharedPreferences.edit();

        for (id = 0; id < imageViews.length; id++) {
            int index = id;
            imageViews[id].setOnClickListener(v->{
                id = index;
                if (booleans[id]) {
                    ActivityOptions animation = ActivityOptions.makeScaleUpAnimation(v, 0, 0, 0, 0);
                    switch (appMap.get(id)) {
                        case "1":
                            startActivity(new Intent(this, MomeActivity.class), animation.toBundle());
                            break;
                        case "2":
                            startActivity(new Intent(this, BlazeActivity.class), animation.toBundle());
                            break;
                        case "3":
                            startActivity(new Intent(this, VideoActivity.class), animation.toBundle());
                            break;
                        case "4":
                            startActivity(new Intent(this, DetectActivity.class), animation.toBundle());
                            break;
                        default:
                            startActivity(getPackageManager().getLaunchIntentForPackage(appMap.get(id)), animation.toBundle());
                    }
                } else {
                    allView.show();
                }
            });
            imageViews[id].setOnLongClickListener(v->{
                id = index;
                if (booleans[id]) {
                    repView.show(appMap.get(id));
                }

                return true;
            });
        }

        allView = new AllView(this);
        allView.setClick(s -> {
            appMap.put(id, s);
            edit.putString(String.valueOf(id), s).apply();
            imageViews[id].setBackground(Util.getIcon(s));
            booleans[id] = true;
            allView.hide();
        });

        repView = new RepView(this);


        repView.setClick(new RepView.Click() {
            @Override
            public void un() {
                appMap.put(id, "");
                edit.putString(String.valueOf(id), "").apply();
                imageViews[id].setBackground(getDrawable(R.drawable.add));
                booleans[id] = false;
                repView.hide();
            }

            @Override
            @RequiresApi(api = Build.VERSION_CODES.O)
            public void fu() {
                ActivityOptions options = ActivityOptions.makeBasic();
                Intent intent = null;
                switch (appMap.get(id)) {
                    case "1":
                        intent = new Intent(getApplicationContext(), MomeActivity.class);
                        break;
                    case "2":
                        intent = new Intent(getApplicationContext(), BlazeActivity.class);
                        break;
                    case "3":
                        intent = new Intent(getApplicationContext(), VideoActivity.class);
                        break;
                    case "4":
                        intent = new Intent(getApplicationContext(), DetectActivity.class);
                        break;
                    default:
                        intent = getPackageManager().getLaunchIntentForPackage(appMap.get(id));
                }
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                intent.addCategory("android.intent.category.DEFAULT");
                if (displays.length > 1) {
                    options.setLaunchDisplayId(displays[1].getDisplayId());
                    startActivity(intent, options.toBundle());
                } else {
                    startActivity(intent);
                }
                repView.hide();
            }

            @Override
            public void rep() {
                allView.show();
                repView.hide();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();

        for (id = 0; id < imageViews.length; id++) {
            String s = sharedPreferences.getString(String.valueOf(id), "");
            if (s.equals("")) {
                continue;
            }
            appMap.put(id, s);
            edit.putString(String.valueOf(id), s).apply();
            imageViews[id].setBackground(Util.getIcon(s));
            booleans[id] = true;}
    }
}