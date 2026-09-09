import FunputShared
import KeyboardConfiguration
import KeyboardInput
import KeyboardLayout
import UIKit

extension KeyboardViewController {
    var allowsClipboardCapture: Bool {
        activationState.isActive && hasFullAccess && configuration.clipboardEnabled
            && !inputCoordinator.state.editorMode.isPassword
    }

    func makeClipboardCapture() -> ClipboardCaptureController {
        ClipboardCaptureController(
            gateway: SystemClipboardGateway(),
            allowsCapture: { [weak self] in self?.allowsClipboardCapture == true },
            save: { [weak self] item in self?.clipboardStore.capture(item) == true },
            // Resolved through `self`, not captured: activation replaces the store,
            // and `ClipboardStore` is a struct, so a copy taken here would freeze.
            marks: KeyboardClipboardMarks { [weak self] in self?.clipboardStore },
            onUpdate: { [weak self] in self?.refreshClipboardPanel() }
        )
    }

    func startClipboardMonitoring() {
        clipboardCapture.begin()
        refreshClipboardOffer()
        clipboardChangeMonitor.start(
            readSnapshot: { [weak self] in
                guard self?.allowsClipboardCapture == true else { return nil }
                return ClipboardSnapshot(.general)
            },
            onChange: { [weak self] in
                self?.refreshClipboardOffer()
                self?.clipboardCapture.synchronize()
            }
        )
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
