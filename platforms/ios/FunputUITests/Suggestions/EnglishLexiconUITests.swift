import XCTest

/// Exercises the real extension, including both controller and token-tracking gates.
final class EnglishLexiconUITests: XCTestCase {
    override func setUpWithError() throws {
        continueAfterFailure = false
        XCUIDevice.shared.orientation = .portrait
    }

    @MainActor
    override func tearDown() {
        ShortcutKeyboardUITestSupport.cleanup()
        super.tearDown()
    }

    @MainActor
    func testDictionarySuggestionInEnglishMode() {
        assertDictionarySuggestion(english: true)
    }

    @MainActor
    func testDictionarySuggestionInVietnameseMode() {
        assertDictionarySuggestion(english: false)
    }

    @MainActor
    private func assertDictionarySuggestion(english: Bool) {
        let app = ShortcutKeyboardUITestSupport.launch(english: english)
        let field = ShortcutKeyboardUITestSupport.openField(app)
        // Confirm the actual extension language, rather than trusting the harness preference.
        ShortcutKeyboardUITestSupport.type("a1 ", app: app)
        let prefix = english ? "a1 " : "á "
        ShortcutsUITestSupport.waitForValue(field, prefix)
        ShortcutKeyboardUITestSupport.type("wh", app: app)
        let candidate = app.buttons["Gợi ý, which"]
        XCTAssertTrue(candidate.waitForExistence(timeout: 5))
        candidate.tap()
        ShortcutsUITestSupport.waitForValue(field, prefix + "which ")
    }
}
