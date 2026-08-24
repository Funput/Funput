# funput-ffi

[English](README.en.md) · **Tiếng Việt**

Biên **C ABI** cho `funput-engine` — để shell **không phải Rust** gọi engine qua hàm C. Engine chạy
trong `.dylib`/`.so`/`.a`; phía native (Swift, C++) load và gọi.

> Consumer **Rust** (Windows shell, `funput-cli`) link `funput-engine` trực tiếp và **không** cần
> crate này. FFI dành cho **macOS** (Swift IMKit), **iOS** (Swift keyboard extension) và
> **addon Fcitx5 + engine IBus trên Linux** (C++/C). Android đi qua `funput-jni`, không qua crate này.

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

Per-app VI/EN memory dùng một opaque handle riêng (`FunputAppLanguage`), cũng độc lập với
`FunputEngine`: nó chỉ quyết định "app này nên VI hay EN", host tự gọi `funput_set_enabled` với kết
quả. `id` là **UTF-8 bytes** (bundle id / exe name / WM_CLASS), không phải UTF-32 như văn bản soạn
thảo — vì đây là định danh kỹ thuật, không phải chữ người dùng gõ. `funput_app_language_note_focus`
trả `-1` (chưa từng gặp app này — host giữ nguyên trạng thái hiện tại), `0` (English) hoặc `1`
(Vietnamese). Handle không đọc/ghi file: host tự nạp lại bộ nhớ đã lưu bằng
`funput_app_language_seed` lúc khởi động (giống `funput_add_shortcut`), và tự lưu xuống đĩa mỗi khi
`funput_app_language_note_toggle` trả `true`.

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

typedef struct {                 // toàn bộ tuỳ chọn, truyền theo giá trị
    uint8_t method;              //   0=Telex, 1=VNI, 2=Telex Advanced
    uint8_t tone_style;          //   0=Traditional, 1=Modern
    bool smart_restore, eager_restore, spell_check, auto_capitalize;
} FunputConfig;

void          funput_configure(FunputEngine *engine, FunputConfig config);   // áp cả cụm
void          funput_set_method(FunputEngine *engine, uint8_t method);       // đổi kiểu gõ lúc chạy
void          funput_set_enabled(FunputEngine *engine, bool enabled);        // VI/EN lúc chạy
void          funput_clear(FunputEngine *engine);                            // ranh giới từ / đổi focus

FunputResult  funput_process_char(FunputEngine *engine, uint32_t codepoint);
FunputResult  funput_backspace(FunputEngine *engine);                       // Backspace khi đang soạn
uintptr_t     funput_buffer(const FunputEngine *engine, uint32_t *out, uintptr_t cap); // chép buffer đang soạn (UTF-32) vào out, trả số ký tự
```

Áp kết quả: `action == 0 (None)` → để app nhận phím như thường; ngược lại xoá `backspace` ký tự rồi
chèn `chars[0..count]`. `funput_buffer` để platform vẽ preedit/marked text từ buffer đang soạn.

```c
typedef struct FunputAppLanguage FunputAppLanguage;   // opaque handle, độc lập FunputEngine

FunputAppLanguage *funput_app_language_new(void);
void               funput_app_language_free(FunputAppLanguage *handle);

bool funput_app_language_seed(FunputAppLanguage *handle, const uint8_t *id, uintptr_t id_len, bool enabled);
void funput_app_language_clear(FunputAppLanguage *handle);
bool funput_app_language_forget(FunputAppLanguage *handle, const uint8_t *id, uintptr_t id_len);

