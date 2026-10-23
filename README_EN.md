# WM Head Unit Software Wizard 1.4.0

Full bundle 1.4.1 adds optional Amap V9.1 and Instrument Companion 1.0.44. Both are unchecked by default; Amap V7.1 is retained as an alternative. Lite remains 1.4.0.

[简体中文](README.md)

Includes WM Launcher wm31 and WePlay perf32 scan-protection test build. USB mode offers removal, APK installation and (full edition) factory restoration. Launcher installation defaults to the system partition and requires an existing root channel, writable storage and a matching signing certificate. Restart manually after successful system installation.

Step two includes Install wizard on head unit. The USB bootstrap already installs the wizard; this action verifies installation and creates its local workspace without copying bundled applications or backups. Opening the wizard from the app launcher provides only removal and online update/install for WM Launcher and WePlay. The standalone wizard APK includes no bundled applications.

Resident mode requires existing su authorization, including after reboot. Without su, use the factory USB entry. It checks official GitHub releases, hashes, package identity, architecture, signatures and version codes, and rejects downgrades. The locally bundled perf32 test build may be newer than the latest public release.

Both editions compile and pass signature verification and 199 policy assertions. No ADB or vehicle connection was made. System partition writes, resident privileges and visual appearance need physical validation. See docs/BUILD.md for building.
