#if DEBUG
public struct KeyboardTouchDiagnosticSnapshot: Equatable, Sendable {
    public let metrics: KeyboardTouchMetrics
    public let activeContactCount: Int
    public let pendingContactCount: Int

    public var isSettled: Bool {
        activeContactCount == 0 && pendingContactCount == 0
    }
}
#endif
