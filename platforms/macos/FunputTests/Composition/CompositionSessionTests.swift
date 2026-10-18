import XCTest
@testable import Funput

/// Which presenter a word gets, and how long it keeps it.
@MainActor
final class CompositionSessionTests: XCTestCase {
    private var switchOn = true
    private var clickCount = 0
    private var clickReads = 0
    private let memory = ProvenClients()

    private func makeSession() -> CompositionSession {
        CompositionSession(
            directEnabled: { [unowned self] in switchOn },
            clicks: { [unowned self] in clickReads += 1; return clickCount },
            memory: memory
        )
    }

    /// A field with text before the caret, which proves the client as the first word starts.
    private func provenField() -> FakeCompositionSurface {
        FakeCompositionSurface(document: "x ")
    }

    func testWithTheSwitchOffEveryWordIsMarkedAndTheClientIsNotAsked() {
        switchOn = false
        var session = makeSession()
        let surface = provenField()
        XCTAssertTrue(session.show("t", on: surface))
        XCTAssertEqual(surface.calls, [.mark("t", nil)])
        XCTAssertEqual(clickReads, 0, "no click monitor for marked words")
    }

    func testAClientWithoutDocumentAccessGetsMarkedText() {
        var session = makeSession()
        let surface = provenField()
        surface.acceptsReplacementRanges = false
        _ = session.show("t", on: surface)
        XCTAssertEqual(surface.calls, [.supportsDocumentAccess, .mark("t", nil)])
    }

    func testAWordKeepsItsPresenterWhenTheSwitchFlips() {
        var session = makeSession()
        let surface = provenField()
        _ = session.show("t", on: surface)
        switchOn = false
        _ = session.show("ti", on: surface)
        session.commit("ti", boundary: " ", on: surface)
        XCTAssertEqual(surface.document, "x ti ")
        _ = session.show("m", on: surface)
        XCTAssertEqual(surface.calls.last, .mark("m", nil), "the next word follows the switch")
    }

    func testADroppedWordLeavesTheNextOneDirect() {
        var session = makeSession()
        let surface = provenField()
        _ = session.show("ti", on: surface)
        surface.replaceDocument("x tix")
        XCTAssertFalse(session.show("tì", on: surface))
        XCTAssertTrue(session.show("a", on: surface))
        XCTAssertEqual(surface.document, "x tixa")
    }

    func testAClickEndsOnlyAWordWrittenIntoTheDocument() {
        var session = makeSession()
        let surface = provenField()
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
        let surface = provenField()
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
