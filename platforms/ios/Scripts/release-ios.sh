#!/bin/sh
# Archive Funput for iOS and export a signed App Store .ipa, ready to upload by
# hand with Xcode Organizer or Transporter. The keyboard extension ships inside
# the app, so this one archive covers both targets.
#
#   ./Scripts/release-ios.sh                            # versions come from the project
#   VERSION=1.2026.66 ./Scripts/release-ios.sh          # stamp a marketing version
#   VERSION=1.2026.66 BUILD=101 ./Scripts/release-ios.sh  # ...and a build number
#
# VERSION is CFBundleShortVersionString, what users see. BUILD is CFBundleVersion,
# which only Apple reads: it must be unique per upload and must never go backwards.
# Overriding neither takes both from the project, in which case bump the version in
# Xcode first — App Store Connect rejects a build number it has already accepted.
set -eu

CONFIGURATION="${CONFIGURATION:-Release}"
SCHEME="${SCHEME:-Funput}"
TEAM_ID="${TEAM_ID:-RSARFZ5CD3}"
VERSION="${VERSION:-}"
# App Store Connect API key, for `-allowProvisioningUpdates` to fetch or create the
# signing assets. Unlike altool, xcodebuild does not look in
# ~/.appstoreconnect/private_keys on its own — the flags have to be passed. Left
# unset (a Mac with Xcode signed in), it uses the logged-in account instead.
ASC_KEY_ID="${ASC_KEY_ID:-}"
ASC_ISSUER_ID="${ASC_ISSUER_ID:-}"
ASC_KEY_PATH="${ASC_KEY_PATH:-$HOME/.appstoreconnect/private_keys/AuthKey_$ASC_KEY_ID.p8}"
# Defaulting to VERSION keeps a bare `VERSION=... ./release-ios.sh` self-consistent;
# CI passes both separately.
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

run_xcodebuild() {
    if [ -n "$ASC_KEY_ID" ] && [ -n "$ASC_ISSUER_ID" ] && [ -f "$ASC_KEY_PATH" ]; then
        xcodebuild "$@" \
            -authenticationKeyPath "$ASC_KEY_PATH" \
            -authenticationKeyID "$ASC_KEY_ID" \
            -authenticationKeyIssuerID "$ASC_ISSUER_ID"
    else
        xcodebuild "$@"
    fi
}

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
    set -- "$@" "MARKETING_VERSION=$VERSION"
fi
if [ -n "$BUILD" ]; then
    set -- "$@" "CURRENT_PROJECT_VERSION=$BUILD"
fi
run_xcodebuild archive "$@"

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

run_xcodebuild -exportArchive \
    -archivePath "$ARCHIVE" \
    -exportPath "$EXPORT" \
    -exportOptionsPlist "$EXPORT_OPTIONS" \
    -allowProvisioningUpdates

echo "release-ios: wrote $EXPORT/Funput.ipa"
