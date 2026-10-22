# funput-lexicon-tool

Builds `crates/funput-suggestions/data/lexicon/en.tsv`, the ranked word list
behind the English lexicon, and packs it into the `en.lex` the keyboards map.
The design lives in
[english-lexicon-suggestion.md](../../docs/features/english-lexicon-suggestion.md);
attribution in [NOTICE.md](../funput-suggestions/data/lexicon/NOTICE.md).

Nothing here ships. Run it by hand when the lexicon's data should change, then
review `en.tsv` as an ordinary diff. The only dependency is `funput-suggestions`
with its `lexicon-build` feature, so the `en.lex` layout is defined once, in
the library that reads it.

## Refresh

```bash
crates/funput-lexicon-tool/refresh.sh
```

- Needs `git`, `make`, `python3`, `curl`, `gzip` and roughly 13 GB of download.
  The 1-grams are streamed, never stored: the work directory (`target/lexicon`,
  or `WORK=…`) ends up holding SCOWL's database (~130 MB) and the counts.
- Interrupted? Run it again with the same `WORK`; finished shards are skipped.
- `TOP=N` sets how many words `en.tsv` keeps (default 30000), `JOBS=N` how many
  shards download at once (default 6).

Changing only the supplement, the blocklist or `TOP` does not need the download:
with the counts already in `WORK`, the script goes straight to the rank step.

## Steps

| Step | Input | Output |
|---|---|---|
| SCOWL | ESDB at a pinned commit, size 60, American, `--dot True` | `allowed.txt` |
| `count` | one decompressed 1-gram shard on stdin | `word\tcount`, lowercased, years 2000–2019, spellings under 100 dropped |
| `rank` | `allowed.txt`, `supplement.tsv`, `blocklist.txt`, every count file | `en.tsv` |
| `pack` | `en.tsv` on stdin | `en.lex` on stdout, checked by the library's own validation first |
| `doubled` | `en.tsv` on stdin, `--overrides`, `--out DIR` | `words.txt` and `decisive.txt` in `DIR` |

`doubled` types every word through `funput-engine` under eight setups (`scan::SETUPS`:
Telex and Telex nâng cao, with and without the z/f/w/j onsets, with eager restore on
and off) and writes two lists from that one scan:

- `words.txt` — every word a revert shortens in some setup, `password` → `pasword`,
  unless some setup shortens it the way the doubled-key habit would (`habit.rs`:
  one key dropped from adjacent doubled keys) to a more frequent word (`off` → `of`)
  or to a single letter (`ww` → `w`, a plain `w` in Telex nâng cao, which `en.tsv`
  never ranks). A key lost anywhere else is no habit, so `markers` (→ `makers`),
  `casts` (→ `cats`) and `doses` (→ `does`) stay listed. Eager restore off adds the
  words only it shortens (`class` → `clas`, since `clá` + `s` reverts). The engine
  gives them their raw keys back at the word boundary.
- `decisive.txt` — for each listed word, the shortest raw-key prefix at which
  neither the word on screen nor what the habit means by the keys (`meant`: the
  keys typed minus every reverting key that repeats the one before it) can begin
  any `en.tsv` word, in every setup with eager restore on that has reverted by then
  (`passw`, since nothing begins with `pasw`). `necess` shows `nêcs`, which begins
  nothing, but means `neces`, which begins `necessary`, so the switch waits for
  `necessa`. The engine switches to the raw keys there, mid-word, which it only does
  with eager restore on. A prefix that begins a word left out on purpose (`off`)
  never qualifies.

Every entry fits the engine's reserved `literal` capacity (32, this tool's `MAX_LEN`);
an engine unit test holds both lists to it.

See [doubled-key-restore.md](../../docs/features/doubled-key-restore.md).
`refresh.sh` rebuilds both after `rank`; after a core change that moves a revert,
rebuild them by hand (the `doubled_table` test says so):

```bash
cargo run -p funput-lexicon-tool -- doubled \
    --overrides crates/funput-engine/data/doubled/overrides.txt \
    --out crates/funput-engine/data/doubled < crates/funput-suggestions/data/lexicon/en.tsv
```

`overrides.txt` holds `+word` / `-word` lines with the reason in a comment; one
that changes nothing is an error, so a stale line cannot linger.

`refresh.sh` ends with `pack` into `WORK` purely as a check; the shells build
the `en.lex` they ship.

```bash
target/release/funput-lexicon-tool pack < crates/funput-suggestions/data/lexicon/en.tsv > en.lex
```

`rank` keeps one spelling per lowercase word (lowercase, then capitalised, then
the rest, e.g. `iPhone`), drops blocklisted words and words the corpus never
saw, and orders the rest by count, ties by the word itself, so the same inputs
always give the same file.

## Supplement

`supplement.tsv` corrects the two sources, one entry per line:

- `word` — offer it even if SCOWL does not list it, spelled exactly so (this
  also overrides SCOWL's spelling, e.g. `lol` for its `LOL`).
- `word<TAB>rank` — also place it no lower than the word holding that rank in
  the books-only order. For words books undercount: `cannot`, which the Google
  tokenizer splits into `can not`, and anything that took off after 2019.

A floor borrows a real count rather than inventing one, so a lifted word sorts
among the rest by the same measure. Keep the reason for each floor in a comment
next to it.

## Blocklist

`blocklist.txt` lists words the lexicon never offers, matched ignoring case. It
starts with the single words of LDNOOBW's `en` list, imported verbatim at a pinned
commit so the import can be diffed against its source, then:

- `!word` — re-admit a word the import blocks, for words whose main sense is
  clinical, legal, historical or everyday. `rank` fails if a re-admit matches
  nothing blocked, so a typo or a changed import cannot quietly undo it.
- `word` after the import — Funput's own additions.
