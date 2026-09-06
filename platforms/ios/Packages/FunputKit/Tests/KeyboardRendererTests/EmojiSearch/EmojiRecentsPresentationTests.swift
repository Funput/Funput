#if canImport(UIKit)
@testable import KeyboardRenderer
import Testing
import UIKit

@MainActor
@Suite("Emoji recent presentation")
struct EmojiRecentsPresentationTests {
    @Test("Repeated recent selections stay in place until the panel resets")
    func deferredOrdering() {
        let items = makeItems(count: 3)
        let view = EmojiKeyboardView(catalog: EmojiCatalog(version: "test", emojis: items))
        view.apply(theme: .funputGlass, recent: items)
        var selected: [EmojiItem] = []
        view.onRecentEmojiSelected = { item in
            selected.append(item)
            view.apply(theme: .funputGlass, recent: [item] + items.filter { $0 != item })
        }
        for index in [2, 2, 1] {
            view.collectionView(view.collectionView, didSelectItemAt: IndexPath(item: index, section: 0))
            #expect(view.sections[0].items == items)
        }
        #expect(selected == [items[2], items[2], items[1]])
        view.reset()
        #expect(view.sections[0].items == [items[1], items[0], items[2]])
        view.apply(theme: .funputGlass, recent: items)
        #expect(view.sections[0].items == items)
    }

    @Test("Recents fit one row on narrow and wide keyboards", arguments: [320.0, 343.0, 375.0, 430.0, 768.0])
    func singleRow(width: Double) throws {
        let items = makeItems(count: 30)
        let view = EmojiKeyboardView(catalog: EmojiCatalog(version: "test", emojis: items))
        view.frame = CGRect(x: 0, y: 0, width: width, height: 304)
        view.apply(theme: .funputGlass, recent: items)
        view.layoutIfNeeded()
        view.collectionView.layoutIfNeeded()
        #expect(view.sections[0].items == Array(items.prefix(8)))
        let frames = try (0..<8).map { index in
            try #require(view.collectionView.layoutAttributesForItem(
                at: IndexPath(item: index, section: 0)
            )).frame
        }
        #expect(Set(frames.map(\.minY)).count == 1)
        #expect(frames.allSatisfy { $0.maxX <= width - 8 })
    }

    private func makeItems(count: Int) -> [EmojiItem] {
        (0..<count).map { EmojiItem(glyph: "emoji-\($0)", name: "Item \($0)", category: .symbols) }
    }
}
#endif
