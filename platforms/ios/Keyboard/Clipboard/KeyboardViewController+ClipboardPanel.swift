import FunputShared
import KeyboardInput
import KeyboardLayout
import KeyboardRenderer
import UIKit

extension KeyboardViewController {
    @discardableResult
    func ensureClipboardPanelView() -> ClipboardKeyboardView {
        if let clipboardPanelView {
            return clipboardPanelView
        }
        let clipboardPanelView = KeyboardLaunchTrace.makePanel(.clipboard, ClipboardKeyboardView())
        clipboardPanelView.onRetry = { [weak self] in
            self?.clipboardCapture.synchronize(retry: true)
            self?.refreshClipboardPanel()
        }
        clipboardPanelView.onSelect = { [weak self] entry in
            self?.pasteFromHistory(entry)
        }
        clipboardPanelView.onTogglePin = { [weak self] entry in
            self?.clipboardStore.setPinned(!entry.isPinned, id: entry.id)
            self?.refreshClipboardPanel()
        }
        clipboardPanelView.onRemove = { [weak self] entry in
            self?.clipboardStore.remove(id: entry.id)
            self?.refreshClipboardPanel()
        }
        clipboardPanelView.onClearAll = { [weak self] in
            self?.clearClipboardHistory()
        }
        clipboardPanelView.onDelete = { [weak self] in
            self?.deleteFromClipboardPanel()
        }
        clipboardPanelView.onReturn = { [weak self] in
            self?.showFunput()
        }
        clipboardPanelView.backgroundImage = cachedBackgroundImage
        self.clipboardPanelView = clipboardPanelView
        attachSupplementarySurface(clipboardPanelView)
        return clipboardPanelView
    }

    func showClipboardPanel() {
        guard currentPresentation.layout.toolbar != nil else { return }
        launchTrace.recordPanelFirstOpen(.clipboard)
        ensureClipboardPanelView()
        inputCoordinator.prepareForLiteralInput()
        clearPersonalSuggestions()
        clipboardCapture.synchronize()
        refreshClipboardPanel()
        switchSurface(to: .clipboard)
    }

    func refreshClipboardPanel() {
        clipboardPanelView?.apply(
            presentation: currentPresentation,
            entries: clipboardStore.load().map(Self.entry),
            hasFullAccess: hasFullAccess,
            needsRetry: clipboardCapture.needsRetry
        )
    }

    private static func entry(_ item: ClipboardItem) -> KeyboardClipboardEntry {
        KeyboardClipboardEntry(
            id: item.id,
            text: item.text,
            capturedAt: item.capturedAt,
            isPinned: item.isPinned
        )
    }

    /// Pasting from history returns to the keyboard: the user came here to fetch one
    /// thing and then keep typing, unlike emoji or kaomoji where several in a row is
    /// the normal case.
    private func pasteFromHistory(_ entry: KeyboardClipboardEntry) {
        let effects = inputCoordinator.insertLiteral(entry.text, writer: makeDocumentWriter())
        showFunput()
        applyPostCommitEffects(effects)
    }

    /// Wiping the history suppresses the clipboard that is still on the pasteboard for
    /// the rest of the session: capture would otherwise re-import, within half a
    /// second, exactly what the user just asked to be rid of. The next session syncs
    /// it again, as opening the keyboard always does.
    private func clearClipboardHistory() {
        clipboardCapture.suppressCurrentAfterClear()
        clipboardStore.clear()
        refreshClipboardPanel()
        refreshClipboardOffer()
    }

    private func deleteFromClipboardPanel() {
        let effects = inputCoordinator.deleteBackward(writer: makeDocumentWriter())
        applyPostCommitEffects(effects)
    }
}
