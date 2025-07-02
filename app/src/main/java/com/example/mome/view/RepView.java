package com.example.mome.view;

import android.content.Context;
import android.graphics.PixelFormat;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.mome.R;
import com.example.mome.utils.Util;
import java.util.ArrayList;

public class RepView extends View {

    private final View view;
    private final ArrayList<String> list;
    private final WindowManager windowManager;
    private boolean isShow;
    private final TextView tvName;
    private final ImageView ivIcon;

    public RepView(Context context) {
        super(context);
        view = LayoutInflater.from(context).inflate(R.layout.rep_view, null, false);

        view.findViewById(R.id.back).setOnClickListener(v->hide());

        list = Util.getList(context);

        tvName = view.findViewById(R.id.tv_name);
        ivIcon = view.findViewById(R.id.iv_icon);

        view.findViewById(R.id.un).setOnClickListener(v->click.un());
        view.findViewById(R.id.fu).setOnClickListener(v->click.fu());
        view.findViewById(R.id.rep).setOnClickListener(v->click.rep());


        isShow = false;
        windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
    }
    public void show(String s) {
        if (!isShow) {
            tvName.setText(Util.getName(s));
            ivIcon.setBackground(Util.getIcon(s));
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
        void un();
        void fu();
        void rep();
    }

    private Click click;

    public void setClick(Click click) {
        this.click = click;
    }
}
