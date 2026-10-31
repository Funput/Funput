#if canImport(UIKit)
@testable import KeyboardRenderer
import Testing
import UIKit

@MainActor
@Suite("Long-press menu on suggested words")
struct SuggestionBarMenuTests {
    private func bar(
        _ candidates: [KeyboardSuggestionCandidate],
        onEdit: ((KeyboardSuggestionCandidate, KeyboardSuggestionAction) -> Void)? = { _, _ in }
    ) -> KeyboardSuggestionBarView {
        let bar = KeyboardSuggestionBarView(frame: CGRect(x: 0, y: 0, width: 360, height: 44))
        bar.onEdit = onEdit
        bar.update(candidates)
        return bar
    }

    @Test("A stored word offers both edits, and each reports its word")
    func storedWordOffersEdits() throws {
        let word = KeyboardSuggestionCandidate(text: "zalo", generation: 7)
        var edits: [KeyboardSuggestionAction] = []
        let bar = bar([word]) { candidate, action in
            #expect(candidate == word)
            edits.append(action)
        }

        let menu = try #require(bar.menu(forSlot: 0))
        #expect(menu.title == "zalo")
        #expect(menu.children.map(\.title) == ["Không gợi ý từ này", "Quên từ này"])
        let forget = try #require(menu.children.last as? UIAction)
        #expect(forget.attributes.contains(.destructive))

        let actions = try #require(bar.accessibilityActions(forSlot: 0))
        actions.forEach { _ = $0.actionHandler?($0) }
        #expect(edits == [.block, .forget])
    }

    @Test("Corrections, undo chips and empty slots offer nothing")
    func onlyStoredWordsHaveAMenu() {
        let bar = bar([
            KeyboardSuggestionCandidate(text: "anh", generation: 1, kind: .correctionCandidate),
            KeyboardSuggestionCandidate(text: "vn", generation: 1, kind: .shortcutUndo),
        ])
        #expect(bar.menu(forSlot: 0) == nil)
        #expect(bar.menu(forSlot: 1) == nil)
        #expect(bar.menu(forSlot: 2) == nil)
        #expect(bar.accessibilityActions(forSlot: 0) == nil)
    }

    @Test("Without a host to apply edits, no slot offers a menu")
    func noHostNoMenu() {
        let bar = bar([KeyboardSuggestionCandidate(text: "zalo", generation: 1)], onEdit: nil)
        #expect(bar.menu(forSlot: 0) == nil)
    }
}
#endif
