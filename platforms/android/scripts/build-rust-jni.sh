#!/usr/bin/env bash
set -euo pipefail

target="${1:?Rust target is required}"
abi="${2:?Android ABI is required}"
profile="${3:?Cargo profile is required}"
output_dir="${4:?Output directory is required}"
ndk_version="${5:?NDK version is required}"

script_dir="$(cd "$(dirname "$0")" && pwd)"
android_dir="$(cd "$script_dir/.." && pwd)"
workspace_dir="$(cd "$android_dir/../.." && pwd)"
sdk_root="${ANDROID_SDK_ROOT:-${ANDROID_HOME:-}}"

if [[ -z "$sdk_root" ]]; then
    sdk_root="$(sed -n 's/^sdk.dir=//p' "$android_dir/local.properties" | head -n 1)"
fi
if [[ -z "$sdk_root" ]]; then
    echo "Android SDK not found. Set ANDROID_SDK_ROOT or sdk.dir." >&2
    exit 1
fi

ndk_dir="$sdk_root/ndk/$ndk_version"
toolchain_dir="$(find "$ndk_dir/toolchains/llvm/prebuilt" -mindepth 1 -maxdepth 1 -type d | head -n 1)"
case "$target" in
    aarch64-linux-android)
        export CARGO_TARGET_AARCH64_LINUX_ANDROID_LINKER="$toolchain_dir/bin/aarch64-linux-android26-clang"
        ;;
    x86_64-linux-android)
        export CARGO_TARGET_X86_64_LINUX_ANDROID_LINKER="$toolchain_dir/bin/x86_64-linux-android26-clang"
        ;;
    *)
        echo "Unsupported Android Rust target: $target" >&2
        exit 1
        ;;
esac

cargo_args=(build --locked -p funput-jni --target "$target")
if [[ "$profile" == "release" ]]; then
    cargo_args+=(--release)
fi

cd "$workspace_dir"
cargo "${cargo_args[@]}"
mkdir -p "$output_dir"
install -m 0644 "target/$target/$profile/libfunput_jni.so" "$output_dir/libfunput_jni.so"
