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

rm -rf "$ARCHIVE" "$EXPORT"
mkdir -p "$OUT"

run_xcodebuild() {
    # Only automatic signing needs to reach Apple. Under manual signing the flags are
    # dead weight, and leaving them off makes "the archive talks to nobody" true
    # rather than merely intended.
    if [ -z "$MANUAL_SIGNING" ] \
        && [ -n "$ASC_KEY_ID" ] && [ -n "$ASC_ISSUER_ID" ] && [ -f "$ASC_KEY_PATH" ]; then
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
if [ -n "$MANUAL_SIGNING" ]; then
    # A build setting given on the command line reaches every target, so there is no
    # way to hand the app and the extension different profiles here. Archive without
    # signing and let the export step do it: -exportArchive re-signs everything and
    # takes a per-bundle-id profile map, which is the only place that distinction can
    # be expressed.
    set -- "$@" CODE_SIGNING_ALLOWED=NO CODE_SIGNING_REQUIRED=NO
else
    set -- "$@" -allowProvisioningUpdates
fi
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

echo "release-ios: wrote $EXPORT/Funput.ipa"
