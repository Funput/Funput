import XCTest
@testable import Funput

/// Backspace right after a gõ tắt expansion, driven the way `FunputInputController` drives
/// it: every key reaches the engine, then `keyHandled`; the field holds what the
/// composition session would have written.
@MainActor
final class ShortcutUndoSessionTests: XCTestCase {
    private var clicks = 0
    private var composer = FunputComposer()
    private lazy var undo = ShortcutUndoSession(clicks: { [unowned self] in clicks })

    override func setUp() {
        super.setUp()
        clicks = 0
        composer = FunputComposer()
        composer.addShortcut(trigger: "vn", expansion: "Việt Nam")
    }

    /// `field` is the document as it stands after these keys: where an expansion ends.
    private func type(_ keys: String, on field: FakeCompositionSurface = FakeCompositionSurface()) {
        for key in keys.unicodeScalars {
            composer.process(key)
            undo.keyHandled(by: composer, on: field)
        }
    }

    private func backspace(on field: FakeCompositionSurface) -> Bool {
        defer { undo.keyHandled(by: composer, on: field) }
        return undo.backspace(composer: composer, on: field)
    }

    func testBackspaceRightAfterAnExpansionPutsTheTriggerBack() {
        type("vn ")
        let field = FakeCompositionSurface(document: "Việt Nam ")

        XCTAssertTrue(backspace(on: field))
        XCTAssertEqual(field.document, "vn ")
        XCTAssertEqual(field.calls.filter { if case .insert = $0 { true } else { false } },
                       [.insert("vn ", NSRange(location: 0, length: 9))])
        XCTAssertFalse(composer.hasShortcutUndo)
    }

    func testTheUndoKeepsTheCaseTheTriggerWasTypedIn() {
        type("Vn ")
        let field = FakeCompositionSurface(document: "Việt Nam ")
        XCTAssertTrue(backspace(on: field))
        XCTAssertEqual(field.document, "Vn ")
    }

    func testOnlyTheFirstBackspaceIsTheUndo() {
        type("vn ")
        let field = FakeCompositionSurface(document: "Việt Nam ")
        XCTAssertTrue(backspace(on: field))
        field.clearCalls()
        XCTAssertFalse(backspace(on: field))
        XCTAssertTrue(field.calls.isEmpty, "a spent undo asks the app nothing")
    }

    func testTheTriggerTakenBackIsKeptOnceWhenTypedAgain() {
        type("vn ")
        XCTAssertTrue(backspace(on: FakeCompositionSurface(document: "Việt Nam ")))
        type("vn")
        let kept = composer.process(" ")
        XCTAssertNotEqual(FunputComposer.output(of: kept), "Việt Nam ")
        XCTAssertFalse(composer.hasShortcutUndo)
    }

    func testAnotherKeySpendsTheUndo() {
        type("vn x")
        XCTAssertFalse(composer.hasShortcutUndo)
        XCTAssertFalse(backspace(on: FakeCompositionSurface(document: "Việt Nam x")))
    }

    func testAClickSinceTheExpansionDropsTheUndo() {
        type("vn ")
        clicks += 1
        let field = FakeCompositionSurface(document: "Việt Nam ")
        XCTAssertFalse(backspace(on: field))
        XCTAssertEqual(field.document, "Việt Nam ")
        XCTAssertTrue(field.reads.isEmpty, "the click alone rules the undo out")
        XCTAssertFalse(composer.hasShortcutUndo)
    }

    func testADocumentThatMovedOnDropsTheUndo() {
        type("vn ")
        let field = FakeCompositionSurface(document: "Việt Nam đã")
        XCTAssertFalse(backspace(on: field))
        XCTAssertEqual(field.document, "Việt Nam đã")
        XCTAssertFalse(composer.hasShortcutUndo, "dropped, not left for a later Backspace")
    }

    /// Enter keeps its native action, so what the engine wrote (`Việt Nam\r`) is not what
    /// the document holds, and there is nothing to take back.
    func testEnterLeavesNothingToTakeBack() {
        type("vn\r")
        let field = FakeCompositionSurface(document: "Việt Nam\n")
        XCTAssertFalse(backspace(on: field))
        XCTAssertEqual(field.document, "Việt Nam\n")
    }

    func testEnglishModeTakesTheExpansionBackToo() {
        composer.apply(.init(enabled: false, shortcutsEnabled: true, shortcutsInEnglish: true))
        type("vn ")
        let field = FakeCompositionSurface(document: "Việt Nam ")
        XCTAssertTrue(backspace(on: field))
        XCTAssertEqual(field.document, "vn ")
    }

    /// #782: typed on past the expansion, then deleted back to its end.
    func testBackspaceBackAtTheExpansionAfterMoreWordsUndoesIt() {
        type("vn ", on: FakeCompositionSurface(document: "Việt Nam "))
        type("nhe ")
        XCTAssertFalse(backspace(on: FakeCompositionSurface(document: "Việt Nam nhe ")))

        let field = FakeCompositionSurface(document: "Việt Nam ")
        XCTAssertTrue(backspace(on: field))
        XCTAssertEqual(field.document, "vn ")
        XCTAssertFalse(backspace(on: FakeCompositionSurface(document: "vn ")), "taken once")
    }

    func testTheSameTextFurtherOnIsNotTakenBack() {
        type("vn ", on: FakeCompositionSurface(document: "Việt Nam "))
        type("x ")
        let field = FakeCompositionSurface(document: "Việt Nam x Việt Nam ")
        XCTAssertFalse(backspace(on: field))
        XCTAssertEqual(field.document, "Việt Nam x Việt Nam ")
    }

    func testAClickBeforeDeletingBackDropsTheExpansion() {
        type("vn ", on: FakeCompositionSurface(document: "Việt Nam "))
        type("x ")
        clicks += 1
        let field = FakeCompositionSurface(document: "Việt Nam ")
        XCTAssertFalse(backspace(on: field))
        XCTAssertTrue(field.reads.isEmpty, "the click alone rules the undo out")
    }

    func testAClientThatWouldNotPlaceItsCaretGetsNoSecondChance() {
        let blind = FakeCompositionSurface(document: "Việt Nam ")
        blind.placesCaret = false
        type("vn ", on: blind)
        type("x ")
        XCTAssertFalse(backspace(on: FakeCompositionSurface(document: "Việt Nam ")))
    }

    func testAClearedComposerHasNothingToTakeBack() {
        type("vn ", on: FakeCompositionSurface(document: "Việt Nam "))
        type("x ")
        composer.clear()
        let field = FakeCompositionSurface(document: "Việt Nam ")
        XCTAssertFalse(backspace(on: field))
        XCTAssertEqual(field.document, "Việt Nam ")
        let later = FakeCompositionSurface(document: "Việt Nam ")
        XCTAssertFalse(backspace(on: later))
        XCTAssertTrue(later.calls.isEmpty, "forgotten, so the app is not asked again")
    }

    func testWithNothingArmedTheAppIsNeverAsked() {
        type("chaof ")
        let field = FakeCompositionSurface(document: "chào ")
        XCTAssertFalse(backspace(on: field))
        XCTAssertTrue(field.calls.isEmpty)
    }
}
