public enum ShiftState: Hashable, Sendable {
    case lowercase
    case uppercase
    case capsLocked

    public var isUppercase: Bool { self != .lowercase }
}
