# Funput benchmarks

Reproducible, open measurements for Funput's speed (a lean Rust core) and
**round-trip coverage** of Vietnamese syllables. Everything here
runs from the `app/` directory with stock `cargo` — no hidden setup, so anyone can
verify the numbers.

> Numbers below were measured on an Apple M-series dev machine; latency is
> machine-dependent, so **re-run on your hardware**. Coverage is deterministic for
> a fixed corpus and Funput revision.

## B1 — Speed (latency / throughput / footprint)

[Criterion](https://github.com/bheisler/criterion.rs) microbenchmarks over a
realistic Vietnamese key sequence (tones, circumflex/horn/breve, the `đ` stroke).

```sh
cargo bench -p funput-core   --bench apply          # per-keystroke transform
cargo bench -p funput-engine --bench process_char   # full engine path (+ boundaries)
cargo bench -p funput-ffi    --bench latency         # end-to-end across the C FFI
```

Criterion writes an interactive HTML summary to
`target/criterion/report/index.html`. Individual benchmark reports live under
their corresponding directories in `target/criterion/`.

Telex V2 baseline (`telex_v2_w_before`, same machine/toolchain): core Telex
**19.39 M keys/s**, engine **9.01 M keys/s**, and C FFI **8.23 M keys/s**.
The V2 suites add equal-length `w-permutation/canonical` and
`w-permutation/deferred` inputs without renaming these common-path IDs.

| Metric | Result |
|---|---|
| Compose latency (core `apply`) | **~0.05 µs / keystroke** (Telex), ~0.05 µs (VNI) |
| Compose throughput | **~19.0 million keystrokes / second** |
| Free-position `aa/ee/oo` compose throughput | **~18.4 million keystrokes / second** |
| Full engine path (`process_char`, incl. boundary + English-restore) | **~0.12 µs / keystroke** (~8.5 M/s) |
| Free-position `aa/ee/oo` through the full engine | **~0.09 µs / keystroke** (~10.6 M/s) |
| **End-to-end across the C FFI** (`process_char` + read composed text back) | **~0.12 µs / keystroke** (~8.5 M/s) |
| `size_of::<Engine>` (per-field session state) | **136 bytes** |
| Heap allocations per keystroke (with or without spell-check) | **~1** (~7 B) |
| Release FFI shared lib (`libfunput_ffi.dylib`, LTO + stripped) | ~0.37 MB |

A human types a few keys per second; Funput answers each in **sub-microsecond**
time, i.e. millions of times faster than needed — composition is never the
bottleneck.

The core and engine suites also contain equal-length canonical/free-position
circumflex pairs (`chaan`/`chana`, `hoom`/`homo`, …). They keep the ordinary
typing benchmark stable while measuring the extra candidate validation directly.

### What "end-to-end" measures (and what it doesn't)

The `latency` bench drives the **real C ABI a platform shell uses** for every key:
`funput_process_char` (returns the ~268-byte `FunputResult` POD by value) **plus**
`funput_buffer` (copies the composed text back out to render the marked text). This
is the layer the pure-core bench skips.

Key finding: it lands at **~0.12 µs/keystroke — essentially identical to the engine
alone**, so the FFI boundary (handle indirection + by-value result + UTF-32 copy)
adds negligible cost.

**Honest scope:** this is *keystroke → composed text available to the platform* —
Funput's full contribution. It does **not** include OS keystroke delivery (IMKit /
ibus / fcitx5 / the Windows hook) or the host app's own text render, which are not
Funput's code and cannot be measured reproducibly here. To measure true
keystroke-to-pixels you need per-platform instrumentation (e.g. a high-FPS capture
or an accessibility-API probe); that is out of scope for an automated benchmark.

Footprint check:

```sh
cargo test -p funput-engine -- --nocapture engine_struct_size   # prints size_of::<Engine>
cargo test -p funput-engine --test alloc_budget -- --nocapture  # heap allocs per keystroke (budget-guarded)
ls -lh target/release/libfunput_ffi.dylib                       # after: cargo build --release -p funput-ffi
```

The `alloc_budget` test doubles as a regression guard: it fails if a change adds
heap allocations to the keystroke hot path beyond the committed budget, and its
budgets are ratcheted down as allocation-removal work lands.

## B2 — Coverage (round-trip)

For each Vietnamese syllable in a corpus we **encode** it to the Telex/VNI
keystrokes that would produce it, type those back through the real engine, and
check we get the original syllable. A syllable is covered if it reproduces
under **either** tone style (`hòa` and `hoà` are both valid). Smart-restore is off
to isolate pure composition. The corpus is filtered to structurally valid
Vietnamese syllables, so acronyms (AIDS), symbols (Ar/As), foreign words, and
malformed entries (stacked or misplaced diacritics) are excluded.

```sh
# Default: the committed, MIT-clean sample corpus.
cargo run --release -p funput-cli -- dev coverage benchmarks/sample.txt --show-mismatches 10

# Headline: a large external word list (downloaded, not vendored — see below).
sh benchmarks/fetch-corpus.sh
cargo run --release -p funput-cli -- dev coverage benchmarks/.corpus/Viet74K.txt
cargo run --release -p funput-cli -- dev coverage benchmarks/.corpus/Viet74K.txt --json
```

| Corpus | Syllables | Telex | VNI |
|---|---|---|---|
| Viet74K (full) | 8,956 | **100%** | **100%** |
| `sample.txt` | 137 | **100%** | **100%** |

Every structurally valid syllable round-trips under both methods. The one Telex
subtlety — the `oo`→`ô` digraph in genuine double-`o` loanwords (`boong`, `xoong`,
`soóc`) — is handled exactly the way a user types them: a third `o` escapes the
digraph (`booong`→`boong`), which the encoder emits. VNI has no digraphs.

Malformed corpus entries (two tone marks in one syllable, or a tone on the wrong
vowel) are **not counted** — they are not valid Vietnamese, and the engine correctly
declines to reproduce them (see `CORPUS_NOISE` in `crates/funput-cli/src/coverage.rs`).

## Data & licensing

`sample.txt` is our own, MIT-licensed. We **do not vendor** the large word list —
its license differs from Funput's MIT — so `fetch-corpus.sh` downloads
[Viet74K](https://github.com/duyet/vietnamese-wordlist) into `.corpus/`
(gitignored). This keeps the third-party corpus separate from the MIT repository.

## What this number means

This is a corpus **round-trip coverage** measurement, not a real-user accuracy
score: Funput's encoder generates one canonical key sequence per syllable and the
engine checks whether it can reproduce that syllable. It does not measure typing
habits, corrections, or false conversions in non-Vietnamese text. We publish
absolute numbers rather than head-to-head claims against closed-source IMEs.
