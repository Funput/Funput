import AppKit
import XCTest
@testable import Funput

/// Password fields that refuse marked text get raw keys (#720). Secure input alone does not
/// decide it: the focused field is asked whether it holds the first key marked.
@MainActor
final class SecureInputGuardTests: XCTestCase {
    private var secureInput = true

    private func makeGuard() -> SecureInputGuard {
        SecureInputGuard(secureInputEnabled: { [unowned self] in secureInput })
    }

    func testWithoutSecureInputNothingIsAskedOrEnded() {
        secureInput = false
        var guarded = makeGuard()
        let field = FakeCompositionSurface.passwordField()
        let decision = guarded.decide(typing: "a", on: field, composing: unexpected("composing read")) {
            XCTFail("word ended")
        }
        XCTAssertEqual(decision, .compose)
        XCTAssertEqual(field.calls, [])
    }

    /// The usual typing path must not pay for the engine's buffer on every key.
    private func unexpected(_ what: String) -> Bool {
        XCTFail(what)
        return false
    }

    func testAPasswordFieldTakesTheProbedKeyAndThenEveryKeyRaw() {
        var guarded = makeGuard()
        let field = FakeCompositionSurface.passwordField()
        XCTAssertEqual(guarded.decide(typing: "a", on: field), .typed)
        XCTAssertEqual(field.document, "a", "the probe typed the key once")
        XCTAssertEqual(guarded.decide(typing: "1", on: field), .handOver)
        XCTAssertEqual(guarded.decide(typing: "b", on: field, composing: true), .handOver)
        XCTAssertEqual(guarded.decide(typing: nil, on: field), .handOver)
        XCTAssertEqual(field.calls, [.mark("a", nil), .markedRange], "asked once, never for the caret")
    }

    func testAFieldThatHoldsMarkedTextKeepsComposing() {
        var guarded = makeGuard()
        let field = FakeCompositionSurface(document: "x ")
        XCTAssertEqual(guarded.decide(typing: "t", on: field), .compose)
        XCTAssertEqual(field.document, "x ", "the probe is cleared before the key goes on")
        XCTAssertEqual(guarded.decide(typing: "i", on: field, composing: true) { XCTFail("word ended") }, .compose)
        XCTAssertEqual(guarded.decide(typing: nil, on: field), .compose, "Backspace between words")
        XCTAssertEqual(field.calls, [.mark("t", nil), .markedRange, .mark("", nil)])
    }

    /// Warp shows marked text but never reports it: the probe must not be left on screen,
    /// and the field composes as it did before the guard existed.
    func testAFieldThatNeverReportsMarkedTextKeepsComposing() {
        var guarded = makeGuard()
        let field = FakeCompositionSurface.unreportedMarksField()
        XCTAssertEqual(guarded.decide(typing: "a", on: field), .compose)
        XCTAssertNil(field.shownMarkedText, "the probe was cleared")
        XCTAssertEqual(field.document, "")
        XCTAssertEqual(field.calls, [.mark("a", nil), .markedRange, .mark("", nil)])
    }

    func testAWordInProgressEndsBeforeTheFieldIsAsked() {
        var guarded = makeGuard()
        let field = FakeCompositionSurface()
        let decision = guarded.decide(typing: "t", on: field, composing: true) { field.insert("vie", replacing: nil) }
        XCTAssertEqual(decision, .compose)
        XCTAssertEqual(field.calls, [.insert("vie", nil), .mark("t", nil), .markedRange, .mark("", nil)])
        XCTAssertEqual(field.document, "vie")
    }

    func testAWordInProgressEndsBeforeAKeyThatTypesNothingIsHandedOver() {
        var guarded = makeGuard()
        var ended = false
        let decision = guarded.decide(typing: nil, on: FakeCompositionSurface(), composing: true) { ended = true }
        XCTAssertEqual(decision, .handOver)
        XCTAssertTrue(ended)
    }

    /// One client can hold several fields: a field that held the last word may not be the
    /// one the next word goes to.
    func testANewWordAsksAgainAfterAFieldThatHeldMarkedText() {
        var guarded = makeGuard()
        XCTAssertEqual(guarded.decide(typing: "t", on: FakeCompositionSurface()), .compose)
        let password = FakeCompositionSurface.passwordField()
        XCTAssertEqual(guarded.decide(typing: "p", on: password), .typed)
        XCTAssertEqual(password.document, "p")
    }

    func testTypingOverASelectionCountsAsTyped() {
        var guarded = makeGuard()
        let field = FakeCompositionSurface.passwordField()
        field.replaceDocument("old")
        field.caret = 0
        field.selectionLength = 3
        XCTAssertEqual(guarded.decide(typing: "a", on: field), .typed)
        XCTAssertEqual(field.document, "a")
    }

    func testUntilTheFieldAnswersAKeyThatTypesNothingReachesItUntouched() {
        var guarded = makeGuard()
        let field = FakeCompositionSurface.passwordField()
        XCTAssertEqual(guarded.decide(typing: nil, on: field), .handOver)
        XCTAssertEqual(field.calls, [])
    }

    func testTheFieldIsAskedAgainOnceSecureInputEnds() {
        var guarded = makeGuard()
        _ = guarded.decide(typing: "a", on: FakeCompositionSurface.passwordField())
        secureInput = false
        XCTAssertEqual(guarded.decide(typing: "i", on: FakeCompositionSurface()), .compose)
        secureInput = true
        XCTAssertEqual(guarded.decide(typing: "t", on: FakeCompositionSurface()), .compose)
    }

    func testFocusOnAnotherClientAsksAgain() {
        var guarded = makeGuard()
        _ = guarded.decide(typing: "a", on: FakeCompositionSurface.passwordField())
        guarded.reset()
        XCTAssertEqual(guarded.decide(typing: "t", on: FakeCompositionSurface()), .compose)
    }

    /// Why the guard exists: a marked word in a password field ends with its first letter
    /// typed twice.
    func testMarkedTextInAPasswordFieldTypesTheFirstLetterTwice() {
        let field = FakeCompositionSurface.passwordField()
        var presenter = MarkedPresenter()
        for word in ["a", "ab", "abc"] { _ = presenter.show(word, on: field) }
        presenter.commit("abc", boundary: "", on: field)
        XCTAssertEqual(field.document, "aabc")
    }

    /// Only a key that types text may be the probe: anything else would be typed, or eaten,
    /// in a field that is then left unanswered.
    func testOnlyKeysThatTypeTextAreProbed() {
        func typed(_ characters: String?, _ modifiers: NSEvent.ModifierFlags = []) -> String? {
            InputEventPolicy.typedText(characters, modifiers: modifiers)
        }
        XCTAssertEqual(typed("a"), "a")
        XCTAssertEqual(typed("A", .shift), "A")
        XCTAssertEqual(typed("å", .option), "å")
        XCTAssertEqual(typed(" "), " ")
        XCTAssertEqual(typed("e\u{301}"), "e\u{301}")
        for key in ["\u{7F}", "\t", "\r", "\u{1B}", "\u{3}"] { XCTAssertNil(typed(key), key.debugDescription) }
        XCTAssertNil(typed("\u{F702}", [.function, .numericPad]), "arrow")
        XCTAssertNil(typed("\u{F704}"), "function key")
        XCTAssertNil(typed("a\u{F702}"), "a function-key character after a printable one")
        XCTAssertNil(typed("v", .command))
        XCTAssertNil(typed("\u{1}", .control))
        XCTAssertNil(typed(""), "dead key")
        XCTAssertNil(typed(nil))
    }
}
