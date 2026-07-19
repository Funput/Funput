#!/usr/bin/env bash

set -euo pipefail

readonly max_lines="${MAX_KOTLIN_FILE_LINES:-150}"
readonly android_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
violations=0

while IFS= read -r -d '' file; do
    lines="$(wc -l < "$file" | tr -d ' ')"
    if (( lines > max_lines )); then
        relative_path="${file#"$android_root"/}"
        printf '%s: %s lines (maximum: %s)\n' "$relative_path" "$lines" "$max_lines"
        violations=1
    fi
done < <(
    find "$android_root" -type f \( -name '*.kt' -o -name '*.kts' \) \
        -not -path '*/build/*' -not -path '*/.gradle/*' -print0
)

if (( violations != 0 )); then
    printf 'Kotlin LOC check failed. Split the files listed above.\n' >&2
    exit 1
fi

printf 'Kotlin LOC check passed (maximum %s lines per file).\n' "$max_lines"
