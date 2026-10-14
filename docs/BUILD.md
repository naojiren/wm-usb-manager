# 构建

使用 Linux、JDK 17、Android SDK Platform 36 和 Build Tools 36.0.0。项目没有 Gradle 或第三方 Java 依赖。

~~~sh
export ANDROID_SDK_ROOT=/path/to/android-sdk
sdkmanager "platforms;android-36" "build-tools;36.0.0"
bash tools/check.sh
bash tools/build.sh full
bash tools/build.sh lite
~~~

默认生成 build/full/manager-unsigned-aligned.apk 和 build/lite/manager-unsigned-aligned.apk。这些是未签名检查产物，不能直接安装。

为自己的构建签名时，设置 WM_KEYSTORE、WM_KEYSTORE_PASSWORD_FILE 和可选 WM_KEY_ALIAS（默认 manager），再运行构建；输出对应目录中的 manager.apk。密钥和密码文件均不进入仓库。原交付签名私钥不公开，自签名版本不能直接覆盖不同签名的已安装向导。

tools/check.sh 执行完整版和精简版的删除、安装路径及在线更新策略断言。Linux 环境用于验证符号链接边界；它不会连接车辆或删除车辆软件。

Android Manifest 的最低/目标 API 仍为 23，应用 versionCode 为 2。完整版 versionName 为 1.3.0，精简版为 1.3.0-lite。编译使用新版平台不改变运行目标。
