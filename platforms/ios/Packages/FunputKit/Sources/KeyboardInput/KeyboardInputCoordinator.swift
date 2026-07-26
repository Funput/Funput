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
    var suggestionTracker = AuthoredTokenTracker()
    var personalSuggestionsEnabled = true
    var suggestionTrackingActive = true
    var preferredTelexMethod: KeyboardInputMethod

    public init(
        inputMethod: KeyboardInputMethod = .vni,
        shiftDoubleTapInterval: TimeInterval = 0.3,
        shiftClock: @escaping () -> TimeInterval = {
            ProcessInfo.processInfo.systemUptime
        }
    ) {
        preferredTelexMethod = inputMethod.isTelexFamily ? inputMethod : .telex
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
        let signpostID = KeyboardInputSignposts.begin("CoordinatorHandle")
        defer { KeyboardInputSignposts.end("CoordinatorHandle", signpostID) }
        synchronizeBeforeInput(document)
        let mutatesDocument = key.role.mutatesDocument
        if mutatesDocument {
            documentSynchronizer.beginMutation(
                closesEpoch: closesCompositionEpoch(for: key)
            )
        }
        defer {
            if mutatesDocument {
                finishDocumentMutation(
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
            performDeleteBackward(document: document)
            reopenPreviousWord()
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

    private func closesCompositionEpoch(for key: KeySpec) -> Bool {
        if state.inputMethod == .telexAdvanced,
           key.role == .punctuation,
           (key.label == "[" || key.label == "]") {
            return false
        }
        return switch key.role {
        case .space, .punctuation, .enter: true
        default: false
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
        case .telexAdvanced: .telexAdvanced
        case .vni: .vni
        }
    }
}
#endif
