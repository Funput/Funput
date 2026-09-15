#!/usr/bin/env bash
# Regenerate crates/funput-suggestions/data/lexicon/en.tsv from its sources.
#
# Fetches SCOWL/ESDB at a pinned commit and streams the ~12.8 GB of Google Books
# Ngram v3 English 1-grams through `funput-lexicon-tool count`, keeping only the
# counts (a few MB). Needs git, make, python3, curl and gzip.
#
#   crates/funput-lexicon-tool/refresh.sh            # WORK=... to reuse a work dir
#
# Re-running with the same WORK skips whatever already finished, so an
# interrupted download resumes at the files still missing.

set -euo pipefail

SCOWL_REPO=https://github.com/en-wl/wordlist.git
SCOWL_COMMIT=1e5b7d3a72f47a71da5d28686c1dd4b397178485
NGRAMS=https://storage.googleapis.com/books/ngrams/books/20200217/eng
TOP=${TOP:-30000}
JOBS=${JOBS:-6}

app=$(cd "$(dirname "$0")/../.." && pwd)
data=$app/crates/funput-suggestions/data/lexicon
work=${WORK:-$app/target/lexicon}
mkdir -p "$work/counts"

cargo build --quiet --release -p funput-lexicon-tool --manifest-path "$app/Cargo.toml"
tool=$app/target/release/funput-lexicon-tool

# 1. SCOWL size 60, American. `--dot True` keeps the dot on abbreviations such as
#    `Blvd.`, which `rank` then rejects as non-letters instead of offering `Blvd`.
if [ ! -f "$work/scowl/scowl.db" ]; then
    rm -rf "$work/scowl"
    git init --quiet "$work/scowl"
    git -C "$work/scowl" fetch --quiet --depth 1 "$SCOWL_REPO" "$SCOWL_COMMIT"
    git -C "$work/scowl" checkout --quiet FETCH_HEAD
    # ESDB's build warns about groups it skips; that is its business, not ours.
    make -C "$work/scowl" > "$work/scowl-make.log" 2>&1
fi
# It echoes its query and word pattern to stderr on every run; keep that in a log.
(cd "$work/scowl" && ./scowl --db scowl.db word-list 60 A 1 --dot True) \
    > "$work/allowed.txt" 2> "$work/scowl-list.log"

# 2. Counts, one file per 1-gram shard, written aside and renamed when complete.
export NGRAMS tool work
seq -f '%05g' 0 23 | xargs -P "$JOBS" -I{} bash -c '
    set -euo pipefail
    out=$work/counts/1-{}.tsv
    [ -f "$out" ] && exit 0
    curl -sSf --retry 5 "$NGRAMS/1-{}-of-00024.gz" | gzip -dc | "$tool" count > "$out.part"
    mv "$out.part" "$out"
    echo "counted shard {}" >&2
'

# 3. Rank into WORK first: a redirect truncates its target before the command
#    runs, so ranking straight into data/ would empty the committed list whenever
#    rank failed.
"$tool" rank --allowed "$work/allowed.txt" \
    --supplement "$data/supplement.tsv" --blocklist "$data/blocklist.txt" \
    --top "$TOP" "$work"/counts/1-*.tsv > "$work/en.tsv"
mv "$work/en.tsv" "$data/en.tsv"
echo "wrote $data/en.tsv ($(grep -vc '^#' "$data/en.tsv") words)" >&2

# 4. Pack, as a check: the list must still make an en.lex the library accepts.
#    The file stays in WORK; the shells build their own.
"$tool" pack < "$data/en.tsv" > "$work/en.lex"
