import XCTest
@testable import Funput

/// The first filter every client's answers meet, and the one that decides whether an edit
/// gets placed on invented indices. Kept apart from `RetoneTests` because it is the half
/// `IMKRetoneDocument` used to hold, where nothing could reach it but a live app.
@MainActor
final class RetoneCaretTests: XCTestCase {
    private let noMarkedText = NSRange(location: NSNotFound, length: 0)

    func testACollapsedCaretIsWhereItSaysItIs() {
        XCTAssertEqual(Retone.caret(selected: NSRange(location: 5, length: 0), marked: noMarkedText), 5)
    }

    func testTheStartOfTheDocumentIsStillACaret() {
        // Refusing this one is `plan`'s job, not this function's: there is a caret here, it
        // simply has nothing in front of it.
        XCTAssertEqual(Retone.caret(selected: NSRange(location: 0, length: 0), marked: noMarkedText), 0)
    }

    func testASelectionIsNotACaret() {
        // Cursor's answer with `chào ` on screen and no selection at all. Backspace deletes
        // a selection outright, so there is no word for the caret to land on either way.
        XCTAssertNil(Retone.caret(selected: NSRange(location: 0, length: 1), marked: noMarkedText))
    }

    func testAClientThatCannotPlaceTheCaretIsRefused() {
        let unplaced = NSRange(location: NSNotFound, length: 0)

        XCTAssertNil(Retone.caret(selected: unplaced, marked: noMarkedText))
    }

    func testStrayMarkedTextIsLeftAlone() {
        let marked = NSRange(location: 2, length: 3)

        XCTAssertNil(Retone.caret(selected: NSRange(location: 5, length: 0), marked: marked))
    }

    func testAnEmptyMarkedRangeCountsAsNoMarkedText() {
        // Clients differ on how they say "nothing is marked" — some send `NSNotFound`,
        // others a zero-length range. Neither is a composition to keep clear of.
        let empty = NSRange(location: 2, length: 0)

        XCTAssertEqual(Retone.caret(selected: NSRange(location: 5, length: 0), marked: empty), 5)
    }
}
