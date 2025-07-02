//
// Created by Administrator on 2025/6/28.
//

#ifndef MOME_PELEENETSSD_SEG_H
#define MOME_PELEENETSSD_SEG_H

#include <opencv2/core/core.hpp>
#include <net.h>
#include "landmark.h"
struct SegObject
{
    cv::Rect_<float> rect;
    int label;
    float prob;
};

class Peleen
{
public:
    Peleen();

    int load(AAssetManager* mgr, bool use_gpu);

    int detect(const cv::Mat& bgr, std::vector<SegObject>& objects, ncnn::Mat &resized);

    int draw(cv::Mat& bgr, const std::vector<SegObject>& objects, ncnn::Mat map);

private:

    ncnn::Net peleenet_net;
    ncnn::UnlockedPoolAllocator blob_pool_allocator;
    ncnn::PoolAllocator workspace_pool_allocator;
};
#endif //MOME_PELEENETSSD_SEG_H



