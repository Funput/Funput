#!/usr/bin/env bash

set -euo pipefail

readonly max_lines="${MAX_SWIFT_FILE_LINES:-150}"
readonly ios_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
violations=0

while IFS= read -r -d '' file; do
    lines="$(wc -l < "$file" | tr -d ' ')"
    if (( lines > max_lines )); then
        relative_path="${file#"$ios_root"/}"
        printf '%s: %s lines (maximum: %s)\n' "$relative_path" "$lines" "$max_lines"
        violations=1
    fi
done < <(
    find "$ios_root" -type f \( -name '*.swift' -o -name 'Package.swift' \) \
        -not -path '*/.agents/*' \
        -not -path '*/.build/*' -not -path "$ios_root/build/*" -print0
)

if (( violations != 0 )); then
    printf 'Swift LOC check failed. Split the files listed above.\n' >&2
    exit 1
fi

printf 'Swift LOC check passed (maximum %s lines per file).\n' "$max_lines"
