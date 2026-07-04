# Funput IME

Owns the Android `InputMethodService` lifecycle and the bridge to the focused
editor. It hosts `FunputKeyboardView`, maps semantic key actions to testable edit
commands, and executes them against the current `InputConnection`.

`EditorInfoActionResolver` keeps Android editor metadata out of the renderer. It
selects the Enter-key presentation and routes standard or custom editor actions
through `performEditorAction`; multiline fields retain normal newline behavior.

`EditorInfoKeyboardModeResolver` maps Android input-type flags to renderer modes.
Email and URL editors receive compact ASCII QWERTY layouts with contextual
punctuation and bypass Vietnamese composition.

Number editors receive a suggestion-free 4×4 keypad with fixed period and comma
keys. Signed and decimal flags remain represented in the resolved editor mode.
Phone editors use a matching 4×4 dial pad with direct `+`, `*`, and `#` keys.

`AndroidCompositionSession` renders the shared Rust engine buffer with
`setComposingText()`. JNI is intentionally narrow: one synchronous call per text
key, no network or storage, and safe registry IDs instead of native pointers.
