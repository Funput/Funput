import FunputShared
import KeyboardConfiguration
import KeyboardInput
import KeyboardLayout
import KeyboardRenderer
import UIKit

extension KeyboardViewController {
    func installClipboard(on surface: KeyboardSurfaceView) {
        surface.onClipboardPaste = { [weak self] text in
            self?.pasteFromClipboard(text)
        }
    }

    /// Re-evaluates whether the toolbar, and the clipboard panel when it is open, may
    /// invite a paste.
    ///
    /// Reads pasteboard metadata only, including its plain-text type identifiers,
    /// which raises no paste alert. The contents are never read by the keyboard; they
    /// arrive only through the `UIPasteControl` the user taps.
    ///
    /// Deliberately not called from `updateInputPresentation()`: that runs on every
    /// keystroke that moves the shift state, and the typing hot path stays free of
    /// pasteboard work.
    func refreshClipboardOffer(
        retriesRemaining: Int = KeyboardViewController.clipboardRetryDelays.count
    ) {
        cancelClipboardRetry()
        let context = ClipboardOfferPolicy.Context(
            editorMode: inputCoordinator.state.editorMode,
            hasToolbar: currentPresentation.layout.toolbar != nil,
            hasFullAccess: hasFullAccess,
            isEnabled: configuration.clipboardEnabled
        )
        // Checked before anything else, so a refused pasteboard read can never leave
        // a paste invitation sitting in a password field.
        guard ClipboardOfferPolicy.allowsOffer(context: context) else {
            showClipboardHint(nil)
            return
        }

        let snapshot = ClipboardSnapshot(.general)
        // iOS declines reads while the host app settles — Chrome is a frequent
        // offender — and answers as if the pasteboard were empty. Leave whatever is
        // on screen alone and ask again rather than believing it.
        guard !snapshot.isIndeterminate else {
            scheduleClipboardRetry(retriesRemaining: retriesRemaining)
            return
        }

        let offer = ClipboardOfferPolicy.offer(
            snapshot: snapshot,
            lastPastedChangeCount: clipboardRecorder.lastPastedChangeCount,
            context: context
        )
        showClipboardHint(offer.map(Self.hint))
    }

    /// One answer for both places a paste can be offered, so the panel can never
    /// invite a paste the toolbar has already withdrawn.
    private func showClipboardHint(_ hint: KeyboardClipboardHint?) {
        keyboardView.updateClipboardHint(hint)
        clipboardPanelView?.updatePasteHint(hint)
    }

    /// Bounded and widening: two more attempts, then leave it until the next thing
    /// that would refresh the offer anyway.
    static let clipboardRetryDelays: [Duration] = [.milliseconds(350), .seconds(1)]

    private func scheduleClipboardRetry(retriesRemaining: Int) {
        let index = Self.clipboardRetryDelays.count - retriesRemaining
        guard Self.clipboardRetryDelays.indices.contains(index),
              activationState.isActive else { return }
        let delay = Self.clipboardRetryDelays[index]
        let generation = activationState.generation
        clipboardRetryTask = Task { @MainActor [weak self] in
            do { try await Task.sleep(for: delay) }
            catch { return }
            guard let self, activationState.accepts(generation) else { return }
            clipboardRetryTask = nil
            refreshClipboardOffer(retriesRemaining: retriesRemaining - 1)
        }
    }

    func cancelClipboardRetry() {
        clipboardRetryTask?.cancel()
        clipboardRetryTask = nil
    }

    private static func hint(for offer: ClipboardOffer) -> KeyboardClipboardHint {
        switch offer.kind {
        case .text: .text
        case .link: .link
        }
    }

    /// Shared by the toolbar chip and the panel's paste row: both hand over text the
    /// user's tap on a `UIPasteControl` released.
    func pasteFromClipboard(_ paste: KeyboardClipboardPaste) {
        guard activationState.isActive, !paste.text.isEmpty else { return }
        let effects = inputCoordinator.insertLiteral(paste.text, writer: makeDocumentWriter())
        clipboardRecorder.didPaste(paste.text, changeCount: paste.changeCount)
        refreshClipboardOffer()
        applyPostCommitEffects(effects)
    }
}
