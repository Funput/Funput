# funput-term

**English** · [Tiếng Việt](README.md)

Type **Vietnamese** inside terminal apps (Claude Code, Cursor, shells, REPLs…) — where the system IME
usually breaks because the terminal runs in raw mode.

`funput-term` is a **transparent PTY wrapper**: it runs your app inside a pseudo-terminal, composes
Vietnamese from the keystroke stream, and injects the finished text into the app. **No** Accessibility
permission, **no** daemon, **no** system hooks — it works in **every** terminal emulator (iTerm2,
Terminal.app, Alacritty, kitty, WezTerm, tmux, SSH…).

## Run

```bash
cargo run -p funput-term -- claude         # type Vietnamese in the Claude CLI
cargo run -p funput-term -m telex -- bash  # pick Telex (VNI is the default)
funput-term                                # no args → wrap $SHELL
funput-term -- cursor
```

- VNI `xin1 chao2` or Telex `xins chaof` → **xín chào**.
- **`Ctrl-\`**: toggle Vietnamese on/off. **`Ctrl-^`**: cycle **Telex↔VNI** live within the session
  (remap/disable via `FUNPUT_CYCLE_METHOD`). State shows in the **window title** as
  `Funput · VI · Telex` (OSC, auto-wrapped as passthrough for **tmux/screen**) and in the **cursor
  color** (green for VI, reset for EN) — still visible even when the title is hidden.
- Non-Vietnamese words auto-restore at the word boundary (`card ` → `card`).

**"Always on":** run `funput-term install` to print (or `--write` to write) an alias block into your
shell rc, or configure your terminal emulator to run `funput-term -- $SHELL` → every app in that
window can type Vietnamese.

```bash
funput-term install --alias claude --alias cursor        # print aliases for $SHELL
funput-term install --shell zsh --alias claude --write   # write to ~/.zshrc (idempotent)
```

## Configuration

funput-term reads Funput's shared config file: `dirs::config_dir()/Funput/settings.json` (on
Linux/Windows it shares the system IME's preferences; macOS uses its own file at the standard path).
It applies: `method`, `toneStyle`, `enabled`, `smartRestore`, `eagerRestore`, `spellCheck`,
`autoCapitalize`, `shortcuts` (text expansion). A file missing keys still loads (every key has a
default).

**Precedence:** CLI flag > environment variable > `settings.json` > built-in default.

- CLI: `-m, --method telex|vni`; the program comes after `--` (defaults to `$SHELL`).
- Env: `FUNPUT_METHOD`, `FUNPUT_TONE_STYLE`, `FUNPUT_ENABLED`, `FUNPUT_TOGGLE` (e.g. `ctrl-\`,
  `ctrl-space`), `FUNPUT_CYCLE_METHOD` (the Telex↔VNI cycle key; `off`/`none` to disable),
  `FUNPUT_CURSOR_COLOR_VI`, `FUNPUT_CONFIG` (a different file path).

## Behavior & scope

| | |
|--|--|
| Terminal emulator | **All** (a TTY is enough) |
| Operating system | macOS, Linux (Unix PTY), Windows (ConPTY) |
| Line-based apps (shell, Claude, Cursor, REPL) | Full composition |
| Full-screen apps (vim, less, htop) | **Auto-disables** composition (alt-screen detection) so the UI isn't corrupted |
| Backspace mid-composition | `engine.on_backspace()` — corrects then keeps composing (`Phua` ⌫ `s` → `Phú`) |
| Enter / Tab while composing | Routed through the engine first → English-restore runs in time (`text`+Enter sends `text`, not `tẽt`) |
| Bracketed paste (`ESC[200~ … ESC[201~`) | Pasted content forwarded **raw** (not composed char by char); markers pass through to the child |
| Escape / arrows / shortcuts / UTF-8 | Forwarded **raw**, composition flushed |

## How it works

A transparent interposer: it only **intercepts printable ASCII letters** to compose; everything else
is forwarded raw (escape/mouse/paste untouched).

```
stdin  ─raw bytes─► classify::Classifier ─► engine ─► inject::result_bytes ─► PTY ─► child
stdout ◄─────────── output: scan alt-screen ◄──────────────────────────── PTY ◄─ child
        runtime::run: spawn child in the PTY, await exit; SIGWINCH→resize thread; RawModeGuard (RAII)
```

Two threads: stdin→pty (`forward_input`) and pty→stdout (`forward_output`). The engine lives entirely
inside the input thread, so **no locking is needed**. The engine returns "delete N chars + insert
string"; the wrapper translates that into bytes (`DEL 0x7f × N` + UTF-8) pushed into the child's
stdin — this is just one "terminal frontend" for the **same** Funput engine.

### Modules

This is a **library crate** (`lib.rs`); the clap CLI (`funput term …`) lives in `funput-cli`.

```
src/
├── lib.rs                # module declarations: runtime · config · install · terminal
├── runtime/              # interposer: spawn the PTY, shuttle bytes, compose Vietnamese
│   ├── mod.rs            #   run() orchestration (spawn PTY, threads, indicators); rejects an empty command
│   ├── driver/           #   input branch: read → classify → compose → inject
│   │   ├── mod.rs        #     forward_input (PURE seam, tested) + Status + other_method
│   │   ├── classify.rs   #     PURE: Classifier byte → ByteKind (Printable/Control/Escape/Utf8/Toggle/CycleMethod/Paste)
│   │   └── inject.rs     #     PURE: result_bytes(char, &ImeResult) → bytes (None→key; Send/Restore→DEL×bs + UTF-8)
│   ├── output.rs         #   forward_output + AltScreenScanner (ESC[?1049h/l, tolerates split chunks)
│   ├── resize.rs         #   spawn_resize_thread (Unix SIGWINCH / non-Unix poll)
│   └── state.rs          #   SharedState: enabled (toggle) + alt_screen (atomics)
├── terminal/             # terminal primitives (crate-internal)
│   ├── mod.rs            #   RawModeGuard (RAII)
│   ├── console.rs        #   Windows: VT input/output + UTF-8 codepage (#[cfg(windows)])
│   └── indicator.rs      #   Mux passthrough, set_title (OSC) + set_cursor_cue (OSC 12/112)
├── config/               # PURE: settings.json → TermConfig; precedence CLI>env>file
│   ├── mod.rs            #   TermConfig, from_json, apply_to(engine), load
│   ├── schema.rs         #   FileSettings serde (camelCase) + defaults
│   └── parse.rs          #   env overlay (apply_env) + key/enum parsing
└── install/              # PURE: wire an alias block into the shell rc
    ├── mod.rs            #   Shell (bash/zsh/fish) + write rc file (idempotent)
    └── snippet.rs        #   snippet(shell, aliases) + parse_alias
```

`classify` / `inject` / `forward_input` / `config` / `install` are **pure, no real I/O** → unit-tested
with in-memory pipes or string inputs.

### Processing rules (in `forward_input`)

- Init: `config.apply_to(engine)` (method, tone, smart/eager restore, spell-check, auto-cap, text expansion).
- `Toggle` (default `0x1c`) → `state.toggle()` + `engine.clear()` + refresh title & cursor color.
- `CycleMethod` (default `Ctrl-^` `0x1e`, remap/disable via config) → `engine.set_method` cycles Telex↔VNI + `engine.clear()` + refresh indicator (`Status{enabled, method}`).
- `Printable` while composing → `engine.process_char` → `result_bytes`.
- Backspace (`0x7f`/`0x08`) while composing → `engine.on_backspace()` + forward the byte (the app deletes its own char).
- Tab/LF/CR (word boundary) while composing → `engine.process_char(boundary)`: `None` → forward the byte;
  otherwise → `result_bytes` (English-restore runs before the key reaches the child).
- Everything else (escape / utf8 / other control / printable while disabled) → `engine.clear()` + forward raw.
- `classify.rs` tracks the ESC → CSI/SS3 state machine so arrows/Alt-combos aren't mistaken for letters.

### Robustness

- `RawModeGuard` enters raw mode **before** spawning → no TTY means a fast fail without orphaning a
  child; drop always restores it (even on panic). On **Windows** it also enables the console's VT
  input/output + UTF-8 codepage (so escape/arrow keys reach the child and the ConPTY's VT renders
  correctly), all restored on drop.
- `portable_pty::openpty` + `CommandBuilder` inherit cwd + env. Child exits → reader EOF → output
  thread stops → exit with the correct status code.
- Resize: Unix `SIGWINCH` (`signal-hook`) → `crossterm::size` → `master.resize`; Windows (no
  SIGWINCH) polls `crossterm::size` every ~120ms and calls `master.resize` on change.
- Alt-screen: `output.rs` sees `ESC[?1049h` → sets `state.alt_screen` → input passthrough (vim/less
  aren't composed).
- Bracketed paste: `classify.rs` sees `ESC[200~` → `in_paste` → pasted content classified as `Paste`
  (forwarded raw) until `ESC[201~`. Tolerates a marker split across chunks; the CSI parameter buffer is bounded.
- Title through a mux: `terminal/indicator.rs::detect_mux` reads `$TMUX`/`$STY`/`$TERM`;
  `title_sequence` wraps DCS passthrough for tmux (doubling ESC) and screen.

## Relationship to the rest

```
funput-core → funput-engine → funput-term   (Rust, linked DIRECTLY; NOT through funput-ffi)
```

`funput-term` solves the **terminal** problem specifically — the blind spot of system IMEs (macOS
IMKit, Windows TSF, Linux Fcitx5). It is useful **even if** a system IME is installed. It is a
standalone binary, not a library for other crates to link.

## Dependencies

`funput-core` · `funput-engine` · `portable-pty` (PTY/ConPTY) · `crossterm` (raw-mode/size) · `clap`
(CLI) · `serde`/`serde_json` (read settings.json) · `dirs` (config path) · `signal-hook` (resize,
unix only). **No** `funput-ffi`.

## Tests

```bash
cargo test   -p funput-term
cargo clippy -p funput-term --all-targets -- -D warnings
cargo run    -p funput-term -- cat       # type "as" → "á"
```

Pure unit tests (in-memory pipes): classifier (printable/control/escape/arrows/Alt/utf8/toggle/cycle-method),
`inject` (None/Send/Restore), `forward_input` (compose `as`→`a`+DEL+`á`, `Phua`⌫`s`→`Phú`, cycle
Telex→VNI mid-line `as`+Ctrl-^+`as`→`áas`, `text`+Enter restore, `mas`+Enter keeps `má`, revert
`mixx`→`mix`, toggle off, bracketed paste kept verbatim + composing again after the paste, config
`enabled=false` doesn't compose, text expansion `vn`→`Việt Nam`), bracketed paste (split marker,
toggle/letters inside a paste stay raw, an over-long CSI isn't a marker), alt-screen scanner
(including split chunks), `config` (parse camelCase, defaults on missing key / malformed JSON, env
override + precedence, toggle-key parsing), `install` (snippet bash/zsh/fish, idempotent, shell
detection), `title_sequence` & `cursor_cue` (none/tmux/screen), state.

## Roadmap (post-v1)

- **Change tone-mark style at runtime:** Telex↔VNI cycling exists (`Ctrl-^`); still missing a key to
  toggle the tone-placement style (traditional/modern) within a session.
- **Per-app profiles:** use `excludedApps` from settings.json to auto enable/disable based on the
  wrapped command.
