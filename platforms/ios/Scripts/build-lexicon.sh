#!/bin/sh
# Build the host-only packer and embed its verified output before extension signing.
set -eu

export PATH="$HOME/.cargo/bin:/opt/homebrew/bin:/usr/local/bin:$PATH"

# Same reason as build-ffi.sh: Xcode exports Swift-only settings its bundled xcrun
# rejects, and an empty XCODE_DEVELOPER_DIR_PATH. Either makes xcrun write to stderr
# while rustc asks for an SDK path, and rustc reports that output as a warning.
unset SWIFT_DEBUG_INFORMATION_FORMAT SWIFT_DEBUG_INFORMATION_VERSION \
    XCODE_DEVELOPER_DIR_PATH

# 512 KiB, the ceiling funput-suggestions guards with en_lex_stays_under_its_size_ceiling.
MAX_LEXICON_BYTES=524288

SCRIPT_DIR="$(CDPATH='' cd -- "$(dirname -- "$0")" && pwd)"
REPO_ROOT="$(CDPATH='' cd -- "$SCRIPT_DIR/../../.." && pwd)"
OUTPUT_DIR="${DERIVED_FILE_DIR:?Xcode must provide DERIVED_FILE_DIR}/Lexicon"
RESOURCE_DIR="${TARGET_BUILD_DIR:?}/${UNLOCALIZED_RESOURCES_FOLDER_PATH:?}"
mkdir -p "$OUTPUT_DIR" "$RESOURCE_DIR"
TEMP_FILE="$(mktemp "$OUTPUT_DIR/en.lex.XXXXXX")"
trap 'rm -f "$TEMP_FILE"' EXIT
trap 'exit 1' HUP INT TERM

# Always run: Cargo handles code changes and pack reads the current committed TSV.
# A distinct host target prevents Xcode's target settings affecting this executable.
HOST="$(rustc -vV | sed -n 's/^host: //p')"
cargo run --locked --release --manifest-path "$REPO_ROOT/Cargo.toml" \
    --target "$HOST" --package funput-lexicon-tool -- pack \
    < "$REPO_ROOT/crates/funput-suggestions/data/lexicon/en.tsv" > "$TEMP_FILE"
SIZE="$(wc -c < "$TEMP_FILE" | tr -d ' ')"
if [ "$SIZE" -eq 0 ] || [ "$SIZE" -gt "$MAX_LEXICON_BYTES" ]; then
    echo "build-lexicon: invalid lexicon size: $SIZE bytes" >&2
    exit 1
fi
# mktemp creates the file 0600; a bundle resource must stay world-readable.
chmod 644 "$TEMP_FILE"
mv -f "$TEMP_FILE" "$OUTPUT_DIR/en.lex"
# -p also fixes the mode of a copy left 0600 by an earlier build.
cp -p "$OUTPUT_DIR/en.lex" "$RESOURCE_DIR/en.lex"
