# 第三方内容

管家的 Android Java 代码、资源和启动程序公开在本仓库；下载包额外包含用户提供的应用 APK 与原厂恢复文件，这些文件不是本仓库自行编写的软件。

- 威马桌面 wm35：源项目为 [naojiren/wm-launcher](https://github.com/naojiren/wm-launcher)。本次采用用户指定对话交付的 0.1.0-wm35 Android 6 APK；对应源码和 GPL-3.0-only、图标许可随原始桌面交付提供。
- DiPlay / WePlay perf34：源项目为 [naojiren/diplay-wm](https://github.com/naojiren/diplay-wm)。本次采用用户指定对话交付的 perf34-bt-defaults-test ARMv7 APK；源码和许可见原项目及其对应交付。
- ADB 工具、ES 文件管理器、强制旋转和完整版其它可选 APK 来自用户已有文件；具体版本及哈希见 docs/software.json。
- 完整版原厂恢复 APK、factorymodel、certificates.xml 和原始工程启动材料来自用户已有备份。该来源不代表车辆厂商授权、背书或通用适配。
- 管家使用的 ISRG Root X1 为公开根证书，仅用于访问 GitHub；不会修改车机系统证书库。

各软件名称、商标、代码和二进制文件的权利及许可归各自权利人。本仓库没有给第三方 APK 或原厂文件追加许可证。
