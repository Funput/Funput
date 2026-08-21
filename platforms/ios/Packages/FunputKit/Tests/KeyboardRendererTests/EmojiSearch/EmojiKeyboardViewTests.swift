#if canImport(UIKit)
@testable import KeyboardRenderer
import KeyboardLayout
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
        var openedKaomoji = false
        view.onEmojiSelected = { selected = $0 }
        view.onDelete = { deleted = true }
        view.onReturn = { returned = true }
        view.onKaomoji = { openedKaomoji = true }
        view.collectionView(view.collectionView, didSelectItemAt: IndexPath(item: 0, section: 0))
        buttons(in: view).first { $0.accessibilityLabel == "Xóa" }?.sendActions(for: .touchUpInside)
        buttons(in: view).first { $0.accessibilityLabel == "Trở về bàn phím Funput" }?
            .sendActions(for: .touchUpInside)
        buttons(in: view).first { $0.accessibilityLabel == "Biểu tượng kaomoji" }?
            .sendActions(for: .touchUpInside)
        #expect(selected == smile)
        #expect(deleted)
        #expect(returned)
        #expect(openedKaomoji)
    }

    @Test("Selecting a recent emoji uses the stable-recents callback")
    func recentSelection() {
        let view = makeView()
        var selectedCatalogItem: EmojiItem?
        var selectedRecentItem: EmojiItem?
        view.onEmojiSelected = { selectedCatalogItem = $0 }
        view.onRecentEmojiSelected = { selectedRecentItem = $0 }
        view.apply(theme: .funputGlass, recent: [smile])

        view.collectionView(view.collectionView, didSelectItemAt: IndexPath(item: 0, section: 0))

        #expect(selectedRecentItem == smile)
        #expect(selectedCatalogItem == nil)
        #expect(view.sections[0].items == [smile])
    }

    @Test("Search keyboard edits only local query")
    func localSearchInput() {
        let view = makeView()
        var selected: EmojiItem?
        view.onEmojiSelected = { selected = $0 }
        view.beginSearch()
        view.handleSearchKey(event("m", .character))
        view.handleSearchKey(event("", .space))
        view.handleSearchKey(event("a", .character))
        view.handleSearchKey(event("", .backspace, phase: .repeated))
        #expect(view.searchQuery == "m ")
        #expect(view.searchState == .editing)
        #expect(selected == nil)
    }

    @Test("Done expands results and reset returns to browsing")
    func stateTransitions() {
        let view = makeView()
        view.beginSearch()
        view.handleSearchKey(event("d", .character))
        view.handleSearchKey(event("", .enter))
        #expect(view.searchState == .showingResults)
        #expect(view.searchQuery == "d")
        view.reset()
        #expect(view.searchState == .browsing)
        #expect(view.searchQuery.isEmpty)
    }

    @Test("Selecting a result preserves the query")
    func searchSelection() {
        let view = makeView()
        var selected: EmojiItem?
        view.onEmojiSelected = { selected = $0 }
        view.beginSearch()
        view.handleSearchKey(event("d", .character))
        view.searchResults.onSelect?(smile)
        #expect(selected == smile)
        #expect(view.searchQuery == "d")
    }

    @Test("Search layout stays within the keyboard")
    func searchLayout() {
        let view = makeView()
        view.frame = CGRect(x: 0, y: 0, width: 430, height: 304)
        view.beginSearch()
        view.layoutIfNeeded()
        #expect(view.searchHeader.frame.maxY <= view.searchResults.frame.minY)
        #expect(view.searchResults.frame.maxY <= view.searchKeyboard.frame.minY)
        #expect(view.searchKeyboard.frame.maxY <= view.bounds.maxY)
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

    private func event(
        _ label: String,
        _ role: KeyRole,
        phase: KeyboardKeyEvent.Phase = .released
    ) -> KeyboardKeyEvent {
        KeyboardKeyEvent(
            key: KeySpec(id: "test-\(role.rawValue)", label: label, role: role),
            phase: phase
        )
    }
}
#endif
