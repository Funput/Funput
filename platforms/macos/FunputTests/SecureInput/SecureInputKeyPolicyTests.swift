import AppKit
import XCTest
@testable import Funput

/// What the secure-field guard rests on: which keys may probe a field, and why a password
/// field must not be given marked text at all (#720).
@MainActor
final class SecureInputKeyPolicyTests: XCTestCase {
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
