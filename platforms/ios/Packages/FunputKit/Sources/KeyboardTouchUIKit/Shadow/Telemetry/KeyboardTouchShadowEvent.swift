enum KeyboardTouchShadowEvent: Int32 {
    case capturedBegan = 1
    case shadowResolved
    case outputReleased
    case matched
    case orderMismatch
    case outputMissing
    case shadowMissing
    case outputLate
    case shadowLate
    case cancelledSystem
    case cancelledTapSlop
    case recoveredTapSlop
    case cancelledDuration
    case cancelledOutside
    case outputCancelled
    case timestampTie
    case captureUnknown
    case resolverUnknown
    case outOfScope
    case layoutChangedWhileActive
    case droppedForCapacity
}
