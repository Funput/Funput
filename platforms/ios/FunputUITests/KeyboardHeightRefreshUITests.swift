import XCTest

/// Guards the host handoff that used to require cycling through Apple's keyboard
/// before Funput adopted its toolbar-less preferred height.
final class KeyboardHeightRefreshUITests: XCTestCase {
    private var app: XCUIApplication!

    override func setUpWithError() throws {
        continueAfterFailure = false
        app = XCUIApplication()
        app.launchArguments = [
            "-uitest-typing-harness",
            "-uitest-warm-toolbar-refresh",
        ]
        app.launch()
    }

    override func tearDownWithError() throws {
        app?.terminate()
        let cleanup = XCUIApplication()
        cleanup.launchArguments = ["-uitest-clear-configuration-override"]
        cleanup.launch()
        cleanup.terminate()
        app = nil
    }

    @MainActor
    func testWarmConfigurationRefreshMatchesKeyboardCycle() throws {
        let field = app.textViews["typingHarness.field"]
        XCTAssertTrue(field.waitForExistence(timeout: 10))
        field.tap()
        XCTAssertTrue(FunputKeyboardDriver.switchToFunputKeyboard(app))

        let withToolbar = try surfaceFrame()
        app.buttons["typingHarness.hideToolbar"].tap()
        XCTAssertFalse(app.keys["Dấu sắc"].waitForExistence(timeout: 1))

        field.tap()
        XCTAssertTrue(FunputKeyboardDriver.switchToFunputKeyboard(app))
        let refreshed = try surfaceFrame()
        cycleAwayFromFunput()
        XCTAssertTrue(FunputKeyboardDriver.switchToFunputKeyboard(app))
        let returned = try surfaceFrame()

        XCTAssertGreaterThan(refreshed.minY, withToolbar.minY)
        XCTAssertEqual(refreshed.minY, returned.minY, accuracy: 1)
        XCTAssertEqual(refreshed.height, returned.height, accuracy: 1)
    }

    @MainActor
    func testToolbarlessSystemLayoutOpensEmojiAndKaomoji() {
        let field = app.textViews["typingHarness.field"]
        XCTAssertTrue(field.waitForExistence(timeout: 10))
        field.tap()
        XCTAssertTrue(FunputKeyboardDriver.switchToFunputKeyboard(app))
        app.buttons["typingHarness.hideToolbar"].tap()

        field.tap()
        XCTAssertTrue(FunputKeyboardDriver.switchToFunputKeyboard(app))
        let emoji = app.keys["Mở bảng biểu tượng cảm xúc"]
        XCTAssertTrue(emoji.waitForExistence(timeout: 3))
        emoji.tap()

        let kaomoji = app.keys["Biểu tượng kaomoji"]
        XCTAssertTrue(kaomoji.waitForExistence(timeout: 3))
        kaomoji.tap()
        XCTAssertTrue(app.keys["Biểu tượng cảm xúc"].waitForExistence(timeout: 3))
    }

    @MainActor
    private func surfaceFrame() throws -> CGRect {
        let surface = app.otherElements["funput.keyboard.surface"]
        XCTAssertTrue(surface.waitForExistence(timeout: 3))
        return surface.frame
    }

    @MainActor
    private func cycleAwayFromFunput() {
        let globe = app.buttons.matching(
            NSPredicate(
                format: "label ==[c] 'next keyboard' OR label == 'Bàn phím tiếp theo'"
            )
        ).firstMatch
        XCTAssertTrue(globe.waitForExistence(timeout: 3))
        globe.tap()
        XCTAssertFalse(app.keys["Dấu sắc"].waitForExistence(timeout: 1))
    }
}
