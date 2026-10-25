## 威马车机软件向导 1.4.6

- **高德安装流程**：第二步选中高德后，先校验安装包及兼容性，再自动清除高德应用数据，最后安装。清理失败停止该项安装。界面、进度和结果明确提示。
- **数据清理提醒**：高德收藏、账号、设置及离线地图可能丢失，请先保存需要的内容。清理应用数据不会消除签名冲突；出现 `INSTALL_FAILED_UPDATE_INCOMPATIBLE` 时仍需完整卸载冲突的旧高德后安装。本版不会自动卸载旧应用。
- **WePlay / DiPlay 更新判断**：按APK文件名的perf数字比较，数字越大越新；相同perf不升级、较小perf不覆盖本地较大版本。查询最近100条已发布Release（含测试版、不含草稿）中的兼容ARMv7安装包。继续校验包名、签名、哈希和Android兼容性。
- **高德一致性**：完整包中的高德与用户提供的测试11完全一致，SHA-256：`c8fb3f9b543cacdff1b59d2bead0e7add7a5f63c7f1fe7a44d498e378d38b2e0`。

## 下载

- **full 完整包**：14个安装APK、13个本地原厂恢复APK。附带桌面wm32、WePlay perf32、高德V9.1测试11、QQ音乐等；高德和QQ音乐默认不选。
- **lite 精简包**：仅ADB工具、DiPlay、威马桌面、ES文件管理器、强制旋转5个默认软件，不含高德、QQ音乐或原厂恢复包；自行加入高德APK后同样使用数据清理流程。
- 附对应完整源码、每包SHA-256和汇总校验文件。两版向导versionCode均为9，签名延续前版。

退出旧向导，完整解压到U盘根目录，再拔插U盘，通过原厂入口重新启动，同时替换界面和后台程序。覆盖解压不会删除旧APK，请按包内“删除旧文件清单.txt”清理，特别是旧高德测试9文件。其它自行添加的APK可保留。

## 高德 V9.1 测试提示

**高德 V9.1 目前还在测试中，如无法使用，请使用上一版本完整包中的 V7.1。**

[1.4.1发布页（含V7.1）](https://github.com/naojiren/wm-usb-manager/releases/tag/v1.4.1-wm31-perf32) · [下载含V7.1的完整包](https://github.com/naojiren/wm-usb-manager/releases/download/v1.4.1-wm31-perf32/WM-USB-Manager-1.4.1-wm31-perf32-full.zip)。解压取出 `apk/3高德_V7.1.0.600067_仪表盘投屏版.apk`。V9.1覆盖安装V7.1属于降级，可能需先卸载V9.1；请先保存所需数据。

## 验证

两版编译、Android 6/API23签名验证及444项检查通过；签名连续性、ZIP CRC、清单和全部APK哈希已核对。公开包与本地1.4.6的向导、附带软件、原厂恢复包和启动文件逐字节一致，仅调整公开说明及移除开发机路径。未进行实车安装、应用数据清理、导航或投屏测试。

---

## English

Selecting Amap in step 2 now clears its application data after APK validation and before installation. A failed clear stops that installation. Favorites, accounts, settings and offline maps may be lost: save required content first. Clearing data does not fix signing-key conflicts; fully uninstall the conflicting old Amap separately if needed. The wizard does not automatically uninstall it.

WePlay/DiPlay updates compare the numeric perf suffix in APK filenames: larger is newer, equal numbers are not upgrades, and lower online perf numbers cannot replace higher local files. The checker considers up to 100 published releases, including prereleases and excluding drafts. Identity, signature, hash, architecture and Android compatibility checks remain.

Full contains 14 installation APKs and 13 factory recovery APKs; Lite contains only the original five default APKs. Bundled Launcher wm32, WePlay perf32 and other APKs are unchanged. Amap test 11 exactly matches the supplied APK. Both wizards retain their signing identity and use versionCode 9. Extract all USB contents and reinsert the USB to restart the updated worker; remove obsolete APKs according to the bundled deletion list.

**Amap V9.1 is still experimental. If unusable, use V7.1 from [release 1.4.1](https://github.com/naojiren/wm-usb-manager/releases/tag/v1.4.1-wm31-perf32) ([full bundle](https://github.com/naojiren/wm-usb-manager/releases/download/v1.4.1-wm31-perf32/WM-USB-Manager-1.4.1-wm31-perf32-full.zip)).** Switching from V9.1 to V7.1 may require uninstalling V9.1 first; save required navigation data.

Both editions compile, verify for Android 6/API23 and pass 444 checks. ZIP CRCs and all payload hashes are verified. No real-vehicle installation, data clearing, navigation or projection test was performed.
