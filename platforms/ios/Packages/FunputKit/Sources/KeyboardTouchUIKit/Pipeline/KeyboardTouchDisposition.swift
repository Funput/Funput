import KeyboardTouchCore

public enum KeyboardTouchFallback: Equatable, Sendable {
    case exceededDuration
    case endedOutside
    case exceededTapSlop
}

public enum KeyboardTouchDisposition: Sendable {
    case began(ContactID, KeyboardTouchHit)
    case tracking(ContactID)
    case resolved(ContactID, ContactResolutionMetadata)
    case fallback(ContactID, KeyboardTouchFallback)
    case cancelled(ContactID)
    case ignored(ContactResolutionNoOp)
}
