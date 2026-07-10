# funput-cli

[English](README.en.md) · **Tiếng Việt**

Binary `funput` — bề mặt dòng lệnh của Funput, gồm hai nhóm lệnh:

- **`funput term …`** — gõ tiếng Việt trong app terminal qua PTY wrapper (thư viện `funput-term`;
  **không** phải IME, **không** cần quyền). Chi tiết xem `crates/funput-term`.
- **`funput dev …`** — chạy `funput-engine` thẳng từ terminal để test, debug, và CI mà **không** cần
  build platform shell hay cấp quyền Accessibility. Trả lời nhanh đúng một câu: **"Engine transform
  đúng không?"** — nạp một chuỗi qua engine rồi **mô phỏng vai trò platform** (áp từng `ImeResult` vào
  app-text model) để in ra text người dùng sẽ thấy.

Phần dưới tập trung vào `funput dev` (khía cạnh dev-tool).

## Dùng — `funput dev`

```bash
cargo run -p funput-cli -- dev run "a1 b2"                # → á b2       (VNI mặc định)
cargo run -p funput-cli -- dev run -m telex "xins chaof"  # → xín chào   (Telex)
# hoặc cài binary tên `funput`
cargo install --path crates/funput-cli
funput dev run "xin1 chao2"          # → xín chào
funput dev run -m telex "card "      # → card    (English-restore khi gặp dấu cách)
funput dev run -m telex "card"       # → cảd     (chưa tới boundary → chưa restore)
funput dev run --steps "a1"          # bảng từng phím
funput dev repl -m telex --steps     # REPL: gõ một dòng + Enter; :q hoặc Ctrl-D để thoát
funput dev coverage benchmarks/sample.txt   # round-trip Telex & VNI trên corpus
```

```
funput dev run      [-m telex|vni] [--steps] <INPUT>       # transform → app-text (hoặc bảng --steps)
funput dev repl     [-m telex|vni] [--steps]               # REPL đọc từng dòng
funput dev coverage [CORPUS] [--json] [--show-mismatches N] [--limit N]
```

- `INPUT` là **chuỗi literal**; dấu cách và dấu câu là **ranh giới từ**. English-restore chỉ kích
  hoạt tại boundary (Telex `"card "` → `card`; `"card"` → `cảd` vì chưa boundary).
- `-m, --method` mặc định `vni` (CLI luôn set method tường minh qua `Engine::set_method`).
- Mặc định in **chỉ app-text** (dễ pipe/diff); `--steps` in bảng từng phím:

```
$ funput dev run --steps "a1"
#   key   action  bs  output   buffer
1   a     None    0   -        a
2   1     Send    1   á        á
→ á
```

REPL **line-based** (không raw-mode → không thêm dep): banner in ra **stderr** để stdout sạch cho
pipe (`printf 'a1\nd9\n:q\n' | funput dev repl`).

### Coverage (round-trip corpus)

`funput dev coverage` mã hoá ngược mỗi âm tiết → phím → gõ lại qua engine → so khớp, cho cả Telex &
VNI (đúng nếu tái tạo được ở **một trong hai** kiểu đặt dấu). `--json` in máy-đọc-được cho CI:

```bash
funput dev coverage benchmarks/sample.txt --show-mismatches 10
funput dev coverage benchmarks/.corpus/Viet74K.txt --json
```

## Mô phỏng platform (`dev/sim.rs` — trái tim, thuần, có test)

`simulate(method, input) -> Simulation { app_text, steps }` làm **đúng** việc một platform shell làm:
áp từng `ImeResult` vào app-text.

```rust
match result.action {
    Action::None           => app_text.push(key),            // app nhận phím
    Action::Send | Restore => { /* pop `backspace` ký tự */ app_text.push_str(&output) },
}
```

`Restore` gộp chung với `Send` để forward-compatible. Mỗi `Step` ghi `{ key, action, backspace,
output, buffer }` cho `--steps`. `sim`/`encode`/`render` thuần I/O-free → unit test trực tiếp; handler
(`term`/`dev`) chỉ lo I/O.

## Cấu trúc module

```
src/
├── main.rs        # thin: clap parse → dispatch → ExitCode; in lỗi tập trung một nơi
├── cli.rs         # Cli, Command{Term, Dev}, MethodArg(→InputMethod), CliError/CliResult
├── term/
│   └── mod.rs     # args + handler `funput term` (wrapper qua funput-term + install)
└── dev/
    ├── mod.rs     # args + dispatch `funput dev` (run/repl/coverage)
    ├── sim.rs     # simulate() — mô phỏng platform, thuần, có test
    ├── render.rs  # steps_table(&Simulation) -> String  (bảng --steps)
    ├── repl.rs    # REPL line-based
    ├── encode.rs  # mã hoá ngược text → phím Telex/VNI (cho coverage)
    └── coverage/  # mod (round-trip check) + corpus (nạp/lọc) + report (human/json)
```

Mỗi nhóm lệnh là **một thư mục** tự chứa args + `run() -> CliResult`. Thêm sản phẩm mới = thêm
`src/<product>/mod.rs` + một biến thể `Command` + một nhánh dispatch trong `main`.

## Phụ thuộc & ai dùng

- `funput-cli → funput-term, funput-engine → funput-core`, thêm `clap` và `unicode-normalization`
  (mã hoá ngược NFD cho coverage). **Không** `funput-ffi` — gọi engine Rust **trực tiếp**, tránh
  overhead FFI khi dev.
- `funput term`: end user gõ tiếng Việt trong terminal. `funput dev`: contributor (test local trước
  khi build app), CI (regression Telex/VNI + coverage), maintainer (tái hiện báo lỗi "gõ X ra Y").

Cùng một engine với mọi platform → `funput dev` chỉ là **cửa sổ debug**, không fork logic.

## Tests

```bash
cargo test   -p funput-cli
cargo clippy -p funput-cli --all-targets -- -D warnings
```

Unit test: `sim` (Telex/VNI cơ bản + đa từ `xins chaof`→`xín chào`, English-restore ở boundary
`card `→`card`, `mas `→`má `, ghi đúng từng `--steps`), `render` (bảng có header + summary), `encode`
(round-trip `text→phím→engine→text` cho danh sách từ, cả Telex & VNI). Đối chiếu app-text với
`funput-engine/tests/fixtures/step_cases.rs` để chắc CLI khớp engine.

## Còn làm

- **Chưa expose các toggle của `SimConfig`**: `funput dev run` mới có `-m method`; seam
  `simulate_with(SimConfig, …)` đã sẵn để thêm `--tone-style`, `--smart-restore`, `--spell-check`.
- **REPL per-keystroke** (raw-mode, cần `crossterm`) — hiện chỉ line-based.
