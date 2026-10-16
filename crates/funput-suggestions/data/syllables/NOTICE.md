# Where the Vietnamese syllable list comes from

`vi.txt` is **not** derived from any third-party word list. It was written by hand
for Funput, so it carries the project's own licence (MIT) and needs no notice on a
third-party licences screen. The English lexicon beside it is a different matter —
see `../lexicon/NOTICE.md`.

That is a deliberate choice, not an accident of availability. The lists that exist
are either licensed in ways that do not fit an MIT app, or would raise the question
of whether a frequency count taken from a corpus is a work derived from it. Writing
the list by hand settles both questions by not asking them.

## What it is

A **ranking in four bands**, not a frequency table. Each `## tier N` header opens a
band — 1 holds the words that hold a sentence together, 4 the rare but real — and
within a band the order is rough frequency, most common first. No entry carries a
number, and none was measured: the bands are a reader's judgement of how common a
syllable is, which is exactly the kind of thing one can write down without a corpus.

`funput_suggestions::syllable_tier` reads the bands, and `syllable_weight` turns
them into the pseudo-use counts typo correction feeds its prior. Which weight goes
with which band is a constant in code, not in this file, so the list stays data.

## Growing it

Add entries to the band they belong in, in rough frequency order, and run:

```bash
cargo test -p funput-lexicon-tool --test vi_syllables
```

That checks every line composes to a real syllable, that no syllable sits in two
bands, that the bands appear in order, and that the lookup in `funput-suggestions`
agrees with the file. It cannot tell a real word from a structurally valid non-word,
so a new entry wants a second reader.

Two things measured over Viet74K with the earlier, flat list of 569 entries, worth
knowing before the list is put to work:

- Used as a **filter** — only correct into a word on the list — coverage is what
  matters, and the number to watch is `funput dev typos --prior shipped --known-only`.
  At 569 entries the filter cost about nine tenths of the repairs; the platforms
  leave it off until that number says otherwise.
- Used as a **prior** — being on the list is evidence, not permission — a flat list
  moved nothing measurable, because `ln(1 + uses)` with every use at 1 is small
  beside the confidence margin. The bands exist to give the prior something to say:
  the top band is worth about 3.7 nats over an unlisted syllable, the bottom one
  about 0.7.

Whether those weights are right is measured, not assumed, by the same harness; the
calibration lives in `docs/features/typo-correction.md`.
