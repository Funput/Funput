# Personal suggestions V2 benchmark

Measured on the same Apple M-series machine as
[`personal-suggestions-v1.md`](personal-suggestions-v1.md), workspace Release
profile, Criterion over a warm 5,000-word personal lexicon. V2 covers the store
durability work and the removal of the trie rebuild from the learn path; the V1
lookup and memory numbers are unchanged and are not repeated here.

## Learning

| Path | Before | After | Change |
|---|---:|---:|---|
| Learn an existing word | 2.39 µs | 2.17 µs | unchanged |
| Learn evicting a promoted word | 1.82 ms | 47.2 µs | **39x faster** |

`suggestions/learn/evicting` is the new benchmark: a lexicon already full of
promoted words, learning one new word twice per iteration so that every iteration
evicts a promoted word. That was the case that rebuilt both tries on the spot —
V1 measured the same effect from the other side, as a 143 ms replay of 100
journal tokens.

The rebuild has not become cheaper; it happens 64 times less often. Trie entries
now carry the generation of the word slot they were written at, so an evicted
word's entries read as dead instead of renaming themselves to whatever moved into
the slot. That makes the sweep postponable, and it moves to `flush` — the crate's
only idle signal, which both keyboard shells already call off the typing path on
a 2 s timer and every 32 learns. `REBUILD_AFTER_EVICTIONS = 64` caps how many
dead entries can accumulate for a caller that never flushes, which is what keeps
the tries from growing with every distinct word ever seen.

## Memory

Retained heap for a warm 5,000-word lexicon, measured with
`cargo run --release -p funput-suggestions --example latency`:

| Revision | Retained | Added |
|---|---:|---:|
| V1 baseline, and after the store durability work | 572,504 B | — |
| After tagging trie entries with a slot generation | 670,820 B | +98,316 B |
| After adding four follower slots per word | 932,964 B | +262,144 B |

The follower figure is exactly 256 KiB because `words` grows by doubling: 32
bytes of edges against a `Vec` capacity of 8,192, not the 5,000 words the
capacity holds. `context_seen` fits in padding the record already had. The tests
enforce a 4 MiB retained-heap budget, so the whole bigram feature spends about
6% of the room it has.

Lookup is unchanged at p99 **292 ns** and 3.85 M queries/second — the edges are
written on the learn path and nothing reads them yet.

## Persistence

| Operation | V1 | V2 | Note |
|---|---:|---:|---|
| Flush 256 mutations | 5.19 ms | 5.42 ms | first append to a new journal only |
| Compact 5,000 words | 9.83 ms | 13.51 ms | +4.1 ms, deliberate |

`compact` now fsyncs the directory after renaming the snapshot into place. A
rename is a directory change, and without that sync a power loss can reorder it
against the journal truncation that follows — leaving an empty journal and no
snapshot to have folded it into. The cost is paid on the shells' background
worker, never on the typing path.

The `flush_256` figure covers the append that *creates* the journal, which is the
only append that syncs the directory; steady-state appends add one `stat` and no
sync.

## Measured and deliberately not changed

**Allocation at the C ABI / JNI boundary.** `funput_suggestion_query` decodes its
prefix into a `String` on every call, which contradicts the crate's
allocation-free lookup guarantee. It is not worth fixing: V1 measured the C FFI
harness at p50 **250 ns**, identical to the Rust harness at p50 **250 ns**, so
the allocation does not show up above the noise. Recorded here so the next reader
does not re-measure it.

**The linear scan in `upsert_word`.** Finding an existing word compares against
up to 5,000 `String`s. Measured at 2.29 µs per learned token on a background
worker — roughly 1/20,000th of the interval between keystrokes at a fast typing
speed. A hash index would cost about 200 KB and a second copy of every word's
text; the trade is not worth making. The bigram work will add a second such scan
for the previous token, taking it to roughly 4.6 µs, which is still not worth it.
