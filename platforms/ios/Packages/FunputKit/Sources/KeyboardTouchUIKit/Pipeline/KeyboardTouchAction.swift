import KeyboardLayout

public enum KeyboardTouchAction: Sendable {
    /// A key the finger lifted on, with what the keys around it say about where the
    /// touch landed. The evidence is absent for a key that types nothing, for a lift
    /// outside the keyboard, and whenever the layout has no pitch to measure in.
    case released(KeyboardTouchHit, KeyboardTouchEvidence?)
    case repeated(KeyboardTouchHit)
    case alternate(KeyboardTouchHit, KeyAlternate)
    case swiped(KeyboardTouchHit, KeySwipeAction)
    case cancelled(KeyboardTouchHit)

    public var hit: KeyboardTouchHit {
        switch self {
        case let .repeated(hit), let .cancelled(hit):
            hit
        case let .released(hit, _), let .alternate(hit, _), let .swiped(hit, _):
            hit
        }
    }
}
