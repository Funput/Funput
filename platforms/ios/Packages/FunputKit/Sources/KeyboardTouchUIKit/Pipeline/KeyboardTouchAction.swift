import KeyboardLayout

public enum KeyboardTouchAction: Sendable {
    case released(KeyboardTouchHit)
    case repeated(KeyboardTouchHit)
    case alternate(KeyboardTouchHit, KeyAlternate)
    case swiped(KeyboardTouchHit, KeySwipeAction)
    case cancelled(KeyboardTouchHit)

    public var hit: KeyboardTouchHit {
        switch self {
        case let .released(hit), let .repeated(hit), let .cancelled(hit):
            hit
        case let .alternate(hit, _), let .swiped(hit, _):
            hit
        }
    }
}
