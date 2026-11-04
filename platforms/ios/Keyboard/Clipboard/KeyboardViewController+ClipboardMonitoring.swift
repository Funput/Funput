import FunputShared
import KeyboardConfiguration
import KeyboardInput
import KeyboardLayout
import UIKit

extension KeyboardViewController {
    /// How long the keyboard is left alone after it appears before any clipboard
    /// work starts: enough to draw the first frame and take the first keys.
    static let clipboardStartDelay: Duration = .milliseconds(300)

    /// Whether the toolbar may invite a paste and a paste may enter the history.
    /// Password fields never get either, whatever the settings say.
    var allowsClipboardOffer: Bool {
        activationState.isActive && hasFullAccess && configuration.clipboardEnabled
            && !inputCoordinator.state.editorMode.isPassword
    }

    func makeClipboardRecorder() -> ClipboardPasteRecorder {
        ClipboardPasteRecorder(
            gateway: SystemClipboardGateway(),
            allowsRecording: { [weak self] in self?.allowsClipboardOffer == true },
            save: { [weak self] item in self?.clipboardStore.record(item) == true },
            // Resolved through `self`, not captured: activation replaces the store,
            // and `ClipboardStore` is a struct, so a copy taken here would freeze.
            marks: KeyboardClipboardMarks { [weak self] in self?.clipboardStore },
            onUpdate: { [weak self] in self?.refreshClipboardPanel() }
        )
    }

    /// Typing comes first. The monitor only samples pasteboard metadata, which
    /// raises no prompt, but even that waits until the keyboard is on screen.
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
            clipboardRecorder.begin()
            refreshClipboardOffer()
            clipboardChangeMonitor.start(
                readSnapshot: { [weak self] in
                    guard self?.allowsClipboardOffer == true else { return nil }
                    return ClipboardSnapshot(.general)
                },
                // One query per tick while the user types, behind the same gate:
                // a password field asks the pasteboard nothing at all.
                readChangeCount: { [weak self] in
                    guard self?.allowsClipboardOffer == true else { return nil }
                    return UIPasteboard.general.changeCount
                },
                // Metadata only: the offer changes, the contents stay unread until
                // the user taps a paste control.
                onChange: { [weak self] in self?.refreshClipboardOffer() }
            )
        }
    }

    func stopClipboardMonitoring() {
        cancelClipboardMonitoringStart()
        isClipboardMonitoring = false
        clipboardChangeMonitor.stop()
        clipboardRecorder.end()
    }
}

/// Reaches the controller's current ``ClipboardStore`` rather than a copy of
/// whichever one existed when the recorder was built.
struct KeyboardClipboardMarks: ClipboardSessionMarkStoring {
    let store: () -> ClipboardStore?

    func lastPastedChangeCount() -> Int? { store()?.lastPastedChangeCount() }

    @discardableResult
    func markPasted(_ changeCount: Int) -> Bool { store()?.markPasted(changeCount) == true }
}
