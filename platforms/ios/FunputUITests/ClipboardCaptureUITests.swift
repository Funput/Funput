import XCTest

/// Requires Funput enabled with Full Access on the selected simulator/device.
final class ClipboardCaptureUITests: XCTestCase {
    private var app: XCUIApplication!

    override func setUpWithError() throws { continueAfterFailure = false }

    override func tearDownWithError() throws {
        app?.terminate()
        let cleanup = XCUIApplication()
        cleanup.launchArguments = ["-uitest-clear-configuration-override"]
        cleanup.launch()
        cleanup.terminate()
    }

    @MainActor
    func testCopyWhileKeyboardOpenSavesBeforePaste() throws {
        app = XCUIApplication()
        let text = "Clipboard " + UUID().uuidString
        app.launchArguments = ["-uitest-typing-harness", "-uitest-clipboard"]
        app.launchEnvironment["CLIPBOARD_TEST_TEXT"] = text
        addUIInterruptionMonitor(withDescription: "Clipboard permission") { alert in
            let allow = alert.buttons.matching(
                NSPredicate(format: "label IN %@", ["Allow Paste", "Cho phép dán"])
            ).firstMatch
            guard allow.exists else { return false }
            allow.tap()
            return true
        }
        app.launch()
        let field = app.textViews["typingHarness.field"]
        XCTAssertTrue(field.waitForExistence(timeout: 10))
        XCTAssertTrue(FunputKeyboardDriver.switchToFunputKeyboard(app))
        allowClipboardPrompt()
        app.buttons["clipboardHarness.copy"].tap()
        allowClipboardPrompt()
        let saved = app.staticTexts["clipboardHarness.status"]
        let ready = NSPredicate(format: "label == 'Saved'")
        XCTAssertEqual(XCTWaiter.wait(
            for: [XCTNSPredicateExpectation(predicate: ready, object: saved)], timeout: 10
        ), .completed, "Text must be saved before tapping Paste; check clipboard permissions")
        XCTAssertEqual(field.value as? String, "")
        let paste = app.buttons["funput.clipboard.paste"]
        XCTAssertTrue(paste.waitForExistence(timeout: 3))
        XCTAssertTrue(paste.isEnabled)
        paste.tap()
        let inserted = NSPredicate(format: "value == %@", text)
        XCTAssertEqual(XCTWaiter.wait(
            for: [XCTNSPredicateExpectation(predicate: inserted, object: field)], timeout: 5
        ), .completed)
    }

    @MainActor
    private func allowClipboardPrompt() {
        let allow = app.alerts.buttons.matching(
            NSPredicate(format: "label IN %@", ["Allow Paste", "Cho phép dán"])
        ).firstMatch
        if allow.waitForExistence(timeout: 2) { allow.tap() }
    }
}
