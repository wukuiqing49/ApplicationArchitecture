---
description: 打测试包运行到手机上
---
检测 ADB 并一键打包运行 APP。本脚本无需手动点击运行。

1. 检测是否有可用的 Android 设备连接
// turbo
```bash
adb devices
```
2. 如果连接成功，开始编译且安装 Debug 版本的 App
// turbo
```bash
./gradlew installDebug
```
3. 安装成功后，自动拉起主 Activity，不需手动点击
// turbo
```bash
adb shell am start -n com.example.framework/.MainActivity
```