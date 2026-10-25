# WM Head Unit Software Wizard 1.4.6

[简体中文](README.md)

The bundled Amap test 11 is byte-for-byte identical to the supplied APK (SHA-256 c8fb3f9b543cacdff1b59d2bead0e7add7a5f63c7f1fe7a44d498e378d38b2e0).

Selecting Amap in step 2 now clears its Android application data after APK validation and before installation. Favorites, accounts, settings and offline maps may be lost: save needed content first. Unselected apps and other packages are unaffected. A failed clear stops that installation; a fresh installation without an existing Amap record skips clearing. The card, progress and result explain the operation. Clearing data does not resolve a signing-key conflict: an incompatible old Amap package must be fully uninstalled separately; this wizard does not automatically uninstall it.

WePlay/DiPlay updates compare the numeric perf suffix in APK filenames. perf32 is newer than perf22, and perf100 is newer than perf99. Equal perf numbers do not trigger an upgrade, and lower online perf numbers cannot replace higher local ones. The checker examines up to 100 published releases, including prereleases and excluding drafts, for compatible ARMv7 assets. Missing local perf numbers produce an explicit unknown-version message. Launcher updates retain the existing official release source. Identity, signature, hash, architecture and Android compatibility checks remain.

Full contains 14 installation APKs and 13 factory recovery APKs; Lite contains the five existing default APKs and no factory backups. All bundled application APKs match the previous bundles. Both wizards use versionCode 9 and retain the previous signing identity. Exit the old wizard, extract the full USB contents and reinsert the USB to restart the updated worker. Overlay extraction does not delete obsolete APKs; follow the bundled deletion list.

Amap V9.1 remains experimental. If it is unusable, use V7.1 from the previous 1.4.1 full bundle; changing versions may require uninstalling V9.1 and saving required data first.

Both variants compile, verify for Android 6/API23, and pass 444 checks. ZIP CRCs, payload hashes, app counts, factory recovery packages and unchanged bootstrap files are verified. The build involved no USB synchronization, vehicle ADB connection or vehicle runtime testing. This public delivery contains verified bundles and source. See [BUILD](docs/BUILD.md).
