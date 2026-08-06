#!/usr/bin/env bash
# Production-like Funput.exe build for local Windows testing.
# Mirrors CI (app/.github/workflows/build-windows.yml): `cargo build --release`
# with the crate's release profile (opt-level=s, LTO=fat, strip), then stages
# Funput-<version>.exe + Funput.exe + .sha256 under build/release/.
#
# Usage (from platforms/windows or via path):
#   ./scripts/build-release.sh
#   TARGET=x86_64-pc-windows-gnu ./scripts/build-release.sh   # cross from macOS/Linux
#   VERSION=1.2026.1 ./scripts/build-release.sh              # override Cargo.toml version
#
# On Windows (Git Bash / MSYS), omit TARGET to build the host triple (MSVC or GNU).
#
# NOTE: the UI now uses Slint's Skia renderer (needed for the Mica backdrop).
# rust-skia only ships prebuilt binaries for *-windows-msvc, and building Skia from
# source needs a Visual C++ install — so cross-compiling to x86_64-pc-windows-gnu
# from macOS/Linux FAILS at the Skia step. Build on Windows with build-release.ps1.
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

VERSION="${VERSION:-}"
if [ -z "$VERSION" ]; then
  VERSION="$(sed -n 's/^version = "\(.*\)"/\1/p' Cargo.toml | head -1)"
fi
VERSION="${VERSION:-dev}"

HOST="$(rustc -vV | awk '/^host:/{print $2}')"
case "$HOST" in
  *-windows-*) DEFAULT_TARGET="" ;; # native host triple
  *) DEFAULT_TARGET="x86_64-pc-windows-gnu" ;;
esac
TARGET="${TARGET-$DEFAULT_TARGET}"

echo "Building Funput $VERSION (release${TARGET:+, target=$TARGET})…"

ARGS=(build --release)
if [ -n "$TARGET" ]; then
  rustup target add "$TARGET" >/dev/null 2>&1 || true
  ARGS+=(--target "$TARGET")
  # Help cross builds find mingw when cargo isn't configured yet.
  case "$TARGET" in
    x86_64-pc-windows-gnu)
      if [ -z "${CARGO_TARGET_X86_64_PC_WINDOWS_GNU_LINKER:-}" ] \
        && command -v x86_64-w64-mingw32-gcc >/dev/null 2>&1; then
        export CARGO_TARGET_X86_64_PC_WINDOWS_GNU_LINKER=x86_64-w64-mingw32-gcc
      fi
      ;;
    aarch64-pc-windows-gnu)
      if [ -z "${CARGO_TARGET_AARCH64_PC_WINDOWS_GNU_LINKER:-}" ] \
        && command -v aarch64-w64-mingw32-gcc >/dev/null 2>&1; then
        export CARGO_TARGET_AARCH64_PC_WINDOWS_GNU_LINKER=aarch64-w64-mingw32-gcc
      fi
      ;;
  esac
fi

cargo "${ARGS[@]}"

if [ -n "$TARGET" ]; then
  EXE="$ROOT/target/$TARGET/release/funput.exe"
else
  EXE="$ROOT/target/release/funput.exe"
fi

if [ ! -f "$EXE" ]; then
  echo "error: expected binary missing: $EXE" >&2
  exit 1
fi

OUT="$ROOT/build/release"
mkdir -p "$OUT"
DEST="$OUT/Funput-$VERSION.exe"
cp "$EXE" "$DEST"
cp "$EXE" "$OUT/Funput.exe"

if command -v sha256sum >/dev/null 2>&1; then
  sha256sum "$DEST" | awk '{print $1}' > "$DEST.sha256"
elif command -v shasum >/dev/null 2>&1; then
  shasum -a 256 "$DEST" | awk '{print $1}' > "$DEST.sha256"
else
  echo "warning: no sha256sum/shasum; skipped checksum" >&2
fi

BYTES="$(wc -c < "$DEST" | tr -d ' ')"
echo "Staged: $DEST ($BYTES bytes)"
echo "Also:   $OUT/Funput.exe (stable name for local/autostart)"
[ -f "$DEST.sha256" ] && echo "SHA256:  $(cat "$DEST.sha256")"
echo "Run Funput.exe (or Funput-$VERSION.exe once — it normalizes to Funput.exe)."
