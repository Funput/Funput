#if DEBUG
import Foundation
import FunputShared
import KeyboardRenderer
import UIKit

@MainActor
final class KeyboardTouchDiagnosticsReporter {
    private let surface: KeyboardSurfaceView
    private let store: KeyboardTouchDiagnosticStore
    private let writeQueue = DispatchQueue(
        label: "app.funput.keyboard.touch-diagnostics",
        qos: .utility
    )
    private var publisher: KeyboardTouchDiagnosticPublisher?
    private var session: KeyboardTouchDiagnosticSession?
    private var generation: UInt64 = 0

    init(
        surface: KeyboardSurfaceView,
        store: KeyboardTouchDiagnosticStore = .init()
    ) {
        self.surface = surface
        self.store = store
    }

    func startIfAvailable(hasFullAccess: Bool) {
        finish()
        guard hasFullAccess,
              let session = store.activeSession()
        else { return }
        guard surface.resetTouchDiagnosticsIfIdle() else { return }

        generation &+= 1
        let expectedGeneration = generation
        self.session = session
        let store = store
        publisher = KeyboardTouchDiagnosticPublisher(
            session: session,
            device: Self.deviceMetadata()
        ) { [weak self] report in
            guard let self, generation == expectedGeneration else { return }
            writeQueue.async {
                _ = store.save(report)
            }
        }
        surface.observeTouchDiagnostics { [weak self] snapshot in
            self?.receive(snapshot, generation: expectedGeneration)
        }
    }

    func finish() {
        guard publisher != nil else { return }
        submit(surface.touchDiagnosticSnapshot)
        publisher?.finalFlush()
        surface.observeTouchDiagnostics(nil)
        publisher?.invalidate()
        publisher = nil
        session = nil
        generation &+= 1
    }

    private func receive(
        _ snapshot: KeyboardRenderer.KeyboardTouchDiagnosticSnapshot,
        generation expectedGeneration: UInt64
    ) {
        guard generation == expectedGeneration, let session else { return }
        guard session.isActive(at: Date()) else {
            finish()
            return
        }
        submit(snapshot)
    }

    private func submit(
        _ snapshot: KeyboardRenderer.KeyboardTouchDiagnosticSnapshot
    ) {
        publisher?.submit(
            metrics: KeyboardTouchDiagnosticMetrics(snapshot),
            activeContactCount: snapshot.activeContactCount,
            pendingComparisonCount: snapshot.pendingComparisonCount,
            isSettled: snapshot.isSettled
        )
    }

    private static func deviceMetadata() -> KeyboardTouchDiagnosticDevice {
        KeyboardTouchDiagnosticDevice(
            model: UIDevice.current.model,
            operatingSystem: UIDevice.current.systemName
                + " " + UIDevice.current.systemVersion,
            maximumFramesPerSecond: UIScreen.main.maximumFramesPerSecond
        )
    }
}
#endif
