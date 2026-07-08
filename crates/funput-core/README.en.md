# funput-core

**English** · [Tiếng Việt](README.md)

The **pure-logic** Vietnamese input crate: given a Latin-character buffer + the key just pressed, per
Telex/VNI, it returns the new buffer. No keyboard hooks, no I/O, no config files, no knowledge of
macOS/Windows/Linux.

## What this crate does

It answers exactly one question:

> Current buffer + the key just pressed, per Telex/VNI, becomes what string?

It is the **only** place that holds:

- VNI rules (`1`→sắc, `6`→`â`, `7`→`ơ`/`ư`, `8`→`ă`, `9`→`đ`, …)
- Telex rules (`s`→sắc, `f`→huyền, `aa`→`â`, `w`→móc/breve, `dd`→`đ`, …)
- Tone placement (traditional/modern) + the mũ/móc/trần shape marks
- Revert on a doubled modifier key (`a11` → `a1`)
- Syllable-structure validation (onset / rhyme / coda)
- Unicode tables (tones, vowel shapes, vowels)

**Stateless & pure:** no session, no backspace count, no English auto-restore — that is
`funput-engine`'s job. The engine diffs the before/after buffer to derive the backspace count to send.

## Public API

A small, stable surface for `funput-engine`. Breaking changes require semver coordination with the
engine.

| Symbol | Description |
|--------|-------------|
| `InputMethod` | `Telex` \| `Vni` |
| `ToneStyle` | `Traditional` (default) \| `Modern` — tone placement (see below) |
| `TransformKind` | `Pending` \| `Applied` \| `Reverted` \| `Ignored` |
| `TransformResult` | `{ kind, text }` — state after one key |
| `apply(buffer, key, method, tone_style) -> TransformResult` | One transform step |
| `apply_checked(buffer, key, method, tone_style, spell_check) -> TransformResult` | Like `apply` + a **spell-check** gate: when `spell_check` is on, a diacritic is placed only if the result can still become a valid VN syllable, otherwise the modifier key stays a literal (`mix` + ngã → `mĩx` is blocked). `spell_check = false` ≡ `apply` |
| `is_valid(buffer) -> bool` | Buffer **could** still be a valid VN syllable (lenient) |
| `is_complete_syllable(buffer) -> bool` | Buffer is a **complete** VN syllable (strict) |
| `is_definitely_invalid(buffer) -> bool` | Buffer can **definitely** never become a VN syllable |

The three `is_*` functions let `funput-engine` decide whether to keep the Vietnamese text or restore the
raw Latin: `is_complete_syllable` is used at a word boundary; `is_definitely_invalid` drives eager
restore (flip back the instant it is certain the word is not Vietnamese).

```rust
use funput_core::{apply, InputMethod, ToneStyle, TransformKind};

let r = apply("a", '1', InputMethod::Vni, ToneStyle::Traditional);
assert_eq!(r.kind, TransformKind::Applied);
assert_eq!(r.text, "á");
```

### TransformKind

- `Pending` — the key is appended, not transformed yet (waiting for the next key). Also used for a
  **pass-through** modifier on non-Vietnamese text: `text` + `1` → `"text1"`.
- `Applied` — a tone / shape / stroke / reposition produced new `text`.
- `Reverted` — a doubled modifier: strip the diacritic, then re-insert the raw key (`a11` → `a1`, Telex
  `ass` → `as`).
- `Ignored` — the modifier was rejected, `text` unchanged (`ng` + `1`, a stroke on a non-`d`).

## Tone placement — `ToneStyle`

The two styles **differ only** on the open glide-initial rhymes: `oa`, `oe`, `uy`.

| Rhyme | `Traditional` (default) | `Modern` |
|-------|-------------------------|----------|
| `oa` | hòa (mark on `o`) | hoà (mark on `a`) |
| `oe` | khỏe | khoẻ |
| `uy` | thúy | thuý |

Everything else is **identical** in both styles:

- `ia`/`ua` → mark on the first vowel: `mía`, `múa`.
- With a final consonant: `hoàn`, `toán`, `huýt`.
- A vowel carrying mũ/móc (`â ê ô ơ ư ă`) always takes the mark: `trường`, `việt`, `người`.
- Triphthongs: mark on the middle vowel: `ngoài`, `xoáy`.

