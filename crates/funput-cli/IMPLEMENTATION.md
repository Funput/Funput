# funput-cli — Tài liệu hiện thực

Binary **dev-tool** chạy `funput-engine` từ terminal. Không phải IME thật: không hook
keyboard, không inject. Nó nạp một chuỗi input qua engine rồi **mô phỏng vai trò
platform** (áp `ImeResult` vào app-text model) để in ra text người dùng sẽ thấy.

**Tiền đề:** `funput-engine` E4 (API frozen) hoàn tất.

---

## Ranh giới

| `funput-engine` | `funput-cli` |
|-----------------|--------------|
| `process_char(char) -> ImeResult` | Lặp qua từng ký tự của input |
| Trả `Action` + `backspace` + `output` | **Áp** vào app-text (None→append, Send→del+inject) |
| Không I/O | Đọc args (clap), in stdout, REPL stdin |

CLI là một "platform" tối giản, scriptable. Cùng engine với `platforms/*` — chỉ là cửa sổ debug, **không fork logic**.

---

## Phụ thuộc
- `funput-engine` (API), `funput-core` (`InputMethod`)
- `clap` (derive) — parser. Core/engine vẫn zero-dep; chỉ binary này thêm dep.

---

## Cấu trúc module

```
src/
├── main.rs   # clap parse → dispatch run | repl
├── cli.rs    # Cli, Command{Run, Repl}, CommonOpts, MethodArg
├── sim.rs    # Method, Step, Simulation, simulate()  ← logic thuần, có unit test
├── render.rs # steps_table(&Simulation) -> String
└── repl.rs   # vòng lặp đọc dòng (dependency-free)
```

`sim::simulate` là trái tim — thuần, không I/O, test trực tiếp. `main`/`repl` chỉ lo I/O.

---

## CLI surface

```
funput run  [-m telex|vni] [--steps] <INPUT>   # transform, in app-text (hoặc bảng)
funput repl [-m telex|vni] [--steps]           # REPL đọc từng dòng
```

- `-m, --method` mặc định `vni` (CLI luôn set method tường minh qua `Engine::set_method`,
  không phụ thuộc default của engine).
- Input là **chuỗi literal**; space/dấu câu = word boundary. Restore tiếng Anh chỉ
  kích hoạt tại boundary (Telex `"card "` → `card`; `"card"` → `cảd` vì chưa boundary).
- Output mặc định: **chỉ app-text** (scriptable). `--steps`: bảng từng phím.
- REPL **line-based** (không raw-mode → không thêm dep): gõ dòng + Enter; `:q`/Ctrl-D thoát. Banner in ra stderr để stdout sạch cho pipe.

---

## Mô phỏng platform (`sim.rs`)

```rust
match result.action {
    Action::None              => app_text.push(key),         // app nhận phím
    Action::Send | Restore    => { pop backspace; push output } // del rồi inject
}
```
`Restore` gộp với `Send` để forward-compatible (E5 ESC); v1 engine chưa phát sinh.

---

## Phase

| Phase | Nội dung | Trạng thái |
|-------|----------|------------|
| C0 | Crate setup, workspace member, `[[bin]] name="funput"` | ✅ |
| C1 | `sim.rs` — `simulate` + unit tests | ✅ |
| C2 | `run` (clap) — app-text mặc định | ✅ |
| C3 | `--steps` + `render.rs` | ✅ |
| C4 | `repl` line-based | ✅ |
| C5 | Doc + README + clippy/doc sạch | ✅ |

---

## Verification

```bash
cargo run -p funput-cli -- run "a1 b2"              # → á b2 (VNI mặc định)
cargo run -p funput-cli -- run -m telex "xins chaof" # → xín chào
cargo run -p funput-cli -- run --steps "a1"         # → bảng 2 bước
printf 'a1\nd9\n:q\n' | cargo run -p funput-cli -- repl
cargo test -p funput-cli
cargo clippy -p funput-cli --all-targets
```

Đối chiếu app-text với `funput-engine/tests/fixtures/step_cases.rs` để chắc CLI khớp engine.

---

## Ngoài phạm vi (sau)
- JSON test-vectors (regression đã ở `cargo test`).
- REPL raw-mode per-keystroke (cần crossterm).
- Phím đặc biệt (Esc/Backspace) — chờ engine E5/E6.
