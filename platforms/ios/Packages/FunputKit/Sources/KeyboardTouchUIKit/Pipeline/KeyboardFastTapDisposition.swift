import KeyboardTouchCore

public enum KeyboardFastTapFallback: Equatable, Sendable {
    case exceededDuration
    case endedOutside
    case exceededTapSlop
}

public enum KeyboardFastTapDisposition: Sendable {
    case began(ContactID, KeyboardTouchHit)
    case tracking(ContactID)
    case resolved(ContactID, ContactResolutionMetadata)
    case fallback(ContactID, KeyboardFastTapFallback)
    case cancelled(ContactID)
    case ignored(ContactResolutionNoOp)
}
