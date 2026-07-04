# Funput IME

Owns the Android `InputMethodService` lifecycle and the bridge to the focused
editor. The initial milestone only registers Funput with the system; keyboard UI
hosting and `InputConnection` editing are added in separate, testable steps.
