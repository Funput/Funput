import XCTest

@MainActor
final class ShortcutsPersistenceUITests: XCTestCase {
    override func setUpWithError() throws { continueAfterFailure = false }

    func testCreateEditDeleteAndOptionsSurviveRelaunch() {
        let app = ShortcutsUITestSupport.makeApp()
        app.launch()
        ShortcutsUITestSupport.open(app)
        XCTAssertTrue(app.staticTexts["Chưa có gõ tắt"].waitForExistence(timeout: 5))
        app.buttons["shortcuts.add"].tap()
        let trigger = app.textFields["shortcuts.editor.trigger"]
        XCTAssertTrue(trigger.waitForExistence(timeout: 3))
        trigger.tap()
        trigger.typeText("abc")
        let expansion = app.textFields["shortcuts.editor.expansion"]
        expansion.tap()
        expansion.typeText("Hello\nWorld")
        app.buttons["shortcuts.editor.save"].tap()
        XCTAssertTrue(app.buttons["shortcuts.entry.abc"].waitForExistence(timeout: 5))
        let enabled = app.switches["shortcuts.enabled"].firstMatch
        enabled.switches.firstMatch.tap()
        ShortcutsUITestSupport.waitForValue(enabled, "0")
        app.buttons["shortcuts.options"].tap()
        app.switches["shortcuts.smartCase"].firstMatch.tap()
        ShortcutsUITestSupport.waitForValue(app.switches["shortcuts.smartCase"].firstMatch, "0")
        app.switches["shortcuts.inEnglish"].firstMatch.tap()
        ShortcutsUITestSupport.waitForValue(app.switches["shortcuts.inEnglish"].firstMatch, "0")
        app.buttons["Xong"].tap()
        app.terminate()
        app.launch()
        ShortcutsUITestSupport.open(app)
        let saved = app.buttons["shortcuts.entry.abc"]
        XCTAssertTrue(saved.waitForExistence(timeout: 5))
        XCTAssertTrue(saved.label.contains("World"))
        ShortcutsUITestSupport.waitForValue(enabled, "0")
        app.buttons["shortcuts.options"].tap()
        ShortcutsUITestSupport.waitForValue(app.switches["shortcuts.smartCase"].firstMatch, "0")
        ShortcutsUITestSupport.waitForValue(app.switches["shortcuts.inEnglish"].firstMatch, "0")
        app.buttons["Xong"].tap()
        saved.tap()
        expansion.tap()
        expansion.typeText("!")
        app.buttons["shortcuts.editor.save"].tap()
        XCTAssertTrue(app.buttons["shortcuts.editor.save"].waitForNonExistence(timeout: 5))
        XCTAssertTrue(saved.label.contains("!"))
        app.terminate()
        app.launch()
        ShortcutsUITestSupport.open(app)
        XCTAssertTrue(saved.waitForExistence(timeout: 5))
        XCTAssertTrue(saved.label.contains("!"))
        saved.tap()
        app.buttons["shortcuts.editor.delete"].tap()
        app.alerts.buttons["Xoá"].tap()
        XCTAssertTrue(app.staticTexts["Chưa có gõ tắt"].waitForExistence(timeout: 5))
        app.terminate()
        app.launch()
        ShortcutsUITestSupport.open(app)
        XCTAssertTrue(app.staticTexts["Chưa có gõ tắt"].waitForExistence(timeout: 5))
        XCTAssertFalse(saved.exists)
    }

    func testGeneralResetPreservesShortcutsAndOptions() {
        let app = ShortcutsUITestSupport.makeApp(seed: true)
        app.launch()
        ShortcutsUITestSupport.open(app)
        let entry = app.buttons["shortcuts.entry.vn"]
        XCTAssertTrue(entry.waitForExistence(timeout: 5))
        let enabled = app.switches["shortcuts.enabled"].firstMatch
        enabled.switches.firstMatch.tap()
        ShortcutsUITestSupport.waitForValue(enabled, "0")
        app.buttons["shortcuts.options"].tap()
        for option in ["shortcuts.smartCase", "shortcuts.inEnglish"] {
            app.switches[option].firstMatch.tap()
            ShortcutsUITestSupport.waitForValue(app.switches[option].firstMatch, "0")
        }
        app.buttons["Xong"].tap()
        app.navigationBars.buttons.firstMatch.tap()
        let reset = app.buttons["settings.data.reset"]
        for _ in 0..<12 where !reset.isHittable { app.swipeUp() }
        reset.tap()
        app.buttons["Khôi phục"].tap()
        app.terminate()
        app.launch()
        ShortcutsUITestSupport.open(app)
        XCTAssertTrue(entry.waitForExistence(timeout: 5))
        ShortcutsUITestSupport.waitForValue(enabled, "0")
        app.buttons["shortcuts.options"].tap()
        for option in ["shortcuts.smartCase", "shortcuts.inEnglish"] {
            ShortcutsUITestSupport.waitForValue(app.switches[option].firstMatch, "0")
        }
    }
}
