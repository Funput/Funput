#!/usr/bin/env bash

set -euo pipefail

readonly max_lines="${MAX_SWIFT_FILE_LINES:-150}"
readonly max_files="${MAX_FILES_PER_DIRECTORY:-5}"
readonly ios_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
readonly roots=(
    "$ios_root/Packages/FunputKit/Sources/KeyboardTouchCore"
    "$ios_root/Packages/FunputKit/Tests/KeyboardTouchCoreTests"
)
violations=0

for root in "${roots[@]}"; do
    if [[ ! -d "$root" ]]; then
        printf 'Missing required source root: %s\n' "$root" >&2
        violations=1
        continue
    fi

    while IFS= read -r -d '' directory; do
        file_count="$(
            find "$directory" -maxdepth 1 -type f -name '*.swift' -print \
                | wc -l | tr -d ' '
        )"
        if (( file_count > max_files )); then
            printf '%s: %s Swift files (maximum: %s)\n' \
                "${directory#"$ios_root"/}" "$file_count" "$max_files"
            violations=1
        fi
    done < <(find "$root" -type d -print0)

    while IFS= read -r -d '' file; do
        lines="$(wc -l < "$file" | tr -d ' ')"
        if (( lines > max_lines )); then
            printf '%s: %s lines (maximum: %s)\n' \
                "${file#"$ios_root"/}" "$lines" "$max_lines"
            violations=1
        fi
    done < <(find "$root" -type f -name '*.swift' -print0)
done

if (( violations != 0 )); then
    printf 'KeyboardTouchCore source layout check failed.\n' >&2
    exit 1
fi

printf 'KeyboardTouchCore layout passed (≤%s lines, ≤%s files per directory).\n' \
    "$max_lines" "$max_files"
