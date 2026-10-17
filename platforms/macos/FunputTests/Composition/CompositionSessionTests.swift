import XCTest
@testable import Funput

/// Which presenter a word gets, and how long the session trusts a client.
@MainActor
final class CompositionSessionTests: XCTestCase {
    private var switchOn = true
    private var clickCount = 0
    private var clickReads = 0

    private func makeSession() -> CompositionSession {
        CompositionSession(
            directEnabled: { [unowned self] in switchOn },
            clicks: { [unowned self] in clickReads += 1; return clickCount }
        )
    }

    func testWithTheSwitchOffEveryWordIsMarkedAndTheClientIsNotAsked() {
        switchOn = false
        var session = makeSession()
        let surface = FakeCompositionSurface()
        XCTAssertTrue(session.show("t", on: surface))
        XCTAssertEqual(surface.calls, [.mark("t", nil)])
        XCTAssertEqual(clickReads, 0, "no click monitor for marked words")
    }

    func testAClientWithoutDocumentAccessGetsMarkedText() {
        var session = makeSession()
        let surface = FakeCompositionSurface()
        surface.acceptsReplacementRanges = false
        XCTAssertTrue(session.show("t", on: surface))
        XCTAssertEqual(surface.calls, [.supportsDocumentAccess, .mark("t", nil)])
    }

    func testAClientWithDocumentAccessGetsTheWordWritten() {
        var session = makeSession()
        let surface = FakeCompositionSurface()
        XCTAssertTrue(session.show("t", on: surface))
        XCTAssertEqual(surface.document, "t")
    }

    func testTheClientIsAskedOncePerActivation() {
        var session = makeSession()
        let surface = FakeCompositionSurface()
        _ = session.show("a", on: surface)
        session.commit("a", boundary: " ", on: surface)
        _ = session.show("b", on: surface)
        session.reset()
        _ = session.show("c", on: surface)
        XCTAssertEqual(surface.calls.filter { $0 == .supportsDocumentAccess }.count, 2)
    }

    func testAWordKeepsItsPresenterWhenTheSwitchFlips() {
        var session = makeSession()
        let surface = FakeCompositionSurface()
        _ = session.show("t", on: surface)
        switchOn = false
        _ = session.show("ti", on: surface)
        session.commit("ti", boundary: " ", on: surface)
        XCTAssertEqual(surface.document, "ti ")
        _ = session.show("x", on: surface)
        XCTAssertEqual(surface.calls.last, .mark("x", nil), "the next word follows the switch")
    }

    func testAnUnsupportedClientIsMarkedUntilFocusMoves() {
        var session = makeSession()
        let surface = FakeCompositionSurface()
        _ = session.show("ti", on: surface)
        surface.placesCaret = false
        XCTAssertFalse(session.show("tì", on: surface))
        XCTAssertTrue(session.show("x", on: surface))
        XCTAssertEqual(surface.calls.last, .mark("x", nil))
        session.reset()
        surface.placesCaret = true
        _ = session.show("y", on: surface)
        XCTAssertEqual(surface.calls.last, .insert("y", nil))
    }

    func testADroppedWordLeavesTheNextOneDirect() {
        var session = makeSession()
        let surface = FakeCompositionSurface()
        _ = session.show("ti", on: surface)
        surface.replaceDocument("tix")
        XCTAssertFalse(session.show("tì", on: surface))
        XCTAssertTrue(session.show("a", on: surface))
        XCTAssertEqual(surface.document, "tixa")
    }

    func testAClickEndsOnlyAWordWrittenIntoTheDocument() {
        var session = makeSession()
        let surface = FakeCompositionSurface()
        _ = session.show("t", on: surface)
        XCTAssertFalse(session.endIfClicked(), "no click yet")
        clickCount += 1
        XCTAssertTrue(session.endIfClicked())
        XCTAssertFalse(session.endIfClicked(), "the word is already over")

        switchOn = false
        _ = session.show("m", on: surface)
        clickCount += 1
        XCTAssertFalse(session.endIfClicked(), "IMKit commits marked text on a click itself")
    }

    func testCommittingWithNoWordOnScreenIsPlainTyping() {
        var session = makeSession()
        let surface = FakeCompositionSurface()
        session.commit("", boundary: "", on: surface)
        XCTAssertEqual(surface.calls, [])
        session.commit("a", boundary: ".", on: surface)
        XCTAssertEqual(surface.calls, [.insert("a.", nil)])
    }

    func testReopenFollowsTheSameChoiceAsANewWord() {
        var session = makeSession()
        let surface = FakeCompositionSurface(document: "chao ")
        session.reopen("chao", replacing: NSRange(location: 0, length: 5), on: surface)
        XCTAssertTrue(session.show("chào", on: surface))
        XCTAssertEqual(surface.document, "chào")
    }
}
