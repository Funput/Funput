# Where the Vietnamese syllable list comes from

`vi.txt` is **not** derived from any third-party word list. It was written by hand
for Funput, so it carries the project's own licence and needs no notice on a
third-party licences screen. The English lexicon beside it is a different matter —
see `../lexicon/NOTICE.md`.

That is a deliberate choice, not an accident of availability. The lists that exist
are either licensed in ways that do not fit an MIT app, or would raise the question
of whether a frequency count taken from a corpus is a work derived from it. Writing
a short list settles both questions by not asking them.

## Growing it

Add entries in rough frequency order, most common first, and run:

```bash
cargo test -p funput-lexicon-tool --test vi_syllables
```

That checks every line composes to a real syllable, so a mistyped diacritic cannot
reach a device. It cannot tell a real word from a structurally valid non-word, so a
new entry wants a second reader.

Two things measured over Viet74K, worth knowing before the list is put to work:

- Used as a **filter** — only correct into a word on the list — coverage is what
  matters, and this list does not have it yet. At 569 entries the filter costs about
  nine tenths of the repairs and buys almost nothing, so the platforms leave it off.
  It becomes worth switching on somewhere well north of here, and the number to
  watch is `funput dev typos --prior shipped --known-only`.
- Used as a **prior** — being on the list is evidence, not permission — it currently
  moves nothing measurable either way, because `ln(1 + uses)` is small beside the
  confidence margin.

If the list ever does grow to real coverage, the frequencies matter more than the
count: a ranking that is roughly right is worth more than a long list in no
particular order.
