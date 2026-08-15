# Funput on Linux

Two input-method shells — **Fcitx5** and **IBus** — over one shared composer, which
in turn drives the Rust engine (`crates/funput-engine`) through the `funput-ffi`
C ABI.

## Layout

```
common/                 Framework-free C++ shared by both shells
  compose/                The typing rules: one copy, no Fcitx5/IBus symbols
    plan.h                  ComposePlan — what the shell is asked to do
    key/
      event.h                 Mods / KeyEvent / keysym constants (no dependencies)
      classify.h/.cpp         KeyKind + what a key means
      boundary.h              Word-boundary test
    composer/
      composer.h              The state machine (engine + settings + VI/EN)
      keys.cpp                onKey(): the keystroke decision tree
      state.cpp               Settings, VI/EN, and the non-key exits
  settings/               ~/.config/Funput/settings.json
    settings.h              The model every shell reads
    lookup.cpp              Where the file lives; app exclusion
    io.cpp                  Parse and save (JSON)
    watch.h/.cpp            inotify watcher for live reload
  ffi/                    The funput-ffi C ABI
    handle.h                RAII wrapper around the engine handle
    utf8.h                  UTF-8 <-> UTF-32 marshalling
  tests/                  doctest, in a tree mirroring the above
fcitx5/src/             Fcitx5 addon -> libfunput.so
ibus/src/               IBus engine -> ibus-engine-funput
  engine.h                The public GObject type
  engine/                 internal.h, object.cpp, callbacks.cpp, client.cpp
settings-gtk/           GTK4 + libadwaita Settings app (its own cargo crate)
packaging/              .desktop file, apt/dnf repo metadata
```

Directories are kept to five files or fewer; when one grows past that, it splits by
concern rather than by size.

The split is deliberate: **the shells decide nothing.** They translate their
framework's key event into a `funput::KeyEvent`, hand it to `funput::Composer`, and
perform the returned `funput::ComposePlan`. Everything about *what* should happen
lives in `common/compose/`. Before it did, the same decision tree existed twice —
once per shell — and drifted.

What genuinely stays per-shell is how a preedit reaches the client and who commits
it when focus is lost (Fcitx5's `setClientPreedit` + its focus-out watcher, IBus's
`IBUS_ENGINE_PREEDIT_COMMIT`). Both are commented where they live.

The model mirrors the other desktop platforms: `crates/funput-desktop/src/key.rs`
is the same classifier for the Windows hook shell, and its `inject.rs` is the same
"the core returns a plan, the shell performs it" split.

## Build

Needs the build deps for whichever shell you are packaging:

```bash
sudo apt-get install cmake nlohmann-json3-dev fcitx5 libfcitx5core-dev libfcitx5utils-dev libfcitx5config-dev ibus libibus-1.0-dev libglib2.0-dev libgtk-4-dev libadwaita-1-dev librsvg2-dev
```

Then, from the repo's `app/` directory:

```bash
FUNPUT_FRAMEWORK=all platforms/linux/build.sh
```

`FUNPUT_FRAMEWORK` takes `fcitx5`, `ibus` or `all`; `FUNPUT_PKG` takes `deb` or
`rpm` (default: whichever the host supports). Each shell is its own top-level CMake
project, so they package independently.

## Tests

`common/` is buildable and testable on its own, with **neither Fcitx5 nor IBus
installed** — that is what `compose/` linking no framework symbol buys. Needs only
`cmake`, `nlohmann-json3-dev` and cargo:

```bash
cmake -S platforms/linux -B platforms/linux/build/tests -DFUNPUT_BUILD_TESTS=ON
```

```bash
cmake --build platforms/linux/build/tests --parallel && ctest --test-dir platforms/linux/build/tests --output-on-failure
```

The tests link the real `libfunput_ffi.so`, so they exercise the Rust engine across
the C ABI rather than a mock. `platforms/linux/CMakeLists.txt` exists only for this
— packaging never goes through it, and `FUNPUT_BUILD_TESTS` is `OFF` by default so
distro packagers never trigger the framework fetch.

CI runs this on every pull request (`.github/workflows/ci.yml`, job `linux-common`).
Note that the two **shells** are only compiled by the release workflow, so a change
under `fcitx5/` or `ibus/` still wants a local build before merging.

Every file here is held to 150 lines by `scripts/check-loc.sh`, test files included.

## Surrounding-text probe (temporary)

Phase 0 of the non-preedit work: measure whether clients report surrounding text
correctly and in time, before designing against a guess. **Off unless
`FUNPUT_PROBE=1`** — a normal install pays nothing and behaves identically.

Install the addon, then restart Fcitx5 with the probe on:

```bash
pkill fcitx5; FUNPUT_PROBE=1 fcitx5 -d
```

Type Vietnamese normally for a while, across the apps you care about — browser
address bar and search box, a terminal, an editor, a chat app, a password field,
GTK and Qt both. Then:

```bash
platforms/linux/fcitx5/src/probe/analyze.sh
```

It reports, per app: which capabilities the client claims, whether surrounding text
matched what we committed, how late it arrived, and whether a selection was live at
commit time (the browser-autofill hazard). Log defaults to
`~/.config/Funput/probe.jsonl`; override with `FUNPUT_PROBE_LOG`. Needs `jq`.

`src/probe/` is throwaway — delete it once the questions in `probe.h` are answered.

## Known gaps

- **Per-app auto-EN is Fcitx5-only.** `Composer::applyPerAppDefault()` exists for
  both, but only the Fcitx5 shell has an app identity to pass it
  (`InputContext::program()`); IBus hands its engine none, so `focusIn()` does not
  call it. `Settings::excludedAppIds` is therefore dead weight in the IBus build.
  Fixing it means sourcing the identity elsewhere (`_NET_ACTIVE_WINDOW` +
  `WM_CLASS` on X11), and note that Wayland's `zwp_text_input_v3` carries no app id
  at all — so verify what `program()` actually returns before relying on it there.
- **Preedit loses a half-typed word in some clients on focus change.** Both shells
  hand the flush to their framework (see the comments in `deactivate()` and
  `updatePreedit()`); clients that drop the preedit instead of committing it lose
  the word anyway. A non-preedit mode — commit as you type, repair with
  `deleteSurroundingText` — is the intended fix, and `ComposePlan`'s `Effect` enum
  is where it will slot in.
