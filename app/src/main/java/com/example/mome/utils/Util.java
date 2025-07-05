package com.example.mome.utils;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;

import com.example.mome.R;

import java.io.IOException;
import java.util.ArrayList;

public class Util {

    private static ArrayList<String> list;
    private static PackageManager packageManager;
    private static Context context;


    private static ArrayList<String> videoList;

    public static ArrayList<String> getVideo(Context context) {
        if (videoList != null) {
            return videoList;
        }
        videoList = new ArrayList<>();

        try {
            for (String s : context.getAssets().list("")) {
                if (s.endsWith(".mp4")) {
                    videoList.add(s);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return videoList;
    }

    public static ArrayList<String> getList(Context context) {
        if (list != null) {
            return list;
        }
        Util.context = context;

        list = new ArrayList<>();
        packageManager = context.getPackageManager();

        Intent intent = new Intent();
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
                .setAction(Intent.ACTION_MAIN)
                .addCategory(Intent.CATEGORY_LAUNCHER);

        for (ResolveInfo resolveInfo : packageManager.queryIntentActivities(intent, 0)) {
            ApplicationInfo applicationInfo = resolveInfo.activityInfo.applicationInfo;
            list.add(applicationInfo.packageName);
        }
        list.add("1");
        list.add("2");
        list.add("3");
        list.add("4");

        return list;
    }

    public static String getName(String s) {
        switch (s) {
            case "1":
                return "360全景摄像头";
            case "2":
                return "疲劳检测";
            case "3":
                return "多媒体播放器";
            case "4":
                return "车道检测";
        }

        try {
            return (String) packageManager.getApplicationLabel(packageManager.getApplicationInfo(s, 0));
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    public static Drawable getIcon(String s) {
        switch (s) {
            case "1":
                return context.getDrawable(R.drawable.icon1);
            case "2":
                return context.getDrawable(R.drawable.icon2);
            case "3":
                return context.getDrawable(R.drawable.icon3);
            case "4":
                return context.getDrawable(R.drawable.icon4);
        }

        try {
            return packageManager.getApplicationIcon(s);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }
}
