import XCTest

@MainActor
final class ShortcutsLayoutUITests: XCTestCase {
    func testEmptyLibraryHasCompactVisibleAddAction() {
        continueAfterFailure = false
        let app = ShortcutsUITestSupport.makeApp()
        app.launchArguments += ["-AppleInterfaceStyle", "Dark"]
        app.launch()
        ShortcutsUITestSupport.open(app)
        XCTAssertTrue(app.staticTexts["Chưa có gõ tắt"].waitForExistence(timeout: 5))
        let add = app.buttons["shortcuts.empty.add"]
        ShortcutsUITestSupport.capture(app, name: "Gõ tắt trống", test: self)
        XCTAssertTrue(add.exists)
        XCTAssertGreaterThan(add.frame.width, 100)
        XCTAssertGreaterThanOrEqual(add.frame.height, 28)
        XCTAssertLessThanOrEqual(add.frame.height, 44)
        XCTAssertTrue(add.isHittable)
        XCTAssertLessThan(add.frame.maxY, app.tabBars.firstMatch.frame.minY)
        add.tap()
        XCTAssertTrue(app.textFields["shortcuts.editor.trigger"].waitForExistence(timeout: 3))
    }

    func testSearchStaysCompactAndCanClearNoResults() {
        continueAfterFailure = false
        let app = ShortcutsUITestSupport.makeApp(seed: true)
        app.launch()
        ShortcutsUITestSupport.open(app)
        let search = app.textFields["shortcuts.search"]
        let cell = app.cells.containing(.textField, identifier: "shortcuts.search").firstMatch
        XCTAssertTrue(search.waitForExistence(timeout: 5))
        XCTAssertGreaterThanOrEqual(cell.frame.height, 44)
        XCTAssertLessThanOrEqual(cell.frame.height, 48)
        search.tap()
        search.typeText("zzzz")
        XCTAssertTrue(app.staticTexts["Không tìm thấy gõ tắt"].waitForExistence(timeout: 3))
        let clear = app.buttons["shortcuts.search.clear"]
        XCTAssertGreaterThanOrEqual(clear.frame.height, 44)
        XCTAssertLessThanOrEqual(cell.frame.height, 48)
        ShortcutsUITestSupport.capture(app, name: "Không có kết quả", test: self)
        clear.tap()
        XCTAssertTrue(app.buttons["shortcuts.entry.vn"].waitForExistence(timeout: 3))
    }

    func testEmptyActionWorksWithAccessibilityTextSize() {
        continueAfterFailure = false
        let app = ShortcutsUITestSupport.makeApp()
        app.launchArguments += ["-UIPreferredContentSizeCategoryName", "UICTContentSizeCategoryAccessibilityXXXL"]
        app.launch()
        ShortcutsUITestSupport.open(app)
        let add = app.buttons["shortcuts.empty.add"]
        for _ in 0..<10 where !add.isHittable { app.swipeUp() }
        XCTAssertTrue(add.isHittable)
        XCTAssertGreaterThanOrEqual(add.frame.minX, 0)
        XCTAssertLessThanOrEqual(add.frame.maxX, app.frame.width)
        ShortcutsUITestSupport.capture(app, name: "Gõ tắt chữ lớn", test: self)
        add.tap()
        XCTAssertTrue(app.textFields["shortcuts.editor.trigger"].waitForExistence(timeout: 3))
    }
}
