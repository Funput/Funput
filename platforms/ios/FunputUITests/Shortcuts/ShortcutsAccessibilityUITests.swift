import XCTest

@MainActor
final class ShortcutsAccessibilityUITests: XCTestCase {
    func testReviewScreensHaveAccessibleControls() throws {
        continueAfterFailure = false
        let app = XCUIApplication()
        app.launchEnvironment["FUNPUT_SHORTCUTS_TEST_DIRECTORY"] = UUID().uuidString
        app.launchEnvironment["FUNPUT_SHORTCUTS_TEST_SEED"] = "1"
        app.launch()
        ShortcutsUITestSupport.open(app)
        XCTAssertTrue(app.buttons["shortcuts.entry.vn"].waitForExistence(timeout: 5))
        try audit(app)
        capture(app, name: "Danh sách")
        app.buttons["shortcuts.options"].tap()
        XCTAssertTrue(app.buttons["Xong"].waitForExistence(timeout: 3))
        try audit(app)
        capture(app, name: "Tuỳ chọn")
        app.buttons["Xong"].tap()
        app.buttons["shortcuts.entry.vn"].tap()
        XCTAssertTrue(app.buttons["shortcuts.editor.save"].waitForExistence(timeout: 3))
        try audit(app)
        capture(app, name: "Form sửa")
        app.textFields["shortcuts.editor.trigger"].tap()
        ShortcutsUITestSupport.assertKeyboardVisible(app)
        XCTAssertTrue(app.buttons["shortcuts.editor.save"].isHittable)
        capture(app, name: "Form và bàn phím")
    }

    private func audit(_ app: XCUIApplication) throws {
        guard #available(iOS 17, *) else {
            throw XCTSkip("The system accessibility audit requires iOS 17 or newer.")
        }
        try app.performAccessibilityAudit(for: [.hitRegion, .sufficientElementDescription, .trait])
    }

    private func capture(_ app: XCUIApplication, name: String) {
        let attachment = XCTAttachment(screenshot: app.screenshot())
        attachment.name = name
        attachment.lifetime = .keepAlways
        add(attachment)
    }
}
