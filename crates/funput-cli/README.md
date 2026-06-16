# funput-cli

Binary **công cụ phát triển** — chạy `funput-engine` trực tiếp từ terminal để test, debug, và CI mà không cần build platform shell hay cấp Accessibility.

## Ý nghĩa

`funput-cli` là cách nhanh nhất trả lời: **“Engine có transform đúng không?”**

Không thay thế bộ gõ thật trên OS. Không hook keyboard. Chỉ mô phỏng input → output để dev và automated test.

## Trách nhiệm

| Làm | Không làm |
|-----|-----------|
| Gọi `funput-engine` với chuỗi key / text | CGEventTap, inject vào app khác |
| In `ImeResult` (action, backspace, chars) | Production IME cho end user |
| Test nhanh Telex/VNI từ command line | Settings UI |
| Scriptable cho CI | FFI export |

## Use cases

### 1. Transform một chuỗi → app-text

Input là **chuỗi literal**; space/dấu câu là word boundary. Output mặc định chỉ in
text cuối (dễ pipe/diff).

```bash
funput run "a1 b2"               # → á b2       (VNI mặc định)
funput run "xin1 chao2"          # → xín chào
funput run -m telex "xins chaof" # → xín chào   (Telex)
funput run -m telex "card "      # → card       (English restore khi gặp space)
funput run -m telex "card"       # → cảd        (chưa boundary → chưa restore)
```

### 2. Xem từng bước (pipeline)

```bash
funput run --steps "a1"
# #   key   action  bs  output   buffer
# 1   a     None    0   -        a
# 2   1     Send    1   á        á
# → á
```

### 3. REPL tương tác

```bash
funput repl              # gõ một dòng + Enter, xem kết quả; :q hoặc Ctrl-D để thoát
funput repl -m telex --steps
```

### 4. CI / regression

```bash
cargo test -p funput-core
cargo test -p funput-engine
cargo test -p funput-cli
```

> Lưu ý: restore tiếng Anh chỉ kích hoạt tại **word boundary**. `run -m telex "card"`
> (không có space cuối) cho thấy trạng thái đang soạn `cảd`; thêm space → `card`.

### 5. Debug khi phát triển platform

Khi macOS inject sai, so sánh:

- Output `funput-cli` (engine đúng)
- vs hành vi thật trên app (inject layer sai)

→ Tách bug engine vs bug platform.

## Cấu trúc module

```
funput-cli/src/
├── main.rs      # clap parse → dispatch run | repl
├── cli.rs       # clap structs: Cli, Command, MethodArg
├── sim.rs       # simulate(method, input) → app-text + per-step (pure, có test)
├── render.rs    # format bảng --steps
└── repl.rs      # vòng lặp đọc dòng tương tác
```

## Phụ thuộc

```
funput-cli → funput-engine → funput-core
```

**Không** phụ thuộc `funput-ffi` — gọi engine Rust trực tiếp, tránh overhead FFI khi dev.

## Ai dùng?

| Đối tượng | Mục đích |
|-----------|----------|
| Contributor | Test local trước khi build macOS app |
| CI | Regression test Telex/VNI |
| Maintainer | Debug báo lỗi từ user (“gõ X ra Y”) |

End user **không** cần cài `funput-cli` — họ dùng app trong `platforms/`.

## Quan hệ với platform shell

```
funput-cli          →  funput-engine  (dev, trực tiếp)
platforms/macos     →  funput-ffi     → funput-engine  (production)
platforms/linux     →  funput-engine  (trực tiếp)
```

Cùng một engine — CLI chỉ là **cửa sổ debug**, không fork logic.

## Build & chạy

```bash
cargo run -p funput-cli -- telex "as"
cargo install --path crates/funput-cli   # optional
```
