# Funput IME

Owns the Android `InputMethodService` lifecycle and the bridge to the focused
editor. It hosts `FunputKeyboardView`, maps semantic key actions to testable edit
commands, and executes them against the current `InputConnection`.

`EditorInfoActionResolver` keeps Android editor metadata out of the renderer. It
selects the Enter-key presentation and routes standard or custom editor actions
through `performEditorAction`; multiline fields retain normal newline behavior.

`EditorInfoKeyboardModeResolver` maps Android input-type flags to renderer modes.
Email editors receive compact ASCII QWERTY layouts and bypass Vietnamese
composition. URI editors are treated as browser omnibox/search fields: they use
web punctuation while retaining Vietnamese composition because the same field
accepts both addresses and natural-language search queries.

`EditorInfoPolicyResolver` also owns capitalization, multiline, suggestion
source, and personalized-learning policy. Auto-capitalization follows the
editor's live cursor caps mode; app-provided completions are transient and are
committed with `commitCompletion()` instead of being retained as Funput data.

Number editors receive a suggestion-free 4×4 keypad. Minus appears only for the
signed flag; period and comma appear only for the decimal flag.
Phone editors use a matching 4×4 dial pad with direct `+`, `*`, and `#` keys.
Text and numeric password variations use suggestion-free layouts, bypass
composition, and keep both symbol pages free of candidate and emoji UI.

`AndroidCompositionSession` renders the shared Rust engine buffer with
`setComposingText()`. JNI is intentionally narrow: one synchronous call per text
key, no network or storage, and safe registry IDs instead of native pointers.

The system-keyboard globe is intentionally always hidden until its interaction
is revisited in a later plan. The dormant switch callback remains isolated from
the typing path.
