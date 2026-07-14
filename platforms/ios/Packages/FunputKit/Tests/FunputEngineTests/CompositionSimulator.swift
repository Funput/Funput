#if os(iOS) && canImport(FunputCore)
import FunputEngine

@MainActor
enum CompositionSimulator {
    static func type(
        _ input: String,
        method: FunputInputMethod,
        configure: (FunputComposer) -> Void = { _ in }
    ) -> String {
        let composer = FunputComposer()
        composer.setInputMethod(method)
        configure(composer)

        var text = ""
        for scalar in input.unicodeScalars {
            let result = composer.process(scalar)
            if result.action == .none {
                text.append(contentsOf: String(scalar))
            } else {
                for _ in 0..<min(result.deleteCount, text.count) {
                    text.removeLast()
                }
                text.append(result.text)
            }
        }
        return text
    }
}
#endif
