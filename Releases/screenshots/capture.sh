#!/bin/bash
#
# Capture the Play Store screenshots that a release invalidated.
#
# Drives the real app through ScreenshotCaptureTest
# (app/src/androidTest/kotlin/com/jrlabapps/coffeegrams/ScreenshotCaptureTest.kt),
# pulls the frames off the device, and fits them to the listing size. Nothing
# capture-only ships in the app itself — what you shoot is the build you ship.
#
# Mirrors the iOS sibling's Releases/screenshots/capture.sh, adapted where
# Android's own tools differ (see ScreenshotCaptureTest's own doc comment for
# the specifics). Usage, from the repo root:
#   ./Releases/screenshots/capture.sh                  # all five shots
#   ./Releases/screenshots/capture.sh 03-guided-timer   # just one
#
set -euo pipefail

cd "$(dirname "${BASH_SOURCE[0]}")/../.."   # repo root, wherever it's called from

# This script, like its iOS sibling (which hard-depends on xcrun/simctl), is
# a manual macOS release-prep tool — nothing in this project's actual
# workflow runs it anywhere else (no CI job touches it; see
# .github/workflows/ci.yml). `sips` isn't portable, so fail fast with a
# clear message instead of a cryptic "command not found" mid-run.
if ! command -v sips >/dev/null 2>&1; then
    echo "sips not found — this script needs macOS (it's used to fit the captured frames to size)." >&2
    exit 1
fi

WANTED="${1:-}"
OUT_DIR="Releases/screenshots"
mkdir -p "$OUT_DIR"
WORK="$(mktemp -d)"

export ANDROID_HOME="${ANDROID_HOME:-$HOME/Library/Android/sdk}"
export PATH="$PATH:$ANDROID_HOME/platform-tools"
if ! command -v adb >/dev/null 2>&1; then
    echo "adb not found under \$ANDROID_HOME/platform-tools ($ANDROID_HOME) — set ANDROID_HOME or check your SDK install." >&2
    exit 1
fi

APP_ID="com.jrlabapps.coffeegrams"
TEST_RUNNER="$APP_ID.test/androidx.test.runner.AndroidJUnitRunner"
DEVICE_SCREENSHOT_DIR="/storage/emulated/0/Android/data/$APP_ID/files/screenshots"

# Teardown runs on every exit path, not just the happy one: a status bar
# left pinned at 9:41/full battery would silently contaminate every later
# manual test and screenshot on that device.
cleanup() {
    adb shell am broadcast -a com.android.systemui.demo -e command exit >/dev/null 2>&1 || true
    rm -rf "$WORK"
    return 0
}
trap cleanup EXIT

# --- Confirm a device is connected ---------------------------------------
# No boot-a-device logic here (unlike the iOS script, which boots a
# simulator itself): the Android emulator can't be launched from this kind
# of sandboxed session (see testing.md's emulator setup notes), so this
# assumes one is already running, same as `:app:connectedAndroidTest` does.
DEVICES="$(adb devices | awk 'NR>1 && $2=="device" {print $1}')"
if [ -z "$DEVICES" ]; then
    echo "no device/emulator connected. Start one (see testing.md), confirm with 'adb devices', then retry." >&2
    exit 1
fi
if [ "$(echo "$DEVICES" | wc -l)" -gt 1 ]; then
    echo "▸ multiple devices connected, using the first: $(echo "$DEVICES" | head -1)"
    export ANDROID_SERIAL="$(echo "$DEVICES" | head -1)"
fi
echo "▸ device $(adb shell getprop ro.product.model 2>/dev/null || echo "$DEVICES")"

# --- Build and install -----------------------------------------------------
echo "▸ building (Debug)"
./gradlew assembleDebug assembleDebugAndroidTest > "$WORK/gradle.log" 2>&1 \
  || { echo "build failed — last 40 lines:" >&2; tail -40 "$WORK/gradle.log" >&2; exit 1; }

# Fresh app state for every capture run: a stale Room brew log from a
# previous run would otherwise accumulate near-duplicate entries in the
# 05-brew-log shot rather than showing a clean, real first-run scene. This
# also clears any screenshots/ left on-device from a prior run (uninstall
# removes the app's external-files-dir with it) — combined with the
# zero-files check below, that's what stops a run that silently produced
# no *new* frames from appearing to succeed off old ones.
adb uninstall "$APP_ID" >/dev/null 2>&1 || true
adb install -r "app/build/outputs/apk/debug/app-debug.apk" >/dev/null
adb install -r "app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk" >/dev/null

