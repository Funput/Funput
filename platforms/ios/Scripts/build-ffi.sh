#!/bin/sh
# Build the Rust C ABI as a static XCFramework for iOS device and simulator.
set -eu

export PATH="$HOME/.cargo/bin:/opt/homebrew/bin:/usr/local/bin:$PATH"

SCRIPT_DIR="$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)"
IOS_ROOT="$(CDPATH= cd -- "$SCRIPT_DIR/.." && pwd)"
REPO_ROOT="$(CDPATH= cd -- "$IOS_ROOT/../.." && pwd)"
OUTPUT="${FUNPUT_FFI_OUTPUT:-$IOS_ROOT/Frameworks/FunputCore.xcframework}"
HEADER="$REPO_ROOT/crates/funput-ffi/include/funput.h"
MODULE_MAP="$IOS_ROOT/Frameworks/FunputCore.modulemap"
DEVICE_TARGET="aarch64-apple-ios"
SIMULATOR_ARM_TARGET="aarch64-apple-ios-sim"
SIMULATOR_X86_TARGET="x86_64-apple-ios"
CARGO_TARGET_DIR="${CARGO_TARGET_DIR:-$REPO_ROOT/target}"
case "$CARGO_TARGET_DIR" in
    /*) ;;
    *) CARGO_TARGET_DIR="$REPO_ROOT/$CARGO_TARGET_DIR" ;;
esac
export CARGO_TARGET_DIR

require_command() {
    if ! command -v "$1" >/dev/null 2>&1; then
        echo "build-ffi: required command not found: $1" >&2
        exit 1
    fi
}

require_file() {
    if [ ! -f "$1" ]; then
        echo "build-ffi: required file not found: $1" >&2
        exit 1
    fi
}

require_command cargo
require_command rustup
require_command xcodebuild
require_command lipo
require_file "$HEADER"
require_file "$MODULE_MAP"
case "$OUTPUT" in
    *.xcframework) ;;
    *)
        echo "build-ffi: output must end with .xcframework: $OUTPUT" >&2
        exit 1
        ;;
esac

for target in \
    "$DEVICE_TARGET" \
    "$SIMULATOR_ARM_TARGET" \
    "$SIMULATOR_X86_TARGET"
do
    rustup target add "$target"
    cargo build \
        --locked \
        --manifest-path "$REPO_ROOT/Cargo.toml" \
        --package funput-ffi \
        --release \
        --target "$target"
done

DEVICE_LIBRARY="$CARGO_TARGET_DIR/$DEVICE_TARGET/release/libfunput_ffi.a"
SIMULATOR_ARM_LIBRARY="$CARGO_TARGET_DIR/$SIMULATOR_ARM_TARGET/release/libfunput_ffi.a"
SIMULATOR_X86_LIBRARY="$CARGO_TARGET_DIR/$SIMULATOR_X86_TARGET/release/libfunput_ffi.a"
require_file "$DEVICE_LIBRARY"
require_file "$SIMULATOR_ARM_LIBRARY"
require_file "$SIMULATOR_X86_LIBRARY"

TEMP_ROOT="$(mktemp -d "${TMPDIR:-/tmp}/funput-ffi.XXXXXX")"
STAGED_OUTPUT="${OUTPUT%.xcframework}.tmp.$$.xcframework"
BACKUP_OUTPUT="${OUTPUT}.old.$$"

cleanup() {
    rm -rf "$TEMP_ROOT" "$STAGED_OUTPUT"
    if [ -e "$BACKUP_OUTPUT" ]; then
        if [ -e "$OUTPUT" ]; then
            rm -rf "$BACKUP_OUTPUT"
        else
            mv "$BACKUP_OUTPUT" "$OUTPUT"
        fi
    fi
}
trap cleanup EXIT
trap 'exit 1' HUP INT TERM

HEADERS="$TEMP_ROOT/Headers"
mkdir -p "$HEADERS" "$(dirname -- "$OUTPUT")"
cp "$HEADER" "$HEADERS/funput.h"
cp "$MODULE_MAP" "$HEADERS/module.modulemap"

SIMULATOR_LIBRARY="$TEMP_ROOT/libfunput_ffi-simulator.a"
lipo -create \
    "$SIMULATOR_ARM_LIBRARY" \
    "$SIMULATOR_X86_LIBRARY" \
    -output "$SIMULATOR_LIBRARY"

rm -rf "$STAGED_OUTPUT" "$BACKUP_OUTPUT"
xcodebuild -create-xcframework \
    -library "$DEVICE_LIBRARY" \
    -headers "$HEADERS" \
    -library "$SIMULATOR_LIBRARY" \
    -headers "$HEADERS" \
    -output "$STAGED_OUTPUT"

if [ -e "$OUTPUT" ]; then
    mv "$OUTPUT" "$BACKUP_OUTPUT"
fi

if mv "$STAGED_OUTPUT" "$OUTPUT"; then
    rm -rf "$BACKUP_OUTPUT"
else
    if [ -e "$BACKUP_OUTPUT" ]; then
        mv "$BACKUP_OUTPUT" "$OUTPUT"
    fi
    exit 1
fi

echo "build-ffi: wrote $OUTPUT"
