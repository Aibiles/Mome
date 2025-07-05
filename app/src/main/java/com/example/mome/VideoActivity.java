package com.example.mome;

import android.app.ActivityOptions;
import android.content.Intent;
import android.hardware.display.DisplayManager;
import android.os.Build;
import android.os.Bundle;
import android.view.Display;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mome.adapter.AllAdapter;
import com.example.mome.utils.Util;

import java.util.ArrayList;

public class VideoActivity extends AppCompatActivity {

    private RecyclerView rvAll;
    private ArrayList<String> list;
    private RelativeLayout rlSel;
    private ImageView ivFu;
    private ImageView ivAll;
    private int index;
    private TextView tvSure;
    private DisplayManager systemService;
    private Display[] displays;
    private AllAdapter allAdapter;
    private ActivityOptions options;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video);
        getWindow().getDecorView().setSystemUiVisibility(5894);

        list = Util.getVideo(this);


        rvAll = findViewById(R.id.rv_all);
        rvAll.setLayoutManager(new GridLayoutManager(this, 4));
        allAdapter = new AllAdapter(this, list);
        rvAll.setAdapter(allAdapter);

        systemService = (DisplayManager) getSystemService(DISPLAY_SERVICE);
        displays = systemService.getDisplays();
        options = ActivityOptions.makeBasic();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            options.setLaunchDisplayId(displays[0].getDisplayId());
        }


        rlSel = findViewById(R.id.rl_sel);
        ivFu = findViewById(R.id.fu);
        ivAll = findViewById(R.id.all);
        tvSure = findViewById(R.id.tv_sure);
        tvSure.setOnClickListener(v->{
            if (ivAll.isSelected()) {
                if (FuActivity.fuActivity != null) {
                    FuActivity.fuActivity.finish();
                }
                Intent intent = new Intent(this, FuActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                intent.putExtra("isFirst", false);
                intent.putExtra("index", index);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    options.setLaunchDisplayId(displays[0].getDisplayId());
                }
                startActivity(intent, options.toBundle());
            }
            if (FuActivity.fuActivity != null) {
                FuActivity.fuActivity.finish();
            }
            Intent intent = new Intent(this, FuActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            intent.putExtra("isFirst", true);
            intent.putExtra("index", index);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                options.setLaunchDisplayId(displays[1].getDisplayId());
            }

            startActivity(intent, options.toBundle());

            rlSel.setVisibility(View.GONE);
        });

        findViewById(R.id.back).setOnClickListener(v->finish());

        allAdapter.setClick(position -> {
            index = position;
            rlSel.setVisibility(View.VISIBLE);
            ivFu.setSelected(false);
            ivAll.setSelected(false);
            tvSure.setClickable(false);
        });

        ivFu.setOnClickListener(v->{
            ivFu.setSelected(true);
            ivAll.setSelected(false);
            tvSure.setClickable(true);
        });
        ivAll.setOnClickListener(v->{
            ivFu.setSelected(false);
            ivAll.setSelected(true);
            tvSure.setClickable(true);
        });
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        allAdapter = new AllAdapter(this, list);
        rvAll.setAdapter(allAdapter);
        allAdapter.setClick(position -> {
            index = position;
            rlSel.setVisibility(View.VISIBLE);
            ivFu.setSelected(false);
            ivAll.setSelected(false);
            tvSure.setClickable(false);
        });

    }
}