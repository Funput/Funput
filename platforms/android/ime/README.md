# Funput IME

Owns the Android `InputMethodService` lifecycle and the bridge to the focused
editor. It hosts `FunputKeyboardView`, maps semantic key actions to testable edit
commands, and executes them against the current `InputConnection`.

`EditorInfoActionResolver` keeps Android editor metadata out of the renderer. It
selects the Enter-key presentation and routes standard or custom editor actions
through `performEditorAction`; multiline fields retain normal newline behavior.

`AndroidCompositionSession` renders the shared Rust engine buffer with
`setComposingText()`. JNI is intentionally narrow: one synchronous call per text
key, no network or storage, and safe registry IDs instead of native pointers.
