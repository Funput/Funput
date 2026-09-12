import XCTest

@MainActor
final class ShortcutsUITests: XCTestCase {
    private var app: XCUIApplication!

    override func setUpWithError() throws {
        continueAfterFailure = false
        app = XCUIApplication()
        app.launchEnvironment["FUNPUT_SHORTCUTS_TEST_DIRECTORY"] = UUID().uuidString
        app.launchEnvironment["FUNPUT_SHORTCUTS_TEST_SEED"] = "1"
        app.launch()
        let entry = app.buttons["settings.shortcuts"]
        XCTAssertTrue(entry.waitForExistence(timeout: 10))
        for _ in 0..<5 where !entry.isHittable { app.swipeUp() }
        entry.tap()
        XCTAssertTrue(app.buttons["shortcuts.add"].waitForExistence(timeout: 5))
        XCTAssertTrue(app.buttons["shortcuts.entry.vn"].waitForExistence(timeout: 5))
    }

    func testAddEditAndSearchWhileDisabled() {
        let enabled = app.switches["shortcuts.enabled"].firstMatch
        enabled.switches.firstMatch.tap()
        XCTAssertEqual(enabled.value as? String, "0")
        let search = app.textFields["shortcuts.search"]
        search.tap()
        search.typeText("zzzz")
        XCTAssertTrue(app.staticTexts["Không tìm thấy gõ tắt"].waitForExistence(timeout: 3))
        app.buttons["shortcuts.add"].tap()
        let save = app.buttons["shortcuts.editor.save"]
        XCTAssertFalse(save.isEnabled)
        let trigger = app.textFields["shortcuts.editor.trigger"]
        XCTAssertTrue(trigger.waitForExistence(timeout: 3))
        trigger.tap()
        trigger.typeText("abc")
        XCTAssertFalse(save.isEnabled)
        let expansion = app.textFields["shortcuts.editor.expansion"]
        expansion.tap()
        expansion.typeText("Hello\nWorld")
        save.tap()
        let created = app.buttons["shortcuts.entry.abc"]
        XCTAssertTrue(created.waitForExistence(timeout: 3))
        XCTAssertEqual(search.value as? String, "Tìm chữ tắt hoặc nội dung")
        search.tap()
        search.typeText("HELLO")
        XCTAssertTrue(created.exists)
        XCTAssertFalse(app.buttons["shortcuts.entry.vn"].exists)
        created.tap()
        expansion.tap()
        expansion.typeText("!")
        app.buttons["Huỷ"].tap()
        XCTAssertTrue(app.alerts["Bỏ thay đổi?"].waitForExistence(timeout: 3))
        app.alerts.buttons["Tiếp tục sửa"].tap()
        save.tap()
        XCTAssertTrue(created.label.contains("!"))
        app.navigationBars.buttons["Cài đặt"].tap()
        app.buttons["settings.shortcuts"].tap()
        XCTAssertTrue(created.exists)
        XCTAssertEqual(app.switches["shortcuts.enabled"].firstMatch.value as? String, "0")
    }

    func testSwipeDismissProtectsDraftAndWarnsAboutDuplicate() {
        app.buttons["shortcuts.add"].tap()
        let trigger = app.textFields["shortcuts.editor.trigger"]
        XCTAssertTrue(trigger.waitForExistence(timeout: 3))
        dismissEditorBySwiping()
        XCTAssertTrue(trigger.waitForNonExistence(timeout: 3))
        app.buttons["shortcuts.add"].tap()
        XCTAssertTrue(trigger.waitForExistence(timeout: 3))
        trigger.tap()
        trigger.typeText("vn")
        XCTAssertTrue(app.staticTexts["Chữ tắt này đã có trong danh sách. Hãy chọn chữ tắt khác."].exists)
        app.textFields["shortcuts.editor.expansion"].tap()
        app.textFields["shortcuts.editor.expansion"].typeText("duplicate")
        XCTAssertFalse(app.buttons["shortcuts.editor.save"].isEnabled)
        dismissEditorBySwiping()
        XCTAssertTrue(app.alerts["Bỏ thay đổi?"].waitForExistence(timeout: 3))
        app.alerts.buttons["Bỏ thay đổi"].tap()
        XCTAssertTrue(app.buttons["shortcuts.add"].waitForExistence(timeout: 3))
        XCTAssertEqual(app.buttons.matching(identifier: "shortcuts.entry.vn").count, 1)
    }

    private func dismissEditorBySwiping() {
        app.navigationBars["Thêm gõ tắt"].coordinate(withNormalizedOffset: CGVector(dx: 0.5, dy: 0))
            .press(forDuration: 0.1, thenDragTo: app.coordinate(withNormalizedOffset: CGVector(dx: 0.5, dy: 0.9)))
    }

    func testOptionsPersistWithinSession() {
        app.buttons["shortcuts.options"].tap()
        let smartCase = app.switches["shortcuts.smartCase"].firstMatch
        XCTAssertTrue(smartCase.waitForExistence(timeout: 3))
        smartCase.tap()
        app.switches["shortcuts.inEnglish"].firstMatch.tap()
        app.buttons["Xong"].tap()
        app.buttons["shortcuts.options"].tap()
        XCTAssertEqual(smartCase.value as? String, "0")
        XCTAssertEqual(app.switches["shortcuts.inEnglish"].firstMatch.value as? String, "0")
    }

    func testDeleteConfirmationAndEmptyState() {
        let first = app.buttons["shortcuts.entry.vn"]
        first.swipeLeft()
        app.buttons["Xoá"].firstMatch.tap()
        XCTAssertTrue(app.alerts["Xoá gõ tắt?"].waitForExistence(timeout: 3))
        app.alerts.buttons["Huỷ"].tap()
        XCTAssertTrue(first.exists)
        for trigger in ["vn", "kg", "dc", "camon"] {
            let entry = app.buttons["shortcuts.entry.\(trigger)"]
            for _ in 0..<3 where !entry.isHittable { app.swipeUp() }
            entry.tap()
            let delete = app.buttons["shortcuts.editor.delete"]
            XCTAssertTrue(delete.waitForExistence(timeout: 3))
            delete.tap()
            app.alerts.buttons["Xoá"].tap()
            XCTAssertTrue(entry.waitForNonExistence(timeout: 5))
        }
        XCTAssertTrue(app.staticTexts["Chưa có gõ tắt"].exists)
        XCTAssertTrue(app.switches["shortcuts.enabled"].firstMatch.exists)
        let attachment = XCTAttachment(screenshot: app.screenshot())
        attachment.name = "Gõ tắt — danh sách rỗng"
        attachment.lifetime = .keepAlways
        add(attachment)
    }
}
