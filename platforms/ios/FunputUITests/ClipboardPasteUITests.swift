import XCTest

/// Requires Funput enabled with Full Access on the selected simulator/device.
final class ClipboardPasteUITests: XCTestCase {
    private var app: XCUIApplication!
    private let pastePromptLabels = ["Allow Paste", "Cho phép dán"]

    override func setUpWithError() throws {
        continueAfterFailure = false
        XCUIDevice.shared.orientation = .portrait
    }

    override func tearDownWithError() throws {
        app?.terminate()
        let cleanup = XCUIApplication()
        cleanup.launchArguments = ["-uitest-clear-configuration-override"]
        cleanup.launch()
        cleanup.terminate()
    }

    /// The keyboard never reads the pasteboard on its own, so a copy made while it
    /// is open raises no paste prompt and saves nothing. The chip is a
    /// `UIPasteControl`: tapping it pastes without a prompt and records the text.
    @MainActor
    func testCopyRaisesNoPromptAndPastingThroughTheChipRecordsIt() throws {
        app = XCUIApplication()
        let text = "Clipboard " + UUID().uuidString
        app.launchArguments = ["-uitest-typing-harness", "-uitest-clipboard"]
        app.launchEnvironment["CLIPBOARD_TEST_TEXT"] = text
        addUIInterruptionMonitor(withDescription: "Paste prompt") { [pastePromptLabels] alert in
            let prompt = alert.buttons.matching(
                NSPredicate(format: "label IN %@", pastePromptLabels)
            ).firstMatch
            guard prompt.exists else { return false }
            XCTFail("The keyboard raised a paste prompt")
            return true
        }
        app.launch()
        let field = app.textViews["typingHarness.field"]
        XCTAssertTrue(field.waitForExistence(timeout: 10))
        XCTAssertTrue(FunputKeyboardDriver.switchToFunputKeyboard(app))
        app.buttons["clipboardHarness.copy"].tap()

        // Longer than the old idle-gated read took to raise its prompt.
        XCTAssertFalse(pastePrompt().waitForExistence(timeout: 4))
        let status = app.staticTexts["clipboardHarness.status"]
        XCTAssertEqual(status.label, "Waiting", "Nothing may be saved before the user pastes")

        let paste = app.buttons["funput.clipboard.paste"]
        XCTAssertTrue(paste.waitForExistence(timeout: 3))
        paste.tap()
        let inserted = NSPredicate(format: "value == %@", text)
        XCTAssertEqual(XCTWaiter.wait(
            for: [XCTNSPredicateExpectation(predicate: inserted, object: field)], timeout: 5
        ), .completed)
        let saved = NSPredicate(format: "label == 'Saved'")
        XCTAssertEqual(XCTWaiter.wait(
            for: [XCTNSPredicateExpectation(predicate: saved, object: status)], timeout: 5
        ), .completed, "Pasting through the chip must record the text in the history")
        XCTAssertFalse(pastePrompt().exists)
    }

    /// The panel's paste row is the toolbar's own chip, so it must paste the same way:
    /// no prompt, the text lands in the field, and the history records it.
    @MainActor
    func testPanelPasteRowPastesWithoutAPrompt() throws {
        app = XCUIApplication()
        let text = "Panel " + UUID().uuidString
        app.launchArguments = ["-uitest-typing-harness", "-uitest-clipboard"]
        app.launchEnvironment["CLIPBOARD_TEST_TEXT"] = text
        app.launch()
        let field = app.textViews["typingHarness.field"]
        XCTAssertTrue(field.waitForExistence(timeout: 10))
        XCTAssertTrue(FunputKeyboardDriver.switchToFunputKeyboard(app))
        app.buttons["clipboardHarness.copy"].tap()

        let history = app.keys["Lịch sử clipboard"]
        XCTAssertTrue(history.waitForExistence(timeout: 3))
        history.tap()
        let paste = app.buttons["funput.clipboard.paste"]
        XCTAssertTrue(paste.waitForExistence(timeout: 3))
        XCTAssertTrue(paste.isHittable, "The panel's paste control must be visible to paste without a prompt")
        XCTAssertGreaterThan(paste.frame.width, 20)
        paste.tap()

        let inserted = NSPredicate(format: "value == %@", text)
        XCTAssertEqual(XCTWaiter.wait(
            for: [XCTNSPredicateExpectation(predicate: inserted, object: field)], timeout: 5
        ), .completed)
        XCTAssertFalse(pastePrompt().exists)
    }

    @MainActor
    private func pastePrompt() -> XCUIElement {
        app.alerts.buttons.matching(NSPredicate(format: "label IN %@", pastePromptLabels)).firstMatch
    }
}
