import XCTest

@MainActor
enum ShortcutKeyboardUITestSupport {
    static func launch(english: Bool = false) -> XCUIApplication {
        let app = ShortcutsUITestSupport.makeApp()
        app.launchArguments = ["-uitest-shortcuts-keyboard"]
        app.launchEnvironment["FUNPUT_SHORTCUTS_TEST_ENGLISH"] = english ? "1" : "0"
        app.launch()
        XCTAssertTrue(app.buttons["shortcuts.add"].waitForExistence(timeout: 10))
        return app
    }

    static func add(_ app: XCUIApplication, expansion: String) {
        app.buttons["shortcuts.add"].tap()
        let trigger = app.textFields["shortcuts.editor.trigger"]
        XCTAssertTrue(trigger.waitForExistence(timeout: 5))
        trigger.tap()
        trigger.typeText("vn")
        let text = app.textFields["shortcuts.editor.expansion"]
        text.tap()
        text.typeText(expansion)
        app.buttons["shortcuts.editor.save"].tap()
        XCTAssertTrue(app.buttons["shortcuts.entry.vn"].waitForExistence(timeout: 5))
    }

    static func openField(_ app: XCUIApplication) -> XCUIElement {
        app.buttons["shortcuts.test.openField"].tap()
        let field = app.textViews["typingHarness.field"]
        XCTAssertTrue(field.waitForExistence(timeout: 5))
        field.tap()
        XCTAssertTrue(FunputKeyboardDriver.switchToFunputKeyboard(app))
        return field
    }

    static func type(_ text: String, app: XCUIApplication) {
        let taps = FunputKeyboardDriver.resolveKeyCoordinates(app, for: text)
        for ch in text { taps[ch]!.tap() }
    }

    static func typeTitleTrigger(_ app: XCUIApplication) {
        let first = FunputKeyboardDriver.resolveKeyCoordinates(app, for: "v")["v"]!
        guard let shift = FunputKeyboardDriver.keyFrame(app, labeled: "Shift") else {
            XCTFail("Missing Funput Shift key")
            return
        }
        app.coordinate(withNormalizedOffset: .zero)
            .withOffset(CGVector(dx: shift.midX, dy: shift.midY)).tap()
        first.tap()
        type("n ", app: app)
    }

    static func back(_ app: XCUIApplication) {
        app.navigationBars.buttons.firstMatch.tap()
        XCTAssertTrue(app.buttons["shortcuts.add"].waitForExistence(timeout: 5))
    }

    static func cleanup() {
        let app = XCUIApplication()
        app.launchArguments = ["-uitest-clear-configuration-override"]
        app.launch()
        app.terminate()
    }
}
