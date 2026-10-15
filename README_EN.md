# WM Vehicle Software Wizard

[简体中文](README.md) | **English**

A USB software wizard for supported Weltmeister (WM) CQ1301 head units running Android 6.0.1 on ARMv7. It opens automatically from a prepared USB drive and provides application removal, local APK installation, and restoration of factory applications.

The application displays its Chinese title, “威马车机软件向导”. This document is the English version of the project README.

## Downloads

Download the full or lite bundle from [Releases](https://github.com/naojiren/wm-usb-manager/releases) and verify it with the supplied SHA-256 files.

The current wizard is version 1.3.0. The bundles include WM Launcher wm26 (versionCode 1026) and DiPlay / WePlay perf29-screen-keys (versionCode 73).

| Bundle | Installation APKs | Factory restoration APKs |
|---|---|---|
| Full | 14 APKs; the first five are selected by default, and the others are optional | Includes 11 existing local restoration APKs supplied by the user |
| Lite | ADB Tool, DiPlay, WM Launcher, ES File Explorer, and Force Rotation only | Does not create backups or include factory restoration APKs |

## Usage

1. Extract the ZIP contents directly to the root of a FAT32 USB drive. Do not place them inside another folder. Keep factorymodel, certificates.xml, yfautorun.sh, yfautorun.md5, and wm_manager.
2. With the vehicle parked, insert the drive into a supported USB port on the head unit. Wait for “威马车机软件向导”. The page opens first, then displays the application scanning status.
3. The removal page selects the factory Baidu Maps application and factory voice keyboard by default when they are still installed. Third-party applications appear in a separate group and are not selected by default. Select applications and run removal, or skip removal to continue to installation.
4. The installation page reads the apk folder at the root of the drive. The five default applications appear in this order: ADB Tool, DiPlay, WM Launcher, ES File Explorer, and Force Rotation. You can deselect them, add other APKs and refresh the list, then install the selected applications with one button.
5. The full version can restore factory applications using the included local APKs from the “恢复已移除软件” page. The lite version has no restoration page; use the full bundle when restoration is needed.

Reinserting the drive only opens the wizard. Batch removal and installation require the user to click the corresponding button. APKs can be added or removed freely; scanning covers APK files directly inside the apk folder, not its subfolders. Declared runtime permissions are granted by default during installation. Special settings such as overlay access, accessibility, the default launcher, and the default input method must still be configured through the application or Android settings.

## Removal Scope

Factory applications are removed for the current Android user while retaining their data. Their original APKs are not deleted from the system partition, and the wizard does not extract backups from the vehicle. Removing factory navigation also removes its navigation functions and associated instrument-cluster output. Before removing the factory voice keyboard, the wizard switches to an available keyboard; if switching fails, removal is cancelled.

The factory removal list covers navigation, the input method, entertainment applications, and their associated cards. Vehicle control, reversing-camera services, instrument-cluster components, TBOX, Bluetooth, location services, system updates, and core audio services are excluded. Third-party scanning and removal protect system applications, the current launcher and input method, persistent foundational services, applications sharing a system identity, and enabled device-administration and accessibility services.

## Manual Updates

The installation page can manually check public Releases for [WM Launcher](https://github.com/naojiren/wm-launcher/releases) and [DiPlay](https://github.com/naojiren/diplay-wm/releases). Downloads are checked for their hash, signing certificate, package name, version, and architecture before replacing the APK on the USB drive. The updated application must then be selected and installed explicitly.

Known limitation: wizard 1.3.0 only accepts DiPlay release assets whose filenames start with diplay-wm-ex5-. The bundled WePlay-perf29-armv7.apk does not follow this naming convention. The installation page can scan and install the local APK; online checks reflect public Releases and may differ from the bundled version. The wizard application and its online-update logic remain unchanged.

## Compatibility and Privileges

Automatic startup and the privileged worker use an existing factory engineering entry point on the target head unit. The device must actually support that entry point. If the channel becomes unavailable, the wizard attempts to use an existing su; otherwise, the USB drive must be reinserted to invoke the factory entry point again. The tool cannot obtain root on a device that provides neither a usable factory entry point nor su.

The privilege channel and application pages were checked using a simulated USB mount. Physical USB insertion and all driving scenarios have not undergone complete vehicle-level validation. Compatibility depends on the actual target firmware.

## Source and Build

- full: Android Java source, resources, Manifest, startup program, and tests for the full version.
- lite: the lite version, which retains removal and installation while omitting factory restoration.
- usb-bootstrap: the USB startup script from the delivered bundle.
- tools: portable build and policy-test entry points.
- docs: software inventory, source hashes, build instructions, and verification notes.

Application source, resources, and startup programs were checked against the locally delivered versions. Public source and Release exports omit historical vehicle diagnostics, development-machine paths, signing private keys, and credentials. Application APKs, startup files, and the full bundle's factory restoration APKs retain their original delivered content.

Builds use JDK 17, Android SDK Platform 36, and Build Tools 36.0.0. The runtime target remains Android 6 / API 23.

~~~sh
export ANDROID_SDK_ROOT=/path/to/android-sdk
sdkmanager "platforms;android-36" "build-tools;36.0.0"
bash tools/check.sh
bash tools/build.sh full
bash tools/build.sh lite
~~~

The default outputs are unsigned APKs and cannot be installed directly. Signing requires your own keystore; its key must match an installed application's signing certificate to update that application. The original delivery's signing private key is not published.

See the [build instructions](docs/BUILD.md), [verification notes](docs/VERIFICATION.md), and [third-party notices](THIRD_PARTY_NOTICES.md). These supporting documents are currently in Chinese.

The source is publicly available, but this repository currently specifies no source-code license. Public visibility does not change the licenses of third-party APKs or factory files.
