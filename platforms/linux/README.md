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
      nonpreedit.h            Commit-as-you-type: the mode, and judging a client
      nonpreedit.cpp          What the composer does with that judgement
  settings/               ~/.config/Funput/settings.json
    settings.h              The model every shell reads, gõ tắt switches included
    lookup.cpp              Where the file lives; app exclusion
    io.cpp                  Parse and save (JSON)
    watch.h/.cpp            inotify watcher for live reload
  ffi/                    The funput-ffi C ABI
    handle.h                RAII wrapper around the engine handle
    utf8.h                  UTF-8 <-> UTF-32 marshalling
  tests/                  doctest, in a tree mirroring the above
fcitx5/src/             Fcitx5 addon -> libfunput.so
  funput_input.cpp        One keystroke: normalize, ask the composer
  funput_client.cpp       Talking to the client, both directions
ibus/src/               IBus engine -> ibus-engine-funput
  engine.h                The public GObject type
  engine/                 internal.h, object.cpp, callbacks.cpp, client.cpp
settings-gtk/           GTK4 + libadwaita Settings app (its own cargo crate,
                        and its own package — see Build)
  src/settings_window/    one submodule per preferences page
    shortcuts/              the gõ tắt switches, the rows, the empty state
  src/convert/            the Chuyển mã window — see below
packaging/              two .desktop files, apt/dnf repo metadata
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

Three packages come out, not two. `funput` (Fcitx5) and `funput-ibus` each hold their
own addon, and **`funput-settings`** holds the GUI, its launcher and its icons. Those
four files used to be shipped by both shells, which meant dpkg refused to unpack the
second one — identical contents are still a file conflict, so installing both was
simply impossible on Debian/Ubuntu. `funput-settings` is built whichever framework you
asked for, since both depend on it.

That dependency is pinned to the exact version (`funput-settings (= <version>)`), and
that is the part worth keeping: the Settings app rewrites `settings.json` from its own
struct, so a copy older than the addon deletes settings the addon has since learned.
The package manager now refuses the mismatch that used to cause it.

## Distribution

Three channels, all fed by the same release:

- **`repo.funput.app`** — signed apt, dnf/zypper and pacman repositories on GitHub
  Pages, built by `.github/workflows/publish-repo.yml`. The only channel with
  automatic upgrades, and what `install.sh` configures by default. See
  `packaging/repo/README.md`.
- **GitHub Releases** — the `.deb`/`.rpm` themselves, plus the portable `.tar.gz`
  trees behind `install.sh --user`.
- **`install.sh`** — detect-then-configure front end over the two above.
Arch is inside the first of those rather than a channel of its own. It has no release
asset — a rolling source distro has no use for a `.deb` or `.rpm` — so the `pacman` job
in `publish-repo.yml` *builds* it, from `packaging/arch/PKGBUILD.in`, and serves the
result under `arch/x86_64/`. The AUR would be the conventional home for that recipe and
it is written to AUR conventions, but nothing uploads it: Arch has new-account
registration paused. `arch-recipe.yml` runs a real `makepkg` on pull requests that
touch the recipe, so a break is caught before it fails a publish.

What binaries cost on a rolling distro is that they link the libraries of the build day,
so a soname bump in fcitx5, ibus or gtk4 breaks them. A dependency cannot express that
constraint: Arch's `fcitx5` and `ibus` packages declare no soname `provides` at all
(only gtk4, libadwaita and glib2 do), so pacman has nothing to check and cannot warn.
The repair is therefore a schedule, not a dependency — `publish-repo.yml` also runs on
the 1st and 15th, rebuilding the current release against current Arch, with `pkgrel`
rendered as the build date so pacman sees an upgrade even though `pkgver` has not
moved. That is
what `@PKGREL@` in the template is for; the AUR renders it as `1`.

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

## Chuyển mã

The charset converter (`settings-gtk/src/convert/`) is **shared by both shells** in the
strongest sense: it never asks which one is running, never reads `settings.json`, and
never touches the engine. Fcitx5 and IBus both depend on the `funput-settings` package
at the exact version, so both get it without a line of C++ changing.

Everything about *what* a conversion is and costs lives in `crates/funput-convert`,
shared with the Slint window on Windows — the loss warning, the batch rules, the
`Đã chuyển mã` folder name and its collision-avoiding `vanban (2).txt`. Writing those
twice is how the two platforms would start disagreeing about the same document. What
stays here is drag-and-drop, the clipboard, the dialogs, and getting file I/O off the
UI thread.

Two ways in, because **there is no tray** for the item Windows puts there:

- Settings → **Chung** → "Công cụ chuyển mã", mirroring Windows' Settings → Dữ liệu.
- `funput-convert.desktop`, which runs `funput-settings --convert`.

Both reach the same process. That needs `ApplicationFlags::HANDLES_COMMAND_LINE` —
GApplication's default flags do not forward argv to the running instance, so `--convert`
would vanish without a trace — and it is why `route()` in `main.rs` looks for a window
*of the right type* rather than calling `active_window()`, which would raise the
converter when someone asked for Settings.

`src/convert/state.rs` is the only file under `convert/` that decides anything, and the
only one with tests. The `ui/*` files read the state and set properties; they decide
nothing. One `refreshing` latch guards every signal a refresh fires — without it, a
refresh setting a dropdown re-enters through `notify::selected` and panics on a nested
`borrow_mut`.

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

