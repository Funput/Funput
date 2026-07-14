#if os(iOS) && canImport(FunputCore)
import FunputCore

extension FunputComposer {
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
