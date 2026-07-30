#if canImport(UIKit) && DEBUG
public extension KeyboardSurfaceView {
    var touchDiagnosticSnapshot: KeyboardTouchDiagnosticSnapshot {
        KeyboardTouchDiagnosticSnapshot(
            metrics: touchCoordinator.metrics,
            activeContactCount: touchCoordinator.activeContactCount,
            pendingContactCount: touchCoordinator.pendingContactCount
        )
    }

    func observeTouchDiagnostics(
        _ observer: (@MainActor (KeyboardTouchDiagnosticSnapshot) -> Void)?
    ) {
        touchCoordinator.observe { [weak self] _ in
            guard let self else { return }
            observer?(touchDiagnosticSnapshot)
        }
    }

    @discardableResult
    func resetTouchDiagnosticsIfIdle() -> Bool {
        guard touchDiagnosticSnapshot.isSettled else { return false }
        touchCoordinator.reset()
        return true
    }
}
#endif
