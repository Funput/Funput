#if canImport(UIKit)
@testable import KeyboardRenderer
import Testing
import UIKit

@MainActor
@Suite("Emoji keyboard view")
struct EmojiKeyboardViewTests {
    let smile = EmojiItem(glyph: "😀", name: "grinning face", category: .smileysPeople)

    @Test("Renders catalog and recent sections")
    func content() {
        let view = makeView()
        view.apply(theme: .funputGlass, recent: [smile])
        #expect(view.numberOfSections(in: view.collectionView) == 3)
        #expect(view.collectionView(view.collectionView, numberOfItemsInSection: 0) == 1)
    }

    @Test("Selection and bottom actions emit callbacks")
    func actions() {
        let view = makeView()
        var selected: EmojiItem?
        var deleted = false
        var returned = false
        view.onEmojiSelected = { selected = $0 }
        view.onDelete = { deleted = true }
        view.onReturn = { returned = true }
        view.collectionView(view.collectionView, didSelectItemAt: IndexPath(item: 0, section: 0))
        buttons(in: view).first { $0.accessibilityLabel == "Xóa" }?.sendActions(for: .touchUpInside)
        buttons(in: view).first { $0.accessibilityLabel == "Trở về bàn phím Funput" }?
            .sendActions(for: .touchUpInside)
        #expect(selected == smile)
        #expect(deleted)
        #expect(returned)
    }

    private func makeView() -> EmojiKeyboardView {
        let catalog = EmojiCatalog(
            version: "test",
            emojis: [smile, EmojiItem(glyph: "🐶", name: "dog", category: .animalsNature)]
        )
        return EmojiKeyboardView(catalog: catalog)
    }

    private func buttons(in view: UIView) -> [UIButton] {
        view.subviews.flatMap { child in
            (child as? UIButton).map { [$0] } ?? buttons(in: child)
        }
    }
}
#endif
