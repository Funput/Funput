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
source, and personalized-learning policy. Funput suggestions are available in
every non-password editor, even when the host requests its own completions or
sets `NO_SUGGESTIONS`. Auto-capitalization follows the editor's live cursor caps
mode; `NO_PERSONALIZED_LEARNING` still prevents the focused editor from adding
tokens to Funput's local data.

Number editors receive a 4×4 keypad below the suggestion toolbar. Minus appears only for the
signed flag; period and comma appear only for the decimal flag.
Phone editors use a matching 4×4 dial pad with direct `+`, `*`, and `#` keys.
Text and numeric password variations use suggestion-free layouts, bypass
composition, and keep both symbol pages free of candidate and emoji UI.

`AndroidCompositionSession` renders the shared Rust engine buffer with
`setComposingText()`. JNI is intentionally narrow: one synchronous call per text
key, no network or storage, and safe registry IDs instead of native pointers.

A hardware keyboard hides the soft Funput view (Android's default
`onEvaluateInputViewShown`). The IME stays bound: `onKeyDown` / `onKeyUp` map
printable keys, Space, Enter, and Backspace onto the same `KeyAction` path as
taps, so Telex and VNI still compose. Navigation and Ctrl/Alt/Meta shortcuts
finish the current composition and pass through to the host. A few OEM builds
do not deliver hardware keys while the input view is hidden; enable the system
*Show on-screen keyboard* setting in that case.

The system-keyboard globe is intentionally always hidden until its interaction
is revisited in a later plan. The dormant switch callback remains isolated from
the typing path.
