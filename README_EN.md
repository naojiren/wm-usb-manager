# WM Software Manager (威马软件管家) 1.4.9

The app label, page heading, user-facing messages and backup keyboard are renamed to 威马软件管家. The package ID and signing identity remain unchanged for in-place upgrades; both editions use versionCode 12.

Both editions include Launcher wm35 and WePlay perf34; Full also includes Amap V9.1 single-app test 15. WePlay perf34 defaults factory Bluetooth audio to on and its microphone capture to off, applying these once on upgrade and then preserving manual choices. Real Bluetooth/voice behavior requires vehicle testing. Amap test 15 changes speed display placement, old guidance cleanup and cruise traffic-light display; real map/traffic data behavior still needs vehicle testing.

BTPhone (com.yftech.btphone) is checked by default for removal and can be unchecked, with a red Bluetooth/CarPlay conflict message. Phone/dialer entry points may become unavailable; underlying Bluetooth and vehicle services remain protected. Full always offers factory Music and BTPhone restoration using retained system APKs or local factory packages.

Amap installation clears application data. A conflicting ordinary Amap signing identity triggers a full uninstall and a bounded reinstall of the validated selected APK. System packages, multiple/unknown users, failed uninstalls or retained package records stop the process. Save favorites, accounts, settings and offline maps first.

Full contains 14 installation APKs and 14 factory recovery APKs; Lite contains the five default installation APKs without recovery files or a restore page. Exit the old app, extract the complete USB bundle and reinsert through the factory entry to restart the updated worker. Remove obsolete APKs according to the included deletion list; overlay extraction does not delete them.

Both editions compile, pass API23 signing checks and 522 policy assertions. New input APKs retain their original package/signing identities without downgrades. Labels, CRCs, payload hashes, defaults, recovery manifests and matching source are verified. No vehicle ADB connection or vehicle runtime testing was performed for this release. Amap V9.1 and WePlay perf34 remain test builds; Amap V7.1 is available in the older 1.4.1 full bundle.
