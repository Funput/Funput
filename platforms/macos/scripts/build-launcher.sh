#!/bin/sh
# Build the Funput launcher app.
#
# Funput's input method lives in /Library/Input Methods as an LSUIElement IMK
# agent, which Spotlight does not surface as an app — so a user who types "Funput"
# into Spotlight finds nothing. This builds a tiny normal app, installed in
# /Applications, whose only job is to be findable: launching it sends
# funput://settings to the input method (opening its Settings window) and exits.
# See Launcher/main.swift.
#
#   build-launcher.sh <out-dir> <version> [sign-identity]
#
# Writes <out-dir>/Funput.app. sign-identity defaults to "-" (ad-hoc); pass
# "Developer ID Application" for a release build (adds hardened runtime + timestamp
# so the bundle can be notarized).
set -eu

OUT="$1"
VERSION="$2"
SIGN_ID="${3:--}"

DIR="$(cd "$(dirname "$0")/.." && pwd)"
SRC="$DIR/Launcher"
DEPLOY_TARGET="26.5"
APP="$OUT/Funput.app"

rm -rf "$APP"
mkdir -p "$APP/Contents/MacOS" "$APP/Contents/Resources"

# Universal binary (arm64 + x86_64), matching the input method.
TMP="$OUT/.launcher-build"
rm -rf "$TMP"; mkdir -p "$TMP"
swiftc -O -target "arm64-apple-macos$DEPLOY_TARGET"  -o "$TMP/launcher-arm64"  "$SRC/main.swift"
swiftc -O -target "x86_64-apple-macos$DEPLOY_TARGET" -o "$TMP/launcher-x86_64" "$SRC/main.swift"
lipo -create -output "$APP/Contents/MacOS/Funput" "$TMP/launcher-arm64" "$TMP/launcher-x86_64"
rm -rf "$TMP"

# Info.plist, version stamped to match the input method.
cp "$SRC/Info.plist" "$APP/Contents/Info.plist"
/usr/libexec/PlistBuddy -c "Set :CFBundleShortVersionString $VERSION" "$APP/Contents/Info.plist"
/usr/libexec/PlistBuddy -c "Set :CFBundleVersion $VERSION" "$APP/Contents/Info.plist"

# App icon (best-effort): build AppIcon.icns from the 1024px master so Spotlight
# and Finder show the Funput logo. Skipped quietly if the tools/source are missing.
ICON_SRC="$DIR/Funput/Assets.xcassets/AppIcon.appiconset/transparent.png"
if [ -f "$ICON_SRC" ] && command -v iconutil >/dev/null 2>&1; then
    ICONSET="$OUT/.AppIcon.iconset"
    rm -rf "$ICONSET"; mkdir -p "$ICONSET"
    for sz in 16 32 128 256 512; do
        dbl=$((sz * 2))
        sips -z "$sz" "$sz" "$ICON_SRC" --out "$ICONSET/icon_${sz}x${sz}.png" >/dev/null 2>&1 || true
        sips -z "$dbl" "$dbl" "$ICON_SRC" --out "$ICONSET/icon_${sz}x${sz}@2x.png" >/dev/null 2>&1 || true
    done
    iconutil -c icns "$ICONSET" -o "$APP/Contents/Resources/AppIcon.icns" 2>/dev/null || true
    rm -rf "$ICONSET"
fi

# Sign. Developer ID builds get the hardened runtime + secure timestamp (required
# to notarize); ad-hoc ("-") builds skip the timestamp.
if [ "$SIGN_ID" = "-" ]; then
    codesign --force --sign "-" "$APP"
else
    codesign --force --options runtime --timestamp --sign "$SIGN_ID" "$APP"
fi

echo "$APP"
