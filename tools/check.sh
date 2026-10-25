#!/usr/bin/env bash
set -euo pipefail
task_root=$(cd "$(dirname "$0")/.." && pwd)
for task_variant in full lite; do
    task_output="$task_root/build/$task_variant/policy-tests"
    mkdir -p "$task_output"
    javac -encoding UTF-8 -d "$task_output" \
        "$task_root/$task_variant/src/com/wm/usbmanager/AmapInstall.java" \
        "$task_root/$task_variant/tools/AmapInstallTest.java" \
        "$task_root/$task_variant/src/com/wm/usbmanager/Policy.java" \
        "$task_root/$task_variant/src/com/wm/usbmanager/UpdatePolicy.java" \
        "$task_root/$task_variant/src/com/wm/usbmanager/CleanupPolicy.java" \
        "$task_root/$task_variant/src/com/wm/usbmanager/StoragePolicy.java" \
        "$task_root/$task_variant/src/com/wm/usbmanager/ScanPolicy.java" \
        "$task_root/$task_variant/tools/ScanPolicyTest.java" \
        "$task_root/$task_variant/tools/StoragePolicyTest.java" \
        "$task_root/$task_variant/tools/CleanupPolicyTest.java" \
        "$task_root/$task_variant/tools/PolicyTest.java" \
        "$task_root/$task_variant/tools/UpdatePolicyTest.java"
    java -cp "$task_output" com.wm.usbmanager.AmapInstallTest
    java -cp "$task_output" com.wm.usbmanager.PolicyTest
    java -cp "$task_output" com.wm.usbmanager.UpdatePolicyTest
    java -cp "$task_output" com.wm.usbmanager.CleanupPolicyTest
    java -cp "$task_output" com.wm.usbmanager.StoragePolicyTest
    java -cp "$task_output" com.wm.usbmanager.ScanPolicyTest
done
