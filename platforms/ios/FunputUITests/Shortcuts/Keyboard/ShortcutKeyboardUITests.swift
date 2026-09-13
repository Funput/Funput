import XCTest

@MainActor
final class ShortcutKeyboardUITests: XCTestCase {
    override func setUpWithError() throws { continueAfterFailure = false }
    override func tearDownWithError() throws { ShortcutKeyboardUITestSupport.cleanup() }

    func testCreateEditDeleteReachRealKeyboard() {
        let app = ShortcutKeyboardUITestSupport.launch()
        ShortcutKeyboardUITestSupport.add(app, expansion: "Việt Nam")
        var field = ShortcutKeyboardUITestSupport.openField(app)
        ShortcutKeyboardUITestSupport.type("vn ", app: app)
        ShortcutsUITestSupport.waitForValue(field, "Việt Nam ")
        ShortcutsUITestSupport.capture(app, name: "Bung chữ trên bàn phím thật", test: self)
        ShortcutKeyboardUITestSupport.back(app)
        app.buttons["shortcuts.entry.vn"].tap()
        let expansion = app.textFields["shortcuts.editor.expansion"]
        expansion.tap()
        expansion.typeText("!")
        let expected = (expansion.value as! String) + " "
        app.buttons["shortcuts.editor.save"].tap()
        XCTAssertTrue(app.buttons["shortcuts.editor.save"].waitForNonExistence(timeout: 5))
        field = ShortcutKeyboardUITestSupport.openField(app)
        ShortcutKeyboardUITestSupport.type("vn ", app: app)
        ShortcutsUITestSupport.waitForValue(field, expected)
        ShortcutKeyboardUITestSupport.back(app)
        app.buttons["shortcuts.entry.vn"].tap()
        app.buttons["shortcuts.editor.delete"].tap()
        app.alerts.buttons["Xoá"].tap()
        XCTAssertTrue(app.buttons["shortcuts.entry.vn"].waitForNonExistence(timeout: 5))
        field = ShortcutKeyboardUITestSupport.openField(app)
        ShortcutKeyboardUITestSupport.type("vn ", app: app)
        ShortcutsUITestSupport.waitForValue(field, "vn ")
    }

    func testEnglishToggleAndExcludedFields() {
        let app = ShortcutKeyboardUITestSupport.launch(english: true)
        ShortcutKeyboardUITestSupport.add(app, expansion: "Hello")
        var field = ShortcutKeyboardUITestSupport.openField(app)
        ShortcutKeyboardUITestSupport.type("vn ", app: app)
        ShortcutsUITestSupport.waitForValue(field, "Hello ")
        app.buttons["Đóng bàn phím"].tap()
        for kind in ["Email", "URL", "Search"] {
            app.buttons[kind].tap()
            field = app.textViews["typingHarness.field"]
            field.tap()
            // Special layouts have no VNI sentinel; the current keyboard is still Funput.
            ShortcutKeyboardUITestSupport.type("vn ", app: app)
            ShortcutsUITestSupport.waitForValue(field, kind == "Search" ? "Hello " : "vn ")
            app.buttons["Đóng bàn phím"].tap()
        }
        ShortcutKeyboardUITestSupport.back(app)
        app.buttons["shortcuts.options"].tap()
        app.switches["shortcuts.inEnglish"].firstMatch.tap()
        ShortcutsUITestSupport.waitForValue(app.switches["shortcuts.inEnglish"].firstMatch, "0")
        app.buttons["Xong"].tap()
        field = ShortcutKeyboardUITestSupport.openField(app)
        ShortcutKeyboardUITestSupport.type("vn ", app: app)
        ShortcutsUITestSupport.waitForValue(field, "vn ")
    }

    func testLongMultilineExpansion() {
        let app = ShortcutKeyboardUITestSupport.launch()
        let expansion = String(repeating: "Dòng tiếng Việt 😀\n", count: 12)
        ShortcutKeyboardUITestSupport.add(app, expansion: expansion)
        let field = ShortcutKeyboardUITestSupport.openField(app)
        ShortcutKeyboardUITestSupport.type("vn ", app: app)
        ShortcutsUITestSupport.waitForValue(field, expansion + " ")
        ShortcutsUITestSupport.capture(app, name: "Bung nội dung dài nhiều dòng", test: self)
    }

    func testSmartCaseAndEnableSwitchReachKeyboard() {
        let app = ShortcutKeyboardUITestSupport.launch()
        ShortcutKeyboardUITestSupport.add(app, expansion: "việt nam")
        var field = ShortcutKeyboardUITestSupport.openField(app)
        ShortcutKeyboardUITestSupport.typeTitleTrigger(app)
        ShortcutsUITestSupport.waitForValue(field, "Việt Nam ")
        ShortcutKeyboardUITestSupport.back(app)
        app.buttons["shortcuts.options"].tap()
        app.switches["shortcuts.smartCase"].firstMatch.tap()
        ShortcutsUITestSupport.waitForValue(app.switches["shortcuts.smartCase"].firstMatch, "0")
        app.buttons["Xong"].tap()
        field = ShortcutKeyboardUITestSupport.openField(app)
        ShortcutKeyboardUITestSupport.typeTitleTrigger(app)
        ShortcutsUITestSupport.waitForValue(field, "Vn ")
        ShortcutKeyboardUITestSupport.back(app)
        let enabled = app.switches["shortcuts.enabled"].firstMatch
        enabled.switches.firstMatch.tap()
        ShortcutsUITestSupport.waitForValue(enabled, "0")
        field = ShortcutKeyboardUITestSupport.openField(app)
        ShortcutKeyboardUITestSupport.type("vn ", app: app)
        ShortcutsUITestSupport.waitForValue(field, "vn ")
    }

}
