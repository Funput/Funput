import XCTest
@testable import Funput

/// How a client earns direct writes, keeps them across activations, and loses them
/// (`ClientTrust`), seen through the session that asks.
@MainActor
final class CompositionTrustTests: XCTestCase {
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

    func testTheCharacterBeforeTheCaretProvesTheClientBeforeAnythingIsWritten() {
        var session = makeSession()
        let surface = provenField()
        _ = session.show("t", on: surface)
        XCTAssertEqual(surface.calls.last, .insert("t", nil))
        XCTAssertEqual(surface.document, "x t")
    }

    func testInAnEmptyFieldTheMarkedFirstLetterProvesTheClient() {
        var session = makeSession()
        let surface = FakeCompositionSurface()
        _ = session.show("t", on: surface)
        XCTAssertEqual(surface.calls.last, .mark("t", nil))
        _ = session.show("ti", on: surface)
        XCTAssertEqual(surface.calls.last, .insert("ti", nil), "typing commits over the marked text")
        _ = session.show("tì", on: surface)
        XCTAssertEqual(surface.document, "tì")
    }

    func testABlindClientStaysMarkedAndIsAskedOnlyAFewTimes() {
        // Warp: claims document access, answers (0, 0), hands over no text.
        var session = makeSession()
        let surface = FakeCompositionSurface()
        surface.blind = true
        for word in ["t", "ti", "tie", "tiê", "tiên", "tiến", "tiếng"] {
            XCTAssertTrue(session.show(word, on: surface))
        }
        XCTAssertEqual(surface.calls.last, .mark("tiếng", nil))
        XCTAssertEqual(surface.reads.count, ClientTrust.probesPerActivation)
        session.commit("tiếng", boundary: " ", on: surface)
        XCTAssertEqual(surface.document, "tiếng ")
    }

    func testAProofIsRememberedForTheApp() {
        var session = makeSession()
        let surface = provenField()
        _ = session.show("t", on: surface)
        var next = makeSession()
        let other = FakeCompositionSurface()
        _ = next.show("a", on: other)
        XCTAssertEqual(other.calls, [.supportsDocumentAccess, .insert("a", nil)], "no proof asked again")
    }

    func testARememberedClientThatCannotPlaceAWordMustProveItselfAgain() {
        memory.insert("app.funput.tests.client")
        var session = makeSession()
        let surface = FakeCompositionSurface()
        surface.blind = true
        _ = session.show("ti", on: surface)
        XCTAssertFalse(session.show("tì", on: surface))
        XCTAssertFalse(memory.contains("app.funput.tests.client"))
        _ = session.show("a", on: surface)
        XCTAssertEqual(surface.calls.last, .mark("a", nil))
    }

    func testTheClientIsAskedOncePerActivation() {
        var session = makeSession()
        let surface = provenField()
        _ = session.show("a", on: surface)
        session.commit("a", boundary: " ", on: surface)
        _ = session.show("b", on: surface)
        session.reset()
        _ = session.show("c", on: surface)
        XCTAssertEqual(surface.calls.filter { $0 == .supportsDocumentAccess }.count, 2)
    }

    func testAClientThatLosesItsCaretIsMarkedUntilFocusMoves() {
        var session = makeSession()
        let surface = provenField()
        _ = session.show("ti", on: surface)
        surface.placesCaret = false
        XCTAssertFalse(session.show("tì", on: surface))
        _ = session.show("m", on: surface)
        XCTAssertEqual(surface.calls.last, .mark("m", nil))
        session.commit("m", boundary: " ", on: surface)
        session.reset()
        surface.placesCaret = true
        _ = session.show("y", on: surface)
        XCTAssertEqual(surface.calls.last, .insert("y", nil), "proven again on the next activation")
    }

    /// English words are held only for gõ tắt; Warp ignores Tab and the arrows while a
    /// composition is open, so an app that never proves itself gets plain keys instead.
    func testOptionalWordsAreHeldOnlyWhereTheyWouldBeWrittenDirectly() {
        var session = makeSession()
        XCTAssertTrue(session.allowsOptionalComposition(on: provenField()))

        var blindSession = makeSession()
        let blind = FakeCompositionSurface(document: "x ")
        blind.blind = true
        blind.clientID = "dev.warp.Warp-Stable"
        XCTAssertFalse(blindSession.allowsOptionalComposition(on: blind))
        XCTAssertEqual(blind.calls.filter { $0 != .supportsDocumentAccess }, [.selection])

        switchOn = false
        var offSession = makeSession()
        XCTAssertTrue(offSession.allowsOptionalComposition(on: blind), "switched off: as before")
    }
}
