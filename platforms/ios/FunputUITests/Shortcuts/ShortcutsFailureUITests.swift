import XCTest

@MainActor
final class ShortcutsFailureUITests: XCTestCase {
    override func setUpWithError() throws { continueAfterFailure = false }

    func testFailedWritesKeepDraftAndRestoreToggle() {
        let app = ShortcutsUITestSupport.makeApp(seed: true, failure: "write")
        app.launch()
        ShortcutsUITestSupport.open(app)
        let entry = app.buttons["shortcuts.entry.vn"]
        XCTAssertTrue(entry.waitForExistence(timeout: 5))
        let enabled = app.switches["shortcuts.enabled"].firstMatch
        enabled.switches.firstMatch.tap()
        XCTAssertTrue(app.alerts["Không thể lưu Gõ tắt"].waitForExistence(timeout: 5))
        app.alerts.buttons["Đóng"].tap()
        ShortcutsUITestSupport.waitForValue(enabled, "1")
        entry.tap()
        let expansion = app.textFields["shortcuts.editor.expansion"]
        XCTAssertTrue(expansion.waitForExistence(timeout: 5))
        expansion.tap()
        expansion.typeText("!")
        app.buttons["shortcuts.editor.save"].tap()
        XCTAssertTrue(app.alerts["Không thể lưu Gõ tắt"].waitForExistence(timeout: 5))
        ShortcutsUITestSupport.capture(app, name: "Lỗi lưu giữ nguyên bản nháp", test: self)
        app.alerts.buttons["Đóng"].tap()
        XCTAssertTrue((expansion.value as? String)?.contains("!") == true)
        app.buttons["Huỷ"].tap()
        app.alerts.buttons["Bỏ thay đổi"].tap()
        XCTAssertFalse(entry.label.contains("!"))
        entry.tap()
        app.buttons["shortcuts.editor.delete"].tap()
        app.alerts.buttons["Xoá"].tap()
        XCTAssertTrue(app.alerts["Không thể lưu Gõ tắt"].waitForExistence(timeout: 5))
        app.alerts.buttons["Đóng"].tap()
        XCTAssertTrue(app.buttons["shortcuts.editor.delete"].exists)
        app.buttons["Huỷ"].tap()
        XCTAssertTrue(entry.exists)
    }

    func testReadFailureBlocksWritesAndShowsRetry() {
        let app = ShortcutsUITestSupport.makeApp(failure: "read")
        app.launch()
        ShortcutsUITestSupport.open(app)
        XCTAssertTrue(app.buttons["shortcuts.retry"].waitForExistence(timeout: 5))
        XCTAssertFalse(app.buttons["shortcuts.add"].isEnabled)
        XCTAssertFalse(app.staticTexts["Chưa có gõ tắt"].exists)
        app.buttons["shortcuts.retry"].tap()
        XCTAssertTrue(app.staticTexts["Không thể mở Gõ tắt"].waitForExistence(timeout: 5))
        ShortcutsUITestSupport.capture(app, name: "Lỗi đọc dữ liệu", test: self)
        app.terminate()
        app.launchEnvironment.removeValue(forKey: "FUNPUT_SHORTCUTS_TEST_FAILURE")
        app.launch()
        ShortcutsUITestSupport.open(app)
        XCTAssertTrue(app.staticTexts["Chưa có gõ tắt"].waitForExistence(timeout: 5))
        XCTAssertTrue(app.buttons["shortcuts.add"].isEnabled)
    }

    func testLargeTextSaveFailureKeepsEditorReachable() {
        let app = ShortcutsUITestSupport.makeApp(failure: "write")
        app.launchArguments += ["-UIPreferredContentSizeCategoryName", "UICTContentSizeCategoryAccessibilityXXXL"]
        app.launch()
        ShortcutsUITestSupport.open(app)
        for _ in 0..<6 where !app.staticTexts["Chưa có gõ tắt"].exists { app.swipeUp() }
        XCTAssertTrue(app.staticTexts["Chưa có gõ tắt"].waitForExistence(timeout: 5))
        app.buttons["shortcuts.add"].tap()
        let trigger = app.textFields["shortcuts.editor.trigger"]
        XCTAssertTrue(trigger.waitForExistence(timeout: 5))
        trigger.tap()
        trigger.typeText("abc")
        let expansion = app.textFields["shortcuts.editor.expansion"]
        for _ in 0..<4 where !expansion.isHittable { app.swipeUp() }
        expansion.tap()
        expansion.typeText("Nội dung dài 😀\nDòng thứ hai")
        XCTAssertTrue(app.buttons["shortcuts.editor.save"].isHittable)
        app.buttons["shortcuts.editor.save"].tap()
        XCTAssertTrue(app.alerts["Không thể lưu Gõ tắt"].waitForExistence(timeout: 5))
        ShortcutsUITestSupport.capture(app, name: "Lỗi lưu với chữ lớn", test: self)
        app.alerts.buttons["Đóng"].tap()
        XCTAssertTrue((expansion.value as? String)?.contains("Dòng thứ hai") == true)
        expansion.tap()
        expansion.typeText("!")
        ShortcutsUITestSupport.assertKeyboardVisible(app)
        XCTAssertTrue(app.buttons["shortcuts.editor.save"].isHittable)
        XCTAssertTrue((expansion.value as? String)?.contains("!") == true)
        ShortcutsUITestSupport.capture(app, name: "Bản nháp chữ lớn và bàn phím", test: self)
    }
}
