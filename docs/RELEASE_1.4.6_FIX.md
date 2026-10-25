## 1.4.6 原位更新：高德残留数据修复

本Release保持1.4.6地址和下载文件名，内置向导更新为1.4.7 / 1.4.7-lite（versionCode 10），延续原签名。

## 本次修复

高德已卸载但保留应用数据时，旧向导调用普通清理入口会返回Failed，导致安装前停止。现在区分正常已安装、仅残留数据及没有旧记录三种状态；残留数据通过Android系统包管理服务仅清理用户0，等待系统明确确认成功后再安装选中的高德。正常安装沿用原清理方式；无旧记录跳过。失败或超时仍停止，不直接删除目录，不自动卸载软件。

启动脚本兼容车机中已安装的较新向导：Android拒绝旧U盘向导降级时，继续使用车机新版并重启对应后台。完整包和精简包均已同步。

## 使用与提醒

退出旧向导，完整解压所选U盘包到U盘根目录，再拔插U盘，通过原厂入口启动，使向导界面及后台一起更新。覆盖解压不删除旧APK，请按“删除旧文件清单.txt”整理。安装高德前清除应用数据可能导致收藏、账号、设置和离线地图丢失，请先保存需要的内容。清理数据不解决不同签名冲突。

完整包仍为14个安装APK、13个原厂恢复APK；精简包仍为5个默认安装APK、无恢复包。附带桌面wm32、WePlay perf32、高德测试11、QQ音乐等应用APK逐字节保持前版。高德与QQ音乐默认不选，apk文件夹可自由增减。perf数字比较、存储显示和其它功能保持原行为。

高德V9.1仍在测试中；如无法使用，请使用1.4.1完整包中的V7.1。切换版本前保存数据，降级可能需先卸载V9.1。

## 验证

两版编译、API23签名及签名连续性检查通过，462项策略检查通过；3项Binder回调协议检查在车机通过。已通过ADB将同一签名的1.4.7向导及后台部署到车机，用户随后确认高德安装验证成功。这只确认该车机上的本次安装流程，不代表导航、投屏及所有车机均已验证。公开包CRC、文件清单、附带APK哈希及源码对应关系已核对。

修复源码见本Release随附source.zip及仓库main分支。原1.4.6 Git标签保留为发布基线；使用附件源码包可取得本次修复对应代码。

**高德 V9.1 目前还在测试中，如无法使用，请使用1.4.1完整包中的 V7.1。**

[1.4.1 发布页](https://github.com/naojiren/wm-usb-manager/releases/tag/v1.4.1-wm31-perf32) · [下载含 V7.1 的完整包](https://github.com/naojiren/wm-usb-manager/releases/download/v1.4.1-wm31-perf32/WM-USB-Manager-1.4.1-wm31-perf32-full.zip)

---

# WM Head Unit Software Wizard: in-place 1.4.6 release fix

The existing 1.4.6 release URL and asset names are retained. Bundled wizards are 1.4.7 / 1.4.7-lite, versionCode 10, with the previous signing identity.

Fixes installation when Amap was uninstalled with data retained: data-only packages use the Android package service for user 0 and require a successful system callback before installation. Installed packages retain the original clear-data flow; no prior record skips clearing. Failures/timeouts stop the operation. No direct directory deletion or automatic uninstall is added. The USB startup script now keeps a newer installed wizard when Android rejects a downgrade from an older USB copy, and restarts the matching worker.

Exit the old wizard, extract all USB contents and reinsert through the factory entry. Clearing Amap data may remove favorites, account state, settings and offline maps: save required content first. Data clearing cannot resolve signing-key conflicts. Full retains 14 installation APKs and 13 factory recovery APKs; Lite retains five default APKs. All bundled application APKs, including Amap test 11, Launcher wm32 and WePlay perf32, are unchanged. Numeric perf update comparison remains.

Both editions compile, pass API23 signing verification and 462 policy checks; three local Binder callback checks passed on the vehicle. The same 1.4.7 wizard and worker were deployed using ADB, after which the user confirmed the Amap installation succeeded. This does not establish navigation, projection or compatibility with every vehicle. CRCs, payload hashes and matching source are verified.

Use the attached source.zip or the main branch for this fix. The original 1.4.6 Git tag is retained as its release baseline. Amap V9.1 remains experimental; V7.1 is available in the older 1.4.1 full release.
