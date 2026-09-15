# funput-suggestions

Local-only personal word suggestions for Funput platform shells. The crate owns a
bounded mutable top-3 trie and crash-safe snapshot/journal persistence, but it does
not observe key events, document context, network state, or the Vietnamese composer.

It can also attach a read-only English lexicon (`en.lex`, built from
`data/lexicon/en.tsv` by `funput-lexicon-tool pack`). The lexicon is memory-mapped
beside the personal store, never merged into it, and only fills the suggestion slots
the personal words leave empty. See
[english-lexicon-suggestion.md](../../docs/features/english-lexicon-suggestion.md);
attribution for its data is in [NOTICE.md](data/lexicon/NOTICE.md).

Platform code must give the engine completed tokens and call it from one serial
worker. Querying is read-only, allocation-free after warm-up, and performs no I/O.
Persistence is explicit through `flush` and `compact`, allowing keyboard shells to
schedule disk work outside their input hot path.
