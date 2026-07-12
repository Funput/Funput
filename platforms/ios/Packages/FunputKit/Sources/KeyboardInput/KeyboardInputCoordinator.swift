#if os(iOS) && canImport(FunputCore)
import Foundation
import FunputEngine
import KeyboardLayout

@MainActor
public final class KeyboardInputCoordinator {
    public internal(set) var state: KeyboardInputState

    let composer: FunputComposer
    var shiftController: ShiftStateController
    var documentSynchronizer = KeyboardDocumentSynchronizer()

    public init(
        inputMethod: KeyboardInputMethod = .vni,
        shiftDoubleTapInterval: TimeInterval = 0.3,
        shiftClock: @escaping () -> TimeInterval = {
            ProcessInfo.processInfo.systemUptime
        }
    ) {
        state = KeyboardInputState(
            inputMethod: inputMethod,
            shiftState: .lowercase,
            autocapitalization: .none
        )
        composer = FunputComposer()
        shiftController = ShiftStateController(
            doubleTapInterval: shiftDoubleTapInterval,
            clock: shiftClock
        )
        composer.setInputMethod(inputMethod.engineMethod)
    }

    public func handle(_ key: KeySpec, document: any KeyboardDocument) {
        synchronizeBeforeInput(document)
        let mutatesDocument = key.role.mutatesDocument
        if mutatesDocument {
            documentSynchronizer.beginMutation()
        }
        defer {
            if mutatesDocument {
                finishDocumentMutation(
                    document,
                    preserveOneShotShift: key.role != .character
                )
            }
        }

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
            if state.usesVietnameseComposition {
                composer.backspace()
            }
            document.deleteBackward()
        case .shift:
            toggleShift()
        case .inputMethod:
            toggleInputMethod()
        case .symbols:
            updateLayoutMode(.symbolsPrimary)
        case .moreSymbols:
            updateLayoutMode(.symbolsSecondary)
        case .letters:
            updateLayoutMode(.letters)
        default:
            break
        }
    }
}

private extension KeyRole {
    var mutatesDocument: Bool {
        switch self {
        case .character, .vniModifier, .punctuation, .space, .enter, .backspace:
            true
        default:
            false
        }
    }
}

extension KeyboardInputMethod {
    var engineMethod: FunputInputMethod {
        switch self {
        case .telex: .telex
        case .vni: .vni
        }
    }
}
#endif
