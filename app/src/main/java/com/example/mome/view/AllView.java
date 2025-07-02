package com.example.mome.view;

import android.content.Context;
import android.graphics.PixelFormat;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;

import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

import com.example.mome.R;
import com.example.mome.adapter.AppAdapter;
import com.example.mome.utils.Util;


public class AllView extends View {

    private final View view;
    private final ArrayList<String> list;
    private final RecyclerView rvAll;
    private final AppAdapter appAdapter;
    private final WindowManager windowManager;
    private boolean isShow;

    public AllView(Context context) {
        super(context);
        view = LayoutInflater.from(context).inflate(R.layout.all_view, null, false);

        view.findViewById(R.id.back).setOnClickListener(v->hide());

        list = Util.getList(context);
        rvAll = view.findViewById(R.id.rv_all);
        rvAll.setLayoutManager(new GridLayoutManager(context, 5));

        appAdapter = new AppAdapter(context, list);
        rvAll.setAdapter(appAdapter);
        appAdapter.setClick(position -> click.click(list.get(position)));

        isShow = false;
        windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
    }
    public void show() {
        if (!isShow) {
            WindowManager.LayoutParams params = new WindowManager.LayoutParams();
            params.format = PixelFormat.TRANSLUCENT;
            params.gravity = Gravity.CENTER;
            params.flags = WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                    | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
            windowManager.addView(view, params);
            isShow = true;
        }
    }
    public void hide() {
        if (isShow) {
            windowManager.removeView(view);
            isShow = false;
        }
    }

    public interface Click {
        void click(String s);
    }

    private Click click;

    public void setClick(Click click) {
        this.click = click;
    }
}
