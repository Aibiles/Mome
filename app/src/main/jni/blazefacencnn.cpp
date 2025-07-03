
#include <android/asset_manager_jni.h>
#include <android/native_window_jni.h>
#include <android/native_window.h>
#include <android/bitmap.h>

#include <android/log.h>

#include <jni.h>

#include <string>
#include <vector>

#include <platform.h>
#include <benchmark.h>

#include "face.h"
#include "peleenetssd_seg.h"

#include "ndkcamera.h"

#include <opencv2/core/core.hpp>
#include <opencv2/imgproc/imgproc.hpp>

#include <unistd.h>

#if __ARM_NEON
#include <arm_neon.h>
#endif // __ARM_NEON


static Face *g_blazeface = 0;
static Peleen *g_paleenseg = 0;
static NdkCamera *n_camera = 0;
static ncnn::Mutex lock;


class MyNdkCamera : public NdkCameraWindow {
public:
    virtual void on_image_render(cv::Mat &rgb) const;

};

void MyNdkCamera::on_image_render(cv::Mat &rgb) const {
    // 添加安全检查
    if (rgb.empty() || rgb.data == nullptr) {
        __android_log_print(ANDROID_LOG_ERROR, "BlazeFaceNcnn", "Invalid rgb Mat object");
        return;
    }

    // 检查图像尺寸
    if (rgb.rows <= 0 || rgb.cols <= 0) {
        __android_log_print(ANDROID_LOG_ERROR, "BlazeFaceNcnn", "Invalid rgb Mat dimensions: %dx%d", rgb.cols, rgb.rows);
        return;
    }

    {
        ncnn::MutexLockGuard g(lock);

//        if (g_blazeface) {
//            //如果模型加载成功 此处进行人脸识别
//            std::vector<Object> face;
//            g_blazeface->detect(rgb, face);
//            g_blazeface->draw(rgb, face);
//        } else {
//            __android_log_print(ANDROID_LOG_WARN, "BlazeFaceNcnn", "g_blazeface is null");
//        }

        if (g_paleenseg) {
            //如果模型加载成功 此处进行车道线分割和汽车人识别
            ncnn::Mat seg_out;
            std::vector<SegObject> objects;
            g_paleenseg->detect(rgb, objects, seg_out);
            g_paleenseg->draw(rgb, objects, seg_out);
        } else {
            __android_log_print(ANDROID_LOG_WARN, "BlazeFaceNcnn", "g_paleenseg is null");
        }

    }
}


static MyNdkCamera *g_camera = 0;

