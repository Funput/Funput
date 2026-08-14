import FunputShared
import KeyboardInput
import KeyboardLayout
import KeyboardRenderer
import UIKit

extension KeyboardViewController {
    @discardableResult
    func ensureEmojiView() -> EmojiKeyboardView {
        if let emojiView {
            return emojiView
        }
        let emojiView = KeyboardLaunchTrace.makePanel(.emoji, EmojiKeyboardView())
        emojiView.onEmojiSelected = { [weak self] item in
            self?.insertEmoji(item)
        }
        emojiView.onDelete = { [weak self] in
            self?.deleteFromEmojiKeyboard()
        }
        emojiView.onReturn = { [weak self] in
            self?.showFunput()
        }
        emojiView.onKaomoji = { [weak self] in
            self?.showKaomoji()
        }
        emojiView.backgroundImage = cachedBackgroundImage
        self.emojiView = emojiView
        attachSupplementarySurface(emojiView)
        return emojiView
    }

    func showEmoji() {
        guard currentPresentation.layout.toolbar != nil else { return }
        launchTrace.recordPanelFirstOpen(.emoji)
        ensureEmojiView()
        inputCoordinator.prepareForLiteralInput()
        clearPersonalSuggestions()
        refreshEmojiPresentation()
        switchSurface(to: .emoji)
    }

    func showFunput() {
        emojiView?.reset()
        kaomojiView?.reset()
        clipboardPanelView?.reset()
        switchSurface(to: .funput)
    }

    func refreshEmojiPresentation() {
        emojiView?.apply(
            presentation: currentPresentation,
            recent: emojiRecentsStore.load().compactMap(emojiItem)
        )
    }

    private func insertEmoji(_ item: EmojiItem) {
        let effects = inputCoordinator.insertLiteral(
            item.glyph,
            writer: makeDocumentWriter()
        )
        _ = emojiRecentsStore.record(
            EmojiRecent(glyph: item.glyph, name: item.name, category: item.category.rawValue)
        )
        refreshEmojiPresentation()
        applyPostCommitEffects(effects)
    }

    private func deleteFromEmojiKeyboard() {
        let effects = inputCoordinator.deleteBackward(
            writer: makeDocumentWriter()
        )
        applyPostCommitEffects(effects)
    }

    private func emojiItem(_ recent: EmojiRecent) -> EmojiItem? {
        guard let category = EmojiCategory(rawValue: recent.category), category != .recent else { return nil }
        return EmojiItem(glyph: recent.glyph, name: recent.name, category: category)
    }
}
