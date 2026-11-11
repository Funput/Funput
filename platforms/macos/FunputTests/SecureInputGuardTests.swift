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

    func testWithoutSecureInputTheFieldIsNeverAsked() {
        secureInput = false
        var guarded = makeGuard()
        let field = FakeCompositionSurface.passwordField()
        XCTAssertEqual(guarded.decide(typing: "a", on: field), .compose)
        XCTAssertEqual(field.calls, [])
    }

    func testAPasswordFieldTakesTheProbedKeyAndThenEveryKeyRaw() {
        var guarded = makeGuard()
        let field = FakeCompositionSurface.passwordField()
        XCTAssertEqual(guarded.decide(typing: "a", on: field), .typed)
        XCTAssertEqual(field.document, "a", "the probe typed the key once")
        XCTAssertEqual(guarded.decide(typing: "1", on: field), .handOver)
        XCTAssertEqual(guarded.decide(typing: nil, on: field), .handOver)
        XCTAssertEqual(field.calls, [.mark("a", nil), .holdsMarkedText], "asked once, never for the caret")
    }

    func testAFieldThatHoldsMarkedTextKeepsComposing() {
        var guarded = makeGuard()
        let field = FakeCompositionSurface(document: "x ")
        XCTAssertEqual(guarded.decide(typing: "t", on: field), .compose)
        XCTAssertEqual(field.document, "x ", "the probe is cleared before the key goes on")
        XCTAssertEqual(guarded.decide(typing: "i", on: field), .compose)
        XCTAssertEqual(field.calls, [.mark("t", nil), .holdsMarkedText, .mark("", nil)])
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
        let field = FakeCompositionSurface(document: "x ")
        _ = guarded.decide(typing: "t", on: field)
        secureInput = false
        XCTAssertEqual(guarded.decide(typing: "i", on: field), .compose)
        secureInput = true
        let password = FakeCompositionSurface.passwordField()
        XCTAssertEqual(guarded.decide(typing: "p", on: password), .typed)
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
        for key in ["\u{7F}", "\t", "\r", "\u{1B}", "\u{3}"] { XCTAssertNil(typed(key), key.debugDescription) }
        XCTAssertNil(typed("\u{F702}", [.function, .numericPad]), "arrow")
        XCTAssertNil(typed("\u{F704}"), "function key")
        XCTAssertNil(typed("v", .command))
        XCTAssertNil(typed("\u{1}", .control))
        XCTAssertNil(typed(""), "dead key")
        XCTAssertNil(typed(nil))
    }
}
