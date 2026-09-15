#!/usr/bin/env bash
# Regenerates the theme gallery artwork for bundled themes from the production keyboard
# renderer. Run it after changing a bundled theme or how the keyboard surface draws.
#
# Usage: Scripts/export-theme-thumbnails.sh [simulator-udid]
# Use an iOS 26+ simulator so glass themes are captured with Liquid Glass; defaults to
# the first booted simulator.

set -euo pipefail

readonly ios_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
readonly bundle_id="app.funput.funput"
readonly derived_data="$ios_root/build/theme-thumbnails-dd"
readonly catalog="$ios_root/Funput/Assets.xcassets/ThemeThumbnails"
readonly manifest="$ios_root/Funput/Appearance/ThemeThumbnails.json"

device="${1:-$(xcrun simctl list devices booted | grep -oE '[0-9A-F]{8}(-[0-9A-F]{4}){3}-[0-9A-F]{12}' | head -1)}"
if [[ -z "$device" ]]; then
    echo "No simulator given and none booted." >&2
    exit 1
fi
xcrun simctl bootstatus "$device" -b >/dev/null

xcodebuild build \
    -project "$ios_root/Funput.xcodeproj" -scheme Funput -configuration Debug \
    -destination "id=$device" -derivedDataPath "$derived_data" -quiet
xcrun simctl install "$device" "$derived_data/Build/Products/Debug-iphonesimulator/Funput.app"

xcrun simctl terminate "$device" "$bundle_id" >/dev/null 2>&1 || true
output="$(xcrun simctl get_app_container "$device" "$bundle_id" data)/Documents/ThemeThumbnails"
rm -rf "$output"
xcrun simctl launch "$device" "$bundle_id" -export-theme-thumbnails >/dev/null

for _ in $(seq 90); do
    [[ -f "$output/done" ]] && break
    sleep 1
done
xcrun simctl terminate "$device" "$bundle_id" >/dev/null 2>&1 || true
if [[ ! -f "$output/done" ]]; then
    echo "Export did not finish; see the app's status label on the simulator." >&2
    exit 1
fi

rm -rf "$catalog"
mkdir -p "$catalog"
cat > "$catalog/Contents.json" <<'JSON'
{
  "info" : {
    "author" : "xcode",
    "version" : 1
  }
}
JSON

for png in "$output"/*.png; do
    name="$(basename "$png" .png)"
    imageset="$catalog/$name.imageset"
    mkdir -p "$imageset"
    sips -s format heic -s formatOptions 85 "$png" --out "$imageset/$name.heic" >/dev/null
    cat > "$imageset/Contents.json" <<JSON
{
  "images" : [
    {
      "filename" : "$name.heic",
      "idiom" : "universal",
      "scale" : "3x"
    }
  ],
  "info" : {
    "author" : "xcode",
    "version" : 1
  }
}
JSON
done

cp "$output/ThemeThumbnails.json" "$manifest"
echo "Wrote $(ls "$output"/*.png | wc -l | tr -d ' ') thumbnails to ${catalog#"$ios_root"/}"
