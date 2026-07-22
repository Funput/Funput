# Known Issues

## Typing silently stops working in Chromium/Electron apps on macOS 26/27 beta

**Symptom:** after switching the input source to Funput (or back to it) while a
text field in a Chromium-based app already has focus, character keys stop
producing any text. Non-text shortcuts that don't need the input method
(⌘C, Delete, arrow keys, …) keep working normally. In the worst case the whole
app freezes for 15–30 seconds instead.

Affected apps: any Chromium/Electron app — Chrome, Cursor, VS Code, Slack,
Discord, Notion desktop, Figma desktop, etc. Not observed in native AppKit
apps (TextEdit, Notes, Mail, Safari, Pages).

### Root cause

This is **not a Funput bug**. Unified-log capture during a live repro shows
`FunputInputController` calling `setMarkedText`/`insertText` normally and on
schedule — the input method itself is healthy. The breakage is downstream, in
how the affected app's Chromium/Electron renderer talks back to macOS's IME
bridge.

Any third-party input method (Funput included, same as OpenKey, EVKey,
GoTiengViet, …) has no choice but to run as its own process and exchange every
keystroke with the focused app over `IMKServer`'s XPC/Mach IPC — there is no
other public API for a custom IME on macOS. A plain keyboard layout (e.g. bare
ABC) never goes through this path at all, since there's no composing step, so
it structurally can't hit this class of bug.

On macOS 26/27 beta, Apple changed internals of `TextInputUIMacHelper`, and
Chromium's `NSTextInputContext` bridge doesn't yet handle the new timing: the
input method's `activate` call (awaiting an XPC reply) can race with
Chromium's own concurrent `firstRectForCharacterRange` cursor-position query,
and the two block on each other — a classic IPC deadlock. Apple's own bundled
composing input methods use the same generic mechanism and have also been
reported to hit related issues, just less often (Apple ships fixes for its own
regression faster, and gets far more test coverage against its own IMEs).

Upstream reports of the same class of bug:

- [electron/electron#51557](https://github.com/electron/electron/issues/51557) — IME candidate window not displaying on macOS 26 (Tahoe) in Electron.
- [electron/electron#52260](https://github.com/electron/electron/issues/52260) — every keystroke during IME composition stalls the browser process on macOS 26.
- [electron/electron#47472](https://github.com/electron/electron/issues/47472) — dropped/duplicated characters with Chromium IME on macOS, reproduces even with Apple's own bundled "2-Set Korean" keyboard, confirmed upstream Chromium issue (not fixable from the Electron app side).
- [runjuu/InputSourcePro#92](https://github.com/runjuu/InputSourcePro/issues/92) — root-caused deadlock: `IMKInputSession activate` vs. Chromium's `firstRectForCharacterRange`, both blocked waiting on the other's XPC reply, in `TextInputUIMacHelper` on macOS 26 Tahoe beta.

### Workaround

No code change in Funput fixes this — the deadlock/race lives in Chromium's
text-input bridge. Until upstream ships a fix:

1. Switch the input source **before** clicking into the text field, not while
   it already has focus (this is the exact sequence that triggers the race).
2. If typing already stopped: switch away and back (⌘Tab), or blur/refocus the
   field. If that doesn't help, close and reopen the tab/document.
3. If the app is fully frozen, it usually recovers on its own within
   15–30 seconds; otherwise quit and reopen the affected app (not Funput —
   restarting Funput does not help since Funput's process isn't the one stuck).

Restarting Funput itself is not expected to help, since the input method's own
process is confirmed healthy during the failure (see unified log evidence
above); the stuck state lives in the other app's renderer process.
