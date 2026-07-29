enum KeyboardTouchShadowEvent: Int32 {
    case capturedBegan = 1
    case shadowResolved
    case legacyReleased
    case matched
    case orderMismatch
    case legacyMissing
    case shadowMissing
    case legacyLate
    case shadowLate
    case cancelledSystem
    case cancelledTapSlop
    case recoveredTapSlop
    case cancelledDuration
    case cancelledOutside
    case legacyCancelled
    case timestampTie
    case captureUnknown
    case resolverUnknown
    case outOfScope
    case layoutChangedWhileActive
    case droppedForCapacity
}