Placement is **independent of where the tone key is typed** — `hoaf` and `hofa` give the same result for
the chosen style. Reference: [Quy tắc đặt dấu thanh của chữ Quốc ngữ](https://vi.wikipedia.org/wiki/Quy_t%E1%BA%AFc_%C4%91%E1%BA%B7t_d%E1%BA%A5u_thanh_c%E1%BB%A7a_ch%E1%BB%AF_Qu%E1%BB%91c_ng%E1%BB%AF).

## Module layout

```
src/
├── lib.rs                    # Public API + apply()
├── input_method/             # Key classification → KeyAction. The ONLY place VNI and Telex differ.
│   ├── vni.rs                # 1–9
│   └── telex.rs              # s/f/r/x/j, aa/dd/ee/oo, w (buffer-aware)
├── composition/              # Transform pipeline, shared by VNI + Telex
│   ├── transform.rs          # Orchestrate: revert → validate → apply
│   ├── apply.rs              # Apply stroke / tone / shape to the buffer
│   └── revert.rs             # Strip the diacritic on a doubled modifier key
├── validation/
│   ├── parse.rs              # Split onset / nucleus / coda
│   ├── rhyme.rs              # Valid-rhyme table — the core of "is this Vietnamese?"
│   └── syllable.rs           # is_valid / is_complete_syllable / is_definitely_invalid + modifier gate
└── unicode/
    ├── marks.rs              # Tone-mark table
    ├── shapes.rs             # mũ / móc / breve table
    ├── vowels.rs             # Source of truth for vowels
    └── tone_position.rs      # Pick the vowel that takes the mark (Traditional/Modern) + reposition
```

## One-keystroke pipeline

```
key
 └─ input_method::{vni,telex}::classify_key(buffer, key) → KeyAction (Tone / Shape / Stroke / RemoveTone / Normal)
     └─ composition::transform::apply_action
         ├─ try revert (doubled modifier)     → Reverted
         ├─ validation::syllable (gate)        → Ignored | PassThrough(Pending)
         └─ composition::apply                 → Applied
             └─ unicode::{tone_position, marks, shapes}
```

The `Tone` and `Normal` branches also take a `ToneStyle` to place/reposition the mark. The first stage
that matches wins.

## Behavior examples

VNI (default `Traditional`):

| Type | Result | Note |
|------|--------|------|
| `a1` | `á` | tone |
| `d9` | `đ` | stroke |
| `uo7` | `uơ` | open rhyme (`thuo73` → `thuở`) |
| `hoa2` | `hòa` | tone placement (Traditional) |
| `vie5t` | `việt` | `ie` → tonal `ê` |
| `ngu7o7i2` | `người` | mark on `ơ` |
| `a11` | `a1` | Reverted |
| `text` + `1` | `text1` | Pending (the engine will restore) |

Telex:

| Type | Result |
|------|--------|
| `as` | `á` |
| `dd` | `đ` |
| `uow` | `uơ` (`thuowr` → `thuở`) |
| `nuocws` | `nước` |
| `hoaf` | `hòa` (Traditional) / `hoà` (Modern) |
| `ass` | `a` (revert tone) |

Difference between the two tone-placement styles:

| Type (VNI) | `Traditional` | `Modern` |
|------------|---------------|----------|
| `hoa2` | `hòa` | `hoà` |
| `thuy3` | `thủy` | `thuỷ` |
| `khoe3` | `khỏe` | `khoẻ` |
| `mua1` | `múa` | `múa` (unchanged) |

## Tests

```bash
cargo test   -p funput-core
cargo test   -p funput-core vni_full_regression
cargo test   -p funput-core telex_full_regression
cargo clippy -p funput-core -- -D warnings
cargo doc    -p funput-core --no-deps
```

Tests are grouped by input method: `tests/telex/` (basic, cases, corpus, parity, `properties` —
**proptest**, regression) and `tests/vni/` (basic, cases, regression), plus `tests/spellcheck_corpus.rs`
for the spell-check gate. The Telex corpus is a TSV table loaded at **compile time** (`include_str!` →
`tests/telex/data/telex_corpus.tsv`); the other cases are **Rust consts** (no serde, no runtime loader).
Shared helpers in `tests/support/`.

## Dependencies & boundaries

- Depends on **no** other Funput crate; `std` only. **No** `serde`, `libc`, `std::os`.
- Only `funput-engine` (and tests) call this crate. Platform code (macOS IMKit, Windows hook, Fcitx5
  addon) does **not** import `funput-core` directly — it goes through `funput-engine`.

| funput-core | funput-engine |
|-------------|---------------|
| Pure one-step `apply()` | Holds the session buffer; computes backspace from the diff |
| Validate syllable structure | Decide English auto-restore at a word boundary |
| Unicode tables + tone-placement rules | Map hardware keycode → char |
