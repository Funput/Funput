import XCTest
@testable import Funput

/// A document that answers from a plain string, standing in for the focused app.
private struct FakeDocument: RetoneDocument {
    var content: String
    var caretLocation: Int?
    /// Hand back less than was asked for, the way a client with a partial text-input
    /// bridge does. The scan has to read that as "cannot be read", not guess at offsets.
    var truncateReads = false

    init(_ content: String, caret: Int? = nil, truncateReads: Bool = false) {
        self.content = content
        self.caretLocation = caret ?? content.utf16.count
        self.truncateReads = truncateReads
    }

    func caret() -> Int? { caretLocation }

    func text(in range: NSRange) -> String? {
        let units = Array(content.utf16)
        guard range.location >= 0, range.location + range.length <= units.count else { return nil }
        let end = range.location + (truncateReads ? max(0, range.length - 1) : range.length)
        return String(decoding: units[range.location..<end], as: UTF16.self)
    }
}

private func takeAnything(_: String) -> Bool { true }
private func takeNothing(_: String) -> Bool { false }

@MainActor
final class RetoneTests: XCTestCase {
    func testBackspaceOverASpaceReopensTheWordBeforeIt() {
        var asked: [String] = []
        let plan = Retone.plan(document: FakeDocument("chào ")) {
            asked.append($0)
            return true
        }

        // The range covers the word *and* the space: one edit, no gap in between.
        XCTAssertEqual(plan, Retone.Plan(word: "chào", replacement: NSRange(location: 0, length: 5)))
        XCTAssertEqual(asked, ["chào"])
    }

    func testBackspaceInsideAWordReopensWhatIsLeftOfIt() {
        let plan = Retone.plan(document: FakeDocument("chào"), adopt: takeAnything)

        XCTAssertEqual(plan, Retone.Plan(word: "chà", replacement: NSRange(location: 0, length: 4)))
    }

    func testOnlyTheWordAtTheCaretIsReopened() {
        let plan = Retone.plan(document: FakeDocument("xin chào "), adopt: takeAnything)

        XCTAssertEqual(plan, Retone.Plan(word: "chào", replacement: NSRange(location: 4, length: 5)))
    }

    func testARefusedWordLeavesTheDocumentAlone() {
        XCTAssertNil(Retone.plan(document: FakeDocument("hello "), adopt: takeNothing))
    }

    func testACaretOnASeparatorHasNoWordToReopen() {
        // Backspace here deletes the second space and lands the caret on the first.
        XCTAssertNil(Retone.plan(document: FakeDocument("chào  "), adopt: takeAnything))
    }

    func testDecomposedTextIsMeasuredAsItSitsInTheDocument() {
        let plan = Retone.plan(document: FakeDocument("cha\u{0300}o "), adopt: takeAnything)

        // Five UTF-16 units of word in the document, four in the composition that replaces
        // it: measuring the range on the precomposed form would eat the `c`.
        XCTAssertEqual(plan?.replacement, NSRange(location: 0, length: 6))
        XCTAssertEqual(plan?.word, "chào")
        XCTAssertEqual(plan?.word.utf16.count, 4)
    }

    func testAPartialReadIsNotGuessedAt() {
        let document = FakeDocument("chào ", truncateReads: true)

        XCTAssertNil(Retone.plan(document: document, adopt: takeAnything))
    }

    func testAClientThatCannotPlaceTheCaretIsLeftAlone() {
        var document = FakeDocument("chào ")
        document.caretLocation = nil

        XCTAssertNil(Retone.plan(document: document, adopt: takeAnything))
    }

    func testThereIsNothingBeforeTheStartOfTheDocument() {
        XCTAssertNil(Retone.plan(document: FakeDocument("a", caret: 0), adopt: takeAnything))
        // Caret 1 is also the second of Cursor's two answers (see `RetoneCaretTests`): a
        // caret that passes the first filter, on a document whose contents it misreports.
        // Only the character Backspace deletes lies before it, so there is no word — and
        // the client is never asked to prove it.
        XCTAssertNil(Retone.plan(document: FakeDocument("a", caret: 1), adopt: takeAnything))
    }

    func testAWordThatOverrunsTheLookbackIsLeftAlone() {
        let long = String(repeating: "a", count: Retone.lookback + 1)

        // The window starts mid-word, so its front is a fragment, not a word — taking it
        // would replace the wrong stretch of document.
        XCTAssertNil(Retone.plan(document: FakeDocument(long + " "), adopt: takeAnything))
    }

    /// End to end through the real engine: the word goes back in, the next key retones it.
    func testAdoptedWordIsRetonedByTheNextKeystroke() {
        let composer = FunputComposer()
        composer.apply(ComposerConfiguration())

        let plan = Retone.plan(document: FakeDocument("chào "), adopt: composer.adopt)

        XCTAssertEqual(plan?.word, "chào")
        composer.process("s")
        XCTAssertEqual(composer.buffer(), "cháo")
    }

    func testTheEngineRefusesAWordThatIsNotVietnamese() {
        let composer = FunputComposer()
        composer.apply(ComposerConfiguration())

        XCTAssertNil(Retone.plan(document: FakeDocument("hello "), adopt: composer.adopt))
        XCTAssertEqual(composer.buffer(), "")
    }
}
