#!/system/bin/sh
# Factory entry launches this as root. --resume is used only by an already available su.
set -u
umask 077
task_usb=/mnt/udisk
task_state=/data/local/tmp/wm-usb-manager
task_source="$task_usb/wm_manager/manager.apk"
task_pkg=com.wm.usbmanager
mkdir -p "$task_state"
chmod 711 "$task_state"
exec >> "$task_state/bootstrap.log" 2>&1
echo "=== USB manager $(date) ==="
if [ "$(id -u)" != 0 ]; then
    echo "ERROR: factory entry did not provide root"
    exit 1
fi
if [ ! -f "$task_usb/factorymodel" ] || [ ! -f "$task_source" ]; then
    echo "ERROR: complete USB package is required"
    exit 1
fi
if [ -f "$task_state/worker.pid" ]; then
    task_old_pid=$(cat "$task_state/worker.pid")
    case "$task_old_pid" in
        *[!0-9]*|'') ;;
        *)
            if [ -r "/proc/$task_old_pid/cmdline" ] && tr '\000' ' ' < "/proc/$task_old_pid/cmdline" | grep -q 'com.wm.usbmanager.RootBridge'; then
                if [ "${1:-}" = "--resume" ]; then exit 0; fi
                kill "$task_old_pid"
                sleep 1
            fi
            ;;
    esac
fi
task_installed=$(pm path "$task_pkg" 2>/dev/null)
task_installed=${task_installed#package:}
task_match=0
if [ -n "$task_installed" ] && [ -f "$task_installed" ]; then
    task_source_md5=$(md5sum "$task_source")
    task_source_md5=${task_source_md5%% *}
    task_installed_md5=$(md5sum "$task_installed")
    task_installed_md5=${task_installed_md5%% *}
    if [ "$task_source_md5" = "$task_installed_md5" ]; then task_match=1; fi
fi
if [ "$task_match" != 1 ]; then
    cp "$task_source" "$task_state/bootstrap.apk" || exit 1
    chmod 644 "$task_state/bootstrap.apk"
    task_install_result=$(pm install -r -g "$task_state/bootstrap.apk" 2>&1)
    echo "$task_install_result"
    case "$task_install_result" in
        *Success*) ;;
        *INSTALL_FAILED_VERSION_DOWNGRADE*)
            # The vehicle was explicitly updated; keep its newer signed wizard.
            if [ -z "$task_installed" ] || [ ! -f "$task_installed" ]; then
                echo "ERROR: newer installed manager is unavailable"; exit 1
            fi
            echo "USB manager is older; using the newer installed manager"
            ;;
        *) echo "ERROR: manager install failed"; exit 1;;
    esac
    rm -f "$task_state/bootstrap.apk"
fi
task_installed=$(pm path "$task_pkg" 2>/dev/null)
task_installed=${task_installed#package:}
if [ -z "$task_installed" ] || [ ! -f "$task_installed" ]; then exit 1; fi
# Show the page first. It waits for the worker and scans asynchronously.
if [ "${1:-}" != "--resume" ]; then
    echo "UI_START $(cat /proc/uptime)"
    am start --user 0 -n "$task_pkg/.MainActivity" --ez usb_mode true
fi
CLASSPATH="$task_installed" toybox nohup /system/bin/app_process /system/bin com.wm.usbmanager.RootBridge > "$task_state/worker.log" 2>&1 < /dev/null &
task_pid=$!
echo "$task_pid" > "$task_state/worker.pid"
task_wait=0
while [ "$task_wait" -lt 45 ]; do
    if grep -q '^READY ' "$task_state/worker.log"; then break; fi
    if ! kill -0 "$task_pid" 2>/dev/null; then cat "$task_state/worker.log"; exit 1; fi
    sleep 1
    task_wait=$((task_wait + 1))
done
if ! grep -q '^READY ' "$task_state/worker.log"; then echo "ERROR: worker startup timeout"; exit 1; fi
echo "USB manager ready"
