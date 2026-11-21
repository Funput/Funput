# Where the pair lists come from

`vi.txt` and `vi-ends.txt` (Vietnamese) and `en.txt` (English) are **not** derived from any
third-party word list or corpus. All were written by hand for Funput, so they carry the project's
own licence (MIT) and need no notice on a third-party licences screen. `en.txt` borrows
nothing from the English lexicon beside it (`../lexicon/`, Google Books Ngram counts);
its test only checks that each word is one the lexicon also lists. This is the same choice, for the same reasons, as
the syllable list beside it: see `../syllables/NOTICE.md`.

Viet74K ([duyet/vietnamese-wordlist](https://github.com/duyet/vietnamese-wordlist)),
which `benchmarks/` uses to measure the engine, is not a source of this file. It has
no clear licence and holds some wrong entries.

## What it is

A map from a first syllable (or English word) to the syllables Predict offers after it, in the
slots the user's own pairs left empty: up to three a line in `vi.txt`, likeliest first, and one
in `en.txt`. No entry carries a number and none was measured: whether a line is sharp enough,
and which follower comes first, is a reader's judgement, the kind of thing one can write down
without a corpus.

`vi-ends.txt` lists two-syllable words whose last syllable starts a line of `vi.txt`
(`dũng cảm`, for the line `cảm ơn`). After such a word that syllable is the end of a word and
not the start of one, so the line says nothing. It only silences shipped guesses: what the
user's own typing taught the store is offered as before.

## Growing it

Add pairs and run:

```bash
cargo test -p funput-lexicon-tool --test vi_pairs --test en_pairs
```

For `vi.txt` that checks every side composes to a real syllable, that a line lists one to
three followers and none twice, that no first syllable is listed twice, that none of the
syllables anything can follow sneaks in, that the engine offers exactly what the file says,
and that it says nothing after a line whose follower starts another line. For
`vi-ends.txt` it checks two real syllables a word, a last syllable that starts a line, no
word listed twice or already spelled by a line, and that the engine is quiet after each
word. When a new first syllable ends words people type, add those words to `vi-ends.txt`
in the same change. Then measure the batch, since no test can: `funput dev suggest` on
`main` and on the branch (`benchmarks/README.md`). For `en.txt` it checks two lowercase ASCII
words per line, both in the English lexicon, no first word listed twice or shared with
`vi.txt`, no first word that is a Vietnamese syllable, and the same engine round trip.
Neither test can tell a sharp pair from a merely common one, so a new entry wants a
second reader. See `docs/features/vietnamese-seed-suggestion.md`.
