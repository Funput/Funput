# Telex V5 benchmark report

Measured on the same Apple M-series machine and Rust toolchain. Core, engine and
FFI were remeasured on 2026-07-18. Criterion baseline `telex_v5_before` was
captured from `main` before the V5 production changes.

## Common path

Existing benchmark IDs were not renamed. The Full Telex pairs live in separate
benchmark binaries so adding them cannot perturb the common-path harness.

| Layer | Baseline median | V5 median | Median delta | V5 throughput |
|---|---:|---:|---:|---:|
| Core Telex | 3.239 µs/corpus | 3.161 µs/corpus | -2.41% | 20.57 M keys/s |
| Engine Telex | 9.022 µs/corpus | 8.337 µs/corpus | -7.59% | 9.48 M keys/s |
| C FFI Telex | 9.560 µs/corpus | 9.001 µs/corpus | -5.85% | 8.78 M keys/s |

No common path regressed; all three layers are faster than the pre-V5 baseline.
Criterion results vary slightly with host load.

## Full Telex pair

Both sides commit the same phrase: `ư ừ tư mơ trường người ừng.`. Canonical Telex
needs 40 keys; Full Telex needs 31. This compares equivalent user-visible work,
instead of comparing composition against a Standard Telex literal pass-through.

| Layer | Canonical: latency / throughput | Full: latency / throughput | Phrase latency |
|---|---:|---:|---:|
| Core | 2.292 µs / 17.02 M keys/s | 1.104 µs / 27.18 M keys/s | **51.8% lower** |
| Engine | 5.174 µs / 7.73 M keys/s | 3.630 µs / 8.54 M keys/s | **29.8% lower** |
| C FFI | 5.571 µs / 7.18 M keys/s | 3.907 µs / 7.93 M keys/s | **29.9% lower** |

Throughput is normalized by each corpus's real key count, while latency is the
total time to produce the same phrase. Full Telex is faster here because it both
uses fewer keys and applies direct vowel shortcuts. These numbers describe the
feature path; the table above remains the regression gate for ordinary Telex.

## Allocation and layout gates

- Common path: 1.1 allocations/key, 4 B/key.
- Full Telex: 1.4 allocations/key, 6 B/key.
- A separate equal-length allocation corpus: 55 events on each path.
- Budgets remain 2 allocations/key and 24 B/key.
- `InputMethod` remains 1 byte; `KeyAction` remains at most 2 bytes; `Engine`
  remains 136 bytes.
