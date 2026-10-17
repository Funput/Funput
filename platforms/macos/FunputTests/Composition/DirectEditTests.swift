import XCTest
@testable import Funput

/// The arithmetic that keeps a direct word in step with the engine. A wrong range here
/// rewrites the user's own text, so every offset is pinned in UTF-16 units.
@MainActor
final class DirectEditTests: XCTestCase {
    private func edit(_ shown: String, _ new: String) -> DirectEdit.Edit? {
        DirectEdit.edit(from: shown, to: new)
    }

    func testALetterAddedAtTheEndIsTypedAtTheCaret() {
        XCTAssertEqual(edit("ti", "tie"), .init(text: "e", replacedTail: 0))
    }

    func testAShapeMarkRewritesFromTheChangedLetter() {
        // `tieng` + `e` in Telex: only `eng` changes, `ti` stays untouched.
        XCTAssertEqual(edit("tieng", "tiêng"), .init(text: "êng", replacedTail: 3))
    }

    func testBackspaceAndEmptyingShrinkTheWord() {
        XCTAssertEqual(edit("tiế", "ti"), .init(text: "", replacedTail: 1))
        XCTAssertEqual(edit("đ", ""), .init(text: "", replacedTail: 1))
    }

    func testNothingChangedMeansNoWrite() {
        XCTAssertNil(edit("tiếng", "tiếng"))
    }

    func testTailsCountUTF16UnitsNotCharacters() {
        // `😀` is one Character but two UTF-16 units; only the vowel after it is replaced.
        XCTAssertEqual(edit("😀a", "😀á"), .init(text: "á", replacedTail: 1))
    }

    func testADecomposedVowelIsReplacedWhole() {
        // The document holds `e` + U+0302 (two units); its tone must not split the pair.
        XCTAssertEqual(edit("tie\u{0302}", "tiế"), .init(text: "ế", replacedTail: 2))
    }

    func testAShortcutExpansionReplacesTheWholeTrigger() {
        XCTAssertEqual(edit("vn", "Việt Nam"), .init(text: "Việt Nam", replacedTail: 2))
    }

    func testTheWordEndsWhereTheSelectionStarts() {
        XCTAssertEqual(
            DirectEdit.wordRange(length: 5, selection: NSRange(location: 20, length: 3)),
            NSRange(location: 15, length: 5)
        )
        XCTAssertNil(DirectEdit.wordRange(length: 5, selection: NSRange(location: 4, length: 0)))
        XCTAssertNil(DirectEdit.wordRange(length: 1, selection: NSRange(location: NSNotFound, length: 0)))
    }

    func testARewriteCoversTheTailAndAnySelectedSuggestion() {
        let tail = DirectEdit.Edit(text: "êng", replacedTail: 3)
        XCTAssertEqual(
            DirectEdit.rewriteRange(tail, selection: NSRange(location: 15, length: 0)),
            NSRange(location: 12, length: 3)
        )
        XCTAssertEqual(
            DirectEdit.rewriteRange(tail, selection: NSRange(location: 15, length: 4)),
            NSRange(location: 12, length: 7)
        )
    }
}
