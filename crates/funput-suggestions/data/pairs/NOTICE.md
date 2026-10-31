# Where the pair lists come from

`vi.txt` (Vietnamese) and `en.txt` (English) are **not** derived from any third-party
word list or corpus. Both were written by hand for Funput, so they carry the project's
own licence (MIT) and need no notice on a third-party licences screen. `en.txt` borrows
nothing from the English lexicon beside it (`../lexicon/`, Google Books Ngram counts);
its test only checks that each word is one the lexicon also lists. This is the same choice, for the same reasons, as
the syllable list beside it: see `../syllables/NOTICE.md`.

Viet74K ([duyet/vietnamese-wordlist](https://github.com/duyet/vietnamese-wordlist)),
which `benchmarks/` uses to measure the engine, is not a source of this file. It has
no clear licence and holds some wrong entries.

## What it is

A map from a first syllable (or English word) to the one second syllable Predict offers after it,
before the user's own pairs can say anything. No entry carries a number and none was
measured: whether a pair is sharp enough is a reader's judgement, the kind of thing
one can write down without a corpus.

## Growing it

Add pairs and run:

```bash
cargo test -p funput-lexicon-tool --test vi_pairs --test en_pairs
```

For `vi.txt` that checks every side composes to a real syllable, that no first syllable
is listed twice, that none of the syllables anything can follow sneaks in, and that the
engine offers exactly what the file says. For `en.txt` it checks two lowercase ASCII
words per line, both in the English lexicon, no first word listed twice or shared with
`vi.txt`, no first word that is a Vietnamese syllable, and the same engine round trip.
Neither test can tell a sharp pair from a merely common one, so a new entry wants a
second reader. See `docs/features/vietnamese-seed-suggestion.md`.
