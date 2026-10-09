import FunputShared
import KeyboardConfiguration
import KeyboardInput
import KeyboardLayout
import UIKit

extension KeyboardViewController {
    /// How long the keyboard is left alone after it appears before any clipboard
    /// work starts: enough to draw the first frame and take the first keys.
    static let clipboardStartDelay: Duration = .milliseconds(300)

    var allowsClipboardCapture: Bool {
        activationState.isActive && hasFullAccess && configuration.clipboardEnabled
            && !inputCoordinator.state.editorMode.isPassword
    }

    func makeClipboardCapture() -> ClipboardCaptureController {
        ClipboardCaptureController(
            gateway: TracedClipboardGateway(),
            allowsCapture: { [weak self] in self?.allowsClipboardCapture == true },
            save: { [weak self] item in self?.clipboardStore.capture(item) == true },
            // Resolved through `self`, not captured: activation replaces the store,
            // and `ClipboardStore` is a struct, so a copy taken here would freeze.
            marks: KeyboardClipboardMarks { [weak self] in self?.clipboardStore },
            onUpdate: { [weak self] in self?.refreshClipboardPanel() }
        )
    }

    /// Typing comes first. Clipboard work used to run synchronously in
    /// `viewDidAppear` and read the contents on the spot. A clipboard copied on a
    /// Mac then held the main thread while it was fetched, and the system paste
    /// prompt popped up before the user could type. Now the monitor starts only
    /// after the keyboard is on screen.
    func scheduleClipboardMonitoringStart() {
        cancelClipboardMonitoringStart()
        let generation = activationState.generation
        clipboardStartTask = Task { @MainActor [weak self] in
            do { try await Task.sleep(for: Self.clipboardStartDelay) }
            catch { return }
            guard let self, activationState.accepts(generation) else { return }
            clipboardStartTask = nil
            startClipboardMonitoring()
        }
    }

    func cancelClipboardMonitoringStart() {
        clipboardStartTask?.cancel()
        clipboardStartTask = nil
    }

    func startClipboardMonitoring() {
        ClipboardSignpost.measure("ClipboardStart") {
            isClipboardMonitoring = true
            clipboardCapture.begin()
            refreshClipboardOffer()
            clipboardChangeMonitor.start(
                readSnapshot: { [weak self] in
                    guard self?.allowsClipboardCapture == true else { return nil }
                    return ClipboardSnapshot(.general)
                },
                onChange: { [weak self] in
                    // Metadata only, so the paste chip can show at once.
                    self?.refreshClipboardOffer()
                    self?.requestIdleClipboardCapture()
                }
            )
        }
    }

    func stopClipboardMonitoring() {
        cancelClipboardMonitoringStart()
        isClipboardMonitoring = false
        clipboardChangeMonitor.stop()
        clipboardIdleGate.cancel()
        clipboardCapture.end()
    }

    /// Reading the contents can stall on a remote clipboard and raises the paste
    /// prompt, so automatic capture waits until the user stops typing. Opening the
    /// clipboard panel still captures immediately: that is the user asking for it.
    private func requestIdleClipboardCapture() {
        ClipboardSignpost.event("ClipboardCaptureDeferred")
        clipboardIdleGate.requestCapture { [weak self] in
            self?.clipboardCapture.synchronize()
        }
    }
}

/// Reaches the controller's current ``ClipboardStore`` rather than a copy of
/// whichever one existed when the capture controller was built.
struct KeyboardClipboardMarks: ClipboardSessionMarkStoring {
    let store: () -> ClipboardStore?

    func lastCapturedChangeCount() -> Int? { store()?.lastCapturedChangeCount() }
    func lastPastedChangeCount() -> Int? { store()?.lastPastedChangeCount() }

    @discardableResult
    func markPasted(_ changeCount: Int) -> Bool { store()?.markPasted(changeCount) == true }
}