extern "C" {

JNIEXPORT jint JNI_OnLoad(JavaVM *vm, void *reserved) {

    g_camera = new MyNdkCamera;

    JNIEnv* env = NULL;
    //获取JNI_VERSION版本
    if (vm->GetEnv(reinterpret_cast<void**>(&env), JNI_VERSION_1_4) != JNI_OK) {
        return -1;
    }
    return JNI_VERSION_1_4;
}


JNIEXPORT void JNI_OnUnload(JavaVM *vm, void *reserved) {
    __android_log_print(ANDROID_LOG_DEBUG, "ncnn", "JNI_OnUnload");

    {
        ncnn::MutexLockGuard g(lock);

        delete g_blazeface;
        g_blazeface = 0;
    }

    delete g_camera;
    g_camera = 0;
}

// public native boolean loadModel(AssetManager mgr, int modelid, int cpugpu);
JNIEXPORT jboolean JNICALL
Java_com_example_mome_BlazeFaceNcnn_loadModel(JNIEnv *env, jobject thiz,
                                               jobject assetManager, jint modelid,
                                               jint cpugpu) {


    AAssetManager* manager = AAssetManager_fromJava(env, assetManager);

    //进行模型加载
    {
        ncnn::MutexLockGuard g(lock);

        if (!g_blazeface)
            g_blazeface = new Face;

        g_blazeface->load(manager, "blazeface", 192, false);

    }


    return JNI_TRUE;
}

extern "C"
JNIEXPORT jboolean JNICALL
Java_com_example_mome_BlazeFaceNcnn_loadSegModel(JNIEnv *env, jobject thiz, jobject assetManager) {
    // TODO: implement loadSegModel()
    AAssetManager* manager = AAssetManager_fromJava(env, assetManager);

    //进行模型加载
    {
        if (!g_paleenseg)
            g_paleenseg = new Peleen;

        g_paleenseg->load(manager, false);
    }


    return JNI_TRUE;
}

void visualizeSegmentation(const ncnn::Mat& seg_out, void* output_pixels, int width, int height) {
    // 创建输出Mat
    cv::Mat output_mat(height, width, CV_8UC4, output_pixels);

    // 将ncnn::Mat转换为OpenCV Mat
    cv::Mat mask(seg_out.h, seg_out.w, CV_32FC1, (float*)seg_out.data);

    // 调整尺寸（如果需要）
    if (mask.size() != output_mat.size()) {
        cv::resize(mask, mask, output_mat.size(), 0, 0, cv::INTER_NEAREST);
    }

    // 应用颜色映射
    cv::Mat colored;
    cv::applyColorMap(mask, colored, cv::COLORMAP_JET);

    // 转换为RGBA
    cv::cvtColor(colored, output_mat, cv::COLOR_BGR2RGBA);
}

extern "C"
JNIEXPORT void JNICALL
Java_com_example_mome_BlazeFaceNcnn_detect(JNIEnv *env, jobject thiz, jobject bitmap) {
    // TODO: implement detectSeg()
        if (!g_blazeface) {
            __android_log_print(ANDROID_LOG_WARN, "BlazeFaceNcnn", "g_blazeface is null");
            return;
        }

    // 1. 获取 Bitmap 信息
    AndroidBitmapInfo info;
    void *pixels;
    if (AndroidBitmap_getInfo(env, bitmap, &info) < 0) {
        return;
    }
    if (info.format != ANDROID_BITMAP_FORMAT_RGBA_8888) {
        return; // 只支持 RGBA_8888 格式
    }

    // 2. 锁定 Bitmap 像素
    if (AndroidBitmap_lockPixels(env, bitmap, &pixels) < 0) {
        return;
    }

    // 3. 创建输入/输出 Mat (RGBA)
    cv::Mat rgba_mat(
            info.height,
            info.width,
            CV_8UC4,
            pixels
    );

    // 4. 转换为 BGR Mat (移除 Alpha 通道)
    cv::Mat bgr_mat;
    cv::cvtColor(rgba_mat, bgr_mat, cv::COLOR_RGBA2BGR);

    // 5. 在 BGR 空间进行检测
    std::vector<Object> face;
    g_blazeface->detect(bgr_mat, face);
    g_blazeface->draw(bgr_mat, face);

//     7. 将处理后的 BGR 图像转换回 RGBA
    cv::cvtColor(bgr_mat, rgba_mat, cv::COLOR_BGR2RGBA);

    // 8. 解锁 Bitmap
    AndroidBitmap_unlockPixels(env, bitmap);
}


// 打开相机
JNIEXPORT jboolean JNICALL
Java_com_example_mome_BlazeFaceNcnn_openCamera(JNIEnv *env, jobject thiz) {

    if (g_camera == nullptr) {
        __android_log_print(ANDROID_LOG_ERROR, "ncnn", "g_camera is null when opening");
        return JNI_FALSE;
    }

    int result = g_camera->open(1, env);
    if (result != 0) {
        __android_log_print(ANDROID_LOG_ERROR, "ncnn", "Failed to open camera, result code: %d", result);
        return JNI_FALSE;
    }

    return JNI_TRUE;
}

// 关闭相机
JNIEXPORT jboolean JNICALL
Java_com_example_mome_BlazeFaceNcnn_closeCamera(JNIEnv *env, jobject thiz) {
    __android_log_print(ANDROID_LOG_DEBUG, "ncnn", "closeCamera");

    if (g_camera != nullptr) {
        g_camera->close();
    } else {
        __android_log_print(ANDROID_LOG_WARN, "ncnn", "g_camera is null when closing");
    }

    return JNI_TRUE;
}

// 设置接收Surface相机流
JNIEXPORT jboolean JNICALL
Java_com_example_mome_BlazeFaceNcnn_setOutputWindow(JNIEnv *env, jobject thiz,
                                                     jobject surface) {

    if (surface == nullptr) {
        __android_log_print(ANDROID_LOG_ERROR, "ncnn", "Surface is null");
        return JNI_FALSE;
    }
    
    if (g_camera == nullptr) {
        __android_log_print(ANDROID_LOG_ERROR, "ncnn", "g_camera is null when setting window");
        return JNI_FALSE;
    }

    ANativeWindow* window = ANativeWindow_fromSurface(env, surface);
    if (window == nullptr) {
        __android_log_print(ANDROID_LOG_ERROR, "ncnn", "Failed to create ANativeWindow from surface");
        return JNI_FALSE;
    }
    
    g_camera->set_window(window);
    
    return JNI_TRUE;
}

}
