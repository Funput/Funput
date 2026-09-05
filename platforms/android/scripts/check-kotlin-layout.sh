#!/usr/bin/env bash

# Keeps the keyboard's gesture and caret sources readable: short files, small directories.
#
# `roots` deliberately lists only directories that already obey the rule. Older ones —
# `keyboard/interaction`, `ime/editing`, `keyboard/model` — are well past it, and pulling them in
# would make this script fail on arrival and be switched off. Add a directory here once it is
# clean, so the rule holds where it has been earned.

set -euo pipefail

readonly max_lines="${MAX_KOTLIN_FILE_LINES:-150}"
readonly max_files="${MAX_FILES_PER_DIRECTORY:-5}"
readonly android_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
readonly renderer_main="$android_root/keyboard-renderer/src/main/java/app/funput/funput/keyboard"
readonly renderer_test="$android_root/keyboard-renderer/src/test/java/app/funput/funput/keyboard"
readonly ime_main="$android_root/ime/src/main/java/app/funput/funput/ime"
readonly ime_test="$android_root/ime/src/test/java/app/funput/funput/ime"
readonly roots=(
    "$renderer_main/interaction/gestures"
    "$renderer_main/accessibility"
    "$renderer_test/interaction/gestures"
    "$renderer_test/accessibility"
    "$ime_main/editing/caret"
    "$ime_main/editing/gestures"
    "$ime_main/editing/typing"
    "$ime_main/editing/keyevent"
    "$ime_main/settings/gestures"
    "$ime_test/editing/caret"
    "$ime_test/editing/gestures"
    "$ime_test/editing/keyevent"
    "$ime_test/settings/gestures"
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
            find "$directory" -maxdepth 1 -type f -name '*.kt' -print | wc -l | tr -d ' '
        )"
        if (( file_count > max_files )); then
            printf '%s: %s Kotlin files (maximum: %s)\n' \
                "${directory#"$android_root"/}" "$file_count" "$max_files"
            violations=1
        fi
    done < <(find "$root" -type d -print0)

    while IFS= read -r -d '' file; do
        lines="$(wc -l < "$file" | tr -d ' ')"
        if (( lines > max_lines )); then
            printf '%s: %s lines (maximum: %s)\n' \
                "${file#"$android_root"/}" "$lines" "$max_lines"
            violations=1
        fi
    done < <(find "$root" -type f -name '*.kt' -print0)
done

if (( violations != 0 )); then
    printf 'Kotlin source layout check failed. Split the entries listed above.\n' >&2
    exit 1
fi

printf 'Kotlin layout passed (≤%s lines, ≤%s files per directory).\n' "$max_lines" "$max_files"
