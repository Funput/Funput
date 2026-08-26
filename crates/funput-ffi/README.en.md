# funput-ffi

**English** · [Tiếng Việt](README.md)

The **C ABI** boundary for `funput-engine` — lets a **non-Rust** shell drive the engine through C
functions. The engine ships in a `.dylib`/`.so`/`.a`; the native side (Swift, C++) loads and calls it.

> A **Rust** consumer (the Windows shell, `funput-cli`) links `funput-engine` directly and does **not**
> need this crate. FFI is for **macOS** (Swift IMKit), **iOS** (the Swift keyboard extension) and the
> **Fcitx5 addon plus the IBus engine on Linux** (C++/C). Android goes through `funput-jni`, not this crate.

## What this crate does

Only **boundary marshalling**: `extern "C"` + `#[repr(C)]`, converting `ImeResult` (Rust) ↔
`FunputResult` (C), plus null-safety. **No** Telex/VNI logic, **no** hook/inject — those belong to the
engine and the platform.

## C API (`include/funput.h`)

Handle-based; results are returned **by value** (POD, no free needed); input is a **codepoint** (the
platform maps keycode → char itself). Every function is **null-safe** (a null handle / invalid codepoint
yields a `None` result) and **panic-safe**: every entry point that touches the engine runs inside
`abi::safe()` (`catch_unwind`), so a panic in the engine is caught at the boundary and turned into a
no-op result — it **never** unwinds into the host (which would abort the whole IME process).

Personal suggestions use a separate opaque `FunputSuggestionEngine` handle. Queries return at most
three UTF-32 candidates as a POD; open/learn/flush/compact/reset failures return null, `false`, or an
empty result. Platforms drive this handle serially on a worker, never from the composition path.

Per-app VI/EN memory uses another separate opaque handle, `FunputAppLanguage`, also independent of
`FunputEngine`: it only decides "should this app be Vietnamese", the host calls `funput_set_enabled`
itself with the answer. `id` is **UTF-8 bytes** (bundle id / exe name / WM_CLASS), not UTF-32 like
composed text — it's a technical identifier, not something the user typed.
`funput_app_language_note_focus` returns `-1` (never seen this app — the host leaves the current
state as-is), `0` (English), or `1` (Vietnamese). The handle does no file I/O: the host reloads its
saved memory with `funput_app_language_seed` at startup (mirroring `funput_add_shortcut`), and
persists it whenever `funput_app_language_note_toggle` returns `true`.

```c
typedef struct FunputEngine FunputEngine;   // opaque handle

typedef struct {
    uint8_t  action;        // 0=None, 1=Send, 2=Restore
    uint32_t backspace;     // chars to delete before inserting
    uint32_t count;         // valid codepoints in chars (<= 64)
    uint32_t chars[64];     // UTF-32 output; chars[0..count] are valid
} FunputResult;

FunputEngine *funput_engine_new(void);
void          funput_engine_free(FunputEngine *engine);

typedef struct {                 // every option, passed by value
    uint8_t method;              //   0=Telex, 1=VNI, 2=Telex Advanced
    uint8_t tone_style;          //   0=Traditional, 1=Modern
    bool smart_restore, eager_restore, spell_check, auto_capitalize;
} FunputConfig;

void          funput_configure(FunputEngine *engine, FunputConfig config);   // apply the whole set
void          funput_set_method(FunputEngine *engine, uint8_t method);       // runtime method switch
void          funput_set_enabled(FunputEngine *engine, bool enabled);        // runtime VI/EN
void          funput_clear(FunputEngine *engine);                            // word boundary / focus change

FunputResult  funput_process_char(FunputEngine *engine, uint32_t codepoint);
FunputResult  funput_backspace(FunputEngine *engine);                       // Backspace while composing
uintptr_t     funput_buffer(const FunputEngine *engine, uint32_t *out, uintptr_t cap); // copy composing buffer (UTF-32) into out, returns char count
```

Applying a result: `action == 0 (None)` → let the app receive the key as usual; otherwise delete
`backspace` chars, then insert `chars[0..count]`. `funput_buffer` lets the platform render the
preedit/marked text from the composing buffer.

```c
typedef struct FunputAppLanguage FunputAppLanguage;   // opaque handle, independent of FunputEngine

FunputAppLanguage *funput_app_language_new(void);
void               funput_app_language_free(FunputAppLanguage *handle);

bool funput_app_language_seed(FunputAppLanguage *handle, const uint8_t *id, uintptr_t id_len, bool enabled);
void funput_app_language_clear(FunputAppLanguage *handle);
bool funput_app_language_forget(FunputAppLanguage *handle, const uint8_t *id, uintptr_t id_len);

// -1 = never seen this app (leave the current state as-is), 0 = English, 1 = Vietnamese.
int32_t funput_app_language_note_focus(const FunputAppLanguage *handle, const uint8_t *id, uintptr_t id_len);
bool    funput_app_language_note_toggle(FunputAppLanguage *handle, const uint8_t *id, uintptr_t id_len, bool enabled);
```

Knowing for certain *which* app is focused when the user toggles (e.g. a tray icon or the Settings
window stealing focus) is a platform concern, not this handle's — the platform resolves `id` before
calling `funput_app_language_note_toggle`, the same way the pending/deferred override already works
today on macOS and Windows.

The header is generated by **cbindgen** (committed). Regenerate after changing the `extern "C"` surface:

```bash
bash scripts/gen-header.sh    # requires: cargo install cbindgen
```

## Marshalling (`src/engine/result.rs`)

`FunputResult::from_ime(&ImeResult)`:
- `Action::{None, Send, Restore}` → `0 / 1 / 2`.
- `output.chars()` → `chars[..count]`, **truncated** at `CHARS_CAP = 64` (the overflow policy lives
  here, not in the engine).
