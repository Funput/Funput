#!/usr/bin/env bash
# Scroll benchmark for the glass spike: for each mode, launch, reset frame stats, fling the list
# up and down ten times, then print the gfxinfo summary. Run after:
#   ./gradlew :glass-spike:assembleProfiling
#   adb install -r glass-spike/build/outputs/apk/profiling/glass-spike-profiling.apk
set -euo pipefail

package=app.funput.funput.glassspike
runs="${RUNS:-3}"

for run in $(seq 1 "$runs"); do
    for mode in solid haze liquid; do
        adb shell am force-stop "$package"
        adb shell am start -W -n "$package/.SpikeActivity" --es mode "$mode" > /dev/null
        sleep 2
        adb shell dumpsys gfxinfo "$package" reset > /dev/null
        for _ in $(seq 1 10); do
            adb shell input swipe 700 2300 700 900 180
            sleep 0.6
            adb shell input swipe 700 900 700 2300 180
            sleep 0.6
        done
        sleep 1
        echo "== $mode run$run"
        adb shell dumpsys gfxinfo "$package" | grep -E \
            "Total frames rendered|Janky frames:|^50th|^90th|^95th|^99th|gpu percentile|Frame deadline missed:"
    done
done
