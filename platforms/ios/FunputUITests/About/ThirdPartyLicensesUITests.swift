import XCTest

final class ThirdPartyLicensesUITests: XCTestCase {
    override func setUpWithError() throws {
        continueAfterFailure = false
        XCUIDevice.shared.orientation = .portrait
    }

    @MainActor
    func testBundledNoticesAreReadableFromAbout() {
        let app = XCUIApplication()
        app.launch()
        let about = app.tabBars.buttons["Giới thiệu"]
        XCTAssertTrue(about.waitForExistence(timeout: 10))
        about.tap()
        let link = app.buttons["about.licenses"]
        for _ in 0..<6 where !link.isHittable { app.swipeUp() }
        XCTAssertTrue(link.waitForExistence(timeout: 5))
        link.tap()
        let content = app.scrollViews["licenses.content"]
        XCTAssertTrue(content.waitForExistence(timeout: 5))
        for heading in [
            "SCOWL / English Speller Database (ESDB)",
            "Google Books Ngram Viewer",
            "List of Dirty, Naughty, Obscene, and Otherwise Bad Words",
            "Funput",
        ] {
            let label = content.staticTexts[heading]
            for _ in 0..<20 where !label.isHittable {
                content.coordinate(withNormalizedOffset: CGVector(dx: 0.5, dy: 0.7))
                    .press(forDuration: 0.1, thenDragTo: content.coordinate(
                        withNormalizedOffset: CGVector(dx: 0.5, dy: 0.4)
                    ))
            }
            XCTAssertTrue(label.isHittable, "Missing notice: \(heading)")
        }
    }
}
