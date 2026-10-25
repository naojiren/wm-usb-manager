# WM Head Unit Software Wizard: in-place 1.4.6 release fix

The existing 1.4.6 release URL and asset names are retained. Bundled wizards are 1.4.7 / 1.4.7-lite, versionCode 10, with the previous signing identity.

Fixes installation when Amap was uninstalled with data retained: data-only packages use the Android package service for user 0 and require a successful system callback before installation. Installed packages retain the original clear-data flow; no prior record skips clearing. Failures/timeouts stop the operation. No direct directory deletion or automatic uninstall is added. The USB startup script now keeps a newer installed wizard when Android rejects a downgrade from an older USB copy, and restarts the matching worker.

Exit the old wizard, extract all USB contents and reinsert through the factory entry. Clearing Amap data may remove favorites, account state, settings and offline maps: save required content first. Data clearing cannot resolve signing-key conflicts. Full retains 14 installation APKs and 13 factory recovery APKs; Lite retains five default APKs. All bundled application APKs, including Amap test 11, Launcher wm32 and WePlay perf32, are unchanged. Numeric perf update comparison remains.

Both editions compile, pass API23 signing verification and 462 policy checks; three local Binder callback checks passed on the vehicle. The same 1.4.7 wizard and worker were deployed using ADB, after which the user confirmed the Amap installation succeeded. This does not establish navigation, projection or compatibility with every vehicle. CRCs, payload hashes and matching source are verified.

Use the attached source.zip or the main branch for this fix. The original 1.4.6 Git tag is retained as its release baseline. Amap V9.1 remains experimental; V7.1 is available in the older 1.4.1 full release.
