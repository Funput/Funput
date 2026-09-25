#if os(iOS) && canImport(FunputCore)
import FunputCore

/// Main-actor Swift ownership boundary around one Rust composition engine.
@MainActor
public final class FunputComposer {
    // Swift 6 deinitializers are nonisolated; ownership remains MainActor-bound.
    nonisolated(unsafe) let handle: OpaquePointer
    nonisolated private let releaseHandle: @Sendable (OpaquePointer) -> Void

    public convenience init() {
        guard let handle = funput_engine_new() else {
            preconditionFailure("funput_engine_new returned null")
        }
        self.init(handle: handle) { funput_engine_free($0) }
    }

    init(
        handle: OpaquePointer,
        releaseHandle: @escaping @Sendable (OpaquePointer) -> Void
    ) {
        self.handle = handle
        self.releaseHandle = releaseHandle
    }

    deinit {
        releaseHandle(handle)
    }

    /// Applies every durable option in one FFI call. Same side effect the per-option
    /// setters had: a method change clears the composition.
    ///
    /// `auto_capitalize` is hard-wired off and is not an option a caller can set. The
    /// engine's sentence tracker would uppercase the first letter of a sentence on its
    /// own, which on iOS could only ever contradict a Shift key the user can see — see
    /// the note below and KeyboardCapitalizationOwnershipTests.
    public func configure(_ options: FunputCompositionOptions) {
        funput_configure(
            handle,
            FunputConfig(
                method: options.inputMethod.rawValue,
                tone_style: options.toneStyle.rawValue,
                smart_restore: options.smartRestore,
                eager_restore: options.eagerRestore,
                spell_check: options.spellCheck,
                auto_capitalize: false
            )
        )
    }

    /// Switches the input method on its own, for the keyboard's Telex/VNI key — the
    /// rest of the configuration is untouched.
    public func setInputMethod(_ method: FunputInputMethod) {
        funput_set_method(handle, method.rawValue)
    }

    /// Re-opens an already-committed `word` as the live composition, so the next
    /// keystroke edits it (Backspace back onto `chào`, then `s` gives `cháo`).
    ///
    /// Returns false when the word is not a Vietnamese syllable — the caller must then
    /// leave the document alone.
    @discardableResult
    public func adopt(_ word: String) -> Bool {
        let scalars = word.unicodeScalars.map(\.value)
        return scalars.withUnsafeBufferPointer { buffer in
            funput_adopt(handle, buffer.baseAddress, UInt(buffer.count))
        }
    }

    public func setEnabled(_ enabled: Bool) {
        funput_set_enabled(handle, enabled)
    }

    // No `armCapitalization()`, and no way to switch the tracker behind it on. The
    // engine's sentence tracker exists for the shells that only see keystrokes; the
    // iOS keyboard draws its own keys, so case follows the Shift state, which
    // `KeyboardCapitalizationResolver` drives from the document context through the
    // same core rules the tracker uses. See KeyboardCapitalizationOwnershipTests.

    public func clear() {
        funput_clear(handle)
    }

    public func clearShortcuts() {
        funput_clear_shortcuts(handle)
    }

    @discardableResult
    public func backspace() -> FunputCompositionResult {
        FunputResultDecoder.decode(funput_backspace(handle))
    }

    @discardableResult
    public func flipComposing() -> FunputCompositionResult {
        FunputResultDecoder.decode(funput_flip_composing(handle))
    }
}
#endif
