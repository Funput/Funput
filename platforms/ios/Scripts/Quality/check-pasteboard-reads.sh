#!/usr/bin/env bash
# Fails when keyboard code reads the pasteboard's contents.
#
# Any content read outside a `UIPasteControl` raises the iOS paste prompt, so the
# keyboard reads metadata only (changeCount, hasStrings, hasURLs, types) and lets
# the paste control deliver text when the user taps it. See
# docs/STARTUP_AND_MEMORY.md, A1. The containing app is out of scope: its UI-test
# harness writes to the pasteboard on purpose.

set -euo pipefail

readonly ios_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
readonly roots=(
    "$ios_root/Keyboard"
    "$ios_root/Packages/FunputKit/Sources"
)
# A content accessor on the general pasteboard, or on a `UIPasteboard` held in a
# variable named `pasteboard`, which is how this codebase passes it around.
readonly read_pattern='(UIPasteboard\.general|pasteboard)\.(string|strings|url|urls|image|images|color|colors|items|itemProviders|itemSet|data|value)\b'
# `grep -n` prefixes `path:line:`; a match whose code starts with `//` is a comment.
readonly comment_pattern='^[^:]+:[0-9]+:[[:space:]]*//'

# `grep -rE` keeps the check runnable on a clean machine; ripgrep is not a prerequisite.
matches="$(
    grep -rEn --include='*.swift' "$read_pattern" "${roots[@]}" \
        | grep -Ev "$comment_pattern" || true
)"

if [[ -n "$matches" ]]; then
    printf '%s\n' "${matches//"$ios_root"\//}"
    printf 'Keyboard code reads pasteboard contents, which raises the iOS paste prompt.\n' >&2
    printf 'Deliver text through UIPasteControl instead (docs/STARTUP_AND_MEMORY.md, A1).\n' >&2
    exit 1
fi

printf 'Pasteboard read check passed (metadata only in keyboard code).\n'
