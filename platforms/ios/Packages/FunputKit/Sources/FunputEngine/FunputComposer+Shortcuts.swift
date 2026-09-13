#if os(iOS) && canImport(FunputCore)
import FunputCore

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
}
#endif
