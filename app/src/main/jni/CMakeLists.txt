project(blazefacencnn)

cmake_minimum_required(VERSION 3.10.2)

set(OpenCV_DIR ${CMAKE_SOURCE_DIR}/sdk/native/jni)

# 调试信息
message(STATUS "CMAKE_SOURCE_DIR: ${CMAKE_SOURCE_DIR}")
message(STATUS "OpenCV_DIR: ${OpenCV_DIR}")
message(STATUS "ANDROID_ABI: ${ANDROID_ABI}")

# 检查OpenCV路径是否存在
if(NOT EXISTS ${OpenCV_DIR})
    message(FATAL_ERROR "OpenCV directory not found at: ${OpenCV_DIR}")
endif()

find_package(OpenCV REQUIRED core imgproc)

message(STATUS "OpenCV_FOUND: ${OpenCV_FOUND}")
message(STATUS "OpenCV_VERSION: ${OpenCV_VERSION}")
message(STATUS "OpenCV_LIBS: ${OpenCV_LIBS}")
message(STATUS "OpenCV_INCLUDE_DIRS: ${OpenCV_INCLUDE_DIRS}")

set(ncnn_DIR ${CMAKE_SOURCE_DIR}/ncnn-20250503-android-vulkan/${ANDROID_ABI}/lib/cmake/ncnn)
find_package(ncnn REQUIRED)

# 添加包含目录
include_directories(${OpenCV_INCLUDE_DIRS})

# 查找Android系统库
find_library(log-lib log)
find_library(android-lib android)
find_library(camera2ndk-lib camera2ndk)
find_library(mediandk-lib mediandk)

add_library(blazefacencnn SHARED blazefacencnn.cpp face.cpp landmark.cpp ndkcamera.cpp peleenetssd_seg.cpp)

# 修正链接顺序 - OpenCV应该在ncnn之前
target_link_libraries(blazefacencnn 
    ${OpenCV_LIBS} 
    ncnn 
    ${camera2ndk-lib} 
    ${mediandk-lib}
    ${log-lib}
    ${android-lib}
)
