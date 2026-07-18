#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
BASE_REF="${1:-origin/main}"
LIMIT=150

cd "${ROOT}"
files=$(git diff --name-only --diff-filter=ACMR "${BASE_REF}" -- platforms/linux |
    grep -E '(\.rs|\.cpp|\.h|\.sh|\.in|\.desktop|CMakeLists\.txt)$' || true)

failed=0
for file in ${files}; do
    [ -f "${file}" ] || continue
    lines=$(wc -l < "${file}")
    if (( lines > LIMIT )); then
        echo "LOC limit: ${file} has ${lines} lines (max ${LIMIT})" >&2
        failed=1
    fi
done

exit "${failed}"
