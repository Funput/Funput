import XCTest
@testable import Funput

/// `DirectPresenter` writes into someone else's document, so the rule these pin is: rewrite
/// only the word Funput wrote, found right before the caret, and otherwise touch nothing.
@MainActor
final class DirectPresenterTests: XCTestCase {
    private func typed(_ steps: [String], into surface: FakeCompositionSurface) -> DirectPresenter {
        var presenter = DirectPresenter()
        for step in steps { XCTAssertEqual(presenter.show(step, on: surface), .shown) }
        return presenter
    }

    func testLettersAddedAtTheEndAreTypedWithoutReadingTheDocument() {
        let surface = FakeCompositionSurface(document: "Xin ")
        _ = typed(["t", "ti", "tie"], into: surface)
        XCTAssertEqual(surface.document, "Xin tie")
        XCTAssertEqual(surface.reads, [])
    }

    func testAToneMarkRewritesOnlyTheChangedTail() {
        let surface = FakeCompositionSurface(document: "Xin ")
        _ = typed(["t", "ti", "tie", "tien", "tieng", "tiêng", "tiếng"], into: surface)
        XCTAssertEqual(surface.document, "Xin tiếng")
        XCTAssertEqual(surface.calls.last, .insert("ếng", NSRange(location: 6, length: 3)))
    }

    func testBackspaceShrinksTheWordInPlace() {
        let surface = FakeCompositionSurface()
        var presenter = typed(["d", "đ", "đi"], into: surface)
        XCTAssertEqual(presenter.show("đ", on: surface), .shown)
        XCTAssertEqual(presenter.show("", on: surface), .shown)
        XCTAssertEqual(surface.document, "")
    }

    func testAClientThatRenumbersBetweenKeysStillGetsTheRightRange() {
        // Cursor: the word started at 1 in its text area, then the area was rebuilt.
        let surface = FakeCompositionSurface(document: "a ")
        var presenter = typed(["t", "ti", "tie"], into: surface)
        surface.windowStart = 2
        XCTAssertEqual(presenter.show("tiê", on: surface), .shown)
        XCTAssertEqual(surface.document, "a tiê")
    }

    func testARewriteReplacesTheInlineSuggestionAfterTheWord() {
        let surface = FakeCompositionSurface()
        var presenter = typed(["t", "ti", "tie", "tien", "tieng"], into: surface)
        surface.replaceDocument("tieng Anh")
        surface.caret = 5
        surface.selectionLength = 4
        XCTAssertEqual(presenter.show("tiêng", on: surface), .shown)
        XCTAssertEqual(surface.document, "tiêng")
    }

    func testTextChangedBehindFunputIsLeftAlone() {
        let surface = FakeCompositionSurface()
        var presenter = typed(["t", "ti", "tie"], into: surface)
        surface.replaceDocument("tied")
        XCTAssertEqual(presenter.show("tiê", on: surface), .dropped)
        XCTAssertEqual(surface.document, "tied")
    }

    func testACaretTooCloseToTheStartIsAMoveNotAnUnsupportedClient() {
        // A stale caret right after re-opening a word: drop this word, keep direct mode.
        let surface = FakeCompositionSurface()
        var presenter = typed(["c", "ch", "cha", "chao"], into: surface)
        surface.caret = 3
        XCTAssertEqual(presenter.show("chào", on: surface), .dropped)
    }

    func testAClientThatCannotPlaceItsCaretIsUnsupported() {
        let surface = FakeCompositionSurface()
        var presenter = typed(["t", "ti", "tie"], into: surface)
        surface.placesCaret = false
        XCTAssertEqual(presenter.show("tiê", on: surface), .unsupported)
        XCTAssertEqual(surface.document, "tie")
    }

    func testCommittingAnUnchangedWordOnlyTypesTheBoundary() {
        let surface = FakeCompositionSurface()
        var presenter = typed(["v", "vi"], into: surface)
        surface.clearCalls()
        presenter.commit("vi", boundary: " ", on: surface)
        XCTAssertEqual(surface.calls, [.insert(" ", nil)])
    }

    func testCommittingAShortcutWritesItsExpansionThenTheBoundary() {
        let surface = FakeCompositionSurface()
        var presenter = typed(["v", "vn"], into: surface)
        presenter.commit("Việt Nam", boundary: " ", on: surface)
        XCTAssertEqual(surface.document, "Việt Nam ")
    }

    func testARefusedFinalRewriteStillTypesTheBoundary() {
        let surface = FakeCompositionSurface()
        var presenter = typed(["v", "vn"], into: surface)
        surface.replaceDocument("vnx")
        presenter.commit("Việt Nam", boundary: " ", on: surface)
        XCTAssertEqual(surface.document, "vnx ")
    }

    func testAReopenedWordCanBeRetonedInPlace() {
        let surface = FakeCompositionSurface(document: "chao ")
        var presenter = DirectPresenter()
        presenter.reopen("chao", replacing: NSRange(location: 0, length: 5), on: surface)
        XCTAssertEqual(presenter.show("chào", on: surface), .shown)
        XCTAssertEqual(surface.document, "chào")
    }
}
