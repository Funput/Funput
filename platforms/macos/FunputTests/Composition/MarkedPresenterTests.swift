import XCTest
@testable import Funput

/// `MarkedPresenter` is how Funput typed before direct composition, and still is wherever
/// it is off or unsupported. These pin its IMKit traffic to exactly what the controller
/// used to send, so the refactor leaves that path untouched.
@MainActor
final class MarkedPresenterTests: XCTestCase {
    func testTheWordIsMarkedAtTheCaretWithoutReadingTheDocument() {
        let surface = FakeCompositionSurface()
        var presenter = MarkedPresenter()
        XCTAssertEqual(presenter.show("tiế", on: surface), .shown)
        XCTAssertEqual(surface.calls, [.mark("tiế", nil)])
    }

    func testEmptyingTheWordClearsTheMarkedText() {
        let surface = FakeCompositionSurface()
        var presenter = MarkedPresenter()
        _ = presenter.show("", on: surface)
        XCTAssertEqual(surface.calls, [.mark("", nil)])
    }

    func testCommitInsertsTheWordAndBoundaryOverTheMarkedText() {
        let surface = FakeCompositionSurface()
        var presenter = MarkedPresenter()
        presenter.commit("tiếng", boundary: " ", on: surface)
        XCTAssertEqual(surface.calls, [.insert("tiếng ", nil)])
    }

    func testCommitAtEnterInsertsOnlyTheWord() {
        // Enter keeps its native action, so the boundary is not part of the text.
        let surface = FakeCompositionSurface()
        var presenter = MarkedPresenter()
        presenter.commit("tiếng", boundary: "", on: surface)
        XCTAssertEqual(surface.calls, [.insert("tiếng", nil)])
    }

    func testReopenMarksTheWordOverTheRangeRetoneChose() {
        let surface = FakeCompositionSurface(document: "Xin chào ")
        var presenter = MarkedPresenter()
        XCTAssertTrue(presenter.reopen("chào", replacing: NSRange(location: 4, length: 5), on: surface))
        XCTAssertEqual(surface.calls, [.mark("chào", NSRange(location: 4, length: 5))])
    }

    func testBackspaceRemarksTheShorterWordAndConsumesTheKey() {
        let surface = FakeCompositionSurface()
        var presenter = MarkedPresenter()
        XCTAssertTrue(presenter.backspace(to: "ti", on: surface))
        XCTAssertEqual(surface.calls, [.mark("ti", nil)])
    }

    func testMarkedTextNeverWritesIntoTheDocument() {
        XCTAssertFalse(MarkedPresenter().writesIntoDocument)
    }
}
