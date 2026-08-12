import FunputShared
import KeyboardInput
import KeyboardLayout
import KeyboardRenderer
import UIKit

extension KeyboardViewController {
    func installKaomojiView() {
        kaomojiView.translatesAutoresizingMaskIntoConstraints = false
        kaomojiView.isHidden = true
        kaomojiView.onKaomojiSelected = { [weak self] item in
            self?.insertKaomoji(item)
        }
        kaomojiView.onDelete = { [weak self] in
            self?.deleteFromKaomojiKeyboard()
        }
        kaomojiView.onReturn = { [weak self] in
            self?.showFunput()
        }
        kaomojiView.onEmoji = { [weak self] in
            self?.showEmoji()
        }
        view.addSubview(kaomojiView)
    }

    func showKaomoji() {
        guard keyboardView.presentation.layout.toolbar != nil else { return }
        launchTrace.recordPanelFirstOpen(.kaomoji)
        inputCoordinator.prepareForLiteralInput()
        clearPersonalSuggestions()
        displayedSurface = .kaomoji
        keyboardView.isHidden = true
        emojiView.isHidden = true
        clipboardPanelView.isHidden = true
        kaomojiView.isHidden = false
        refreshKaomojiPresentation()
    }

    func refreshKaomojiPresentation() {
        kaomojiView.apply(
            presentation: keyboardView.presentation,
            recent: kaomojiRecentsStore.load().compactMap(kaomojiItem)
        )
    }

    private func insertKaomoji(_ item: KaomojiItem) {
        let effects = inputCoordinator.insertLiteral(
            item.text,
            writer: makeDocumentWriter()
        )
        _ = kaomojiRecentsStore.record(
            EmojiRecent(glyph: item.text, name: item.name, category: item.category.rawValue)
        )
        refreshKaomojiPresentation()
        applyPostCommitEffects(effects)
    }

    private func deleteFromKaomojiKeyboard() {
        let effects = inputCoordinator.deleteBackward(
            writer: makeDocumentWriter()
        )
        applyPostCommitEffects(effects)
    }

    private func kaomojiItem(_ recent: EmojiRecent) -> KaomojiItem? {
        guard let category = KaomojiCategory(rawValue: recent.category), category != .recent else { return nil }
        return KaomojiItem(text: recent.glyph, name: recent.name, category: category)
    }
}
