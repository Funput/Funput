# Funput IME

Owns the Android `InputMethodService` lifecycle and the bridge to the focused
editor. It hosts `FunputKeyboardView` in the system IME window; `InputConnection`
editing is added as a separate, testable step.
