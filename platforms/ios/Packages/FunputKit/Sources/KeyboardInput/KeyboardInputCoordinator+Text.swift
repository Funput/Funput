#if os(iOS) && canImport(FunputCore)
import FunputEngine
import KeyboardLayout

extension KeyboardInputCoordinator {
    func input(_ text: String, document: any KeyboardDocument) {
        guard state.usesVietnameseComposition else {
            document.insertText(text)
            return
        }

        for scalar in text.unicodeScalars {
            let result = composer.process(scalar)
            if result.action == .none {
                document.insertText(String(scalar))
            } else {
                apply(result, document: document)
            }
        }
    }

    func apply(
        _ result: FunputCompositionResult,
        document: any KeyboardDocument
    ) {
        for _ in 0..<result.deleteCount {
            document.deleteBackward()
        }
        if !result.text.isEmpty {
            document.insertText(result.text)
        }
    }

    func characterText(for key: KeySpec) -> String {
        guard state.shiftState.isUppercase else { return key.label }
        return key.shiftedLabel ?? key.label.uppercased()
    }
}
#endif
