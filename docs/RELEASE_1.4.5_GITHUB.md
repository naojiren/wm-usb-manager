## 高德 V9.1 测试提示

**高德 V9.1 目前还在测试中，如无法使用，请使用上一发布版本完整包中的 V7.1。**

- 本版完整包仅包含高德 V9.1“仪表指引与后台投屏·单APP测试9”，不含 V7.1 和独立仪表伴侣。
- [上一发布版本 1.4.1](https://github.com/naojiren/wm-usb-manager/releases/tag/v1.4.1-wm31-perf32) · [下载含 V7.1 的上一版完整包](https://github.com/naojiren/wm-usb-manager/releases/download/v1.4.1-wm31-perf32/WM-USB-Manager-1.4.1-wm31-perf32-full.zip)。解压后取出 `apk/3高德_V7.1.0.600067_仪表盘投屏版.apk`，放入所用 U 盘的 `apk` 文件夹再刷新。
- 车机已安装 V9.1 时，直接覆盖 V7.1 属于降级，可能被系统或向导阻止；需要先卸载 V9.1 再安装 V7.1。更换大版本前请备份所需导航数据。

## 本次软件与功能

- **完整包 full 1.4.5**：14 个安装 APK，13 个本地原厂恢复 APK；桌面 wm32、WePlay perf32，新增高德单APP测试9和 QQ音乐车机版 3.15.0.3。新高德和 QQ音乐默认不选。
- **精简包 lite 1.4.5**：只保留 ADB工具、DiPlay、威马桌面、ES文件管理器、强制旋转五个默认软件，不附高德、QQ音乐或原厂恢复包。
- 桌面从指定对话更新至 wm32；WePlay 对话中的最新仍是 perf32 扫描抑制测试版。
- 第一阶段使用轻量 APK 元信息扫描和缓存，所选软件在实际安装前完整验签、校验文件状态、包名、版本和内容。
- 支持可选临时文件清理、显示保留的备份/未知文件占用；“可清理 0 MiB”与数据分区剩余容量分开显示。安装页显示数据分区剩余空间。
- 完整版修复 Android 6 原厂恢复后仍为 `installed=false` 的问题。桌面默认写入系统分区，可关闭开关。

## 下载与升级

`full` 为完整包，`lite` 为精简包，附 SHA-256 文件、汇总校验和及对应向导源码。
公开附件基于已交付的本地包，仅清理说明中的开发机路径并加入本次测试提示；APK、向导程序、原厂恢复包和启动文件逐字节一致。文件校验以本 Release 附件为准。

退出旧向导，解压到 U 盘根目录，再拔插 U 盘，通过原厂入口启动。覆盖解压不会删除旧 APK，请按包内“删除旧文件清单.txt”移除旧高德两包、仪表伴侣和 wm31 桌面，避免继续被扫描；其它自加 APK 可保留。

两版向导编译、签名和 345 项策略检查通过；新桌面、高德和 QQ音乐通过 Android 6 签名验证，ZIP CRC 及哈希已检查。这些检查不等同于实车导航、投屏、CarPlay 或音乐播放验证。WePlay perf32 也是测试构建，扫描保护需要车机本地 ADB 已开启并授权。

---

## Amap V9.1 is experimental

**Amap V9.1 is still being tested. If it does not work, use V7.1 from the previous full release.**

[Previous release 1.4.1](https://github.com/naojiren/wm-usb-manager/releases/tag/v1.4.1-wm31-perf32) · [Download the full bundle containing V7.1](https://github.com/naojiren/wm-usb-manager/releases/download/v1.4.1-wm31-perf32/WM-USB-Manager-1.4.1-wm31-perf32-full.zip). Extract `apk/3高德_V7.1.0.600067_仪表盘投屏版.apk` into your USB's `apk` directory. Installing V7.1 over V9.1 is a downgrade and may be blocked: uninstall V9.1 first if needed, and back up any required navigation data before changing major versions.

Full 1.4.5 includes 14 installation APKs and 13 local factory recovery APKs: Launcher wm32, WePlay perf32, optional Amap single-app test 9 and QQ Music Car 3.15.0.3. Both new apps are unchecked by default. Lite contains only the original five default apps, without Amap, QQ Music or factory backups.

Public bundles retain the exact APKs, wizard, factory recovery packages and bootstrap files from the local deliveries. Only documentation paths are sanitized and this experimental notice added; use the attached checksums. Extract to the USB root and reinsert to replace both the wizard and its root worker. Remove obsolete APKs listed in the bundled deletion list; overlay extraction does not remove them.

Both editions compile and pass signing checks and 345 policy assertions. These checks do not establish real vehicle navigation, projection, CarPlay or playback behavior. WePlay perf32 is also a test build and requires existing local ADB authorization for scan protection.
