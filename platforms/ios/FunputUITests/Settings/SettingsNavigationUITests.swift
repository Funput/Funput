import XCTest

/// The keyboard tab is a list of page links; each must open its page and come back.
final class SettingsNavigationUITests: XCTestCase {
    override func setUpWithError() throws {
        continueAfterFailure = false
        XCUIDevice.shared.orientation = .portrait
    }

    @MainActor
    func testEveryPageOpensAndReturns() {
        // The shortcuts test environment also gives settings throwaway stores.
        let app = ShortcutsUITestSupport.makeApp()
        app.launch()
        let pages = [
            ("typing", "Gõ tiếng Việt"),
            ("layout", "Bố cục và kích thước"),
            ("smartInput", "Thông minh"),
            ("feedback", "Phản hồi khi chạm"),
            ("clipboard", "Clipboard"),
        ]
        for (id, title) in pages {
            let row = app.buttons["settings.destination.\(id)"]
            XCTAssertTrue(row.waitForExistence(timeout: 10), "Missing row: \(id)")
            for _ in 0..<4 where !row.isHittable { app.swipeUp() }
            row.tap()
            XCTAssertTrue(app.navigationBars[title].waitForExistence(timeout: 5), "Page did not open: \(title)")
            app.navigationBars[title].buttons.element(boundBy: 0).tap()
        }
    }

    @MainActor
    func testPickerOpensFromItsPage() {
        let app = ShortcutsUITestSupport.makeApp()
        app.launch()
        let row = app.buttons["settings.destination.typing"]
        XCTAssertTrue(row.waitForExistence(timeout: 10))
        row.tap()
        let method = app.buttons.matching(NSPredicate(format: "label BEGINSWITH %@", "Kiểu gõ")).firstMatch
        XCTAssertTrue(method.waitForExistence(timeout: 5))
        method.tap()
        XCTAssertTrue(app.navigationBars["Kiểu gõ"].waitForExistence(timeout: 5))
    }

    @MainActor
    func testDataPageLivesUnderMe() {
        let app = ShortcutsUITestSupport.makeApp()
        app.launch()
        let me = app.tabBars.buttons["Tôi"]
        XCTAssertTrue(me.waitForExistence(timeout: 10))
        me.tap()
        let row = app.buttons["settings.destination.data"]
        XCTAssertTrue(row.waitForExistence(timeout: 5))
        row.tap()
        XCTAssertTrue(app.buttons["settings.data.reset"].waitForExistence(timeout: 5))
    }
}
