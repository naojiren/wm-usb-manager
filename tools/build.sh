#!/usr/bin/env bash
set -euo pipefail
task_root=$(cd "$(dirname "$0")/.." && pwd)
if [ "$#" -ne 1 ]; then echo "Usage: bash tools/build.sh full|lite" >&2; exit 2; fi
task_variant="$1"
case "$task_variant" in full|lite) ;; *) echo "Unknown variant" >&2; exit 2;; esac
task_sdk=$(printenv ANDROID_SDK_ROOT || printenv ANDROID_HOME || true)
if [ -z "$task_sdk" ]; then echo "Set ANDROID_SDK_ROOT or ANDROID_HOME" >&2; exit 2; fi
task_buildtools=$(printenv WM_BUILD_TOOLS || true)
if [ -z "$task_buildtools" ]; then task_buildtools=36.0.0; fi
task_bt="$task_sdk/build-tools/$task_buildtools"
task_platform="$task_sdk/platforms/android-36/android.jar"
task_source="$task_root/$task_variant"
task_output="$task_root/build/$task_variant"
for task_tool in aapt d8 zipalign apksigner; do
    test -x "$task_bt/$task_tool" || { echo "Missing SDK tool: $task_tool" >&2; exit 2; }
done
test -f "$task_platform"
mkdir -p "$task_output/classes" "$task_output/dex"
javac -encoding UTF-8 -source 8 -target 8 -classpath "$task_platform" \
    -d "$task_output/classes" "$task_source"/src/com/wm/usbmanager/*.java
"$task_bt/aapt" package -f -M "$task_source/AndroidManifest.xml" \
    -S "$task_source/res" -I "$task_platform" -F "$task_output/resources.apk"
jar cf "$task_output/classes.jar" -C "$task_output/classes" .
"$task_bt/d8" --min-api 23 --lib "$task_platform" --output "$task_output/dex" "$task_output/classes.jar"
cp "$task_output/resources.apk" "$task_output/manager-unsigned.apk"
( cd "$task_output/dex" && zip -q -u ../manager-unsigned.apk classes.dex )
"$task_bt/zipalign" -f 4 "$task_output/manager-unsigned.apk" "$task_output/manager-unsigned-aligned.apk"
task_keystore=$(printenv WM_KEYSTORE || true)
if [ -n "$task_keystore" ]; then
    task_password_file=$(printenv WM_KEYSTORE_PASSWORD_FILE || true)
    test -f "$task_password_file"
    task_alias=$(printenv WM_KEY_ALIAS || true)
    if [ -z "$task_alias" ]; then task_alias=manager; fi
    "$task_bt/apksigner" sign --ks "$task_keystore" --ks-pass "file:$task_password_file" \
        --ks-key-alias "$task_alias" --out "$task_output/manager.apk" "$task_output/manager-unsigned-aligned.apk"
    "$task_bt/apksigner" verify --verbose --min-sdk-version 23 "$task_output/manager.apk"
fi
echo "Build complete: $task_variant"
