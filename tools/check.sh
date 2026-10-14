#!/usr/bin/env bash
set -euo pipefail
task_root=$(cd "$(dirname "$0")/.." && pwd)
for task_variant in full lite; do
    task_output="$task_root/build/$task_variant/policy-tests"
    mkdir -p "$task_output"
    javac -encoding UTF-8 -d "$task_output" \
        "$task_root/$task_variant/src/com/wm/usbmanager/Policy.java" \
        "$task_root/$task_variant/src/com/wm/usbmanager/UpdatePolicy.java" \
        "$task_root/$task_variant/tools/PolicyTest.java" \
        "$task_root/$task_variant/tools/UpdatePolicyTest.java"
    java -cp "$task_output" com.wm.usbmanager.PolicyTest
    java -cp "$task_output" com.wm.usbmanager.UpdatePolicyTest
done
