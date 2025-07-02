// Tencent is pleased to support the open source community by making ncnn available.
//
// Copyright (C) 2021 THL A29 Limited, a Tencent company. All rights reserved.
//
// Licensed under the BSD 3-Clause License (the "License"); you may not use this file except
// in compliance with the License. You may obtain a copy of the License at
//
// https://opensource.org/licenses/BSD-3-Clause
//
// Unless required by applicable law or agreed to in writing, software distributed
// under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
// CONDITIONS OF ANY KIND, either express or implied. See the License for the
// specific language governing permissions and limitations under the License.

package com.example.mome;

import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.util.Log;
import android.view.Surface;
import org.opencv.android.OpenCVLoader;


public class BlazeFaceNcnn {
    public native boolean loadModel(AssetManager mgr, int modelid, int cpugpu);

    public native boolean openCamera();

    public native boolean closeCamera();

    public native boolean setOutputWindow(Surface surface);

    public native boolean isWarn();

    public void test(String message) {
        Log.d(message, "test: ");
    }

    // 添加静态变量跟踪OpenCV加载状态
    private static boolean isOpenCVLoaded = false;

    public static boolean isOpenCVLoaded() {
        return isOpenCVLoaded;
    }

    public native boolean loadSegModel(AssetManager mgr);

    public native void detectSeg(Bitmap bitmap);

    static {
        // 首先尝试加载OpenCV
        if (!OpenCVLoader.initLocal()) {
            Log.w("BlazeFaceNcnn", "Unable to load OpenCV using debug mode");
        } else {
            Log.d("BlazeFaceNcnn", "OpenCV loaded successfully");
            isOpenCVLoaded = true;
        }
        
        // 然后加载我们的native库
        System.loadLibrary("blazefacencnn");
    }
}
