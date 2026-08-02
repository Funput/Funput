import FunputShared
import KeyboardInput
import KeyboardLayout
import KeyboardRenderer
import UIKit

extension KeyboardViewController {
    func installClipboardPanel() {
        clipboardPanelView.translatesAutoresizingMaskIntoConstraints = false
        clipboardPanelView.isHidden = true
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
        view.addSubview(clipboardPanelView)
    }

    func showClipboardPanel() {
        guard keyboardView.presentation.layout.toolbar != nil else { return }
        inputCoordinator.prepareForLiteralInput()
        clearPersonalSuggestions()
        displayedSurface = .clipboard
        keyboardView.isHidden = true
        emojiView.isHidden = true
        kaomojiView.isHidden = true
        clipboardPanelView.isHidden = false
        refreshClipboardPanel()
    }

    func refreshClipboardPanel() {
        clipboardPanelView.apply(
            presentation: keyboardView.presentation,
            entries: clipboardStore.load().map(Self.entry),
            hasFullAccess: hasFullAccess
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

    /// Wiping the history also clears the captured change count, so whatever is still
    /// on the pasteboard is offered again — after an explicit wipe the user should be
    /// able to start over rather than face a keyboard that has quietly written it off.
    private func clearClipboardHistory() {
        clipboardStore.clear()
        refreshClipboardPanel()
        refreshClipboardOffer()
    }

    private func deleteFromClipboardPanel() {
        let effects = inputCoordinator.deleteBackward(writer: makeDocumentWriter())
        applyPostCommitEffects(effects)
    }
}
