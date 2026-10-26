# 威马软件管家 1.4.9

应用桌面名称、界面标题、操作提示及备用键盘名称已统一改为“威马软件管家”。包名及签名延续原版，可直接覆盖更新。

## 本次软件更新

- **威马桌面 wm35**：更新首页音乐卡片和原车蓝牙音乐信息/控制、后台清理及音乐入口选择行为；实际自动播放和歌词显示需实车确认。
- **WePlay perf34 蓝牙默认设置测试版**：默认开启原车蓝牙音频，默认停用WePlay麦克风采集；升级首次启动应用一次，之后保留用户手动选择。仍需手机连接原车蓝牙并使用对应音源，不能保证Siri使用手机麦克风。
- **完整包高德 V9.1 单APP测试15**：速度表位置、退出指引残留清理和巡航红绿灯显示调整，仪表后台地图继续投屏。真实灯组数据及显示效果需实车验证。
- ADB工具使用adb_toggle.apk；安装默认仍为ADB工具、WePlay/DiPlay、威马桌面、ES文件管理器、强制旋转。高德和QQ音乐默认不选。

## 删除、恢复与高德安装

BTPhone实际包名为com.yftech.btphone，默认勾选删除，用户可取消。删除项红字提示“蓝牙电话与 CarPlay 的蓝牙音频冲突”。蓝牙电话/拨号入口可能因此不可用，底层蓝牙和车辆服务保留。完整包恢复页固定优先显示原厂音乐和BTPhone，使用已有系统包或本地原厂APK恢复，不重新从车机备份。

高德安装会清除应用数据。普通高德签名冲突时完整卸载旧高德及其数据后重装已校验的新APK；失败或残留记录未清除则停止，不无限重试。系统高德、多用户或用户状态不可核实时不自动完整卸载。收藏、账号、设置和离线地图可能丢失，请先保存需要的内容。

## 包内容与使用

管家1.4.9 / 1.4.9-lite，versionCode均为12，保持原包名com.wm.usbmanager和签名。完整包14个安装APK、14个本地原厂恢复APK；精简包只含5个默认安装APK，没有原厂恢复包或恢复页面。需要恢复原厂软件请使用完整包。

退出旧管家，将所选包完整解压到U盘根目录，再拔插U盘，从原厂入口重新启动，同时更新界面和后台。覆盖解压不会删除旧APK，请按“删除旧文件清单.txt”清理旧wm32桌面、WePlay perf33、旧高德测试12及旧ADB工具，避免同包名重复选择。其它自行添加的APK可保留。启动脚本兼容车机中已安装的较新管家版本。

高德V9.1和WePlay perf34仍为测试版本。高德不可用时可使用1.4.1完整包中的V7.1；切换前保存数据，降级可能需先卸载V9.1。

## 验证范围

两版编译、API23签名及签名连续性检查通过，522项检查通过；三份新应用与前包同包名、同签名且版本未降低。应用名称、压缩包CRC、全部APK哈希、默认项、恢复清单和源码对应关系已核对。本次未连接车机ADB，也未执行新版本的车机安装、BTPhone删除、原厂恢复、高德重装或导航/音频效果测试。

**高德 V9.1 目前还在测试中，如无法使用，请使用1.4.1完整包中的 V7.1。**

[1.4.1发布页](https://github.com/naojiren/wm-usb-manager/releases/tag/v1.4.1-wm31-perf32) · [下载含V7.1的完整包](https://github.com/naojiren/wm-usb-manager/releases/download/v1.4.1-wm31-perf32/WM-USB-Manager-1.4.1-wm31-perf32-full.zip)

---

# WM Software Manager (威马软件管家) 1.4.9

The app label, page heading, user-facing messages and backup keyboard are renamed to 威马软件管家. The package ID and signing identity remain unchanged for in-place upgrades; both editions use versionCode 12.

Both editions include Launcher wm35 and WePlay perf34; Full also includes Amap V9.1 single-app test 15. WePlay perf34 defaults factory Bluetooth audio to on and its microphone capture to off, applying these once on upgrade and then preserving manual choices. Real Bluetooth/voice behavior requires vehicle testing. Amap test 15 changes speed display placement, old guidance cleanup and cruise traffic-light display; real map/traffic data behavior still needs vehicle testing.

BTPhone (com.yftech.btphone) is checked by default for removal and can be unchecked, with a red Bluetooth/CarPlay conflict message. Phone/dialer entry points may become unavailable; underlying Bluetooth and vehicle services remain protected. Full always offers factory Music and BTPhone restoration using retained system APKs or local factory packages.

Amap installation clears application data. A conflicting ordinary Amap signing identity triggers a full uninstall and a bounded reinstall of the validated selected APK. System packages, multiple/unknown users, failed uninstalls or retained package records stop the process. Save favorites, accounts, settings and offline maps first.

Full contains 14 installation APKs and 14 factory recovery APKs; Lite contains the five default installation APKs without recovery files or a restore page. Exit the old app, extract the complete USB bundle and reinsert through the factory entry to restart the updated worker. Remove obsolete APKs according to the included deletion list; overlay extraction does not delete them.

Both editions compile, pass API23 signing checks and 522 policy assertions. New input APKs retain their original package/signing identities without downgrades. Labels, CRCs, payload hashes, defaults, recovery manifests and matching source are verified. No vehicle ADB connection or vehicle runtime testing was performed for this release. Amap V9.1 and WePlay perf34 remain test builds; Amap V7.1 is available in the older 1.4.1 full bundle.
