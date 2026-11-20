import XCTest
@testable import Funput

/// When the text an expansion wrote is provably right before the caret, and every way a
/// client can fail to prove it.
@MainActor
final class ShortcutUndoDocumentTests: XCTestCase {
    private let written = "Việt Nam "

    func testFindsTheExpansionRightBeforeTheCaret() {
        let field = FakeCompositionSurface(document: "Hôm nay Việt Nam ")
        XCTAssertEqual(ShortcutUndoDocument.range(of: written, on: field), NSRange(location: 8, length: 9))
    }

    func testFindsAnExpansionThatIsTheWholeDocument() {
        let field = FakeCompositionSurface(document: written)
        XCTAssertEqual(ShortcutUndoDocument.range(of: written, on: field), NSRange(location: 0, length: 9))
    }

    func testRefusesOtherTextBeforeTheCaret() {
        XCTAssertNil(ShortcutUndoDocument.range(of: written, on: FakeCompositionSurface(document: "Việt Nam,")))
        XCTAssertNil(ShortcutUndoDocument.range(of: written, on: FakeCompositionSurface(document: "Việt Nam x")))
    }

    func testRefusesADocumentShorterThanTheExpansion() {
        XCTAssertNil(ShortcutUndoDocument.range(of: written, on: FakeCompositionSurface(document: "Nam ")))
    }

    func testRefusesAClientThatIgnoresReplacementRanges() {
        let field = FakeCompositionSurface(document: written)
        field.acceptsReplacementRanges = false
        XCTAssertNil(ShortcutUndoDocument.range(of: written, on: field))
        XCTAssertTrue(field.reads.isEmpty, "nothing is read from a client that cannot be edited")
    }

    func testRefusesABlindClient() {
        let field = FakeCompositionSurface(document: written)
        field.blind = true
        XCTAssertNil(ShortcutUndoDocument.range(of: written, on: field))
    }

    func testRefusesAClientThatCannotPlaceItsCaret() {
        let field = FakeCompositionSurface(document: written)
        field.placesCaret = false
        XCTAssertNil(ShortcutUndoDocument.range(of: written, on: field))
    }

    func testRefusesASelection() {
        let field = FakeCompositionSurface(document: written + "x")
        field.caret = written.utf16.count
        field.selectionLength = 1
        XCTAssertNil(ShortcutUndoDocument.range(of: written, on: field))
    }

    func testRefusesMarkedTextThatIsNotFunputs() {
        let field = FakeCompositionSurface(document: written)
        field.mark("ab", replacing: nil)
        XCTAssertNil(ShortcutUndoDocument.range(of: written, on: field))
    }
}
