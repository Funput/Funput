# funput-term — Tài liệu hiện thực

**Terminal interposer** gõ tiếng Việt trong app CLI (Claude Code, Cursor, shell…).
Chạy app con trong một pseudo-terminal, soạn tiếng Việt trên luồng phím rồi đẩy bytes
đã hoàn chỉnh vào app con. **Không** hook hệ thống, **không** quyền, **không** daemon —
chạy trong mọi terminal emulator.

**Tiền đề:** `funput-engine` E4 (frozen). Link **trực tiếp** (Rust→Rust), không qua `funput-ffi`.

---

## Mô hình

```
real stdin ─raw bytes─► [input::Classifier] ─► engine? ─► [inject] ─► PTY ─► child
real stdout ◄────────── [output: scan alt-screen] ◄──────────────── PTY ◄─ child
            main: spawn child, chờ exit; thread SIGWINCH→resize; RAII khôi phục raw-mode
```

- Forward **thô** là mặc định (giữ nguyên escape/mouse/paste); chỉ **chặn** phím chữ ASCII để soạn.
- 2 thread: stdin→pty (`forward_input`) và pty→stdout (`forward_output`). Engine sống gọn trong thread input → không cần khoá.

---

## Có hỗ trợ tất cả terminal không?
- **Mọi terminal emulator** (iTerm2, Terminal.app, Alacritty, kitty, WezTerm, tmux, SSH…): CÓ — interposer trong suốt, chỉ cần TTY.
- **OS:** v1 macOS + Linux (Unix PTY). Windows ConPTY = TT6 (stack `portable-pty` đã sẵn).
- **App con:** nhập-theo-dòng (shell, Claude, Cursor) soạn đầy đủ; full-screen (vim/less) **tự tắt soạn** qua phát hiện alt-screen.

---

## Module
```
src/
├── main.rs    # clap: -m telex|vni, [-- command]; default $SHELL; toggle Ctrl-\
├── app.rs     # forward_input (PURE seam, có test) + run() orchestration
├── input.rs   # PURE: Classifier byte→ByteKind (Printable/Control/Escape/Utf8/Toggle)
├── inject.rs  # PURE: (char, &ImeResult) → bytes  (None→key; Send/Restore→DEL×bs+UTF-8)
├── output.rs  # forward_output + AltScreenScanner (ESC[?1049h/l, chịu split chunk)
├── term.rs    # RawModeGuard (RAII), set_title (OSC)
└── state.rs   # SharedState: enabled (toggle) + alt_screen (atomics)
```

`forward_input`/classifier/inject **thuần I/O-free** → unit test bằng pipe in-memory.

---

## Quy tắc chính
- `input.rs`: ESC → state machine (AfterEsc→Csi) forward thô tới hết chuỗi; toggle byte (`0x1c`) → `Toggle`; printable ASCII → `Printable(char)`; còn lại Control/Utf8.
- `app::forward_input`: `Printable` khi `state.composing()` → engine → `inject::result_bytes`; mọi thứ khác → `engine.clear()` + forward thô (control/escape/utf8/disabled là word boundary).
- `inject`: `None`→UTF-8 phím; `Send|Restore`→ `[0x7f; backspace]` + `output` (0x7f = DEL line-editor).
- Toggle: `state.toggle()` + `engine.clear()` + đổi title OSC (VI/EN).
- **Giới hạn v1:** Backspace người dùng = flush (engine chưa có `on_backspace`, để E6); composition khởi động lại sau backspace.

---

## Vòng đời / robustness
- `RawModeGuard` (RAII) vào raw-mode **trước** khi spawn → lỗi (không TTY) fail nhanh, không bỏ rơi child; drop luôn khôi phục (kể cả panic).
- `portable_pty::openpty` + `CommandBuilder` (kế thừa cwd + env). Child thoát → reader EOF → output thread dừng → exit đúng status.
- Resize: Unix `SIGWINCH` (`signal-hook`) → `crossterm::size` → `master.resize`. Windows: TT6.
- Alt-screen: `output.rs` thấy `ESC[?1049h` → `state.alt_screen` → input passthrough.

---

## Tests
- **Unit (16)**: classifier (printable/control/escape/arrow/alt-key/utf8/toggle), inject (None/Send/Restore), `forward_input` (compose "as"→`a`+DEL+`á`, control passthrough, toggle off), alt-screen scanner (kể cả split chunk), state.
- **E2E thủ công** (đã xác nhận): cấp TTY giả qua Python `pty` chạy `funput-term -m telex -- cat`, gõ `"xins chaof\r"` → render backspace → **`xín chào`**; VNI `vie65t`→`việt`; `card `→restore `card`. (Không commit vào CI vì phụ thuộc timing/PTY; là quy trình kiểm thử tay.)

---

## Phase
| Phase | Nội dung | Trạng thái |
|-------|----------|------------|
| TT0 | Plumbing trong suốt (PTY + raw-mode + forward) | ✅ |
| TT1 | Pure core `input`/`inject` + tests | ✅ |
| TT2 | Nối engine (soạn Telex/VNI) + e2e | ✅ |
| TT3 | Toggle `Ctrl-\` + title VI/EN | ✅ |
| TT4 | Alt-screen auto-off | ✅ |
| TT5 | Doc + README + clippy/doc sạch | ✅ |
| TT6 | Windows ConPTY (input/resize) | ⬜ sau |

---

## Verification
```bash
cargo test -p funput-term
cargo clippy -p funput-term --all-targets
cargo run -p funput-term -- cat        # gõ "as" → "á"
cargo run -p funput-term -- "$SHELL"   # gõ "xins chaof" → "xín chào"; Ctrl-\ toggle
cargo run -p funput-term -- claude     # ➜ kiểm chứng render app thật
```

---

## Dùng "luôn bật"
- Per-app alias: `alias claude='funput-term -- claude'`.
- Bọc shell: cấu hình terminal emulator chạy `funput-term -- $SHELL` → mọi app trong terminal đó gõ được tiếng Việt.

## Phụ thuộc
`funput-engine` · `portable-pty` (PTY/ConPTY) · `crossterm` (raw-mode) · `clap` · `signal-hook` (unix). **Không** `funput-ffi`.
