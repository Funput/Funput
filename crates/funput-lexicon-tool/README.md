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
