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
    var nextTransactionSequence: UInt64 = 1

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