- `backspace as u32`. Input via `char::from_u32(codepoint)`; `None` → empty result.

## Memory ownership

| Side | Responsibility |
|------|----------------|
| Rust (`funput_engine_new`) | Allocates the handle |
| Caller (Swift/C++) | Calls `funput_engine_free()` exactly **once** per handle |
| `funput_process_char` / `funput_backspace` | Return **by value** — no allocation, no per-result free |

Only the **handle** needs freeing (Swift typically `deinit { funput_engine_free(handle) }`). A result is
POD on the stack → no leak.

## macOS flow (example)

```
IMKInputController.handle (Swift)
   └─ keycode → codepoint
      funput_process_char(engine, cp)        ← funput-ffi
         └─ funput-engine → FunputResult (by value)
            Swift reads action / backspace / chars[0..count]
            → setMarkedText / insertText      ← outside funput-ffi
```

## Layout & build

```
src/lib.rs          # crate root: docs + module tree + flat C re-export surface
src/engine/         # composition IME C API (FunputEngine)
                    #   mod.rs      opaque FunputEngine + new/free + group re-exports
                    #   compose.rs  process_char/key, buffer, backspace, flip, clear, arm
                    #   config.rs   FunputConfig + configure() + set_method/set_enabled
                    #   shortcuts.rs add_shortcut/clear_shortcuts (text expansion, "gõ tắt")
                    #   result.rs   #[repr(C)] FunputResult + from_ime() + CHARS_CAP/ACTION_*
src/suggestion/     # personal-suggestion C API (FunputSuggestionEngine)
                    #   engine.rs (handle new/open/free), query.rs (learn/query),
                    #   store.rs (flush/compact/reset/stats), types.rs (candidate/stats PODs)
src/app_language/   # per-app VI/EN memory C API (FunputAppLanguage), independent of FunputEngine
                    #   handle.rs (handle new/free + shared UTF-8 marshalling),
                    #   memory.rs (seed/clear/forget), focus.rs (note_focus/note_toggle),
                    #   types.rs (APP_LANG_UNKNOWN/ENGLISH/VIETNAMESE)
src/charset/       # C API charset conversion (convert + detect), behind `charset`
                    #   mod.rs      count/name + charset indices + UTF-32 writing
                    #   convert.rs  #[repr(C)] FunputConversion + funput_charset_convert
                    #   detect.rs   funput_charset_detect
src/abi/            # shared C-ABI plumbing
                    #   guard.rs safe(): catch_unwind + null-handle; codec.rs UTF-32 marshalling
cbindgen.toml
scripts/gen-header.sh
include/funput.h     # GENERATED (committed)
```

`crate-type = ["cdylib", "staticlib", "rlib"]`. Artifacts: macOS `libfunput_ffi.a`/`.dylib` + header
(built via `platforms/macos/scripts/build-ffi.sh`); iOS `FunputCore.xcframework` (built via
`platforms/ios/Scripts/build-ffi.sh`, device + simulator slices); Windows does not use it (the shell
links the engine directly); the Linux Fcitx5 addon and the IBus engine link `libfunput_ffi` and
include `funput.h`.

Edition 2024 note: use `#[unsafe(no_mangle)]` and explicit `unsafe { }` around `Box::from_raw` /
`ptr.as_mut()`.

## The `charset` feature (**off** by default)

Charset conversion (`funput_charset_*`) sits behind a cargo feature. The iOS and Android keyboards
link this crate and have no use for the tables, so the default build does not carry them. CI checks
both halves: the default library exports no `funput_charset_*` symbol, and the feature-on build still
lints and tests clean.

A desktop shell turns it on in two places — the library and the header:

```bash
cargo build -p funput-ffi --release --features charset
cc -DFUNPUT_CHARSET ...        # the header declares them inside #ifdef FUNPUT_CHARSET
```

`platforms/macos/scripts/build-ffi.sh` does **not** enable it yet: no macOS UI calls it, and enabling
it early only puts tables in a binary nothing reads. When that UI is written, add `--features charset`
to the script's `cargo build` and `FUNPUT_CHARSET` to the Xcode target's
`GCC_PREPROCESSOR_DEFINITIONS`.

A charset is named by its **index** into `funput_core::charset::ALL`, not by a name the host spells
for itself: `Charset` is `#[non_exhaustive]`, so no code outside `funput-core` can enumerate it.
`funput_charset_count()` and `funput_charset_name()` are between them enough to build a menu, and a
charset added later turns up in it. That list is **append-only**, so the index is safe to persist as
the user's saved choice.

## Dependencies & callers

- `funput-ffi → funput-engine → funput-core`, plus independent `funput-ffi → funput-suggestions`.
  `app_language/` depends on neither `funput-engine` nor `funput-suggestions` — only `std`.
- Consumers: `platforms/macos` (Swift, bridging header), `platforms/ios` (Swift, via
  `FunputCore.xcframework`), `platforms/linux/fcitx5` (C++, `ffi_handle.h`) and
  `platforms/linux/ibus` (C). **Not** used by: `funput-cli`, the Windows shell (both link the engine
  directly), Android (via `funput-jni`).

## Tests

```bash
cargo test  -p funput-ffi
cargo clippy -p funput-ffi --all-targets -- -D warnings
cargo build -p funput-ffi && ls target/debug/libfunput_ffi.*   # .a .dylib .rlib
```

`src/engine/result.rs` (unit: `from_ime`, truncate > 64) + `tests/round_trip.rs` (drives the `extern "C"` API
like a C caller: Telex/VNI/English-restore, null-safety, surrogate). `tests/app_language.rs` covers the
`FunputAppLanguage` API the same way (seed/note_focus/note_toggle/forget/clear, null-safety).