# --- Pin the status bar ------------------------------------------------------
# Android's "Demo Mode" broadcasts — the direct equivalent of iOS's
# `simctl status_bar override`. 9:41/full battery matches the iOS asset's
# own convention, for a consistent look across both platforms' listings.
echo "▸ pinning status bar"
adb shell settings put global sysui_demo_allowed 1
adb shell am broadcast -a com.android.systemui.demo -e command enter >/dev/null
adb shell am broadcast -a com.android.systemui.demo -e command clock -e hhmm 0941 >/dev/null
adb shell am broadcast -a com.android.systemui.demo -e command battery -e plugged false -e level 100 >/dev/null
adb shell am broadcast -a com.android.systemui.demo -e command network -e wifi show -e level 4 >/dev/null
adb shell am broadcast -a com.android.systemui.demo -e command network -e mobile show -e level 4 -e datatype none >/dev/null

# --- Run the capture test ----------------------------------------------------
INSTRUMENT_ARGS=(-e captureScreenshots true)
if [ -n "$WANTED" ]; then
    case "$WANTED" in
        01-home)         METHOD=testCaptureHome ;;
        02-calculator)   METHOD=testCaptureCalculator ;;
        03-guided-timer) METHOD=testCaptureGuidedTimer ;;
        04-paywall)      METHOD=testCapturePaywall ;;
        05-brew-log)     METHOD=testCaptureBrewLog ;;
        *) echo "unknown screenshot '$WANTED' (try 01-home, 02-calculator, 03-guided-timer, 04-paywall, 05-brew-log)" >&2; exit 2 ;;
    esac
    INSTRUMENT_ARGS+=(-e class "com.jrlabapps.coffeegrams.ScreenshotCaptureTest#$METHOD")
else
    INSTRUMENT_ARGS+=(-e class "com.jrlabapps.coffeegrams.ScreenshotCaptureTest")
fi

echo "▸ running capture test"
adb shell am instrument -w "${INSTRUMENT_ARGS[@]}" "$TEST_RUNNER" | tee "$WORK/instrument.log"
grep -q "^OK " "$WORK/instrument.log" \
  || { echo "capture run failed — see output above" >&2; exit 1; }

# --- Pull the frames and fit to the listing size -----------------------------
# adb pull's destination behavior depends on whether it already exists: pre-
# create it so the remote "screenshots" dir's *contents* land directly here,
# rather than adb using this path itself as the rename target for that
# remote directory (which is what happens when it doesn't pre-exist).
mkdir -p "$WORK/pulled"
adb pull "$DEVICE_SCREENSHOT_DIR" "$WORK/pulled" >/dev/null

# 1080x1920 is Play's own recommended phone screenshot size. Unlike Apple's
# exact-pixel-match requirement (any size from 320-3840px per side, 16:9 to
# 9:16 aspect is actually accepted), this fit-down is for consistency across
# the set, not because Play demands it.
PROCESSED=0
for shot in "$WORK"/pulled/screenshots/*.png; do
    [ -f "$shot" ] || continue
    name="$(basename "$shot")"
    sips -z 1920 1080 "$shot" >/dev/null
    dims=$(sips -g pixelWidth -g pixelHeight "$shot" | awk '/pixel/{printf "%s ", $2}')
    echo "▸ $name: ${dims% }"
    [ "$dims" = "1080 1920 " ] || { echo "  ✗ expected 1080 1920" >&2; exit 1; }
    cp "$shot" "$OUT_DIR/$name"
    PROCESSED=$((PROCESSED + 1))
done

# The test reporting "OK" only proves the assertions passed, not that any
# capture() call actually ran (captureScreenshots could be unset/false, the
# glob above could silently match nothing) — this is what turns a would-be
# false "done" into a real failure instead of leaving stale tracked assets
# untouched with no warning.
if [ "$PROCESSED" -eq 0 ]; then
    echo "no screenshots were produced — check the instrument log above" >&2
    exit 1
fi
if [ -n "$WANTED" ] && [ ! -f "$OUT_DIR/$WANTED.png" ]; then
    echo "expected $OUT_DIR/$WANTED.png but it wasn't produced" >&2
    exit 1
fi

echo "▸ done ($PROCESSED screenshot(s))"   # the status bar and demo mode are undone by the EXIT trap
