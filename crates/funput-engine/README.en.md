# funput-engine

**English** · [Tiếng Việt](README.md)

A **stateful orchestration** crate: it takes one key at a time, holds the composing buffer, calls
`funput-core`, and returns an `ImeResult` telling the **platform what to do** (how many chars to
delete, what string to insert). No keyboard hooks, no injection, no C ABI, no UI.

## What this crate does

`funput-core` answers "what does this string transform into". `funput-engine` answers:

> After the key just pressed, **what should the platform do** — pass the key, swallow it, delete how
> many chars, insert what?

It is the **single source of truth** for typing state: the composition buffer, input method
(Telex/VNI), tone-placement style, enabled flag, word boundaries, and English restore.

## `ImeResult` — the platform contract

```rust
pub enum Action {
    None,    // Pass the key through to the app — no transform
    Send,    // Transform — delete `backspace` chars, then insert `output`
    Restore, // Restore the raw Latin text (ESC, …)
}

pub struct ImeResult {
    pub action: Action,
    pub backspace: usize,  // chars to delete in the app
    pub output: String,    // string to insert after deleting
}
```

`ImeResult` is a Rust-native shape. Only `funput-ffi` marshals it into its `#[repr(C)]` struct at the
FFI boundary (`backspace: u32`, `count: u32`, `chars: [u32; 64]`) — the 64-char cap and the overflow
policy live there, **not** in the engine. The platform reads `ImeResult` and decides **how** to inject
(Backspace+Unicode, preedit/marked text…); injection logic is not part of this crate.

## Engine API

| Method | Description |
|--------|-------------|
| `new()` | Create an engine (defaults: enabled, Telex, Traditional) |
| `process_char(key: char) -> ImeResult` | Process one Unicode scalar (the platform maps keycode → char) |
| `on_backspace() -> ImeResult` | User pressed Backspace while composing → resync the buffer |
| `flip_composing() -> ImeResult` | Flip the composing word between its Vietnamese form and raw keys (`card` ⇄ `cải`); the choice is sticky for the rest of the word |
| `set_enabled(bool)` / `is_enabled()` | Enable/disable Vietnamese input (English pass-through) |
| `set_method(InputMethod)` / `method()` | Telex ↔ VNI |
| `set_tone_style(ToneStyle)` / `tone_style()` | Tone placement (traditional `hòa` / modern `hoà`) |
| `set_smart_restore(bool)` | Auto-restore non-Vietnamese words to their raw Latin keys |
| `set_eager_restore(bool)` | Restore the instant it is certain, without waiting for a space |
| `set_spell_check(bool)` | Spell-check — only place a diacritic if the result can still be a valid VN syllable |
| `set_auto_capitalize(bool)` | Auto-capitalize the first letter of a sentence |
| `arm_capitalization()` | Arm capitalization for the next word (platform calls this on text-field focus) |
| `clear()` | Reset buffer + keys (word boundary, focus change) |
| `buffer() -> &str` | The composing text — the platform renders it as preedit/marked text |
| `keys() -> &str` | Raw keystrokes since the last word boundary — used for English restore |
| `add_shortcut(trigger, expansion)` | Define a text shortcut (`vn` → `Việt Nam`); an empty trigger is ignored |
| `remove_shortcut(&str)` | Remove one shortcut by its trigger |
| `clear_shortcuts()` | Clear the whole shortcut table (clear + re-add = replace-all when syncing config) |
| `shortcuts() -> &HashMap<String, String>` | Read the current shortcut table |

Spell-check, auto-capitalize, flip, and the 4 shortcut methods are all **additive** changes on the E4
API (added only, nothing in the old surface broken).

Re-exports: `Action`, `ImeResult` (from `result.rs`). Breaking changes require semver coordination with
`funput-ffi`.

## Per-key processing flow

```
platform → engine.process_char(key)
   ├─ word boundary (space / punctuation)? → boundary: (English restore | keep) then clear()
   └─ otherwise → funput-core::apply(buffer, key, method, tone_style)
        → diff(old buffer, new buffer) → (backspace, output)
        → update session.buffer → ImeResult
```

Example, Telex `a` → `s` → `á`:

| Key | Action | backspace | output |
|-----|--------|-----------|--------|
| `a` | `None` | 0 | — (waiting for the next key) |
| `s` | `Send` | 1 | `á` |

At step 2 the platform deletes 1 char, inserts `á`, and swallows the `s`.

## `TransformKind` → `ImeResult`

How a core result maps to a platform action:

| `TransformKind` (core) | `action` | `session.buffer` after | backspace / output |
|------------------------|----------|------------------------|--------------------|
| `Pending` | `None` (pass key) | `result.text` (= old + key) | 0 / — |
| `Ignored` | `None` (pass key) | `old + key` (engine appends itself) | 0 / — |
| `Applied` | `Send` (swallow key) | `result.text` | diff(old, new) |
| `Reverted` | `Send` (swallow key) | `result.text` | diff(old, new) |

`Pending`/`Ignored` both turn the key into an ordinary character so app ↔ buffer stay in sync — the
only difference is whether core already appended it (`Pending`) or the engine joins it (`Ignored`).
When `enabled = false`, the engine skips core entirely (`Action::None`).

