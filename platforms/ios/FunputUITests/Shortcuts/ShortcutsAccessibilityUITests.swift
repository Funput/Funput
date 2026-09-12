import XCTest

@MainActor
final class ShortcutsAccessibilityUITests: XCTestCase {
    func testReviewScreensHaveAccessibleControls() throws {
        continueAfterFailure = false
        let app = XCUIApplication()
        app.launch()
        let entry = app.buttons["settings.shortcuts"]
        XCTAssertTrue(entry.waitForExistence(timeout: 10))
        for _ in 0..<5 where !entry.isHittable { app.swipeUp() }
        entry.tap()
        XCTAssertTrue(app.buttons["shortcuts.add"].waitForExistence(timeout: 5))
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
        XCTAssertTrue(app.keyboards.firstMatch.waitForExistence(timeout: 3))
        XCTAssertTrue(app.buttons["shortcuts.editor.save"].isHittable)
        capture(app, name: "Form và bàn phím")
    }

    private func audit(_ app: XCUIApplication) throws {
        try app.performAccessibilityAudit(for: [.hitRegion, .sufficientElementDescription, .trait])
    }

    private func capture(_ app: XCUIApplication, name: String) {
        let attachment = XCTAttachment(screenshot: app.screenshot())
        attachment.name = name
        attachment.lifetime = .keepAlways
        add(attachment)
    }
}