// -1 = chưa từng gặp app này (giữ nguyên trạng thái hiện tại), 0 = English, 1 = Vietnamese.
int32_t funput_app_language_note_focus(const FunputAppLanguage *handle, const uint8_t *id, uintptr_t id_len);
bool    funput_app_language_note_toggle(FunputAppLanguage *handle, const uint8_t *id, uintptr_t id_len, bool enabled);
```

Việc "biết chắc app nào đang focus khi người dùng bấm toggle" (ví dụ tray icon/cửa sổ Settings cướp
focus) là việc của platform, không phải của handle này — platform tự resolve `id` trước khi gọi
`funput_app_language_note_toggle`, giống cách `pending`/deferred override đã hoạt động hôm nay trên
macOS và Windows.

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
                    #   config.rs   FunputConfig + configure() + set_method/set_enabled
                    #   shortcuts.rs add_shortcut/clear_shortcuts (gõ tắt)
                    #   result.rs   #[repr(C)] FunputResult + from_ime() + CHARS_CAP/ACTION_*
src/suggestion/     # C API personal suggestions (FunputSuggestionEngine)
                    #   engine.rs (handle new/open/free), query.rs (learn/query),
                    #   store.rs (flush/compact/reset/stats), types.rs (POD candidate/stats)
src/app_language/   # C API per-app VI/EN memory (FunputAppLanguage), độc lập FunputEngine
                    #   handle.rs (handle new/free + marshalling UTF-8 dùng chung),
                    #   memory.rs (seed/clear/forget), focus.rs (note_focus/note_toggle),
                    #   types.rs (APP_LANG_UNKNOWN/ENGLISH/VIETNAMESE)
src/charset/       # C API chuyển mã (chuyển đổi + nhận diện), sau feature `charset`
                    #   mod.rs      count/name + chỉ số bảng mã + ghi UTF-32
                    #   convert.rs  #[repr(C)] FunputConversion + funput_charset_convert
                    #   detect.rs   funput_charset_detect
src/abi/            # plumbing C-ABI dùng chung
                    #   guard.rs safe(): catch_unwind + null-handle; codec.rs UTF-32 marshalling
cbindgen.toml
scripts/gen-header.sh
include/funput.h     # GENERATED (committed)
```

`crate-type = ["cdylib", "staticlib", "rlib"]`. Artifact: macOS `libfunput_ffi.a`/`.dylib` + header
(build qua `platforms/macos/scripts/build-ffi.sh`); iOS `FunputCore.xcframework` (build qua
`platforms/ios/Scripts/build-ffi.sh`, slice device + simulator); Windows không dùng (shell link
engine trực tiếp); addon Fcitx5 và engine IBus trên Linux link `libfunput_ffi` + include `funput.h`.

Lưu ý edition 2024: dùng `#[unsafe(no_mangle)]` và `unsafe { }` tường minh quanh
`Box::from_raw` / `ptr.as_mut()`.

## Feature `charset` (mặc định **tắt**)

Công cụ chuyển mã (`funput_charset_*`) nằm sau một cargo feature. Bàn phím iOS và Android link crate
này và không dùng tới bảng mã, nên bản mặc định không mang chúng theo. CI kiểm cả hai nửa: bản mặc
định **không** export symbol `funput_charset_*` nào, và bản bật feature vẫn lint/test sạch.

Một shell desktop bật nó bằng hai bước — thư viện và header:

```bash
cargo build -p funput-ffi --release --features charset
cc -DFUNPUT_CHARSET ...        # header khai báo trong #ifdef FUNPUT_CHARSET
```

`platforms/macos/scripts/build-ffi.sh` **chưa** bật feature này: chưa có UI macOS nào gọi tới, và bật
sớm chỉ nhét bảng mã vào binary mà không ai dùng. Khi viết UI đó, thêm `--features charset` vào lệnh
`cargo build` trong script và `FUNPUT_CHARSET` vào `GCC_PREPROCESSOR_DEFINITIONS` của target Xcode.

Bảng mã được gọi tên bằng **chỉ số** trong `funput_core::charset::ALL`, không phải bằng tên host tự
đặt: `Charset` là `#[non_exhaustive]` nên không code nào ngoài `funput-core` liệt kê được nó.
`funput_charset_count()` + `funput_charset_name()` đủ để dựng menu, và bảng mã thêm sau tự hiện ra.
Danh sách đó **chỉ thêm vào cuối**, nên chỉ số dùng làm lựa chọn lưu lại được.

## Phụ thuộc & ai gọi

- `funput-ffi → funput-engine → funput-core`, và `funput-ffi → funput-suggestions` độc lập.
  `app_language/` không phụ thuộc `funput-engine` hay `funput-suggestions` — chỉ `std`.
- Consumer: `platforms/macos` (Swift, bridging header), `platforms/ios` (Swift, qua
  `FunputCore.xcframework`), `platforms/linux/fcitx5` (C++, `ffi_handle.h`) và
  `platforms/linux/ibus` (C). **Không** dùng: `funput-cli`, Windows shell (đều link engine trực
  tiếp), Android (qua `funput-jni`).

## Tests

```bash
cargo test  -p funput-ffi
cargo clippy -p funput-ffi --all-targets -- -D warnings
cargo build -p funput-ffi && ls target/debug/libfunput_ffi.*   # .a .dylib .rlib
```

`src/engine/result.rs` (unit: `from_ime`, truncate > 64) + `tests/round_trip.rs` (gọi `extern "C"` như C
caller: Telex/VNI/English-restore, null-safety, surrogate). `tests/app_language.rs` kiểm cả API
`FunputAppLanguage` theo cùng phong cách (seed/note_focus/note_toggle/forget/clear, null-safety).
