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

## Lookup, with a context and without

| Path | Prefix only | With a context |
|---|---:|---:|
| Exact prefix, 5 scalars | 232 ns | **3.0 µs** |
| Miss | 154 ns | **3.0 µs** |

| Prediction (no prefix) | |
|---|---:|
| The context is sharp, so it speaks | **3.9 µs** |
| The context is flat, so it stays quiet | **3.9 µs** |
| The context was never learned | **3.0 µs** |

The context figures are quoted to two significant figures because that is what
they hold: they move about 3% between runs, and the third digit would be noise
dressed up as measurement.

**These replace an earlier figure of 444 ns that was wrong, and wrong in a way
worth recording.** The first version of the benchmark learned its context word
*after* the 5,000-word fixture, so that word landed in a slot the fixture had just
freed near the front of `words` — and resolving it took about five comparisons
instead of a realistic number. The benchmark was measuring its own setup.
Resolving a context is a linear scan, so where the word sits in the list is the
whole cost, and the benchmarks now take their contexts from the middle of the
lexicon.

So the context costs about **2.7 µs**, nearly all of it that scan.

**What the scan actually costs is not the number of words it walks.** A context
the engine never learned is the one case that walks all 5,000 rather than half of
them, and it is the *fastest* of the three: 3.0 µs against 3.9. The word used
for it is longer than anything in the fixture, so every comparison fails on the
length check — which is read straight out of the record — and never follows the
pointer to the text. A word found halfway through pays a `memcmp`, and a cache
miss with it, for each of the 2,500 same-length words ahead of it.

The cost is therefore the number of words **of the same length** standing in
front of the target. That also means these figures are close to a worst case: the
fixture is 5,000 words of identical length, while Vietnamese syllables run from
two to twenty-one UTF-8 bytes, so most comparisons in real use would fail on
length and never reach the heap. How much cheaper is not measured here, and is
not guessed at either.

### On the index this was said not to need

The earlier justification for leaving an O(1) index over `words` unbuilt — "200 ns
does not justify 200 KB" — rested on the 444 ns that turned out to be a fixture
artefact. The conclusion still holds; the reasoning that reached it does not, and
a decision that is right for a wrong reason breaks the next time somebody reads it.

The reasoning that does hold: neither caller is on the typing path. `upsert_word`
runs once per space and `context_slot` once per keystroke, both on the shells'
background worker, and `funput-core` never touches either. Against a 16 ms frame,
3.9 µs is 0.02%; at five keystrokes a second it is about 20 µs of work per second
of typing. What it delays is the suggestion bar arriving a few microseconds later.

Against that, an index costs roughly 200 KB and a second copy of every word's
text — and, more to the point, a second structure that has to stay in step with
`words` through every insert, eviction and rebuild. That is the exact class of bug
the generation tags exist to make impossible, reintroduced somewhere new.

**If this ever does need to be faster, the cheap fix comes first.** `prev` does not
change while a word is being typed: four keystrokes into "chào" is four scans for
the same "xin". Remembering the last resolved context and comparing one string
against it turns four scans into one for effectively no memory. It needs interior
mutability, since `suggest_with` takes `&self`, which would make the engine
`!Sync` — worth checking against the FFI and JNI handles before reaching for it,
and still two orders of magnitude cheaper than a hash table.

Prediction is measured in all three outcomes because each runs once per space.
Speaking and staying quiet cost the same, which follows once the scan is
understood to dominate. The difference between prediction and the reranking path
is about a microsecond that the scan alone does not account for, and is left
unexplained here rather than explained wrongly.

Prefix-only lookup is 72.6 ns to 80.3 ns on the shortest prefix, about 10%. That is
the cost of `suggest` becoming `suggest_with(None, ...)` and the merge moving
behind two reusable calls; `#[inline]` recovers part of it, and the alternative is
the same merge written twice.

Warm lookup makes zero heap allocations on all three paths.

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

Lookup was p99 **292 ns** and 3.85 M queries/second while nothing read the edges;
see above for what reading them costs.

## Persistence

| Operation | V1 | V2 | Note |
|---|---:|---:|---|
| Open a 5,000-word snapshot | 2.17 ms | 2.31 ms | reads a follower section per word |
| Open + replay 100 journal tokens | 3.98 ms | 2.92 ms | replay no longer rebuilds the tries |
| Flush 256 mutations | 5.19 ms | 5.37 ms | first append to a new journal only |
| Compact 5,000 words | 9.83 ms | 13.69 ms | +3.9 ms, deliberate |

`compact` now fsyncs the directory after renaming the snapshot into place. A
rename is a directory change, and without that sync a power loss can reorder it
against the journal truncation that follows — leaving an empty journal and no
snapshot to have folded it into. The cost is paid on the shells' background
worker, never on the typing path.

The `flush_256` figure covers the append that *creates* the journal, which is the
only append that syncs the directory; steady-state appends add one `stat` and no
sync.

## Storage format

The snapshot is at v2 and the journal at v2; the two are versioned apart, because
they are separate formats that happened to share a number. Both still read v1, so
upgrading loses nothing, and a v1 file is rewritten at the current schema on its
first compact.

Costs are fixed by the format rather than measured. A snapshot word grows by
three bytes for its context count and follower count, plus six per occupied
follower slot — so a word nobody has followed costs three bytes and a word with
all four slots taken costs twenty-seven. Against the V1 record of twenty-two
bytes plus the word itself, a lexicon with no edges grows about 14%, and one with
every slot filled roughly doubles. The 2 MiB snapshot budget the tests enforce is
an order of magnitude away either way.

A journal token grows by exactly one byte: a flag saying whether it followed the
token before it. Recording pairs any other way was measurably worse — writing a
separate break record doubled the entries for every token learned without a
context, which is every token until the platforms pass one, and pushed `pending`
past its 256-entry limit after 128 learns, turning every flush into a full
compact. `flush_256` went to 9.97 ms before the flag byte replaced it.

## Measured and deliberately not changed

**Allocation at the C ABI / JNI boundary.** `funput_suggestion_query` decodes its
prefix into a `String` on every call, which contradicts the crate's
allocation-free lookup guarantee. It is not worth fixing: V1 measured the C FFI
harness at p50 **250 ns**, identical to the Rust harness at p50 **250 ns**, so
the allocation does not show up above the noise. Recorded here so the next reader
does not re-measure it.

**The linear scan over `words`.** Finding an existing word compares against up to
5,000 `String`s: 2.29 µs on the learn path, and — since the context work — about
2.7 µs on the query path as well, once per keystroke. A hash index would cost
roughly 200 KB and a second copy of every word's text.

This was recorded twice as "not worth it", the second time on the strength of a
444 ns measurement that turned out to be a benchmark artefact. The answer is still
no, for the reasons set out under the lookup figures above — and if it ever
becomes yes, a one-entry cache of the last resolved context comes before a hash
table.
