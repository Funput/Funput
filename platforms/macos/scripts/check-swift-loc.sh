#!/usr/bin/env bash

set -euo pipefail

readonly max_lines="${MAX_SWIFT_FILE_LINES:-150}"
readonly max_files="${MAX_SWIFT_FILES_PER_FEATURE_DIR:-5}"
readonly macos_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
violations=0

while IFS= read -r -d '' file; do
    lines="$(wc -l < "$file" | tr -d ' ')"
    if (( lines > max_lines )); then
        relative_path="${file#"$macos_root"/}"
        printf '%s: %s lines (maximum: %s)\n' "$relative_path" "$lines" "$max_lines"
        violations=1
    fi
done < <(find "$macos_root/Funput" -type f -name '*.swift' -print0)

for feature_root in "$macos_root/Funput/Convert" "$macos_root/FunputTests/Convert"; do
    [[ -d "$feature_root" ]] || continue
    while IFS= read -r -d '' directory; do
        count="$(find "$directory" -maxdepth 1 -type f -name '*.swift' | wc -l | tr -d ' ')"
        if (( count > max_files )); then
            relative_path="${directory#"$macos_root"/}"
            printf '%s: %s Swift files (maximum: %s)\n' "$relative_path" "$count" "$max_files"
            violations=1
        fi
    done < <(find "$feature_root" -type d -print0)
done

if (( violations != 0 )); then
    printf 'Swift LOC check failed. Split the files listed above.\n' >&2
    exit 1
fi

printf 'Swift quality check passed (maximum %s lines and %s feature files per directory).\n' \
    "$max_lines" "$max_files"