A Backspace with nothing composing is performed by *us*, not passed to the app, so the
deletion travels the same channel as every other repair. Letting the app do it is what
broke re-toning in Chrome's address bar: the app edited behind our back and then
discarded the repair issued on the next keystroke. It is only taken over on positive
evidence that the document being read is current — a change we predicted and then saw
land, whether that was a repair or a plain character the app typed itself. Without that
evidence the key goes through, which is what keeps a mouse selection deletable: the
selection flag comes from the same cache and had not caught up, so on the strength of
that alone the key was swallowed and Backspace appeared dead until pressed twice.

That repair cannot be checked afterwards. It carries no text, so "applied" and
"dropped" read as the same document — the silent-client signature below — and a client
that refuses it will simply appear to ignore Backspace. A Backspace *inside* a word is
still left to the app; the same hazard applies in principle, but nothing has been seen
to fail there.

Whether it engages is decided per input context. Both shells wait until the client
has actually sent surrounding text this focus — Fcitx5 via `SurroundingTextUpdated`,
IBus via `set_surrounding_text` — then re-ask between words, on any keystroke with
nothing composing. Neither snapshots capability flags or a cache at focus-in: the
text often arrives only after the client answers (Calc, an empty GTK field), and a
stale `isValid()` from the previous focus would turn the mode on in a terminal that
never sends updates. Never mid-word: the modes disagree about where the composing
word lives, and switching under one leaves the engine and the client describing
different things.

### What was measured, and what follows from it

On GNOME/Wayland, where Fcitx5 is reached through its **ibus** frontend and GNOME
Shell is the client for every application:

- **Capability flags lie in both directions.** Contexts report `SurroundingText`
  absent while it works perfectly, and some report `caps: []` — not even `Preedit` —
  while preedit plainly works. Neither shell gates on them; what the client actually
  does decides instead.
- **`program()` is always `gnome-shell`**, never the real app. Funput therefore
  has no per-app VI/EN list on Linux — a policy keyed on that value would apply to
  every client at once.
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
conflating the two let Chrome's address bar corrupt text for a while: it took the
commit and dropped the `deleteSurroundingText` beside it, so `phủ` came out `phủú`.
That particular case is gone — the Backspace is no longer the app's to handle — but the
check earned its place and stays, because the next client of this kind will not
announce itself. It costs nothing: the shells already read the document each keystroke. After
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
matching none of the three is no verdict either. `NonPreeditState` in
`common/compose/composer/nonpreedit.h` holds this; the shells only hand it the text.

A verdict stands down as narrowly as the failure allows. A dropped repair that followed
a **re-opened word** costs only re-toning: that was the one shape ever observed to fail,
in an address bar that took ordinary repairs perfectly well. Any other dropped repair
costs the whole mode, since nothing it writes can then be trusted.

The verdict is a latch, not a variable. A shell may re-assert the mode — IBus does, on
every keystroke — and must not be able to revive one a client has already been caught
breaking; only `Composer::onFocusChanged()` clears it, because a new client is a new
question.

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

- **Preedit loses a half-typed word in some clients on focus change.** Both shells
  hand the flush to their framework (see the comments in `deactivate()` and
  `updatePreedit()`); clients that drop the preedit instead of committing it lose
  the word anyway. Non-preedit above is the fix on both shells, so this now only bites
  where that mode cannot run: a client that reports no surrounding text, one that
  ignores `deleteSurroundingText`, and anyone who leaves the mode off.
- **Chrome shows no preedit at all.** Commits reach it fine — non-preedit works
  throughout, address bar included — but nothing that needs the client to draw and
  manage a composition does. Chrome is a native Wayland client here, so that path runs
  through `zwp_text_input_v3`; the same session shows preedit working normally in other
  apps, so this is Chrome's end and nothing in Funput can reach it. **Non-preedit is
  the answer for Chrome**, which is the shape the mode was built for.
- **Re-toning after Backspace is non-preedit only.** In preedit mode Backspace just
  shortens the composition, so `phủ` ␣ ⌫ `s` types a literal `s` — while macOS, Windows
  and Android all re-open the word. macOS manages it with one atomic
  `setMarkedText(replacementRange:)`, replacing the word and the deleted character in a
  single edit; neither Fcitx5 nor IBus has that, so a port would be delete-then-preedit,
  with the gap between them that this file spends so long on. Worth doing, and worth
  copying macOS's two guards when it is done: a setting of its own, and skipping key
  auto-repeat so holding Backspace does not re-open a word per repeat.
- **The Settings app rewrites `settings.json` wholesale.** Unlike the addon's merging
  writer, it serializes its own struct over the file, so a key it does not know is
  deleted the next time the user changes anything there. Every setting the addon owns
  still needs a field in `settings-gtk`'s `Settings` too. The *release* half of this is
  no longer a matter of remembering — the shells depend on `funput-settings` at their
  exact version — but a field left out of the struct is still silent data loss, and
  nothing checks for that.
- **No AppStream metainfo.** `funput-settings` ships a `.desktop` entry but no
  `metainfo.xml`, so it has no entry in GNOME Software or KDE Discover and distro
  reviewers will ask for one before packaging it downstream.
