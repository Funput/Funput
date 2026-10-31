# Where the Vietnamese pair list comes from

`vi.txt` is **not** derived from any third-party word list or corpus. It was written
by hand for Funput, so it carries the project's own licence (MIT) and needs no notice
on a third-party licences screen. This is the same choice, for the same reasons, as
the syllable list beside it: see `../syllables/NOTICE.md`.

Viet74K ([duyet/vietnamese-wordlist](https://github.com/duyet/vietnamese-wordlist)),
which `benchmarks/` uses to measure the engine, is not a source of this file. It has
no clear licence and holds some wrong entries.

## What it is

A map from a first syllable to the one second syllable Predict offers after it,
before the user's own pairs can say anything. No entry carries a number and none was
measured: whether a pair is sharp enough is a reader's judgement, the kind of thing
one can write down without a corpus.

## Growing it

Add pairs and run:

```bash
cargo test -p funput-lexicon-tool --test vi_pairs
```

That checks every side composes to a real syllable, that no first syllable is listed
twice, that none of the syllables anything can follow sneaks in, and that the engine
offers exactly what the file says. It cannot tell a sharp pair from a merely common
one, so a new entry wants a second reader. See `docs/features/vietnamese-seed-suggestion.md`.
