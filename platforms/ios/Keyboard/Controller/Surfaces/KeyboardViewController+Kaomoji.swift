import FunputShared
import KeyboardInput
import KeyboardLayout
import KeyboardRenderer
import UIKit

extension KeyboardViewController {
    @discardableResult
    func ensureKaomojiView() -> KaomojiKeyboardView {
        if let kaomojiView {
            return kaomojiView
        }
        let kaomojiView = KeyboardLaunchTrace.makePanel(.kaomoji, KaomojiKeyboardView())
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
        kaomojiView.backgroundImage = cachedBackgroundImage
        self.kaomojiView = kaomojiView
        attachSupplementarySurface(kaomojiView)
        return kaomojiView
    }

    func showKaomoji() {
        guard currentPresentation.layout.allowsEmojiPanel else { return }
        launchTrace.recordPanelFirstOpen(.kaomoji)
        ensureKaomojiView()
        inputCoordinator.prepareForLiteralInput()
        clearPersonalSuggestions()
        refreshKaomojiPresentation()
        switchSurface(to: .kaomoji)
    }

    func refreshKaomojiPresentation() {
        kaomojiView?.apply(
            presentation: currentPresentation,
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
