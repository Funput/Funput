#!/bin/sh
# Archive Funput for iOS and export a signed App Store .ipa, ready to upload by
# hand with Xcode Organizer or Transporter. The keyboard extension ships inside
# the app, so this one archive covers both targets.
#
#   ./Scripts/release-ios.sh                  # versions come from the project
#   VERSION=1.2026.66 ./Scripts/release-ios.sh  # stamp a version instead
#
# Without VERSION, bump the version in Xcode first — App Store Connect rejects a
# build number it has already accepted.
set -eu

CONFIGURATION="${CONFIGURATION:-Release}"
SCHEME="${SCHEME:-Funput}"
TEAM_ID="${TEAM_ID:-RSARFZ5CD3}"
VERSION="${VERSION:-}"
# CFBundleVersion is what App Store Connect checks for uniqueness. The calver
# doubles as one: it is three dot-separated integers and rises with every release.
BUILD="${BUILD:-$VERSION}"

SCRIPT_DIR="$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)"
IOS_ROOT="$(CDPATH= cd -- "$SCRIPT_DIR/.." && pwd)"
cd "$IOS_ROOT"

OUT="$IOS_ROOT/build/release"
ARCHIVE="$OUT/Funput.xcarchive"
EXPORT="$OUT/export"
EXPORT_OPTIONS="$OUT/ExportOptions.plist"

rm -rf "$ARCHIVE" "$EXPORT"
mkdir -p "$OUT"

# The Release scheme pre-action builds this too, but doing it up front fails with
# a readable error instead of a missing-framework link error. cargo caches, so the
# pre-action's second run costs nothing.
"$SCRIPT_DIR/build-ffi.sh"

# Positional params so the version overrides stay single arguments. Setting them on
# the command line applies them to every target, which is what keeps the keyboard
# extension's CFBundleVersion identical to the app's — App Store Connect rejects the
# upload when they differ.
set -- -project Funput.xcodeproj -scheme "$SCHEME" \
    -configuration "$CONFIGURATION" \
    -destination "generic/platform=iOS" \
    -archivePath "$ARCHIVE" \
    -allowProvisioningUpdates
if [ -n "$VERSION" ]; then
    set -- "$@" "MARKETING_VERSION=$VERSION" "CURRENT_PROJECT_VERSION=$BUILD"
fi
xcodebuild archive "$@"

# manageAppVersionAndBuildNumber stays false: left on, Xcode picks its own build
# number at export time and the .ipa stops matching the project.
cat >"$EXPORT_OPTIONS" <<EOF
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
<plist version="1.0">
<dict>
    <key>method</key><string>app-store-connect</string>
    <key>teamID</key><string>$TEAM_ID</string>
    <key>destination</key><string>export</string>
    <key>uploadSymbols</key><true/>
    <key>manageAppVersionAndBuildNumber</key><false/>
</dict>
</plist>
EOF

xcodebuild -exportArchive \
    -archivePath "$ARCHIVE" \
    -exportPath "$EXPORT" \
    -exportOptionsPlist "$EXPORT_OPTIONS" \
    -allowProvisioningUpdates

echo "release-ios: wrote $EXPORT/Funput.ipa"
