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

# Manual signing. Set both profiles (UUID or name) and the archive stops talking to
# Apple entirely: nothing is fetched, nothing is created, the assets are already on
# disk. Leave them unset and the script keeps its automatic-signing behaviour, which
# is what a Mac with Xcode signed in wants.
PROFILE_APP="${PROFILE_APP:-}"
PROFILE_KEYBOARD="${PROFILE_KEYBOARD:-}"
SIGN_IDENTITY="${SIGN_IDENTITY:-Apple Distribution}"
APP_BUNDLE_ID="${APP_BUNDLE_ID:-app.funput.funput}"
APP_GROUP="${APP_GROUP:-group.app.funput.funput}"
KEYBOARD_BUNDLE_ID="${KEYBOARD_BUNDLE_ID:-app.funput.funput.Keyboard}"
MANUAL_SIGNING=
if [ -n "$PROFILE_APP" ] && [ -n "$PROFILE_KEYBOARD" ]; then
    MANUAL_SIGNING=1
fi
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
IPA="$EXPORT/Funput.ipa"

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
    -archivePath "$ARCHIVE"
# The archive is always signed, whatever the export does afterwards. Skipping it
# also skips entitlement processing, and the App Group entitlement then never
# reaches the binary: `UserDefaults(suiteName:)` returns nil, both sides silently
# fall back to their own defaults, and the keyboard stops seeing the app's settings
# — no crash, no log, just a build that quietly does not work.
#
# A build setting given on the command line reaches every target, so the app and the
# extension cannot be handed different profiles here. Automatic signing resolves
# that per target on its own; the manual profiles are for the export, which is where
# a per-bundle-id map can be expressed.
set -- "$@" -allowProvisioningUpdates
if [ -n "$VERSION" ]; then
    set -- "$@" "MARKETING_VERSION=$VERSION"
fi
if [ -n "$BUILD" ]; then
    set -- "$@" "CURRENT_PROJECT_VERSION=$BUILD"
fi
run_xcodebuild archive "$@"

# manageAppVersionAndBuildNumber stays false: left on, Xcode picks its own build
# number at export time and the .ipa stops matching the project.
{
    cat <<EOF
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
<plist version="1.0">
<dict>
    <key>method</key><string>app-store-connect</string>
    <key>teamID</key><string>$TEAM_ID</string>
    <key>destination</key><string>export</string>
    <key>uploadSymbols</key><true/>
    <key>manageAppVersionAndBuildNumber</key><false/>
EOF
    if [ -n "$MANUAL_SIGNING" ]; then
        cat <<EOF
    <key>signingStyle</key><string>manual</string>
    <key>signingCertificate</key><string>$SIGN_IDENTITY</string>
    <key>provisioningProfiles</key>
    <dict>
        <key>$APP_BUNDLE_ID</key><string>$PROFILE_APP</string>
        <key>$KEYBOARD_BUNDLE_ID</key><string>$PROFILE_KEYBOARD</string>
    </dict>
EOF
    fi
    printf '</dict>\n</plist>\n'
} >"$EXPORT_OPTIONS"

set -- -exportArchive -archivePath "$ARCHIVE" -exportPath "$EXPORT" \
    -exportOptionsPlist "$EXPORT_OPTIONS"
if [ -z "$MANUAL_SIGNING" ]; then
    set -- "$@" -allowProvisioningUpdates
fi
run_xcodebuild "$@"

# The App Group is what lets the keyboard read the app's settings and report Full
# Access back to it. Losing it produces a build that installs, launches and types,
# and is broken in a way only a person on a device would notice — so check the
# artifact itself rather than trusting that the signing settings did their job.
VERIFY="$OUT/verify"
rm -rf "$VERIFY"
mkdir -p "$VERIFY"
unzip -q "$IPA" -d "$VERIFY"
for bundle in "$VERIFY"/Payload/*.app "$VERIFY"/Payload/*.app/PlugIns/*.appex; do
    [ -d "$bundle" ] || continue
    if ! codesign -d --entitlements :- "$bundle" 2>/dev/null | grep -q "$APP_GROUP"; then
        echo "release-ios: $(basename "$bundle") is missing the $APP_GROUP entitlement" >&2
        echo "release-ios: the keyboard and the app would not see each other's data" >&2
        exit 1
    fi
done
rm -rf "$VERIFY"
echo "release-ios: $APP_GROUP present in the app and the extension"

echo "release-ios: wrote $EXPORT/Funput.ipa"
