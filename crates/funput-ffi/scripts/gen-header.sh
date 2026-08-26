#!/usr/bin/env bash
# Regenerate the committed C header from the Rust source.
# Requires: cargo install cbindgen
#
# Usage: gen-header.sh           # rewrite include/funput.h
#        gen-header.sh --check   # fail if the committed header is out of date
#
# `--check` is what CI runs. The header is generated but *committed*, because the
# consumers that read it — the macOS bridging header, the Linux CMake include path,
# the iOS xcframework — build without cargo in the loop and cannot regenerate it.
# Committed generated files drift silently, so something has to look.
set -euo pipefail

crate_dir="$(cd "$(dirname "$0")/.." && pwd)"
committed="$crate_dir/include/funput.h"

generate() {
    cbindgen --config "$crate_dir/cbindgen.toml" \
             --crate funput-ffi \
             --output "$1" \
             "$crate_dir"
}

if [ "${1:-}" = "--check" ]; then
    fresh="$(mktemp)"
    trap 'rm -f "$fresh"' EXIT
    generate "$fresh"
    if ! diff -u "$committed" "$fresh"; then
        echo >&2
        echo "include/funput.h is out of date — run crates/funput-ffi/scripts/gen-header.sh" >&2
        exit 1
    fi
    echo "OK: include/funput.h matches the source."
    exit 0
fi

generate "$committed"
echo "wrote $committed"
