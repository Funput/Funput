#if os(iOS) && canImport(FunputCore)
import Foundation
import FunputEngine
import KeyboardLayout

@MainActor
public final class KeyboardInputCoordinator {
    public internal(set) var state: KeyboardInputState

    let composer: FunputComposer
    var shiftController: ShiftStateController
    var spaceTapTracker: SpaceTapTracker
    /// Mirrors ``FunputConfiguration/smartGesturesEnabled`` for the gestures the engine
    /// owns; the touch-side gestures read it from ``KeyboardPresentation`` instead.
    public internal(set) var smartGesturesEnabled = true
    var documentSynchronizer = KeyboardDocumentSynchronizer()
    var suggestionTracker = AuthoredTokenTracker()
    /// True while Shift was raised by autocapitalization (sentence/word start), not by
    /// a Shift key tap. Backspace must recompute in that case; a manual one-shot Shift
    /// must survive punctuation/space/delete.
    var automaticShiftArmed = false
    var personalSuggestionsEnabled = true
    var suggestionTrackingActive = true
    var preferredTelexMethod: KeyboardInputMethod
    var nextTransactionSequence: UInt64 = 1
    /// Column the spacebar trackpad is aiming for, paired with the transaction
    /// sequence it stays valid at. Keeping the caret's target column across a short
    /// line is what stops a run of up-steps from dragging it left for good.
    var caretPan: (desiredColumn: Int, sequence: UInt64)?

    public init(
        inputMethod: KeyboardInputMethod = .vni,
        shiftDoubleTapInterval: TimeInterval = 0.3,
        shiftClock: @escaping () -> TimeInterval = {
            ProcessInfo.processInfo.systemUptime
        },
        gestureClock: @escaping () -> TimeInterval = {
            ProcessInfo.processInfo.systemUptime
        },
        echoClock: @escaping () -> TimeInterval = {
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
        // Its own clock, not `shiftClock`: a test that freezes shift's clock would
        // otherwise turn every second space into a full stop.
        spaceTapTracker = SpaceTapTracker(clock: gestureClock)
        documentSynchronizer.clock = echoClock
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
