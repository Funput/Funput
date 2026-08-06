#!/usr/bin/env bash
# Enforce the 150-lines-of-code budget for Rust source files.
#
# Counts each file's lines up to its inline `#[cfg(test)]` module (tests
# colocated at the end of a file don't count against the budget). Whole-file
# test modules (`tests.rs`, `src/**/tests/`) and integration tests/benches/
# examples are exempt by construction.
#
# Covers the shared crates *and* the platform shells. The platform shells are
# excluded from the cargo workspace (each links a heavy OS-only dependency), so
# nothing else looks at them — which is exactly how they drifted to 550-line
# files while `crates/` stayed inside the budget.
#
# Usage: scripts/check-loc.sh        # from anywhere; exits 1 on violation

set -euo pipefail

LIMIT=150
cd "$(dirname "$0")/.."

violations=0
checked=0
while IFS= read -r file; do
    checked=$((checked + 1))
    lines=$(awk '/^[[:space:]]*#\[cfg\(test\)\]/ { exit } { n++ } END { print n + 0 }' "$file")
    if [ "$lines" -gt "$LIMIT" ]; then
        echo "FAIL $file: $lines code lines (limit $LIMIT)"
        violations=$((violations + 1))
    fi
done < <(
    # `platforms/*/src` would miss platforms/linux/settings-gtk/src, which sits one
    # level deeper; match any `src` dir under platforms instead. `target/` is
    # excluded explicitly — it holds generated .rs files from build scripts.
    find crates/*/src platforms -path '*/src/*' -name '*.rs' \
        -not -name 'tests.rs' -not -path '*/tests/*' -not -path '*/target/*' |
        sort -u
)

if [ "$violations" -gt 0 ]; then
    echo "$violations file(s) over the $LIMIT-line budget — split them into submodules."
    exit 1
fi
echo "OK: $checked source files within the $LIMIT-line budget."
