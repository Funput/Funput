#!/usr/bin/env bash
# The app draws with FunputUI only. Material 3 stays inside :funput-ui, which borrows it for a
# few behaviours (sheets, dialogs, sliders, swipe) behind its own components and theme bridge.
# A Material import in :app would bypass that bridge and bring back the look the redesign
# removed, so this fails on any, and on the dependency itself.

set -euo pipefail

readonly android_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
readonly app="$android_root/app"
violations=0

while IFS= read -r file; do
    printf '%s: imports Material 3 (use FunputUI components instead)\n' "${file#"$android_root"/}"
    violations=1
done < <(grep -rlE --include='*.kt' 'import androidx\.compose\.material3\b' "$app/src" || true)

if grep -qE 'compose\.material3' "$app/build.gradle.kts"; then
    printf 'app/build.gradle.kts: declares Material 3 (it belongs to :funput-ui only)\n'
    violations=1
fi

if (( violations )); then
    printf 'The app must stay Material-free.\n' >&2
    exit 1
fi
printf 'App is Material-free (FunputUI only).\n'
