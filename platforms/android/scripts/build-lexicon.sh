#!/usr/bin/env bash
set -euo pipefail
readonly workspace="$1"
readonly output="$2"
mkdir -p "$output/lexicon"
temporary="$(mktemp "$output/lexicon/en.lex.XXXXXX")"
trap 'rm -f "$temporary"' EXIT
host="$(rustc -vV | sed -n 's/^host: //p')"
cargo run --locked --release --manifest-path "$workspace/Cargo.toml" \
    --target "$host" --package funput-lexicon-tool -- pack \
    < "$workspace/crates/funput-suggestions/data/lexicon/en.tsv" > "$temporary"
size="$(wc -c < "$temporary" | tr -d ' ')"
if (( size < 24 || size > 524288 )); then
    echo "Invalid lexicon size: $size" >&2
    exit 1
fi
chmod 644 "$temporary"
mv -f "$temporary" "$output/lexicon/en.lex"
cp "$workspace/crates/funput-suggestions/data/lexicon/NOTICE.md" "$output/lexicon/NOTICE.md"
