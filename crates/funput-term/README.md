# funput-term

Gõ **tiếng Việt** trong các app terminal (Claude Code, Cursor, shell, REPL…) — nơi bộ gõ
hệ thống thường lỗi vì terminal chạy raw-mode.

`funput-term` là một **PTY wrapper trong suốt**: nó chạy app của bạn trong một
pseudo-terminal, soạn tiếng Việt từ phím gõ rồi đẩy text đã hoàn chỉnh vào app. **Không**
cần quyền Accessibility, **không** daemon, **không** hook hệ thống — và chạy trong **mọi**
terminal emulator (iTerm2, Terminal.app, Alacritty, kitty, WezTerm, tmux, SSH…).

## Cài & chạy

```bash
cargo run -p funput-term -- claude        # gõ tiếng Việt trong Claude CLI
cargo run -p funput-term -- cursor
cargo run -p funput-term -m telex -- bash # chọn Telex (mặc định VNI)
funput-term                               # không tham số → bọc $SHELL
```

- Gõ như bình thường: VNI `xin1 chao2` hoặc Telex `xins chaof` → **xín chào**.
- **`Ctrl-\`**: bật/tắt tiếng Việt (trạng thái VI/EN hiện ở **tiêu đề cửa sổ**).
- Từ tiếng Anh không hợp lệ tiếng Việt tự khôi phục khi gặp dấu cách (`card ` → `card`).

## "Luôn bật" cho mọi app trong terminal

```bash
alias claude='funput-term -- claude'      # từng app
# hoặc cấu hình terminal emulator chạy:  funput-term -- $SHELL
```
Bọc shell một lần → mọi app trong cửa sổ terminal đó đều gõ được tiếng Việt.

## Phạm vi & giới hạn (v1)

| | |
|--|--|
| Terminal emulator | **Tất cả** (chỉ cần TTY) |
| Hệ điều hành | macOS, Linux. Windows (ConPTY): đang làm |
| App nhập theo dòng (shell, Claude, Cursor, REPL) | Soạn đầy đủ |
| App full-screen (vim, less, htop) | **Tự tắt** soạn để không phá UI |
| Backspace giữa lúc soạn | Xoá 1 ký tự, giữ ngữ cảnh để soạn tiếp (`Phua` ⌫ `s` → `Phú`) |

## Cách hoạt động

```
bàn phím ─► funput-term ─(soạn tiếng Việt)─► app con (trong PTY) ─► màn hình
```
Engine (`funput-core`/`funput-engine`) trả lệnh "xoá N ký tự + chèn chuỗi"; wrapper dịch
thành bytes (`DEL × N` + UTF-8) đẩy vào stdin app con. Cùng engine với toàn bộ hệ Funput —
đây chỉ là một "frontend" cho terminal.

## Quan hệ

```
funput-core → funput-engine → funput-term   (Rust, link trực tiếp; KHÔNG qua funput-ffi)
```
`funput-term` giải bài toán terminal; IME hệ thống cho mọi app GUI (macOS IMKit, Windows
TSF, Linux Fcitx5) là hướng riêng, dùng `funput-ffi`. Terminal là điểm mù của IME hệ thống,
nên `funput-term` vẫn hữu ích ngay cả khi đã có chúng.

## TODO (sau v1)

- [ ] **Bracketed paste**: khi dán text/code (`ESC[200~ … ESC[201~`), pass-through thô
  thay vì soạn từng ký tự (tránh méo nội dung dán). Phát hiện tương tự alt-screen.
- [ ] **Windows (ConPTY)**: hiện chỉ macOS/Linux (Unix PTY). Stack `portable-pty` đã sẵn,
  cần thêm tầng input/resize cho Windows.
- [ ] **Cấu hình**: phương thức (Telex/VNI) và phím toggle đang cố định qua CLI arg; thêm
  config bền (env/file) + đổi phương thức lúc đang chạy.
- [ ] **Docs**: cập nhật `IMPLEMENTATION.md` với các tính năng smart (eager restore, bảng
  vần, backspace sync, Enter/Tab routing).
