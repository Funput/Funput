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
      nonpreedit.cpp          Commit-as-you-type mode, and re-opening a word
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
  funput_input.cpp        One keystroke: normalize, ask the composer
  funput_client.cpp       The other half: how a plan reaches the client
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

## Non-preedit mode

Off by default; the switch is in Settings under **Kiểu gõ**, or set
`"nonPreedit": true` in `~/.config/Funput/settings.json` directly. Either way the
settings watcher picks it up live, with no restart. Both shells perform it.

Instead of parking the composing word in a preedit, each keystroke commits into the
document and repairs what the previous one wrote — `Effect::Replace` carries "delete
N characters, then commit this". That is the instruction the Windows hook shell
already performs (`crates/funput-desktop/src/inject.rs`), and N comes straight from
the engine as `FunputResult::backspace`, so the platforms stay one behaviour rather
than three. Backspacing onto a finished word re-opens it, so `phủ` Space Backspace
`s` gives `phú`; Windows keeps a shadow copy of what it typed to manage that
(`retone.rs`), while here the document can simply be read.

Whether it engages is decided per input context, and the two shells cannot decide it
the same way. Fcitx5 settles it once at focus-in, from `surroundingText().isValid()`.
IBus has no such question to ask that early — surrounding text only starts arriving
after the client answers — so it re-asks between words, on any keystroke with nothing
composing. Never mid-word in either shell: the modes disagree about where the
composing word lives, and switching under one leaves the engine and the client
describing different things.

### What was measured, and what follows from it

On GNOME/Wayland, where Fcitx5 is reached through its **ibus** frontend and GNOME
Shell is the client for every application:

- **Capability flags lie in both directions.** Contexts report `SurroundingText`
  absent while it works perfectly, and some report `caps: []` — not even `Preedit` —
  while preedit plainly works. Neither shell gates on them; what the client actually
  does decides instead.
- **`program()` is always `gnome-shell`**, never the real app, so a per-app policy
  there would be actively wrong rather than merely unavailable.
- **`deleteSurroundingText` counts characters, not bytes** — verified with `ế`, one
  character and three bytes. `ComposePlan::deleteChars` is a character count end to
  end for that reason, and an ASCII-only test would not have caught the difference.
- **The client answers only 61% of commits**, and the channel dies and revives within
  one session, so nothing may wait on it.
- **A commit/delete/commit burst issued with no gap destroys the user's text**: `;;;`
  came back as `;;y` in all nine attempts.
- **But typing cannot produce that burst.** A `Replace` needs a keystroke that makes
  the engine rewrite its tail. Consecutive ones were never closer than 49ms, and
  holding a key down does not help — after the second press the engine stops
  rewriting and the repeats pass straight through, leaving 500ms between `Replace`es
  under auto-repeat, ten times slower than typing by hand.

The last three together are why **writes are deliberately not serialized**. Waiting
for the client to confirm one write before issuing the next would cost ~25ms per
keystroke and stall outright on the commits it never answers — a certain cost against
a failure real typing cannot reach. Reconsider only if something starts issuing
several `Replace`es for one keystroke.

Not waiting before a write is not the same as never checking after one, though, and
conflating the two let Chrome's address bar corrupt text for a while: it takes the
commit and drops the `deleteSurroundingText` beside it, so `phủ` became `phủú`. The
check costs nothing because the shells already read the document each keystroke. After
a `Replace{N, T}` issued against document `D`, the next reading can only be one of
three strings, and they differ:

| Reading | Meaning |
|---|---|
| `D` less N characters, then `T` | it worked |
| `D` then `T` | the client dropped the delete — turn the mode off for this client |
| `D` | the client has not answered yet — no verdict |

Only the middle one is a verdict, and that restraint is the point: treating "not what
I expected" as failure would disable the mode on the 61% of commits that go
unanswered, curing one broken client by breaking the feature for everyone. Anything
matching none of the three is no verdict either. `Composer::observeDocument()` holds
this; the shells only hand it the text.

The cost is that detection is one beat late, so the first word is still mangled before
the mode stands down. Catching it sooner would mean writing probe text into the user's
document, which is not on the table.

A selection live at delete time would take the highlighted text instead of ours, so
both shells abandon the repair *and* the composition when they see one (`applyPlan`
in `fcitx5/src/funput_client.cpp` and `ibus/src/engine/client.cpp`). Abandoning only
the delete would double the word; abandoning it silently would leave the engine owning
a word it could not correct, aiming the next keystroke's delete at text Funput never
wrote. Measuring never caught a live selection in 93 commits, so that guard is
reasoned rather than measured.

### On the IBus side

Three API facts, each of which cost several rounds to find, and none of which the
headers tell you:

- **Registering for surrounding text is per input context, not per engine.** IBus
  documents `ibus_engine_get_surrounding_text()` with null out-params as the way to
  register and says to call it "in the enable handler" — but enable fires once, and an
  engine that only asks there receives nothing for the rest of the session. It has to
  be asked again on every focus-in. Until that was found, non-preedit had never once
  engaged on IBus, while looking as though it had.
- **Reading it back does not work.** `ibus_engine_get_surrounding_text()` returns a
  null text immediately after the client has sent a perfectly good string. The only
  reliable copy is the one arriving in the `set_surrounding_text` vfunc, so the engine
  keeps it in `EngineState` — which also means no question of how stale a framework
  cache is.
- **`g_debug` reaches nobody.** ibus-daemon points a spawned engine's stdio at
  `/dev/null` no matter how the daemon itself was started, so diagnostics here have to
  be written to a file, the way the Fcitx5 probe once did.

The character count was confirmed a second time from this side, without inference: the
client reported cursor 4 for `phủ ` (five bytes) and 3 for `phủ` (four).

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
  the word anyway. Non-preedit above is the fix on both shells, so this now only bites
  where that mode cannot run: a client that reports no surrounding text, one that
  ignores `deleteSurroundingText`, and anyone who leaves the mode off.
- **Chrome's address bar cannot take non-preedit.** It accepts a commit and discards
  the delete next to it, on both shells. The check above catches it and falls back
  after one mangled word — a real cost, and the reason a client fixing this would be
  worth more than any further work here. Ordinary text fields inside the same Chrome
  are fine.
- **The Settings app rewrites `settings.json` wholesale.** Unlike the addon's merging
  writer, it serializes its own struct over the file, so a key it does not know is
  deleted the next time the user changes anything there. Every setting the addon owns
  needs a field in `settings-gtk`'s `Settings` too, and the two have to ship in the
  same release — a shipped addon paired with an older Settings app silently loses the
  new setting.
