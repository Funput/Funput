#if canImport(UIKit)
import KeyboardLayout
import KeyboardTouchCore
import KeyboardTouchUIKit

/// Every tuning decision the touch pipeline runs with, in one place and stated out loud.
/// Anything left implicit here reads as dead code later.
extension KeyboardTouchCoordinator {
    static let touchRoles: Set<KeyRole> = [
        .character, .vniModifier, .punctuation, .shift, .backspace,
        .symbols, .moreSymbols, .letters, .inputMethod, .systemInputMode,
        .space, .enter, .emoji,
    ]

    /// The rollover ordering window. 40 ms is a starting point, not a measured value: the
    /// architecture document (§19.1) still needs 60 Hz and 120 Hz device traces to settle it
    /// inside the 24–50 ms candidate range. `maximumBypassHoldMilliseconds` is the counter
    /// that feeds that decision.
    static let arbiterConfiguration = PressArbiterConfiguration(rolloverWindow: 0.040)

    /// Every eligible role recovers from both kinds of drift. A fast two-thumb tap that slid
    /// past the slop, or lifted off the tracked area, still meant the key it landed on.
    ///
    /// This includes Space, which the architecture document's Phase 2.5 note originally wanted
    /// cancelled: the swipe tracker already claims the contact at 32pt, so cancelling at the
    /// 16pt slop would only lose spaces the user did mean to type.
    static let recoveryPolicy = KeyboardTouchRecoveryPolicy.recoveringAll(touchRoles)

    /// No tap duration cap. Holding a key is a gesture — the alternate and repeat lanes own it
    /// — not something the resolver should turn into a cancellation. `ContactResolver` keeps
    /// the capability for hosts that want it.
    static let resolverConfiguration = ContactResolverConfiguration(
        maximumTapDuration: .greatestFiniteMagnitude
    )
}
#endif
