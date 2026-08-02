#if canImport(UIKit)
import Foundation
@testable import KeyboardRenderer
import Testing
import UIKit

@MainActor
@Suite("Clipboard panel")
struct ClipboardPanelTests {
    private let now = Date(timeIntervalSince1970: 1_800_000_000)

    @Test("Pinned and recent entries become two sections")
    func sections() {
        let view = makeView(entries: [entry("ghim", pinned: true), entry("thường")])
        #expect(view.numberOfSections(in: view.collectionView) == 2)
        #expect(view.collectionView(view.collectionView, numberOfItemsInSection: 0) == 1)
    }

    @Test("Selecting a row hands back the entry behind it")
    func selection() throws {
        let pick = entry("dán tôi")
        let view = makeView(entries: [pick])
        var selected: KeyboardClipboardEntry?
        view.onSelect = { selected = $0 }
        view.collectionView(view.collectionView, didSelectItemAt: IndexPath(item: 0, section: 0))
        #expect(selected == pick)
    }

    @Test("Bottom bar emits its actions")
    func bottomBarActions() {
        let view = makeView(entries: [entry("a")])
        var cleared = false
        var returned = false
        view.onClearAll = { cleared = true }
        view.onReturn = { returned = true }
        tap("Xoá tất cả", in: view)
        tap("Trở về bàn phím Funput", in: view)
        #expect(cleared)
        #expect(returned)
    }

    /// A bare empty list reads as a broken feature, so each reason gets its own words.
    @Test("Empty history explains itself instead of showing nothing")
    func emptyState() {
        let view = makeView(entries: [])
        #expect(!view.emptyStateView.isHidden)
        #expect(view.collectionView.isHidden)
        #expect(view.emptyState == .nothingSaved)
    }

    @Test("Without Full Access the panel says so rather than looking empty")
    func withoutFullAccess() {
        let view = ClipboardKeyboardView()
        view.apply(theme: .funputGlass, entries: [entry("a")], hasFullAccess: false, now: now)
        #expect(!view.emptyStateView.isHidden)
        #expect(view.emptyState == .needsFullAccess)
        #expect(view.numberOfSections(in: view.collectionView) == 0)
    }

    @Test("Clear-all is disabled while there is nothing to clear")
    func clearDisabledWhenEmpty() {
        let empty = makeView(entries: [])
        let filled = makeView(entries: [entry("a")])
        #expect(button("Xoá tất cả", in: empty)?.isEnabled == false)
        #expect(button("Xoá tất cả", in: filled)?.isEnabled == true)
    }

    @Test("Swiping a row from the trailing edge deletes that entry")
    func swipeToDelete() throws {
        let target = entry("xoá tôi")
        let view = makeView(entries: [entry("giữ lại"), target])
        var removed: KeyboardClipboardEntry?
        view.onRemove = { removed = $0 }

        let configuration = try #require(
            view.trailingSwipeActions(at: IndexPath(item: 1, section: 0))
        )
        let action = try #require(configuration.actions.first)
        #expect(action.style == .destructive)
        action.handler(action, UIView()) { _ in }
        #expect(removed == target)
    }

    @Test("An index path with no entry behind it offers no swipe action")
    func swipeOutOfRange() {
        let view = makeView(entries: [entry("a")])
        #expect(view.trailingSwipeActions(at: IndexPath(item: 9, section: 0)) == nil)
    }

    @Test("Rows sit above the bottom bar")
    func layout() {
        let view = makeView(entries: [entry("a")])
        view.frame = CGRect(x: 0, y: 0, width: 393, height: 300)
        view.layoutIfNeeded()
        #expect(view.collectionView.frame.maxY <= view.bottomBar.frame.minY)
        #expect(view.bottomBar.frame.maxY <= view.bounds.maxY)
    }

    private func makeView(entries: [KeyboardClipboardEntry]) -> ClipboardKeyboardView {
        let view = ClipboardKeyboardView()
        view.apply(theme: .funputGlass, entries: entries, now: now)
        return view
    }

    private func entry(_ text: String, pinned: Bool = false) -> KeyboardClipboardEntry {
        KeyboardClipboardEntry(id: UUID(), text: text, capturedAt: now, isPinned: pinned)
    }

    private func tap(_ label: String, in view: UIView) {
        button(label, in: view)?.sendActions(for: .touchUpInside)
    }

    private func button(_ label: String, in view: UIView) -> UIButton? {
        buttons(in: view).first { $0.accessibilityLabel == label }
    }

    private func buttons(in view: UIView) -> [UIButton] {
        view.subviews.flatMap { child in
            (child as? UIButton).map { [$0] } ?? buttons(in: child)
        }
    }
}
#endif
