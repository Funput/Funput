#if os(iOS) && canImport(FunputCore)
import FunputEngine
import KeyboardLayout

@MainActor
public final class KeyboardInputCoordinator {
    public private(set) var state: KeyboardInputState

    private let composer: FunputComposer

    public init(inputMethod: KeyboardInputMethod = .vni) {
        state = KeyboardInputState(inputMethod: inputMethod, shiftState: .lowercase)
        composer = FunputComposer()
        composer.setInputMethod(inputMethod.engineMethod)
    }

    public func handle(_ key: KeySpec, document: any KeyboardDocument) {
        switch key.role {
        case .character:
            input(characterText(for: key), document: document)
            consumeOneShotShift()
        case .vniModifier, .punctuation:
            input(key.label, document: document)
        case .space:
            input(" ", document: document)
        case .enter:
            input("\n", document: document)
        case .backspace:
            composer.backspace()
            document.deleteBackward()
        case .shift:
            toggleShift()
        case .inputMethod:
            toggleInputMethod()
        default:
            break
        }
    }

    private func input(_ text: String, document: any KeyboardDocument) {
        for scalar in text.unicodeScalars {
            let result = composer.process(scalar)
            if result.action == .none {
                document.insertText(String(scalar))
            } else {
                apply(result, document: document)
            }
        }
    }

    private func apply(
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

    private func characterText(for key: KeySpec) -> String {
        guard state.shiftState.isUppercase else { return key.label }
        return key.shiftedLabel ?? key.label.uppercased()
    }

    private func consumeOneShotShift() {
        guard state.shiftState == .uppercase else { return }
        updateState(shiftState: .lowercase)
    }

    private func toggleShift() {
        let next: ShiftState = state.shiftState == .lowercase ? .uppercase : .lowercase
        updateState(shiftState: next)
    }

    private func toggleInputMethod() {
        let next: KeyboardInputMethod = state.inputMethod == .vni ? .telex : .vni
        composer.clear()
        composer.setInputMethod(next.engineMethod)
        state = KeyboardInputState(inputMethod: next, shiftState: state.shiftState)
    }

    private func updateState(shiftState: ShiftState) {
        state = KeyboardInputState(inputMethod: state.inputMethod, shiftState: shiftState)
    }
}

private extension KeyboardInputMethod {
    var engineMethod: FunputInputMethod {
        switch self {
        case .telex: .telex
        case .vni: .vni
        }
    }
}
#endif
