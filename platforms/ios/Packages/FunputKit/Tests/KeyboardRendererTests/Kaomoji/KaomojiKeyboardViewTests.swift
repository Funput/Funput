#if canImport(UIKit)
@testable import KeyboardRenderer
import Testing
import UIKit

@MainActor
@Suite("Kaomoji keyboard view")
struct KaomojiKeyboardViewTests {
    let smile = KaomojiItem(text: "(^_^)", name: "cười", category: .happy)
    let shrug = KaomojiItem(text: "¯\\_(ツ)_/¯", name: "chịu thôi", category: .confused)

    @Test("Renders catalog and recent sections")
    func content() {
        let view = makeView()
        view.apply(theme: .funputGlass, recent: [smile])
        #expect(view.numberOfSections(in: view.collectionView) == 3)
        #expect(view.collectionView(view.collectionView, numberOfItemsInSection: 0) == 1)
    }

    @Test("Recent section disappears when there is no history")
    func withoutRecents() {
        let view = makeView()
        view.apply(theme: .funputGlass, recent: [])
        #expect(view.numberOfSections(in: view.collectionView) == 2)
    }

    @Test("Selection and bottom actions emit callbacks")
    func actions() {
        let view = makeView()
        var selected: KaomojiItem?
        var deleted = false
        var returned = false
        var openedEmoji = false
        view.onKaomojiSelected = { selected = $0 }
        view.onDelete = { deleted = true }
        view.onReturn = { returned = true }
        view.onEmoji = { openedEmoji = true }
        view.collectionView(view.collectionView, didSelectItemAt: IndexPath(item: 0, section: 0))
        tap("Xóa", in: view)
        tap("Trở về bàn phím Funput", in: view)
        tap("Biểu tượng cảm xúc", in: view)
        #expect(selected == smile)
        #expect(deleted)
        #expect(returned)
        #expect(openedEmoji)
    }

    @Test("Cells are sized from their measured text")
    func itemWidth() {
        let view = makeView()
        let narrow = view.itemWidth(for: "(^_^)", available: 390)
        let wide = view.itemWidth(for: "(╯°□°)╯︵ ┻━┻", available: 390)
        #expect(wide > narrow)
        #expect(narrow >= KaomojiKeyboardView.minimumItemWidth)
    }

    @Test("A kaomoji longer than the row is clamped to it")
    func clampsToRow() {
        let view = makeView()
        let available: CGFloat = 320
        let width = view.itemWidth(
            for: String(repeating: "(╯°□°)╯︵ ┻━┻", count: 6),
            available: available
        )
        #expect(width <= available - KaomojiKeyboardView.horizontalInset * 2)
    }

    /// Plain flow layout spreads a row's leftover width into its gaps, which makes
    /// rows of wide kaomoji look randomly spaced. Rows must pack from the left.
    @Test("Rows pack from the left instead of justifying")
    func leftAlignedRows() {
        let view = KaomojiKeyboardView(
            catalog: KaomojiCatalog(version: "test", items: [
                KaomojiItem(text: "(^_^)", name: "cười", category: .happy),
                KaomojiItem(text: "(T_T)", name: "khóc", category: .happy),
            ])
        )
        view.frame = CGRect(x: 0, y: 0, width: 402, height: 304)
        view.layoutIfNeeded()
        let layout = view.collectionView.collectionViewLayout
        let first = try? #require(layout.layoutAttributesForItem(at: IndexPath(item: 0, section: 0)))
        let second = try? #require(layout.layoutAttributesForItem(at: IndexPath(item: 1, section: 0)))
        #expect(first?.frame.minX == KaomojiKeyboardView.horizontalInset)
        #expect(
            second?.frame.minX
                == (first?.frame.maxX ?? 0) + KaomojiKeyboardView.interitemSpacing
        )
    }

    @Test("Browser sits above the bottom bar")
    func layout() {
        let view = makeView()
        view.frame = CGRect(x: 0, y: 0, width: 430, height: 304)
        view.layoutIfNeeded()
        #expect(view.collectionView.frame.maxY <= view.bottomBar.frame.minY)
        #expect(view.bottomBar.frame.maxY <= view.bounds.maxY)
    }

    private func makeView() -> KaomojiKeyboardView {
        KaomojiKeyboardView(catalog: KaomojiCatalog(version: "test", items: [smile, shrug]))
    }

    private func tap(_ label: String, in view: UIView) {
        buttons(in: view).first { $0.accessibilityLabel == label }?
            .sendActions(for: .touchUpInside)
    }

    private func buttons(in view: UIView) -> [UIButton] {
        view.subviews.flatMap { child in
            (child as? UIButton).map { [$0] } ?? buttons(in: child)
        }
    }
}
#endif
