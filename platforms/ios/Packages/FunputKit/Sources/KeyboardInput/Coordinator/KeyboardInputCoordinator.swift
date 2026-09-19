#if os(iOS) && canImport(FunputCore)
import Foundation
import FunputEngine
import KeyboardLayout

@MainActor
public final class KeyboardInputCoordinator {
    public internal(set) var state: KeyboardInputState

    var shortcuts = KeyboardShortcutState()
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
    /// Where the finger landed for the key being handled right now. Set by `handle`
    /// and spent on the first scalar that reaches the engine; never outlives the key
    /// it belongs to.
    var pendingTouch: KeyboardTouchEvidence?
    /// What the host knows about words. Nil until it supplies one, and while it is
    /// nil no correction is ever applied — see `chooseCorrection`.
    public weak var correctionDictionary: (any CorrectionDictionary)?
    /// Whether the field the user is typing in wants autocorrect at all. A password
    /// box, a code field or a search bar that asked for `.no` gets none.
    public var allowsAutocorrect = true
    /// Set for the one commit that undoes a correction, so re-opening the previous
    /// word is skipped for it.
    var undidCorrection = false
    /// How far this user's touches land from key centres. Read by the host to decide
    /// whether correcting is worth doing at all.
    public internal(set) var touchSpread = KeyboardTouchSpread()

    /// Forget the touches counted so far — called once a session has been reported.
    public func resetTouchSpread() {
        touchSpread.reset()
    }

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
