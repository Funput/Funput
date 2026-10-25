#if os(iOS) && canImport(FunputCore)
import FunputCore

/// Copies the UTF-8 the engine lends for the length of one call, before Rust takes it back.
private func receiveUTF8(
    _ context: UnsafeMutableRawPointer?, _ bytes: UnsafePointer<UInt8>?, _ count: UInt
) {
    guard let context, let bytes else { return }
    context.assumingMemoryBound(to: String.self).pointee = String(
        decoding: UnsafeBufferPointer(start: bytes, count: Int(count)), as: UTF8.self
    )
}

extension FunputComposer {
    public func setShortcutsEnabled(_ enabled: Bool) {
        funput_set_shortcuts_enabled(handle, enabled)
    }

    public func setShortcutSmartCase(_ enabled: Bool) {
        funput_set_shortcut_smart_case(handle, enabled)
    }

    public func setShortcutsInEnglish(_ enabled: Bool) {
        funput_set_shortcuts_in_english(handle, enabled)
    }

    public func addShortcut(trigger: String, expansion: String) {
        let triggerScalars = trigger.unicodeScalars.map(\.value)
        let expansionScalars = expansion.unicodeScalars.map(\.value)

        triggerScalars.withUnsafeBufferPointer { triggerBuffer in
            expansionScalars.withUnsafeBufferPointer { expansionBuffer in
                funput_add_shortcut(
                    handle,
                    triggerBuffer.baseAddress,
                    UInt(triggerBuffer.count),
                    expansionBuffer.baseAddress,
                    UInt(expansionBuffer.count)
                )
            }
        }
    }

    /// The word the last gõ tắt expansion replaced, while it can still be taken back;
    /// `nil` once the next key has spent the undo. Asking does not spend it.
    public var shortcutUndoText: String? {
        var text = ""
        let armed = withUnsafeMutablePointer(to: &text) { pointer in
            funput_engine_shortcut_undo_text(handle, receiveUTF8, pointer)
        }
        return armed ? text : nil
    }

    /// Take the last expansion back: delete it and write the word it replaced, boundary
    /// character included. `deleteCount` counts Unicode scalars, like every other result.
    ///
    /// Spends the undo, so the caller checks the document still ends with the expansion
    /// first. The refused trigger is then kept as typed once.
    @discardableResult
    public func undoShortcut() -> FunputCompositionResult {
        var output = ""
        let result = withUnsafeMutablePointer(to: &output) { pointer in
            funput_engine_undo_shortcut(handle, receiveUTF8, pointer)
        }
        return FunputCompositionResult(
            action: FunputCompositionAction(rawValue: result.action) ?? .none,
            deleteCount: Int(result.backspace),
            text: output
        )
    }

    /// Forget the undo and the refusal it left, because the user went on editing.
    public func dismissShortcutUndo() {
        funput_engine_dismiss_shortcut_undo(handle)
    }
}
#endif
