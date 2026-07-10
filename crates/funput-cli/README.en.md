# funput-cli

**English** · [Tiếng Việt](README.md)

The `funput` binary — Funput's command-line surface, with two families of commands:

- **`funput term …`** — type Vietnamese inside terminal apps via a PTY wrapper (the `funput-term`
  library; **not** an IME, **no** permissions). See `crates/funput-term`.
- **`funput dev …`** — drive `funput-engine` straight from the terminal for quick checks, debugging,
  and CI, with **no** need to build a platform shell or grant Accessibility. It answers one question:
  **"Does the engine transform correctly?"** — it feeds a string through the engine and **simulates the
  platform's role** (applying each `ImeResult` to an app-text model) to print the text a user would see.

The rest of this README focuses on `funput dev` (the dev-tool side).

## Usage — `funput dev`

```bash
cargo run -p funput-cli -- dev run "a1 b2"                # → á b2       (VNI, the default)
cargo run -p funput-cli -- dev run -m telex "xins chaof"  # → xín chào   (Telex)
# or install the binary named `funput`
cargo install --path crates/funput-cli
funput dev run "xin1 chao2"          # → xín chào
funput dev run -m telex "card "      # → card    (English-restore at the space boundary)
funput dev run -m telex "card"       # → cảd     (no boundary yet → not restored)
funput dev run --steps "a1"          # per-keystroke table
funput dev repl -m telex --steps     # REPL: type a line + Enter; :q or Ctrl-D to quit
funput dev coverage benchmarks/sample.txt   # round-trip Telex & VNI over a corpus
```

```
funput dev run      [-m telex|vni] [--steps] <INPUT>       # transform → app-text (or a --steps table)
funput dev repl     [-m telex|vni] [--steps]               # line-by-line REPL
funput dev coverage [CORPUS] [--json] [--show-mismatches N] [--limit N]
```

- `INPUT` is a **literal string**; spaces and punctuation are **word boundaries**. English-restore
  only fires at a boundary (Telex `"card "` → `card`; `"card"` → `cảd`, no boundary yet).
- `-m, --method` defaults to `vni` (the CLI always sets the method explicitly via `Engine::set_method`).
- By default it prints **only the app text** (easy to pipe/diff); `--steps` prints a per-keystroke table:

```
$ funput dev run --steps "a1"
#   key   action  bs  output   buffer
1   a     None    0   -        a
2   1     Send    1   á        á
→ á
```

The REPL is **line-based** (no raw mode → no extra dependency): its banner goes to **stderr** so
stdout stays clean for pipes (`printf 'a1\nd9\n:q\n' | funput dev repl`).

### Coverage (corpus round-trip)

`funput dev coverage` reverse-encodes each syllable → keystrokes → types it back through the engine →
compares, for both Telex & VNI (correct if it reproduces under **either** tone style). `--json` emits
a machine-readable report for CI:

```bash
funput dev coverage benchmarks/sample.txt --show-mismatches 10
funput dev coverage benchmarks/.corpus/Viet74K.txt --json
```

## Platform simulation (`dev/sim.rs` — the heart; pure, tested)

`simulate(method, input) -> Simulation { app_text, steps }` does **exactly** what a platform shell
does: apply each `ImeResult` to the app text.

```rust
match result.action {
    Action::None           => app_text.push(key),            // the app receives the key
    Action::Send | Restore => { /* pop `backspace` chars */ app_text.push_str(&output) },
}
```

`Restore` is folded in with `Send` to stay forward-compatible. Each `Step` records `{ key, action,
backspace, output, buffer }` for `--steps`. `sim`/`encode`/`render` are pure and I/O-free → unit-tested
directly; the handlers (`term`/`dev`) own the I/O.

## Module layout

```
src/
├── main.rs        # thin: clap parse → dispatch → ExitCode; errors reported in one place
├── cli.rs         # Cli, Command{Term, Dev}, MethodArg(→InputMethod), CliError/CliResult
├── term/
│   └── mod.rs     # args + handler for `funput term` (wrapper via funput-term + install)
└── dev/
    ├── mod.rs     # args + dispatch for `funput dev` (run/repl/coverage)
    ├── sim.rs     # simulate() — platform simulation; pure, tested
    ├── render.rs  # steps_table(&Simulation) -> String  (the --steps table)
    ├── repl.rs    # line-based REPL
    ├── encode.rs  # reverse-encode text → Telex/VNI keystrokes (for coverage)
    └── coverage/  # mod (round-trip check) + corpus (load/filter) + report (human/json)
```

Each command family is **one folder** owning its args + `run() -> CliResult`. Adding a product = a new
`src/<product>/mod.rs` + one `Command` variant + one dispatch arm in `main`.

## Dependencies & who uses it

- `funput-cli → funput-term, funput-engine → funput-core`, plus `clap` and `unicode-normalization`
  (NFD reverse-encoding for coverage). **No** `funput-ffi` — it calls the Rust engine **directly**,
  avoiding FFI overhead during development.
- `funput term`: end users typing Vietnamese in the terminal. `funput dev`: contributors (local checks
  before building an app), CI (Telex/VNI regression + coverage), maintainers (reproducing "typed X, got
  Y" reports).

The same engine backs every platform → `funput dev` is just a **debug window**, not a fork of the logic.

## Tests

```bash
cargo test   -p funput-cli
cargo clippy -p funput-cli --all-targets -- -D warnings
```

Unit tests: `sim` (basic Telex/VNI + multi-word `xins chaof`→`xín chào`, English-restore at boundaries
`card `→`card`, `mas `→`má `, and correct `--steps` recording), `render` (table with header + summary),
`encode` (round-trip `text→keys→engine→text` over a word list, both Telex & VNI). App text is
cross-checked against `funput-engine/tests/fixtures/step_cases.rs` to keep the CLI in step with the engine.

## Roadmap

- **`SimConfig` toggles not yet exposed**: `funput dev run` only has `-m method`; the
  `simulate_with(SimConfig, …)` seam is ready for `--tone-style`, `--smart-restore`, `--spell-check`.
- **Per-keystroke REPL** (raw mode, would need `crossterm`) — currently line-based only.
```
