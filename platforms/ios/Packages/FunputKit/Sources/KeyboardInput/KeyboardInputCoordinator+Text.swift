#if os(iOS) && canImport(FunputCore)
import FunputEngine
import KeyboardLayout

extension KeyboardInputCoordinator {
    func input(_ text: String, document: any KeyboardDocument) {
        guard state.usesVietnameseComposition else {
            insertDocumentText(text, document: document)
            return
        }

        for scalar in text.unicodeScalars {
            let signpostID = KeyboardInputSignposts.begin("ComposerFFI")
            let result = composer.process(scalar)
            KeyboardInputSignposts.end("ComposerFFI", signpostID)
            if result.action == .none {
                insertDocumentText(String(scalar), document: document)
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
            deleteDocumentBackward(document)
        }
        if !result.text.isEmpty {
            insertDocumentText(result.text, document: document)
        }
    }

    func characterText(for key: KeySpec) -> String {
        guard state.shiftState.isUppercase else { return key.label }
        return key.shiftedLabel ?? key.label.uppercased()
    }
}
#endif
