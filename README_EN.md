# WM Head Unit Software Wizard 1.4.5

## Amap V9.1 experimental notice

**Amap V9.1 is still being tested. If it does not work, use V7.1 from the previous full release.**

[Previous 1.4.1 release](https://github.com/naojiren/wm-usb-manager/releases/tag/v1.4.1-wm31-perf32) · [Full bundle containing V7.1](https://github.com/naojiren/wm-usb-manager/releases/download/v1.4.1-wm31-perf32/WM-USB-Manager-1.4.1-wm31-perf32-full.zip). A downgrade from V9.1 may require uninstalling the current Amap app first. Preserve any needed navigation data before changing major versions.


[简体中文](README.md)

The full bundle uses WM Launcher wm32 and the latest WePlay conversation build, perf32 scan-protection test (unchanged from the previous bundle). Both old Amap APKs (V7.1 and V9.1 test 2) and WM-Amap-Instrument-Companion 1.0.44 are removed. They are replaced by the supplied Amap V9.1 single-app test 9, combining instrument guidance and background projection, and QQ Music Car 3.15.0.3. Both new apps are optional and unchecked. Full contains 14 installation APKs and 13 factory recovery APKs. Lite retains only the original five default APKs and also uses wm32.

Extract into the USB root and reinsert to replace the wizard and root worker. Overlay extraction does not delete obsolete files: remove the old USB APKs listed in 删除旧文件清单.txt, including the two old Amap packages, old companion, and wm31 launcher. Other user-added APKs can remain. Replacing USB files does not uninstall applications already installed on the vehicle.

VersionCode is 8 for both variants. Existing lightweight scanning, optional cleanup, storage display, five default selections, and system-partition launcher installation remain. Cleanup preserves backups and application data; zero cleanup candidates does not mean zero available storage.

Both wizard variants compile, match the prior signing identity and pass 345 policy assertions. Launcher wm32, Amap test 9 and QQ Music pass Android 6 signature verification; WePlay matches the prior verified APK exactly. Bundle CRCs, manifest hashes, app counts, defaults and unchanged payload bytes are checked. Vehicle installation, navigation, projection and music playback were not tested in this packaging task. QQ Music was read from the supplied local H drive; no USB synchronization was performed. See [BUILD](docs/BUILD.md).
