# funput-ffi

[English](README.en.md) · **Tiếng Việt**

Biên **C ABI** cho `funput-engine` — để shell **không phải Rust** gọi engine qua hàm C. Engine chạy
trong `.dylib`/`.so`/`.a`; phía native (Swift, C++) load và gọi.

> Consumer **Rust** (Windows shell, `funput-cli`) link `funput-engine` trực tiếp và **không** cần
> crate này. FFI chỉ dành cho **macOS** (Swift IMKit) và **addon Fcitx5 trên Linux** (C++).

## Crate này làm gì

Chỉ **marshal tại biên**: `extern "C"` + `#[repr(C)]`, chuyển `ImeResult` (Rust) ↔ `FunputResult`
(C), null-safety. **Không** logic Telex/VNI, **không** hook/inject — đó là việc của engine và
platform.

## C API (`include/funput.h`)

Handle-based; kết quả trả **theo giá trị** (POD, không cần free); input là **codepoint** (platform
tự map keycode → char). Mọi hàm **null-safe** (handle null / codepoint không hợp lệ → kết quả
`None`) và **panic-safe**: mọi entry point chạm engine chạy trong `support::safe()`
(`catch_unwind`), nên panic trong engine bị chặn tại biên và trả kết quả no-op — **không bao giờ**
unwind sang host (điều sẽ abort cả tiến trình IME).

Personal suggestion dùng một opaque handle riêng (`FunputSuggestionEngine`). Query trả tối đa ba
candidate UTF-32 bằng POD; open/learn/flush/compact/reset lỗi đều trả null, `false` hoặc kết quả rỗng.
Handle này chỉ được gọi tuần tự trên worker của platform và không tham gia đường compose.

```c
typedef struct FunputEngine FunputEngine;   // opaque handle

typedef struct {
    uint8_t  action;        // 0=None, 1=Send, 2=Restore
    uint32_t backspace;     // số ký tự xoá trước khi chèn
    uint32_t count;         // số codepoint hợp lệ trong chars (<= 64)
    uint32_t chars[64];     // UTF-32 output; chars[0..count] hợp lệ
} FunputResult;

FunputEngine *funput_engine_new(void);
void          funput_engine_free(FunputEngine *engine);

void          funput_set_method(FunputEngine *engine, uint8_t method);      // 0=Telex, 1=VNI
void          funput_set_tone_style(FunputEngine *engine, uint8_t style);   // 0=Traditional, 1=Modern
void          funput_set_enabled(FunputEngine *engine, bool enabled);
void          funput_set_smart_restore(FunputEngine *engine, bool on);
void          funput_set_eager_restore(FunputEngine *engine, bool on);
void          funput_clear(FunputEngine *engine);                            // ranh giới từ / đổi focus

FunputResult  funput_process_char(FunputEngine *engine, uint32_t codepoint);
FunputResult  funput_backspace(FunputEngine *engine);                       // Backspace khi đang soạn
uintptr_t     funput_buffer(const FunputEngine *engine, uint32_t *out, uintptr_t cap); // chép buffer đang soạn (UTF-32) vào out, trả số ký tự
```

Áp kết quả: `action == 0 (None)` → để app nhận phím như thường; ngược lại xoá `backspace` ký tự rồi
chèn `chars[0..count]`. `funput_buffer` để platform vẽ preedit/marked text từ buffer đang soạn.

Header sinh bằng **cbindgen** (đã commit). Regen sau khi đổi `extern "C"` surface:

```bash
bash scripts/gen-header.sh    # cần: cargo install cbindgen
```

## Marshalling (`src/engine/result.rs`)

`FunputResult::from_ime(&ImeResult)`:
- `Action::{None, Send, Restore}` → `0 / 1 / 2`.
- `output.chars()` → `chars[..count]`, **cắt** ở `CHARS_CAP = 64` (chính sách tràn nằm ở đây, không
  ở engine).
- `backspace as u32`. Input `char::from_u32(codepoint)`; `None` → kết quả rỗng.

## Sở hữu bộ nhớ

| Bên | Trách nhiệm |
|-----|-------------|
| Rust (`funput_engine_new`) | Cấp phát handle |
| Caller (Swift/C++) | Gọi `funput_engine_free()` đúng **một lần** mỗi handle |
| `funput_process_char` / `funput_backspace` | Trả **by value** — không cấp phát, không free per-result |

Chỉ cần free **handle** (Swift thường `deinit { funput_engine_free(handle) }`). Result là POD trên
stack → không rò rỉ.

## Luồng trên macOS (ví dụ)

```
IMKInputController.handle (Swift)
   └─ keycode → codepoint
      funput_process_char(engine, cp)        ← funput-ffi
         └─ funput-engine → FunputResult (by value)
            Swift đọc action / backspace / chars[0..count]
            → setMarkedText / insertText      ← ngoài funput-ffi
```

## Cấu trúc & build

```
src/lib.rs          # crate root: docs + module tree + re-export surface phẳng (C)
src/engine/         # C API composition (FunputEngine)
                    #   mod.rs      opaque FunputEngine + new/free + re-export nhóm
                    #   compose.rs  process_char/key, buffer, backspace, flip, clear, arm
                    #   config.rs   7 setter: method/tone_style/enabled/smart|eager/spell/autocap
                    #   shortcuts.rs add_shortcut/clear_shortcuts (gõ tắt)
                    #   result.rs   #[repr(C)] FunputResult + from_ime() + CHARS_CAP/ACTION_*
src/suggestion/     # C API personal suggestions (FunputSuggestionEngine)
                    #   engine.rs (handle new/open/free), query.rs (learn/query),
                    #   store.rs (flush/compact/reset/stats), types.rs (POD candidate/stats)
src/abi/            # plumbing C-ABI dùng chung
                    #   guard.rs safe(): catch_unwind + null-handle; codec.rs UTF-32 marshalling
cbindgen.toml
scripts/gen-header.sh
include/funput.h     # GENERATED (committed)
```

`crate-type = ["cdylib", "staticlib", "rlib"]`. Artifact: macOS `libfunput_ffi.a`/`.dylib` + header
(build qua `platforms/macos/scripts/build-ffi.sh`); Windows không dùng (shell link engine trực
tiếp); Linux addon Fcitx5 link `libfunput_ffi` + include `funput.h`.

Lưu ý edition 2024: dùng `#[unsafe(no_mangle)]` và `unsafe { }` tường minh quanh
`Box::from_raw` / `ptr.as_mut()`.

## Phụ thuộc & ai gọi

- `funput-ffi → funput-engine → funput-core`, và `funput-ffi → funput-suggestions` độc lập.
- Consumer: `platforms/macos` (Swift, bridging header) và `platforms/linux/fcitx5` (C++,
  `ffi_handle.h`). **Không** dùng: `funput-cli`, Windows shell (đều link engine trực tiếp).

## Tests

```bash
cargo test  -p funput-ffi
cargo clippy -p funput-ffi --all-targets -- -D warnings
cargo build -p funput-ffi && ls target/debug/libfunput_ffi.*   # .a .dylib .rlib
```

`src/engine/result.rs` (unit: `from_ime`, truncate > 64) + `tests/round_trip.rs` (gọi `extern "C"` như C
caller: Telex/VNI/English-restore, null-safety, surrogate).
