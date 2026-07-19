import FunputShared
import KeyboardInput
import KeyboardLayout
import KeyboardRenderer
import UIKit

extension KeyboardViewController {
    func installEmojiView() {
        emojiView.translatesAutoresizingMaskIntoConstraints = false
        emojiView.isHidden = true
        emojiView.onEmojiSelected = { [weak self] item in
            self?.insertEmoji(item)
        }
        emojiView.onDelete = { [weak self] in
            self?.deleteFromEmojiKeyboard()
        }
        emojiView.onReturn = { [weak self] in
            self?.showFunput()
        }
        view.addSubview(emojiView)
    }

    func showEmoji() {
        guard keyboardView.presentation.layout.toolbar != nil else { return }
        inputCoordinator.prepareForLiteralInput()
        clearPersonalSuggestions()
        displayedSurface = .emoji
        keyboardView.isHidden = true
        emojiView.isHidden = false
        refreshEmojiPresentation()
    }

    func showFunput() {
        displayedSurface = .funput
        emojiView.isHidden = true
        keyboardView.isHidden = false
    }

    func refreshEmojiPresentation() {
        emojiView.apply(
            theme: keyboardView.presentation.theme,
            recent: emojiRecentsStore.load().compactMap(emojiItem)
        )
    }

    private func insertEmoji(_ item: EmojiItem) {
        let previousState = inputCoordinator.state
        let document = TextDocumentProxyAdapter(proxy: textDocumentProxy)
        inputCoordinator.insertLiteral(item.glyph, document: document)
        _ = emojiRecentsStore.record(
            EmojiRecent(glyph: item.glyph, name: item.name, category: item.category.rawValue)
        )
        refreshEmojiPresentation()
        if inputCoordinator.state != previousState {
            updateInputPresentation()
        }
    }

    private func deleteFromEmojiKeyboard() {
        let previousState = inputCoordinator.state
        let document = TextDocumentProxyAdapter(proxy: textDocumentProxy)
        inputCoordinator.deleteBackward(document: document)
        if inputCoordinator.state != previousState {
            updateInputPresentation()
        }
    }

    private func emojiItem(_ recent: EmojiRecent) -> EmojiItem? {
        guard let category = EmojiCategory(rawValue: recent.category), category != .recent else { return nil }
        return EmojiItem(glyph: recent.glyph, name: recent.name, category: category)
    }
}
