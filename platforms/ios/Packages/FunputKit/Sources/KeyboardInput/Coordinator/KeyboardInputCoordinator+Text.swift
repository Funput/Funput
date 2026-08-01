#if os(iOS) && canImport(FunputCore)
import FunputEngine
import KeyboardLayout

extension KeyboardInputCoordinator {
    func input(
        _ text: String,
        builder: inout InputTransactionBuilder
    ) {
        guard state.usesVietnameseComposition else {
            builder.insert(text)
            return
        }

        for scalar in text.unicodeScalars {
            let signpostID = KeyboardInputSignposts.begin("ComposerFFI")
            let result = composer.process(scalar)
            KeyboardInputSignposts.end("ComposerFFI", signpostID)
            if result.action == .none {
                builder.insert(String(scalar))
            } else {
                apply(result, builder: &builder)
            }
        }
    }

    func apply(
        _ result: FunputCompositionResult,
        builder: inout InputTransactionBuilder
    ) {
        builder.deleteBackward(count: result.deleteCount)
        builder.insert(result.text)
    }

    func characterText(for key: KeySpec) -> String {
        guard state.shiftState.isUppercase else { return key.label }
        return key.shiftedLabel ?? key.label.uppercased()
    }
}
#endif
