#!/bin/sh
# Prepare generated native dependencies before Xcode resolves Swift packages.
set -eu

SCRIPT_DIR="$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)"
IOS_ROOT="$(CDPATH= cd -- "$SCRIPT_DIR/.." && pwd)"

"$SCRIPT_DIR/build-ffi.sh"
xcodebuild \
    -resolvePackageDependencies \
    -project "$IOS_ROOT/Funput.xcodeproj"

echo "bootstrap-ios: iOS dependencies are ready"
