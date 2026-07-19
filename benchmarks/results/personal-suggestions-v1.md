# Personal suggestions V1 benchmark

Measured on 2026-07-18 with Rust 1.96.0 in the workspace Release profile. Criterion
used a warm 5,000-word personal lexicon; the percentile harness sampled 100,000
queries. Disk measurements include `sync_data`/`sync_all` and run outside the
typing hot path.

## Lookup and memory

| Path | Median |
|---|---:|
| Exact prefix, 1 scalar | 68 ns |
| Exact prefix, 5 scalars | 214 ns |
| Exact prefix, 8 scalars | 326 ns |
| Accent-folded prefix | 111 ns |
| Miss | 141 ns |
| C FFI top-3 query | 267 ns |
| Learn existing word | 2.39 µs |

The Rust latency harness measured p50 **250 ns**, p95 **250 ns**, p99 **292 ns**
and 3.87 million queries/second. The C FFI harness measured p50 **250 ns**, p95
**292 ns**, p99 **292 ns** and 3.54 million queries/second. Warm Rust lookup made
zero heap allocations across 100,000 queries.

The 5,000-word fixture retained approximately **572,504 bytes**. Tests enforce a
4 MiB retained-heap budget, a 2 MiB snapshot budget, a hard 5,000-word capacity,
and non-growing trie node counts during 100,000-operation churn.

## Persistence

| Operation | Median |
|---|---:|
| Open 5,000-word snapshot | 2.17 ms |
| Open snapshot + replay 100 journal tokens | 3.98 ms |
| Flush 256 mutations | 5.19 ms |
| Compact 5,000 words | 9.83 ms |

The journal automatically compacts at 64 KiB and rejects oversized input files;
tests cover truncated frames, corrupted snapshots, newer schemas, abandoned
temporary files, and bounded journal growth.

An initial eviction implementation rebuilt the trie for every journal token and
took 143 ms to reopen. Stable slot replacement reduced the same benchmark to
3.98 ms; a rebuild now happens only when an already-promoted word is evicted.

## Existing typing regression guard

Criterion comparisons against `before-suggestions` report:

| Existing path | Median change | Result |
|---|---:|---|
| funput-core Telex compose | -0.38% | No change detected |
| funput-engine Telex paragraph | +1.39% | Within noise threshold |
| funput-ffi Telex process + render | +1.04% | No change detected |
| funput-ffi VNI process + render | +0.62% | Within noise threshold |

The engine allocation guard is unchanged at **1.1 allocations / 4 bytes per
keypress** for both default and spell-check paths. Suggestion functions are new,
independent exports and are not called by `funput_process_char` or the composer.

The stripped Release dynamic library grew from 402,592 bytes to 585,216 bytes
(+182,624 bytes); the static archive grew by 601,400 bytes. Both include the new
Unicode normalizer, persistence code, trie, and C ABI.
