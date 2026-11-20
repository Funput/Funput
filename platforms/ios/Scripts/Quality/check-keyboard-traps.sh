#!/usr/bin/env bash
# Fails when keyboard code gains a way to kill its own process.
#
# A trap in the keyboard extension takes the process down, and iOS answers by
# showing the system keyboard instead. The traps that remain only ever see values
# written in code, and each file that holds one is listed in keyboard-traps.allowlist
# with how many it has. A new trap changes that count, so it cannot land without
# someone deciding it is a programmer invariant rather than a check on runtime data
# (configuration, the host app, disk, touches, layout size). Those are clamped or
# skipped instead. See docs/KEYBOARD_RELIABILITY.md, B1.
#
# Force unwraps are out of reach of a line pattern and are not counted here.

set -euo pipefail

readonly ios_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
readonly allowlist="$ios_root/Scripts/Quality/keyboard-traps.allowlist"
readonly roots=(
    "$ios_root/Keyboard"
    "$ios_root/Packages/FunputKit/Sources"
)
readonly trap_pattern='precondition\(|preconditionFailure\(|fatalError\(|try!| as! '
# `grep -n` prefixes `path:line:`; a match whose code starts with `//` is a comment.
readonly comment_pattern='^[^:]+:[0-9]+:[[:space:]]*//'
# The initializer UIKit requires of every view subclass; it never runs here.
readonly coder_stub='init(coder:) has not been implemented'

# `grep -rE` keeps the check runnable on a clean machine; ripgrep is not a prerequisite.
found="$(
    grep -rEn --include='*.swift' "$trap_pattern" "${roots[@]}" \
        | grep -Ev "$comment_pattern" \
        | grep -Fv "$coder_stub" \
        | cut -d: -f1 \
        | sed "s|^$ios_root/||" \
        | LC_ALL=C sort | uniq -c | awk '{ print $1 " " $2 }' || true
)"
allowed="$(
    grep -Ev '^[[:space:]]*(#|$)' "$allowlist" \
        | awk '{ print $1 " " $2 }' | LC_ALL=C sort -k2 || true
)"

if [[ "$found" != "$allowed" ]]; then
    diff <(printf '%s\n' "$allowed") <(printf '%s\n' "$found") || true
    printf 'Keyboard trap sites differ from Scripts/Quality/keyboard-traps.allowlist.\n' >&2
    printf 'Lines marked > are in the code but not allowed; clamp or skip runtime values instead of trapping.\n' >&2
    printf 'Lines marked < are allowed but gone; remove them from the allowlist.\n' >&2
    exit 1
fi

printf 'Keyboard trap check passed (no trap sites beyond the allowlist).\n'