### diff (old buffer → new)

```
prefix    = count of shared leading chars
backspace = old.chars().count() - prefix
output    = new.chars().skip(prefix).collect()
```

`hoa` → `hoà`: prefix 2 (`ho`) → backspace 1, output `à`. `diff` returns `(usize, String)`, with **no**
cap — size limits apply only in `funput-ffi`.

## English restore

While typing an English word, core still adds diacritics (`card` → `cảd`). At a word boundary, if the
buffer is **not** a complete Vietnamese syllable (`funput_core::is_complete_syllable`, strict) and
`keys != buffer`, the engine `Send`s the **raw keystrokes** (`keys`) + the boundary key, then
`clear()`s. `eager_restore` does this the instant the buffer becomes a dead end instead of waiting for
a space.

Dictionary-free: an English word that happens to be a valid VN syllable (`test` → `tét`) is **not**
auto-restored — in exchange it never breaks correctly-typed Vietnamese (like UniKey without a
dictionary).

## Flip VN ⇄ raw keys

`flip_composing()` flips the **composing word** between its Vietnamese form and its raw keystrokes
(`card` ⇄ `cải`), and back on the next call. It returns `Send` (delete + insert) for hosts that type
real text, or `None` when there is nothing to flip (nothing composing, or the VN form equals the raw
keys like `the`). Because it only touches the *uncommitted* word, the host just re-renders its marked
text — so it works in every app.

The choice is **sticky**: after a flip, further keystrokes keep the chosen form and the word boundary
will **not** English-restore it back. The override is reset per word (`clear()`). The logic lives in
`flip.rs`; `session.vn_form` keeps the VN form captured before an eager restore can collapse the
buffer to raw keys, so a flip can recover it even after a restore.

## Shortcuts / Text expansion (macro)

A user-defined trigger → expansion table (`vn` → `việt nam`, `kg` → `không`). At a **word boundary**
the engine matches the **raw keystrokes** (`keys`) against the table using **smart-case** matching:
typing `vn`/`Vn`/`VN` all resolve to the same trigger, and the expansion is re-cased to match —
`vn` → `việt nam`, `Vn` → `Việt Nam` (capitalize every word), `VN` → `VIỆT NAM` (all uppercase). Keys
with a mixed case that don't fit one of those three patterns (e.g. `vNa`) fall back to an **exact**
match only, so a deliberately mixed-case trigger (e.g. `iOS`) still works exactly as defined. See
`classify_case`/`apply_shortcut_case` in `compose/boundary.rs`.

- Hit → `Send`: delete what is displayed (`backspace = buffer.chars().count()`), insert `expansion +
  boundary key`, then `clear()`. Backspace counts the displayed buffer, so `as` → `á` (one char) is
  still deleted correctly.
- Shortcuts take **priority** over English restore and over keeping the composed buffer.
- They expand only at a word boundary (never mid-word), and only while the IME is **enabled**
  (`process_char` returns early when off) — a shortcut is part of the input method.

The shortcut table is **session-lived config**: `clear()` (word boundary / focus change) does not touch
it, only `clear_shortcuts()` clears it. The engine does no file I/O — the platform loads config and
calls `add_shortcut`.

## Module layout

```
src/
├── lib.rs        # Engine + public API, re-exports Action/ImeResult
├── result.rs     # Action, ImeResult
├── session.rs    # state: enabled, method, tone_style, buffer, keys, toggles (restore/spell/autocap), shortcuts, flip override
├── pipeline.rs   # process(session, key): TransformKind → ImeResult
├── boundary.rs   # word boundary + English-restore decision
├── flip.rs       # flip VN ⇄ raw keys for the composing word (sticky override)
└── diff.rs       # buffer diff → (backspace, output)
```

## Dependencies & callers

- Depends on: `funput-core` only. **No** `serde`, no platform crate.
- **Consumers that link the engine directly (Rust):** `funput-ffi`, `funput-cli`, and the **Windows
  shell** (`platforms/windows/src-tauri` keeps the `Engine` in-process).
- **Via `funput-ffi` (C ABI):** macOS (Swift IMKit), iOS (the Swift keyboard extension, through
  `FunputCore.xcframework`), and the Fcitx5 addon plus the IBus engine on Linux (C++/C). None of
  these link the engine directly.
- **Via `funput-jni`:** Android (the Kotlin IME).

## Tests

```bash
cargo test   -p funput-engine
cargo test   -p funput-engine engine_full_regression
cargo clippy -p funput-engine -- -D warnings
cargo doc    -p funput-engine --no-deps
```

`tests/` is grouped by theme — each group is one test binary (an entry file) plus a subfolder:

- `methods/` (telex, vni) · `restore/` (english, toggle) · `diacritics/` (placement, remove_tone,
  stroke) · `words/` (boundary, shortcut).
- `engine_api.rs`: end-to-end public `Engine` API. `engine_fixtures.rs` + `fixtures/step_cases.rs`:
  fixture regression (`engine_full_regression`). Shared helpers in `support/`.
