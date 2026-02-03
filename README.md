# Mome - 智能车载辅助系统

![Android](https://img.shields.io/badge/Android-24%2B-green)
![OpenCV](https://img.shields.io/badge/OpenCV-4.x-blue)
![License](https://img.shields.io/badge/License-MIT-yellow)

## 项目概述

Mome是一款功能丰富的Android智能车载辅助系统，集成了多摄像头管理、智能驾驶辅助、传感器数据处理、人脸检测等先进功能。该系统旨在为驾驶员提供全方位的安全辅助和便捷体验。

## 主要功能

### 🚗 智能驾驶辅助
- **360度全景显示**：支持前、后、左、右四个方向摄像头
- **动态辅助线**：基于传感器数据的智能辅助线绘制
- **转向指示**：实时左转/右转辅助线显示
- **倒车辅助**：透视梯形倒车辅助线

### 📊 实时仪表盘
- **智能时速显示**：基于加速度传感器的实时时速计算
- **电量监控**：根据加速度动态显示电量消耗
- **传感器集成**：陀螺仪和加速度传感器数据融合

### 📱 人机交互
- **人脸检测**：使用BlazeFace模型进行疲劳检测
- **视觉效果**：动态颜色变化和视觉增强
- **应用管理**：内置应用列表和多媒体功能
- **摄像头切换**：智能摄像头方向管理

### 🎨 视觉增强
- **动态色彩**：根据驾驶状态变化的视觉效果
- **全屏模式**：支持摄像头全屏显示
- **实时渲染**：高性能图像处理和显示

## 技术栈

### 核心技术
- **Android SDK**: 24+ (Android 7.0+)
- **OpenCV 4.x**: 计算机视觉和图像处理
- **NDK/JNI**: 原生C++代码集成
- **CMake**: 构建系统

### AI模型
- **BlazeFace**: 轻量级人脸检测模型
- **NCNN**: 高性能神经网络推理框架

### 传感器支持
- 陀螺仪 (Gyroscope)
- 加速度传感器 (Accelerometer)
- 摄像头 (Camera2 API)

### 开发工具
- Android Studio
- Gradle 构建系统
- ProGuard 代码混淆

## 系统要求

### 硬件要求
- **Android版本**: 7.0+ (API Level 24+)
- **内存**: 最低2GB RAM，推荐4GB+
- **存储**: 至少100MB可用空间
- **摄像头**: 支持Camera2 API
- **传感器**: 陀螺仪、加速度传感器

### 支持架构
- ARM64-v8a
- ARMv7 (armeabi-v7a)
- x86_64

## 快速开始

### 1. 环境准备
```bash
# 安装Android Studio
# 配置Android SDK (API Level 24+)
# 安装NDK和CMake工具
```

### 2. 克隆项目
```bash
git clone https://github.com/your-repo/mome.git
cd mome
```

### 3. 导入和构建
1. 使用Android Studio打开项目
2. 等待Gradle同步完成
3. 连接Android设备或启动模拟器
4. 点击"Run"按钮构建并安装应用

### 4. 权限配置
应用需要以下权限：
- 摄像头权限 (CAMERA)
- 录音权限 (RECORD_AUDIO)
- 网络权限 (INTERNET)
- 传感器权限 (自动获取)

## 项目结构

```
Mome/
├── app/                          # 主应用模块
│   ├── src/main/
│   │   ├── java/com/example/mome/
│   │   │   ├── view/            # 自定义视图组件
│   │   │   ├── manager/         # 管理器类
│   │   │   ├── opencv/          # OpenCV相关功能
│   │   │   ├── config/          # 配置文件
│   │   │   └── utils/           # 工具类
│   │   ├── jni/                 # JNI/NDK代码
│   │   ├── assets/              # AI模型文件
│   │   └── res/                 # Android资源文件
├── OpenCV/                       # OpenCV模块
└── docs/                        # 项目文档
```

## 主要类说明

- `MomeActivity`: 主界面，负责摄像头显示和车辆控制
- `InitActivity`: 初始化界面，显示仪表盘信息
- `AssistLineOverlay`: 辅助线绘制组件
- `VehicleControlManager`: 车辆控制管理器
- `CameraConnectionManager`: 摄像头连接管理器
- `VisualEffectProcessor`: 视觉效果处理器

## 开发指南

### 添加新功能
1. 在相应的包下创建新类
2. 更新相关的管理器类
3. 添加必要的UI组件
4. 更新权限配置

### 传感器集成
```java
// 注册传感器监听器
sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_UI);

// 处理传感器数据
@Override
public void onSensorChanged(SensorEvent event) {
    // 处理传感器数据
}
```

### OpenCV使用
```java
// 初始化OpenCV
if (!OpenCVLoader.initLocal()) {
    // 处理初始化失败
}
```
