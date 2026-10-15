import XCTest

/// Searching the keyboard tab leads straight to the page that holds a setting.
final class SettingsSearchUITests: XCTestCase {
    override func setUpWithError() throws {
        continueAfterFailure = false
        XCUIDevice.shared.orientation = .portrait
    }

    @MainActor
    func testSearchOpensTheSettingsPage() {
        let app = ShortcutsUITestSupport.makeApp()
        app.launch()
        let field = app.searchFields.firstMatch
        if !field.waitForExistence(timeout: 5) {
            // The field hides under the large title until the list is pulled down.
            app.swipeDown()
        }
        XCTAssertTrue(field.waitForExistence(timeout: 5))
        field.tap()
        field.typeText("rung")
        let result = app.buttons.matching(identifier: "settings.search.result").firstMatch
        XCTAssertTrue(result.waitForExistence(timeout: 5))
        result.tap()
        XCTAssertTrue(app.navigationBars["Phản hồi khi chạm"].waitForExistence(timeout: 5))
    }
}
