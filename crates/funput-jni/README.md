# funput-jni

Minimal JNI boundary between Funput's Android IME and `funput-engine`.

- Kotlin owns `InputMethodService`, `InputConnection`, and composing spans.
- Rust owns all Telex/VNI rules and per-word engine state.
- Handles are registry IDs, not raw pointers, so stale or repeated destroy calls
  are safe no-ops.
- Each text key crosses JNI once and returns only the short composition buffer.

The Android Gradle build produces `arm64-v8a` and `x86_64` shared libraries.
